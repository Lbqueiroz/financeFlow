package com.lucas.financeflow.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.lucas.financeflow.data.model.Lancamento;

import java.util.List;

@Dao
public interface  LancamentoDao {
    @Insert
    void inserir(Lancamento lancamento);

    @Query("SELECT * FROM lancamentos ORDER BY data DESC, id DESC")
    LiveData<List<Lancamento>> listarTodos();

    @Query("SELECT * FROM lancamentos WHERE id = :id")
    LiveData<Lancamento> porId(int id);

    @Query("SELECT * FROM lancamentos ORDER BY data DESC, id DESC")
    List<Lancamento> snapshot();

    @Query("DELETE FROM lancamentos")
    void limpar();

    @Insert
    void inserirTodos(List<Lancamento> itens);

    @androidx.room.Transaction
    default void restaurar(List<Lancamento> itens) {
        limpar();
        inserirTodos(itens);
    }

    @Delete
    void deletar(Lancamento lancamento);

    @Update
    void atualizar(Lancamento lancamento);

}
