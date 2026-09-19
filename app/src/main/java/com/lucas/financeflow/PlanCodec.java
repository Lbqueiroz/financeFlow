package com.lucas.financeflow;
import com.lucas.financeflow.data.model.PlanItem;
import org.json.*;
import java.util.*;
public final class PlanCodec {
    private PlanCodec() { }
    public static JSONArray encode(List<PlanItem> items) throws JSONException {
        JSONArray array=new JSONArray();
        for(PlanItem p:items) array.put(new JSONObject().put("id",p.id).put("kind",p.kind).put("name",p.name).put("account",p.account).put("target",p.target)
            .put("category",p.category).put("type",p.type).put("date",p.date).put("cents",p.cents).put("created",p.created).put("day",p.day));
        return array;
    }
    public static List<PlanItem> decode(JSONArray array) throws JSONException {
        if(array.length()>100000) throw new JSONException("Arquivo muito grande");
        List<PlanItem> result=new ArrayList<>();
        for(int i=0;i<array.length();i++) {
            JSONObject o=array.getJSONObject(i); PlanItem p=new PlanItem();
            p.id=o.getString("id"); p.kind=o.getString("kind"); p.name=o.getString("name"); p.account=o.getString("account"); p.target=o.getString("target");
            p.category=o.getString("category"); p.type=o.getString("type"); p.date=o.getString("date");
            p.cents=integer(o,"cents"); p.created=integer(o,"created"); long day=integer(o,"day"); if(day>31) throw new JSONException("Dia inválido"); p.day=(int)day;
            result.add(p);
        }
        result.sort(Comparator.comparingLong((PlanItem p)->p.created).thenComparing(p->p.id));
        try {Planning.validateLedger(result);} catch(IllegalArgumentException e) {throw new JSONException(e.getMessage());}
        return result;
    }
    private static long integer(JSONObject o,String key) throws JSONException {
        String value=o.get(key).toString(); if(!value.matches("[0-9]{1,18}")) throw new JSONException("Número inválido");
        return Long.parseLong(value);
    }
}
