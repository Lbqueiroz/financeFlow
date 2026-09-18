package com.lucas.financeflow.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.lucas.financeflow.data.model.Lancamento;
import com.lucas.financeflow.data.model.Cadastro;

import java.util.List;

@Dao
public interface  LancamentoDao {
    @Query("SELECT * FROM cadastros ORDER BY tipo, nome COLLATE NOCASE")
    LiveData<List<Cadastro>> cadastros();

    @Query("SELECT * FROM cadastros ORDER BY tipo, nome COLLATE NOCASE")
    List<Cadastro> snapshotCadastros();

    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    void cadastrar(Cadastro cadastro);

    @Delete
    void excluirCadastro(Cadastro cadastro);

    @Query("DELETE FROM cadastros")
    void limparCadastros();

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
        restaurarCompleto(itens, java.util.Collections.emptyList());
    }

    @androidx.room.Transaction
    default void restaurarCompleto(List<Lancamento> itens, List<Cadastro> cadastros) {
        limpar();
        limparCadastros();
        inserirTodos(itens);
        for (Cadastro cadastro : cadastros) cadastrar(cadastro);
    }

    @Delete
    void deletar(Lancamento lancamento);

    @Update
    void atualizar(Lancamento lancamento);

}
