package com.lucas.financeflow;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.room.Room;
import com.lucas.financeflow.data.local.AppDatabase;
import com.lucas.financeflow.data.model.Lancamento;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class DatabaseTest {
    private AppDatabase db;
    @After public void close() { if (db != null) db.close(); }

    @Test public void migratesVersionTwoAndRepairsMisplacedFields() {
        Context context = RuntimeEnvironment.getApplication();
        context.deleteDatabase("migration-test");
        SQLiteDatabase legacy = context.openOrCreateDatabase("migration-test", 0, null);
        legacy.execSQL("CREATE TABLE lancamentos (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, descricao TEXT, conta TEXT, origemDestino TEXT, valor REAL NOT NULL, tipo TEXT, categoria TEXT, data TEXT, origem TEXT, syncStatus TEXT)");
        legacy.execSQL("INSERT INTO lancamentos VALUES (1, 'Mercado', 'PENDENTE', 'CELULAR', 45.5, 'SAIDA', 'Alimentação', 'INTER', 'Eu', '18-09-2026')");
        legacy.execSQL("INSERT INTO lancamentos VALUES (2, 'Salário', 'NUBANK', 'Empresa', 2500, 'ENTRADA', 'Salário', '01-08-2026', 'CELULAR', 'PENDENTE')");
        legacy.setVersion(2); legacy.close();
        db = Room.databaseBuilder(context, AppDatabase.class, "migration-test").addMigrations(AppDatabase.MIGRATION_2_3).allowMainThreadQueries().build();
        List<Lancamento> items = db.lancamentoDao().snapshot();
        assertEquals(2, items.size());
        Lancamento repaired = items.get(0);
        assertEquals("INTER", repaired.conta); assertEquals("Eu", repaired.origemDestino);
        assertEquals("2026-09-18", repaired.data); assertEquals("CELULAR", repaired.origem);
        assertEquals("PENDENTE", repaired.syncStatus);
        assertEquals("2026-08-01", items.get(1).data); assertEquals("NUBANK", items.get(1).conta);
    }

    @Test public void migratesVersionOneWithoutDeletingHistory() {
        Context context = RuntimeEnvironment.getApplication();
        context.deleteDatabase("migration-v1-test");
        SQLiteDatabase legacy = context.openOrCreateDatabase("migration-v1-test", 0, null);
        legacy.execSQL("CREATE TABLE lancamentos (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, descricao TEXT, valor REAL NOT NULL, tipo TEXT, categoria TEXT, data TEXT, origem TEXT, syncStatus TEXT)");
        legacy.execSQL("INSERT INTO lancamentos VALUES (1, 'Antigo', 100, 'ENTRADA', 'Outros', '02-01-2026', 'CELULAR', 'PENDENTE')");
        legacy.setVersion(1); legacy.close();
        db = Room.databaseBuilder(context, AppDatabase.class, "migration-v1-test").addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3).allowMainThreadQueries().build();
        Lancamento item = db.lancamentoDao().snapshot().get(0);
        assertEquals("Antigo", item.descricao); assertEquals("Outros", item.conta); assertEquals("2026-01-02", item.data);
    }

    @Test public void crudAndRestoreRollback() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase.class).allowMainThreadQueries().build();
        Lancamento item = new Lancamento("Teste", 10.5, "ENTRADA", "Outros", "2026-09-18", "CELULAR", "LOCAL", "Eu", "INTER");
        db.lancamentoDao().inserir(item);
        item = db.lancamentoDao().snapshot().get(0); item.descricao = "Editado";
        db.lancamentoDao().atualizar(item);
        assertEquals("Editado", db.lancamentoDao().snapshot().get(0).descricao);
        try { db.lancamentoDao().restaurar(Arrays.asList(item, item)); fail("Duplicate IDs must roll back"); }
        catch (android.database.sqlite.SQLiteConstraintException expected) { }
        assertEquals("Editado", db.lancamentoDao().snapshot().get(0).descricao);
        db.lancamentoDao().deletar(item); assertTrue(db.lancamentoDao().snapshot().isEmpty());
    }
}
