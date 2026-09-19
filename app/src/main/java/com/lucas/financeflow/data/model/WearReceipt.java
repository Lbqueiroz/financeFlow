package com.lucas.financeflow.data.model;
import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
/** Permanent deduplication receipt, intentionally retained when restoring a backup. */
@Entity(tableName="wear_receipts")
public class WearReceipt {
    @PrimaryKey @NonNull public String id;
    public WearReceipt(@NonNull String id) { this.id=id; }
}
