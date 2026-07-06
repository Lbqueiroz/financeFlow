package com.lucas.financeflow.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "lancamentos")
public class Lancamento {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String descricao;
    public double valor;
    public String tipo; // ENTRADA ou SAIDA
    public String categoria;
    public String data;
    public String origem; // CELULAR ou RELOGIO
    public String syncStatus; // PENDENTE ou SINCRONIZADO

    public Lancamento(String descricao, double valor, String tipo, String categoria, String data, String origem, String syncStatus) {
        this.descricao = descricao;
        this.valor = valor;
        this.tipo = tipo;
        this.categoria = categoria;
        this.data = data;
        this.origem = origem;
        this.syncStatus = syncStatus;
    }
}