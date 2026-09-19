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
    @Test public void deletingRegistrationsPreservesHistoryAndDoesNotReturnAfterBackup() throws Exception {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase.class).allowMainThreadQueries().build();
        com.lucas.financeflow.data.model.Cadastro conta = new com.lucas.financeflow.data.model.Cadastro("CONTA", "Carteira");
        com.lucas.financeflow.data.model.Cadastro origem = new com.lucas.financeflow.data.model.Cadastro("ORIGEM", "Trabalho");
        db.lancamentoDao().cadastrar(conta); db.lancamentoDao().cadastrar(origem);
        db.lancamentoDao().inserir(new Lancamento("Pagamento", 50, "ENTRADA", "Outros", "2026-09-18", "CELULAR", "LOCAL", "Trabalho", "Carteira"));
        db.lancamentoDao().excluirCadastro(conta);
        assertEquals(1, db.lancamentoDao().snapshotCadastros().size());
        db.lancamentoDao().excluirCadastro(origem);
        assertTrue(db.lancamentoDao().snapshotCadastros().isEmpty());
        assertEquals("Carteira", db.lancamentoDao().snapshot().get(0).conta);
        assertEquals("Trabalho", db.lancamentoDao().snapshot().get(0).origemDestino);
        BackupCodec.Documento backup = BackupCodec.decodeCompleto(BackupCodec.encode(db.lancamentoDao().snapshot(), db.lancamentoDao().snapshotCadastros()));
        db.lancamentoDao().restaurarCompleto(backup.itens, backup.cadastros);
        assertTrue(db.lancamentoDao().snapshotCadastros().isEmpty());
        assertEquals(50, db.lancamentoDao().snapshot().get(0).valor, 0);
    }
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
        db = Room.databaseBuilder(context, AppDatabase.class, "migration-test").addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5).allowMainThreadQueries().build();
        List<Lancamento> items = db.lancamentoDao().snapshot();
        assertEquals(2, items.size());
        Lancamento repaired = items.get(0);
        assertEquals("INTER", repaired.conta); assertEquals("Eu", repaired.origemDestino);
        assertEquals("2026-09-18", repaired.data); assertEquals("CELULAR", repaired.origem);
        assertEquals("PENDENTE", repaired.syncStatus);
        assertEquals("2026-08-01", items.get(1).data); assertEquals("NUBANK", items.get(1).conta);
        assertEquals(4, db.lancamentoDao().snapshotCadastros().size());
    }

    @Test public void migratesVersionOneWithoutDeletingHistory() {
        Context context = RuntimeEnvironment.getApplication();
        context.deleteDatabase("migration-v1-test");
        SQLiteDatabase legacy = context.openOrCreateDatabase("migration-v1-test", 0, null);
        legacy.execSQL("CREATE TABLE lancamentos (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, descricao TEXT, valor REAL NOT NULL, tipo TEXT, categoria TEXT, data TEXT, origem TEXT, syncStatus TEXT)");
        legacy.execSQL("INSERT INTO lancamentos VALUES (1, 'Antigo', 100, 'ENTRADA', 'Outros', '02-01-2026', 'CELULAR', 'PENDENTE')");
        legacy.setVersion(1); legacy.close();
        db = Room.databaseBuilder(context, AppDatabase.class, "migration-v1-test").addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5).allowMainThreadQueries().build();
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
        db.lancamentoDao().cadastrar(new com.lucas.financeflow.data.model.Cadastro("CONTA", "Carteira"));
        try { db.lancamentoDao().restaurar(Arrays.asList(item, item)); fail("Duplicate IDs must roll back"); }
        catch (android.database.sqlite.SQLiteConstraintException expected) { }
        assertEquals("Editado", db.lancamentoDao().snapshot().get(0).descricao);
        assertEquals("Carteira", db.lancamentoDao().snapshotCadastros().get(0).nome);
        db.lancamentoDao().deletar(item); assertTrue(db.lancamentoDao().snapshot().isEmpty());
    }

    @Test public void registrationsPersistWithoutTransactionsAndIgnoreCaseDuplicates() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase.class).allowMainThreadQueries().build();
        db.lancamentoDao().cadastrar(new com.lucas.financeflow.data.model.Cadastro("CONTA", "Poupança"));
        db.lancamentoDao().cadastrar(new com.lucas.financeflow.data.model.Cadastro("CONTA", "poupança"));
        db.lancamentoDao().cadastrar(new com.lucas.financeflow.data.model.Cadastro("ORIGEM", "Trabalho"));
        assertEquals(2, db.lancamentoDao().snapshotCadastros().size());
        assertTrue(db.lancamentoDao().snapshot().isEmpty());
    }
}
