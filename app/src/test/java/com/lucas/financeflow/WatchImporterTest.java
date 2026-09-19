package com.lucas.financeflow;
import androidx.room.Room;
import com.lucas.financeflow.data.local.AppDatabase;
import com.lucas.financeflow.data.model.Cadastro;
import com.lucas.financeflow.wearlink.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class WatchImporterTest {
    private AppDatabase db;
    @Before public void setup() { db=Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(),AppDatabase.class).allowMainThreadQueries().build(); }
    @After public void close() { db.close(); }
    private WearProtocol.Entry entry() { return new WearProtocol.Entry("12345678-1234-1234-1234-123456789abc","SAIDA","Carteira","Outros","2026-09-19",12345); }
    @Test public void duplicateDeliveryAndRestoreNeverReinsertConfirmedEntry() {
        db.lancamentoDao().cadastrar(new Cadastro("CONTA","Carteira"));
        WatchImporter.accept(db,entry()); WatchImporter.accept(db,entry());
        assertEquals(1,db.lancamentoDao().snapshot().size()); assertEquals(123.45,db.lancamentoDao().snapshot().get(0).valor,0.001);
        db.lancamentoDao().restaurar(java.util.Collections.emptyList()); WatchImporter.accept(db,entry());
        assertTrue(db.lancamentoDao().snapshot().isEmpty());
    }
    @Test public void rejectedAccountCanBeRetriedAfterRegistration() {
        try { WatchImporter.accept(db,entry()); fail(); } catch(IllegalArgumentException expected) { }
        assertEquals(0,db.lancamentoDao().wearReceipt(entry().id));
        db.lancamentoDao().cadastrar(new Cadastro("CONTA","Carteira")); WatchImporter.accept(db,entry());
        assertEquals(1,db.lancamentoDao().snapshot().size());
    }
    @Test public void contractRejectsFractionalCentsAndImpossibleDate() throws Exception {
        String json=entry().json(); assertEquals(12345,WearProtocol.parse(json).cents);
        for(String invalid:new String[]{json.replace("12345","12.5"),json.replace("2026-09-19","2026-09-31")}) {
            try { WearProtocol.parse(invalid); fail(); } catch(org.json.JSONException expected) { }
        }
    }
}
