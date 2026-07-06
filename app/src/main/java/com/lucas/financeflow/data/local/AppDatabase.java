package com.lucas.financeflow.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.lucas.financeflow.data.model.Lancamento;

@Database(
        entities = {Lancamento.class},
        version = 2,
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
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return INSTANCE;
    }
}
