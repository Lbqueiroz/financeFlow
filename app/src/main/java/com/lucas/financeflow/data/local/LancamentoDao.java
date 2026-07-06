package com.lucas.financeflow.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.lucas.financeflow.data.model.Lancamento;

import java.util.List;

@Dao
public interface  LancamentoDao {
    @Insert
    void inserir(Lancamento lancamento);

    @Query("SELECT * FROM lancamentos ORDER BY data DESC")
    LiveData<List<Lancamento>> listarTodos();

    @Query("SELECT COALESCE(SUM(valor), 0) FROM lancamentos WHERE tipo = 'ENTRADA'")
    LiveData<Double> totalEntradas();

    @Query("SELECT COALESCE(SUM(valor), 0) FROM lancamentos WHERE tipo = 'SAIDA'")
    LiveData<Double> totalSaidas();
}
