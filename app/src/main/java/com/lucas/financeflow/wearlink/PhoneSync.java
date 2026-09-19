package com.lucas.financeflow.wearlink;

import android.content.Context;
import com.google.android.gms.wearable.*;
import com.lucas.financeflow.data.local.AppDatabase;
import com.lucas.financeflow.data.model.*;
import org.json.*;
import java.util.concurrent.Executors;

public final class PhoneSync {
    private static final java.util.concurrent.ExecutorService IO = Executors.newSingleThreadExecutor();
    private PhoneSync() { }
    public static void publish(Context context) {
        Context app = context.getApplicationContext();
        IO.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(app);
                long income=0, expense=0;
                String month=WearProtocol.month();
                for (Lancamento item: db.lancamentoDao().snapshot()) {
                    if (item.data == null || !item.data.startsWith(month+"-")) continue;
                    long cents=java.math.BigDecimal.valueOf(item.valor).movePointRight(2).setScale(0,java.math.RoundingMode.HALF_UP).longValueExact();
                    if ("ENTRADA".equals(item.tipo)) income+=cents; else if ("SAIDA".equals(item.tipo)) expense+=cents;
                }
                JSONArray accounts=new JSONArray();
                for (Cadastro item: db.lancamentoDao().snapshotCadastros()) if ("CONTA".equals(item.tipo)) accounts.put(item.nome);
                JSONObject summary=new JSONObject().put("version",1).put("month",month).put("balanceCents",income-expense)
                    .put("incomeCents",income).put("expenseCents",expense).put("accounts",accounts).put("updatedAt",System.currentTimeMillis());
                put(app,WearProtocol.SUMMARY,summary.toString());
            } catch (Exception e) { android.util.Log.w("PhoneSync","Não foi possível atualizar o relógio",e); }
        });
    }
    public static void receive(Context context, String path, String json) {
        Context app=context.getApplicationContext();
        IO.execute(() -> {
            String id=path.substring(WearProtocol.TX.length());
            if (!id.matches("[0-9a-fA-F-]{36}")) return;
            try {
                boolean ok=false; String error="";
                try {
                    WearProtocol.Entry entry=WearProtocol.parse(json);
                    if (!id.equals(entry.id)) throw new IllegalArgumentException("Identificador inválido");
                    WatchImporter.accept(AppDatabase.getInstance(app),entry); ok=true;
                } catch (Exception e) { error=e instanceof IllegalArgumentException || e instanceof JSONException ? e.getMessage() : "Falha ao salvar. Tente novamente."; }
                put(app,WearProtocol.ACK+id,new JSONObject().put("id",id).put("ok",ok).put("error",error).put("time",System.currentTimeMillis()).toString());
                publish(app);
            } catch (Exception e) { android.util.Log.w("PhoneSync","Falha na confirmação",e); }
        });
    }
    private static void put(Context context,String path,String json) {
        PutDataMapRequest request=PutDataMapRequest.create(path); request.getDataMap().putString("json",json);
        Wearable.getDataClient(context).putDataItem(request.asPutDataRequest().setUrgent())
            .addOnFailureListener(e -> android.util.Log.w("PhoneSync","Sincronização indisponível",e));
    }
}
