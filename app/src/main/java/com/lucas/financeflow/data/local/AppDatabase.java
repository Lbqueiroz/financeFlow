package com.lucas.financeflow.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.lucas.financeflow.data.model.Lancamento;

@Database(entities = {Lancamento.class, com.lucas.financeflow.data.model.Cadastro.class, com.lucas.financeflow.data.model.WearReceipt.class, com.lucas.financeflow.data.model.PlanItem.class}, version = 6, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;
    public abstract LancamentoDao lancamentoDao();
    public abstract PlanDao planDao();

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build();
        }
        return instance;
    }
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS cadastros (tipo TEXT NOT NULL, nome TEXT COLLATE NOCASE NOT NULL, PRIMARY KEY(tipo, nome))");
            db.execSQL("INSERT OR IGNORE INTO cadastros SELECT 'CONTA', trim(conta) FROM lancamentos WHERE conta IS NOT NULL AND trim(conta) != ''");
            db.execSQL("INSERT OR IGNORE INTO cadastros SELECT 'ORIGEM', trim(origemDestino) FROM lancamentos WHERE origemDestino IS NOT NULL AND trim(origemDestino) != ''");
        }
    };
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS wear_receipts (id TEXT NOT NULL PRIMARY KEY)");
        }
    };
    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS planning (id TEXT NOT NULL PRIMARY KEY, kind TEXT NOT NULL, name TEXT NOT NULL, account TEXT NOT NULL, target TEXT NOT NULL, category TEXT NOT NULL, type TEXT NOT NULL, date TEXT NOT NULL, cents INTEGER NOT NULL, created INTEGER NOT NULL, day INTEGER NOT NULL)");
        }
    };
}
