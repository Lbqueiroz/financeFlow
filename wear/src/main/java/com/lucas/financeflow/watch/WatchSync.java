package com.lucas.financeflow.watch;
import android.content.Context;
import android.net.Uri;
import com.google.android.gms.wearable.*;
import com.lucas.financeflow.wearlink.WearProtocol;
import org.json.*;
public final class WatchSync {
    static final Object LOCK=new Object();
    private WatchSync() { }
    public static void refresh(Context context) {
        Context app=context.getApplicationContext();
        Wearable.getDataClient(app).getDataItems().addOnSuccessListener(items -> {
            try { for(DataItem item:items) receive(app,item); } finally { items.release(); }
            flush(app);
        }).addOnFailureListener(e -> flush(app));
        Wearable.getNodeClient(app).getConnectedNodes().addOnSuccessListener(nodes -> {
            for(Node node:nodes) Wearable.getMessageClient(app).sendMessage(node.getId(),WearProtocol.REFRESH,new byte[0]);
        });
    }
    public static void flush(Context context) {
        synchronized(LOCK) {
            try {
                JSONArray list=new WatchStore(context).pending();
                for(int i=0;i<list.length();i++) {
                    JSONObject item=list.getJSONObject(i);
                    PutDataMapRequest request=PutDataMapRequest.create(WearProtocol.TX+item.getString("id"));
                    request.getDataMap().putString("json",item.toString());
                    request.getDataMap().putLong("retry",System.currentTimeMillis());
                    Wearable.getDataClient(context).putDataItem(request.asPutDataRequest().setUrgent());
                }
            } catch(Exception e) { android.util.Log.w("WatchSync","Falha ao enviar",e); }
        }
    }
    public static void receive(Context context,DataItem item) {
        synchronized(LOCK) {
            try {
                String path=item.getUri().getPath();
                String json=DataMapItem.fromDataItem(item).getDataMap().getString("json");
                WatchStore store=new WatchStore(context);
                if(WearProtocol.SUMMARY.equals(path)) store.summary(json);
                else if(path!=null && path.startsWith(WearProtocol.ACK)) {
                    JSONObject ack=new JSONObject(json); String id=ack.getString("id");
                    if(!path.equals(WearProtocol.ACK+id)) return;
                    boolean ok=ack.getBoolean("ok"); store.acknowledge(id,ok,ack.optString("error"));
                    if(ok) {
                        Uri tx=new Uri.Builder().scheme("wear").authority("*").path(WearProtocol.TX+id).build();
                        Wearable.getDataClient(context).deleteDataItems(tx,DataClient.FILTER_LITERAL);
                    }
                }
            } catch(Exception e) { android.util.Log.w("WatchSync","Falha ao receber",e); }
        }
    }
}
