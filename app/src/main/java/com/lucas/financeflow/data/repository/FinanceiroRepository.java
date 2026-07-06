package com.lucas.financeflow.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.lucas.financeflow.data.local.AppDatabase;
import com.lucas.financeflow.data.local.LancamentoDao;
import com.lucas.financeflow.data.model.Lancamento;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FinanceiroRepository {

    private final LancamentoDao lancamentoDao;
    private final ExecutorService executorService;

    public FinanceiroRepository(Context context){
        AppDatabase database = AppDatabase.getInstance(context);
        lancamentoDao = database.lancamentoDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void inserir(Lancamento lancamento){
        executorService.execute(() -> lancamentoDao.inserir(lancamento));
    }

    public LiveData<List<Lancamento>> listarTodos(){
        return lancamentoDao.listarTodos();
    }

    public LiveData<Double> totalEntradas(){
        return lancamentoDao.totalEntradas();
    }

    public LiveData<Double> totalSaidas(){
        return lancamentoDao.totalSaidas();
    }
}
