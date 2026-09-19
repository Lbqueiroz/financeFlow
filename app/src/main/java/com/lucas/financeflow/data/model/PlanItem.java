package com.lucas.financeflow.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Separate ledger: moving assets must never inflate income or consumption. */
@Entity(tableName = "planning")
public class PlanItem {
    @PrimaryKey @NonNull public String id = java.util.UUID.randomUUID().toString();
    @NonNull public String kind = "";
    @NonNull public String name = "";
    @NonNull public String account = "";
    @NonNull public String target = "";
    @NonNull public String category = "Outros";
    @NonNull public String type = "SAIDA";
    @NonNull public String date = "";
    public long cents;
    public long created = System.currentTimeMillis();
    public int day;
}
