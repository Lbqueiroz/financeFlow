package com.lucas.financeflow.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.lucas.financeflow.data.local.AppDatabase;
import com.lucas.financeflow.data.local.LancamentoDao;
import com.lucas.financeflow.data.model.Lancamento;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FinanceiroRepository {

    private final LancamentoDao lancamentoDao;
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public FinanceiroRepository(Context context){
        AppDatabase database = AppDatabase.getInstance(context);
        lancamentoDao = database.lancamentoDao();
    }

    public LiveData<Lancamento> porId(int id) { return lancamentoDao.porId(id); }

    public interface Resultado { void concluir(boolean sucesso); }

    public LiveData<List<com.lucas.financeflow.data.model.Cadastro>> cadastros() { return lancamentoDao.cadastros(); }
    public void cadastrar(String tipo, String nome, Resultado resultado) {
        executar(() -> lancamentoDao.cadastrar(new com.lucas.financeflow.data.model.Cadastro(tipo, nome.trim())), resultado);
    }
    public void excluirCadastro(com.lucas.financeflow.data.model.Cadastro cadastro, Resultado resultado) {
        executar(() -> lancamentoDao.excluirCadastro(cadastro), resultado);
    }

    public void salvar(Lancamento item, Resultado resultado) {
        executar(() -> { if (item.id == 0) lancamentoDao.inserir(item); else lancamentoDao.atualizar(item); }, resultado);
    }

    public void excluir(Lancamento item, Resultado resultado) {
        executar(() -> lancamentoDao.deletar(item), resultado);
    }

    private void executar(Runnable operacao, Resultado resultado) {
        executorService.execute(() -> {
            boolean sucesso;
            try { operacao.run(); sucesso = true; } catch (RuntimeException e) { sucesso = false; }
            final boolean ok = sucesso;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> resultado.concluir(ok));
        });
    }

    public LiveData<List<Lancamento>> listarTodos(){
        return lancamentoDao.listarTodos();
    }

}
