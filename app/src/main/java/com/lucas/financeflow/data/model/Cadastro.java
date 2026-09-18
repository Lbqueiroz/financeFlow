package com.lucas.financeflow.data.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(tableName = "cadastros", primaryKeys = {"tipo", "nome"})
public class Cadastro {
    public static final String CONTA = "CONTA", ORIGEM = "ORIGEM";
    @NonNull public String tipo;
    @NonNull @ColumnInfo(collate = ColumnInfo.NOCASE) public String nome;
    public Cadastro(@NonNull String tipo, @NonNull String nome) { this.tipo = tipo; this.nome = nome; }
}
