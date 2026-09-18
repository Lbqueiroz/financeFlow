package com.lucas.financeflow.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.lucas.financeflow.data.model.Lancamento;

@Database(entities = {Lancamento.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;
    public abstract LancamentoDao lancamentoDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE lancamentos ADD COLUMN conta TEXT");
            db.execSQL("ALTER TABLE lancamentos ADD COLUMN origemDestino TEXT");
            db.execSQL("UPDATE lancamentos SET conta = 'Outros', origemDestino = ''");
        }
    };
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            // Recover the original form's misplaced constructor arguments, using SQLite's old row values.
            db.execSQL("UPDATE lancamentos SET conta = data, origemDestino = origem, " +
                    "data = syncStatus, origem = origemDestino, syncStatus = conta " +
                    "WHERE syncStatus GLOB '[0-9][0-9]-[0-9][0-9]-[0-9][0-9][0-9][0-9]' " +
                    "AND origemDestino = 'CELULAR'");
            db.execSQL("UPDATE lancamentos SET data = substr(data,7,4) || '-' || substr(data,4,2) || '-' || substr(data,1,2) " +
                    "WHERE data GLOB '[0-9][0-9]-[0-9][0-9]-[0-9][0-9][0-9][0-9]'");
        }
    };
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "financeFlow_db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3).build();
        }
        return instance;
    }
}
