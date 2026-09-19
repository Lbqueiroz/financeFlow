package com.lucas.financeflow.data.repository;
import android.content.Context;
import com.lucas.financeflow.Installments;
import com.lucas.financeflow.data.local.AppDatabase;
import com.lucas.financeflow.data.model.InstallmentPlan;
import java.util.List;
public final class InstallmentRepository {
    private final AppDatabase db;
    private static final java.util.concurrent.ExecutorService IO=java.util.concurrent.Executors.newSingleThreadExecutor();
    public interface Result {void done(String error);}
    public InstallmentRepository(Context context) {db=AppDatabase.getInstance(context);}
    public androidx.lifecycle.LiveData<List<InstallmentPlan>> observe() {return db.installmentDao().observe();}
    private void run(Runnable action,Result result) {
        IO.execute(() -> {String error=null; try {db.runInTransaction(action);} catch(Exception e) {error=e instanceof IllegalArgumentException?e.getMessage():"Não foi possível salvar. Tente novamente.";}
            String message=error; new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> result.done(message));});
    }
    public void save(InstallmentPlan plan,Result result) {run(() -> {
        List<com.lucas.financeflow.data.model.Lancamento> entries=Installments.schedule(plan);
        if(db.installmentDao().get(plan.id)!=null) return;
        if(db.lancamentoDao().contaExiste(plan.account)==0) throw new IllegalArgumentException("Conta indisponível. Cadastre-a na aba Contas.");
        db.installmentDao().insert(plan); db.lancamentoDao().inserirTodos(entries);
    },result);}
    public void delete(String id,Result result) {run(() -> {db.lancamentoDao().deleteInstallments(id); db.installmentDao().delete(id);},result);}
}
