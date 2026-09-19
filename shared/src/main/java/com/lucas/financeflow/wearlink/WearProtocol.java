package com.lucas.financeflow.wearlink;

import org.json.*;
import java.text.*;
import java.util.*;

/** Versioned contract shared by phone and watch. Money is always integer cents. */
public final class WearProtocol {
    public static final String SUMMARY = "/financeflow/summary", TX = "/financeflow/tx/", ACK = "/financeflow/ack/", REFRESH = "/financeflow/refresh";
    public static final Locale BR = new Locale("pt", "BR");
    private WearProtocol() { }
    public static String today() { return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date()); }
    public static String month() { return today().substring(0, 7); }
    public static String money(long cents) { return NumberFormat.getCurrencyInstance(BR).format(java.math.BigDecimal.valueOf(cents, 2)); }
    public static String amount(long cents) {
        NumberFormat format=NumberFormat.getNumberInstance(BR); format.setMinimumFractionDigits(2); format.setMaximumFractionDigits(2);
        return format.format(java.math.BigDecimal.valueOf(cents,2));
    }
    public static class Entry {
        public final String id, type, account, category, date;
        public final long cents;
        public Entry(String id, String type, String account, String category, String date, long cents) {
            this.id=id; this.type=type; this.account=account; this.category=category; this.date=date; this.cents=cents;
        }
        public String json() throws JSONException {
            return new JSONObject().put("version",1).put("id",id).put("type",type).put("account",account).put("category",category).put("date",date).put("cents",cents).toString();
        }
    }
    public static Entry parse(String json) throws JSONException {
        if (json == null || json.length() > 4000) throw new JSONException("Lançamento inválido");
        JSONObject obj = new JSONObject(json);
        if (obj.getInt("version") != 1) throw new JSONException("Atualize o FinanceFlow nos dois aparelhos");
        String id = obj.getString("id");
        if (!id.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")) throw new JSONException("Identificador inválido");
        String type = obj.getString("type");
        if (!type.equals("ENTRADA") && !type.equals("SAIDA")) throw new JSONException("Tipo inválido");
        String number = obj.get("cents").toString();
        if (!number.matches("[0-9]{1,11}")) throw new JSONException("Valor inválido");
        long cents = Long.parseLong(number);
        if (cents <= 0 || cents > 99999999999L) throw new JSONException("Valor inválido");
        String date = obj.getString("date");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT); format.setLenient(false);
        ParsePosition pos = new ParsePosition(0);
        if (!date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}") || format.parse(date,pos)==null || pos.getIndex()!=date.length()) throw new JSONException("Data inválida");
        return new Entry(id,type,required(obj,"account"),required(obj,"category"),date,cents);
    }
    private static String required(JSONObject obj, String key) throws JSONException {
        String value = obj.getString(key).trim();
        if (value.isEmpty() || value.length()>120) throw new JSONException("Campo inválido");
        return value;
    }
}
