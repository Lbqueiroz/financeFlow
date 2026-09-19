package com.lucas.financeflow.data.model;
import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName="installment_plans")
public class InstallmentPlan {
    @PrimaryKey @NonNull public String id=java.util.UUID.randomUUID().toString();
    @NonNull public String name="";
    @NonNull public String account="";
    @NonNull public String category="Outros";
    @NonNull public String firstDate="";
    public long totalCents;
    public int count;
}
