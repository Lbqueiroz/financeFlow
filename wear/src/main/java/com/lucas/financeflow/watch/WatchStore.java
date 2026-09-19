package com.lucas.financeflow.watch;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import com.lucas.financeflow.wearlink.WearProtocol;
public final class WatchStore {
    private final SharedPreferences prefs;
    public WatchStore(Context context) { prefs=context.getSharedPreferences("watch",Context.MODE_PRIVATE); }
    public synchronized JSONObject summary() { try { return new JSONObject(prefs.getString("summary","{}")); } catch(JSONException e) { return new JSONObject(); } }
    public synchronized void summary(String json) throws JSONException { JSONObject obj=new JSONObject(json); if(obj.getInt("version")!=1) return; save(prefs.edit().putString("summary",json)); }
    public synchronized JSONArray pending() { try { return new JSONArray(prefs.getString("pending","[]")); } catch(JSONException e) { throw new IllegalStateException("Não foi possível ler os lançamentos salvos",e); } }
    public synchronized void add(WearProtocol.Entry entry) throws JSONException {
        JSONArray list=pending(); if(list.length()>=50) throw new IllegalStateException("Sincronize os lançamentos pendentes primeiro");
        list.put(new JSONObject(entry.json())); save(prefs.edit().putString("pending",list.toString()));
    }
    public synchronized void acknowledge(String id,boolean ok,String error) throws JSONException {
        JSONArray result=new JSONArray(), list=pending();
        for(int i=0;i<list.length();i++) { JSONObject item=list.getJSONObject(i); if(id.equals(item.getString("id"))) { if(ok) continue; item.put("error",error); } result.put(item); }
        save(prefs.edit().putString("pending",result.toString()));
    }
    private void save(SharedPreferences.Editor edit) { if(!edit.commit()) throw new IllegalStateException("Não foi possível salvar no relógio"); }
}
