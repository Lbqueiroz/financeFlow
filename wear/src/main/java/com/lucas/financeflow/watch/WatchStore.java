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
        add(entry,0);
    }
    public synchronized void add(WearProtocol.Entry entry,long notBefore) throws JSONException {
        JSONArray list=pending(); if(list.length()>=50) throw new IllegalStateException("Sincronize os lançamentos pendentes primeiro");
        list.put(new JSONObject(entry.json()).put("notBefore",notBefore)); save(prefs.edit().putString("pending",list.toString()));
    }
    public synchronized boolean undo(String id,long now) throws JSONException {
        JSONArray list=pending(),result=new JSONArray(); boolean removed=false;
        for(int i=0;i<list.length();i++) {JSONObject item=list.getJSONObject(i); if(id.equals(item.getString("id")) && now<item.optLong("notBefore")) removed=true; else result.put(item);}
        if(removed) save(prefs.edit().putString("pending",result.toString())); return removed;
    }
    public synchronized JSONArray favorites() {try {return new JSONArray(prefs.getString("favorites","[]"));} catch(JSONException e) {throw new IllegalStateException("Favoritos indisponíveis",e);} }
    public synchronized void favorite(String name,String type,String account,String category) throws JSONException {
        if(name.trim().isEmpty() || name.length()>30) throw new IllegalArgumentException("Use um nome de até 30 letras");
        JSONArray items=favorites(); if(items.length()>=8) throw new IllegalArgumentException("Limite de 8 favoritos. Remova um antes de adicionar.");
        items.put(new JSONObject().put("id",java.util.UUID.randomUUID().toString()).put("name",name.trim()).put("type",type).put("account",account).put("category",category)); save(prefs.edit().putString("favorites",items.toString()));
    }
    public synchronized void removeFavorite(String id) throws JSONException {
        JSONArray result=new JSONArray(),items=favorites(); for(int i=0;i<items.length();i++) if(!items.getJSONObject(i).getString("id").equals(id)) result.put(items.getJSONObject(i)); save(prefs.edit().putString("favorites",result.toString()));
    }
    public synchronized void acknowledge(String id,boolean ok,String error) throws JSONException {
        JSONArray result=new JSONArray(), list=pending();
        for(int i=0;i<list.length();i++) { JSONObject item=list.getJSONObject(i); if(id.equals(item.getString("id"))) { if(ok) continue; item.put("error",error); } result.put(item); }
        save(prefs.edit().putString("pending",result.toString()));
    }
    private void save(SharedPreferences.Editor edit) { if(!edit.commit()) throw new IllegalStateException("Não foi possível salvar no relógio"); }
}
