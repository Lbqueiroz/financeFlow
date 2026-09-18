package com.lucas.financeflow.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.lucas.financeflow.data.model.Lancamento;

@Database(
        entities = {Lancamento.class},
        version = 3,
        exportSchema = false
)public abstract class AppDatabase extends RoomDatabase{

    private static AppDatabase INSTANCE;

    public abstract LancamentoDao lancamentoDao();

    public static synchronized AppDatabase getInstance(Context context){
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class, "financeFlow_db"
            )
                    .addMigrations(new androidx.room.migration.Migration(2, 3) {
                        @Override public void migrate(androidx.sqlite.db.SupportSQLiteDatabase db) {
                            // Recover rows written by the original form's misplaced constructor arguments.
                            db.execSQL("UPDATE lancamentos SET conta = data, origemDestino = origem, " +
                                    "data = syncStatus, origem = origemDestino, syncStatus = conta " +
                                    "WHERE syncStatus GLOB '[0-9][0-9]-[0-9][0-9]-[0-9][0-9][0-9][0-9]' " +
                                    "AND origemDestino = 'CELULAR'");
                            db.execSQL("UPDATE lancamentos SET data = substr(data,7,4) || '-' || substr(data,4,2) || '-' || substr(data,1,2) " +
                                    "WHERE data GLOB '[0-9][0-9]-[0-9][0-9]-[0-9][0-9][0-9][0-9]'");
                        }
                    })
                    .build();
        }
        return INSTANCE;
    }
}
