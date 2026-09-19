package com.lucas.financeflow;

import com.lucas.financeflow.data.local.AppDatabase;
import com.lucas.financeflow.data.model.*;
import com.lucas.financeflow.data.repository.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class PlanningTest {
    private AppDatabase db; private PlanRepository repo;
    @Before public void setup() throws Exception {
        db=androidx.room.Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(),AppDatabase.class).allowMainThreadQueries().setQueryExecutor(Runnable::run).setTransactionExecutor(Runnable::run).build();
        java.lang.reflect.Field field=AppDatabase.class.getDeclaredField("instance"); field.setAccessible(true); field.set(null,db); repo=new PlanRepository(RuntimeEnvironment.getApplication());
        db.lancamentoDao().cadastrar(new Cadastro("CONTA","Banco")); db.lancamentoDao().cadastrar(new Cadastro("CONTA","Carteira"));
    }
    @After public void close() throws Exception {db.close(); java.lang.reflect.Field field=AppDatabase.class.getDeclaredField("instance"); field.setAccessible(true); field.set(null,null);}
    private PlanItem item(String kind,long cents,String target) {PlanItem p=new PlanItem(); p.kind=kind; p.cents=cents; p.date="2026-01-31"; p.account="Banco"; p.target=target; p.name="Reserva"; return p;}
    private String await(java.util.function.Consumer<PlanRepository.Result> operation) throws Exception {
        AtomicReference<String> result=new AtomicReference<>("waiting"); operation.accept(result::set);
        long end=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        while("waiting".equals(result.get()) && System.nanoTime()<end) {ShadowLooper.idleMainLooper(); Thread.sleep(10);}
        assertNotEquals("waiting",result.get()); return result.get();
    }
    private void save(PlanItem p) throws Exception {assertNull(await(done -> repo.save(p,done)));}
    @Test public void transfersAndInvestmentsPreserveWealthAndDoNotBecomeExpenses() throws Exception {
        PlanItem transfer=item("TRANSFER",20000,"Carteira"); save(transfer); save(transfer);
        PlanItem asset=item("ASSET",0,""); save(asset);
        save(item("APPLY",50000,asset.id)); save(item("VALUE",55000,asset.id)); save(item("REDEEM",10000,asset.id));
        List<PlanItem> plans=db.planDao().snapshot(); Map<String,Long> balances=new TreeMap<>(); balances.put("Banco",100000L); Planning.adjustAccounts(balances,plans);
        assertEquals(Long.valueOf(40000),balances.get("Banco")); assertEquals(Long.valueOf(20000),balances.get("Carteira"));
        assertEquals(45000,Planning.assetValue(plans,asset.id)); assertEquals(5000,Planning.assetValue(plans,asset.id)-Planning.netContributions(plans,asset.id));
        assertEquals(105000,balances.get("Banco")+balances.get("Carteira")+Planning.assetValue(plans,asset.id)); assertTrue(db.lancamentoDao().snapshot().isEmpty());
    }
    @Test public void invalidWithdrawalsAndDeletingTheirFundingAreAtomic() throws Exception {
        PlanItem asset=item("ASSET",0,""); save(asset); PlanItem funding=item("APPLY",10000,asset.id); save(funding);
        assertNotNull(await(done -> repo.save(item("REDEEM",10001,asset.id),done))); assertEquals(2,db.planDao().snapshot().size());
        save(item("REDEEM",5000,asset.id)); assertNotNull(await(done -> repo.delete(funding.id,done))); assertEquals(3,db.planDao().snapshot().size());
        assertNotNull(await(done -> repo.delete(asset.id,done)));
    }
    @Test public void recurringConfirmationIsIdempotentAndKeepsDayThirtyOne() throws Exception {
        PlanItem rule=item("RULE",12345,""); rule.day=31; rule.category="Moradia"; save(rule);
        assertNull(await(done -> repo.confirm(rule.id,"2026-01-31",done))); assertNull(await(done -> repo.confirm(rule.id,"2026-01-31",done)));
        assertEquals(1,db.lancamentoDao().snapshot().size()); assertEquals("2026-02-28",db.planDao().get(rule.id).date);
        assertNull(await(done -> repo.confirm(rule.id,"2026-02-28",done))); assertEquals("2026-03-31",db.planDao().get(rule.id).date); assertEquals(2,db.lancamentoDao().snapshot().size());
        assertEquals("2028-02-29",Planning.nextDate("2028-01-31",31));
    }
    @Test public void budgetUsesOnlyCategoryExpensesInTheSelectedMonth() {
        List<Lancamento> entries=Arrays.asList(new Lancamento("A",50,"SAIDA","Alimentação","2026-09-01","","","","Banco"),new Lancamento("B",80,"ENTRADA","Alimentação","2026-09-01","","","","Banco"),new Lancamento("C",10,"SAIDA","Alimentação","2026-08-31","","","","Banco"));
        assertEquals(5000,Planning.spent(entries,"2026-09","Alimentação"));
    }
    @Test public void backupIncludesPlanningAndRejectsBrokenLedgerBeforeRestore() throws Exception {
        PlanItem asset=item("ASSET",0,""); save(asset); save(item("APPLY",10000,asset.id));
        String json=BackupCodec.encode(db.lancamentoDao().snapshot(),db.lancamentoDao().snapshotCadastros(),db.planDao().snapshot());
        BackupCodec.Documento document=BackupCodec.decodeCompleto(json); assertEquals(2,document.plans.size()); assertEquals(10000,Planning.assetValue(document.plans,asset.id));
        org.json.JSONObject root=new org.json.JSONObject(json); root.getJSONArray("planning").getJSONObject(1).put("kind","REDEEM");
        try {BackupCodec.decodeCompleto(root.toString()); fail();} catch(org.json.JSONException expected) { }
        assertEquals(10000,Planning.assetValue(db.planDao().snapshot(),asset.id));
        assertTrue(BackupCodec.decodeCompleto("{\"app\":\"FinanceFlow\",\"version\":2,\"lancamentos\":[],\"cadastros\":[]}").plans.isEmpty());
    }
    @Test public void undoRestoresEditedEntryInsteadOfDeletingIt() throws Exception {
        Lancamento original=new Lancamento("Original",10,"SAIDA","Outros","2026-01-01","CELULAR","LOCAL","","Banco"); original.id=(int)db.lancamentoDao().inserir(original);
        SaveViewModel model=new SaveViewModel(RuntimeEnvironment.getApplication()); model.previous(original);
        Lancamento edited=new Lancamento("Editado",20,"SAIDA","Outros","2026-01-01","CELULAR","LOCAL","","Banco"); edited.id=original.id;
        model.salvar(edited); long end=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        while(!Integer.valueOf(2).equals(model.estado.getValue()) && System.nanoTime()<end) {ShadowLooper.idleMainLooper(); Thread.sleep(10);}
        assertEquals(Integer.valueOf(2),model.estado.getValue()); assertEquals("Editado",db.lancamentoDao().snapshot().get(0).descricao);
        assertNull(await(done -> model.undo(ok -> done.done(ok?null:"failed")))); assertEquals("Original",db.lancamentoDao().snapshot().get(0).descricao); assertEquals(10,db.lancamentoDao().snapshot().get(0).valor,0);
    }
}
