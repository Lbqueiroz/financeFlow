package com.lucas.financeflow;

import android.view.*;
import android.widget.*;
import com.lucas.financeflow.data.local.AppDatabase;
import com.lucas.financeflow.data.model.*;
import com.lucas.financeflow.data.repository.InstallmentRepository;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowLooper;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,qualifiers="w411dp-h891dp-mdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class InstallmentsTest {
    private AppDatabase db;
    private InstallmentRepository repository;
    @Before public void setup() throws Exception {
        db=androidx.room.Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(),AppDatabase.class).allowMainThreadQueries().setQueryExecutor(Runnable::run).setTransactionExecutor(Runnable::run).build();
        java.lang.reflect.Field field=AppDatabase.class.getDeclaredField("instance"); field.setAccessible(true); field.set(null,db);
        db.lancamentoDao().cadastrar(new Cadastro("CONTA","Cartão Inter")); repository=new InstallmentRepository(RuntimeEnvironment.getApplication());
    }
    @After public void close() throws Exception {db.close(); java.lang.reflect.Field field=AppDatabase.class.getDeclaredField("instance"); field.setAccessible(true); field.set(null,null);}
    private InstallmentPlan plan() {InstallmentPlan p=new InstallmentPlan(); p.name="Compra"; p.account="Cartão Inter"; p.category="Outros"; p.firstDate="2026-01-31"; p.count=3; p.totalCents=10000; return p;}
    private void waitFor(java.util.function.BooleanSupplier condition) throws Exception {long end=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(10); while(!condition.getAsBoolean() && System.nanoTime()<end) {ShadowLooper.idleMainLooper(); Thread.sleep(10);} assertTrue(condition.getAsBoolean());}
    private String await(java.util.function.Consumer<InstallmentRepository.Result> operation) throws Exception {AtomicReference<String> result=new AtomicReference<>("waiting"); operation.accept(result::set); waitFor(() -> !"waiting".equals(result.get())); return result.get();}
    @Test public void divisionPreservesEveryCentAndOriginalDayAcrossShortMonths() {
        List<Lancamento> items=Installments.schedule(plan()); assertEquals(3,items.size());
        assertEquals(3333,FinanceUtils.centavos(items.get(0).valor)); assertEquals(3334,FinanceUtils.centavos(items.get(2).valor));
        assertEquals(10000,items.stream().mapToLong(p -> FinanceUtils.centavos(p.valor)).sum());
        assertEquals("2026-01-31",items.get(0).data); assertEquals("2026-02-28",items.get(1).data); assertEquals("2026-03-31",items.get(2).data); assertEquals("Compra · 3/3",items.get(2).descricao);
        InstallmentPlan leap=plan(); leap.firstDate="2028-01-31"; assertEquals("2028-02-29",Installments.schedule(leap).get(1).data);
    }
    @Test public void invalidAmountsAndCountsDoNotCreateZeroValueEntries() {
        for(int count:new int[]{0,1,121}) {InstallmentPlan p=plan(); p.count=count; try {Installments.schedule(p); fail();} catch(IllegalArgumentException expected) { }}
        InstallmentPlan p=plan(); p.totalCents=2; try {Installments.schedule(p); fail();} catch(IllegalArgumentException expected) { }
    }
    @Test public void creationIsIdempotentAndDeletionOnlyAffectsLinkedEntries() throws Exception {
        InstallmentPlan p=plan(); assertNull(await(done -> repository.save(p,done))); assertNull(await(done -> repository.save(p,done)));
        assertEquals(1,db.installmentDao().snapshot().size()); assertEquals(3,db.lancamentoDao().snapshot().size());
        Lancamento unrelated=new Lancamento("Compra · 1/3",33.33,"SAIDA","Outros","2026-01-31","CELULAR","LOCAL","","Cartão Inter"); db.lancamentoDao().inserir(unrelated);
        assertNull(await(done -> repository.delete(p.id,done))); assertTrue(db.installmentDao().snapshot().isEmpty()); assertEquals(1,db.lancamentoDao().snapshot().size()); assertNull(db.lancamentoDao().snapshot().get(0).installmentPlanId);
    }
    @Test public void unavailableAccountLeavesNeitherPlanNorParcels() throws Exception {
        InstallmentPlan p=plan(); p.account="Inexistente"; assertNotNull(await(done -> repository.save(p,done)));
        assertTrue(db.installmentDao().snapshot().isEmpty()); assertTrue(db.lancamentoDao().snapshot().isEmpty());
    }
    @Test public void backupRestoresLinksAndRejectsUnknownOrDuplicateParcels() throws Exception {
        InstallmentPlan p=plan(); assertNull(await(done -> repository.save(p,done)));
        String json=BackupCodec.encode(db.lancamentoDao().snapshot(),db.lancamentoDao().snapshotCadastros(),db.planDao().snapshot(),db.installmentDao().snapshot());
        BackupCodec.Documento backup=BackupCodec.decodeCompleto(json); assertEquals(1,backup.installments.size()); assertEquals(3,backup.itens.size()); assertEquals(p.id,backup.itens.get(0).installmentPlanId);
        org.json.JSONObject root=new org.json.JSONObject(json); root.getJSONArray("lancamentos").getJSONObject(0).put("installmentPlanId","missing");
        try {BackupCodec.decodeCompleto(root.toString()); fail();} catch(org.json.JSONException expected) { }
        root=new org.json.JSONObject(json); root.getJSONArray("lancamentos").getJSONObject(0).put("installmentNumber",root.getJSONArray("lancamentos").getJSONObject(1).getInt("installmentNumber"));
        try {BackupCodec.decodeCompleto(root.toString()); fail();} catch(org.json.JSONException expected) { }
        assertTrue(BackupCodec.decodeCompleto("{\"app\":\"FinanceFlow\",\"version\":3,\"lancamentos\":[],\"cadastros\":[],\"planning\":[]}").installments.isEmpty());
    }
    @Test public void separateFormPreservesDraftAndCreatesMonthlyExpenses() throws Exception {
        android.os.Bundle state=new android.os.Bundle();
        try(var controller=Robolectric.buildActivity(NewInstallmentActivity.class).setup()) {
            NewInstallmentActivity activity=controller.get(); ShadowLooper.idleMainLooper();
            assertEquals("Outros",((Spinner)activity.findViewById(R.id.form_categoria)).getSelectedItem());
            ((EditText)activity.findViewById(R.id.form_descricao)).setText("Fone de ouvido"); ((EditText)activity.findViewById(R.id.form_valor)).setText("10000"); ((EditText)activity.findViewById(R.id.installment_count)).setText("3");
            ((Spinner)activity.findViewById(R.id.form_conta)).setSelection(1); controller.saveInstanceState(state);
        }
        try(var restored=Robolectric.buildActivity(NewInstallmentActivity.class).setup(state)) {
            NewInstallmentActivity activity=restored.get(); ShadowLooper.idleMainLooper();
            assertEquals("Fone de ouvido",((EditText)activity.findViewById(R.id.form_descricao)).getText().toString()); assertEquals("Cartão Inter",((Spinner)activity.findViewById(R.id.form_conta)).getSelectedItem()); assertEquals("3",((EditText)activity.findViewById(R.id.installment_count)).getText().toString());
            capture(activity,"parcelamento-formulario"); button(activity.findViewById(android.R.id.content),"Criar parcelas").performClick();
            ((androidx.appcompat.app.AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog()).getButton(-1).performClick(); waitFor(activity::isFinishing);
        }
        assertEquals(3,db.lancamentoDao().snapshot().size()); assertEquals(10000,db.lancamentoDao().snapshot().stream().mapToLong(p -> FinanceUtils.centavos(p.valor)).sum());
        try(var list=Robolectric.buildActivity(InstallmentsActivity.class).setup()) {ShadowLooper.idleMainLooper(); assertNotNull(button(list.get().findViewById(android.R.id.content),"Ver parcelas")); capture(list.get(),"parcelamentos");}
    }
    private Button button(View view,String text) {if(view instanceof Button && text.contentEquals(((Button)view).getText())) return (Button)view; if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) {Button found=button(((ViewGroup)view).getChildAt(i),text); if(found!=null) return found;} return null;}
    private void capture(android.app.Activity activity,String name) throws Exception {
        View root=activity.findViewById(android.R.id.content); root.measure(View.MeasureSpec.makeMeasureSpec(411,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(843,View.MeasureSpec.EXACTLY)); root.layout(0,0,411,843); root.getViewTreeObserver().dispatchOnPreDraw();
        android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(411,843,android.graphics.Bitmap.Config.ARGB_8888); android.graphics.Canvas canvas=new android.graphics.Canvas(image); canvas.drawColor(0xfff4f7f5); root.draw(canvas);
        java.io.File file=new java.io.File("build/previews/"+name+".png"); file.getParentFile().mkdirs(); try(java.io.FileOutputStream out=new java.io.FileOutputStream(file)) {image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
    }
}
