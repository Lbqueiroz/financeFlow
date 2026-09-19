package com.lucas.financeflow.watch;
import com.lucas.financeflow.wearlink.WearProtocol;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=30)
public class WatchStoreTest {
    @Test public void undoOnlyRemovesEntriesBeforeTheyCanBeSent() throws Exception {
        WatchStore store=new WatchStore(RuntimeEnvironment.getApplication());
        String id="12345678-1234-1234-1234-123456789abc";
        WearProtocol.Entry entry=new WearProtocol.Entry(id,"SAIDA","Carteira","Outros","2026-09-19",100);
        store.add(entry,10000); assertTrue(store.undo(id,9999)); assertEquals(0,store.pending().length());
        store.add(entry,10000); assertFalse(store.undo(id,10000)); assertEquals(1,store.pending().length());
    }
    @Test public void favoritePreservesAccountCategoryAndTypeAcrossReopen() throws Exception {
        WatchStore store=new WatchStore(RuntimeEnvironment.getApplication()); store.favorite("Café","SAIDA","Carteira","Alimentação");
        org.json.JSONObject favorite=new WatchStore(RuntimeEnvironment.getApplication()).favorites().getJSONObject(0);
        assertEquals("Alimentação",favorite.getString("category")); assertEquals("Carteira",favorite.getString("account")); assertEquals("SAIDA",favorite.getString("type"));
        store.removeFavorite(favorite.getString("id")); assertEquals(0,store.favorites().length());
    }
    @Before public void clear() { RuntimeEnvironment.getApplication().getSharedPreferences("watch",0).edit().clear().commit(); }
    @Test public void queuedEntrySurvivesNewStoreAndOnlySuccessRemovesIt() throws Exception {
        WatchStore store=new WatchStore(RuntimeEnvironment.getApplication());
        String id="12345678-1234-1234-1234-123456789abc";
        store.add(new WearProtocol.Entry(id,"SAIDA","Carteira","Outros","2026-09-19",100));
        WatchStore reopened=new WatchStore(RuntimeEnvironment.getApplication()); assertEquals(1,reopened.pending().length());
        reopened.acknowledge(id,false,"Conta indisponível"); assertEquals("Conta indisponível",store.pending().getJSONObject(0).getString("error"));
        reopened.acknowledge("outro",true,""); assertEquals(1,store.pending().length());
        reopened.acknowledge(id,true,""); reopened.acknowledge(id,true,""); assertEquals(0,store.pending().length());
    }
}
