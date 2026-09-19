package com.lucas.financeflow.data.repository;

import android.content.Context;
import com.lucas.financeflow.Planning;
import com.lucas.financeflow.FinanceUtils;
import com.lucas.financeflow.data.local.AppDatabase;
import com.lucas.financeflow.data.model.*;
import java.util.*;

public final class PlanRepository {
    private final AppDatabase db;
    private static final java.util.concurrent.ExecutorService IO=java.util.concurrent.Executors.newSingleThreadExecutor();
    public interface Result { void done(String error); }
    public PlanRepository(Context context) { db=AppDatabase.getInstance(context); }
    public androidx.lifecycle.LiveData<List<PlanItem>> observe() { return db.planDao().observe(); }
    private void run(Runnable operation,Result result) {
        IO.execute(() -> { String error=null; try { db.runInTransaction(operation); } catch(Exception e) { error=e instanceof IllegalArgumentException?e.getMessage():"Não foi possível salvar. Tente novamente."; }
            String message=error; new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> result.done(message)); });
    }
    public void save(PlanItem item,Result result) { run(() -> {
        Planning.validate(item);
        if(Arrays.asList("TRANSFER","APPLY","REDEEM","RULE").contains(item.kind) && db.lancamentoDao().contaExiste(item.account)==0) throw new IllegalArgumentException("Conta indisponível. Cadastre-a novamente.");
        if(item.kind.equals("TRANSFER") && db.lancamentoDao().contaExiste(item.target)==0) throw new IllegalArgumentException("Conta de destino indisponível");
        List<PlanItem> all=db.planDao().snapshot();
        boolean replacing=item.kind.equals("RULE") || item.kind.equals("BUDGET");
        if(!replacing && db.planDao().get(item.id)!=null) return; // retry of an already committed form
        all.removeIf(p -> p.id.equals(item.id));
        if(!replacing && !all.isEmpty()) item.created=Math.max(item.created,all.get(all.size()-1).created+1);
        all.add(item); all.sort(Comparator.comparingLong((PlanItem p)->p.created).thenComparing(p->p.id)); Planning.validateLedger(all);
        if(replacing && db.planDao().get(item.id)!=null) db.planDao().update(item); else db.planDao().insert(item);
    },result); }
    public void delete(String id,Result result) { run(() -> {
        List<PlanItem> all=db.planDao().snapshot(); all.removeIf(p -> p.id.equals(id)); Planning.validateLedger(all); db.planDao().delete(id);
    },result); }
    public void confirm(String id,String expectedDate,Result result) { run(() -> {
        PlanItem rule=db.planDao().get(id); if(rule==null || !rule.kind.equals("RULE")) throw new IllegalArgumentException("Recorrência não encontrada");
        String receipt="occurrence:"+id+":"+expectedDate;
        if(db.planDao().get(receipt)!=null) {
            if(rule.date.equals(expectedDate)) {rule.date=Planning.nextDate(rule.date,rule.day); db.planDao().update(rule);}
            return;
        }
        if(!rule.date.equals(expectedDate)) throw new IllegalArgumentException("Esta parcela já foi atualizada");
        if(db.lancamentoDao().contaExiste(rule.account)==0) throw new IllegalArgumentException("Cadastre a conta novamente antes de confirmar");
        if(rule.date.compareTo(FinanceUtils.hoje())>0) throw new IllegalArgumentException("Confirme a partir do vencimento");
        db.lancamentoDao().inserir(new Lancamento(rule.name,java.math.BigDecimal.valueOf(rule.cents,2).doubleValue(),rule.type,rule.category,rule.date,"CELULAR","LOCAL","",rule.account));
        PlanItem mark=new PlanItem(); mark.id=receipt; mark.kind="CONFIRMED"; mark.target=id; mark.date=expectedDate; db.planDao().insert(mark);
        rule.date=Planning.nextDate(rule.date,rule.day); db.planDao().update(rule);
    },result); }
}
