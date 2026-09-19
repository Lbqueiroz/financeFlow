package com.lucas.financeflow;
import com.lucas.financeflow.data.model.InstallmentPlan;
import org.json.*;
import java.util.*;
public final class InstallmentCodec {
    private InstallmentCodec() { }
    public static JSONArray encode(List<InstallmentPlan> plans) throws JSONException {
        JSONArray array=new JSONArray();
        for(InstallmentPlan p:plans) array.put(new JSONObject().put("id",p.id).put("name",p.name).put("account",p.account).put("category",p.category).put("firstDate",p.firstDate).put("totalCents",p.totalCents).put("count",p.count));
        return array;
    }
    public static List<InstallmentPlan> decode(JSONArray array) throws JSONException {
        if(array.length()>100000) throw new JSONException("Arquivo muito grande");
        List<InstallmentPlan> plans=new ArrayList<>();
        for(int i=0;i<array.length();i++) {
            JSONObject o=array.getJSONObject(i); InstallmentPlan p=new InstallmentPlan();
            p.id=o.getString("id"); p.name=o.getString("name"); p.account=o.getString("account"); p.category=o.getString("category"); p.firstDate=o.getString("firstDate");
            String total=o.get("totalCents").toString(),count=o.get("count").toString();
            if(!total.matches("[0-9]{1,11}") || !count.matches("[0-9]{1,3}")) throw new JSONException("Parcelamento inválido");
            p.totalCents=Long.parseLong(total); p.count=Integer.parseInt(count); plans.add(p);
        }
        return plans;
    }
}
