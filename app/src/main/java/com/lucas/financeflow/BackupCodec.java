package com.lucas.financeflow;

import com.lucas.financeflow.data.model.Lancamento;
import org.json.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;

/** Versioned, portable backup. Validate the complete file before changing the database. */
public final class BackupCodec {
    private BackupCodec() { }
    public static String encode(List<Lancamento> itens) throws JSONException {
        JSONArray records = new JSONArray();
        for (Lancamento item : itens) {
            JSONObject record = new JSONObject();
            record.put("descricao", item.descricao); record.put("valor", java.math.BigDecimal.valueOf(item.valor).toPlainString());
            record.put("tipo", item.tipo); record.put("categoria", item.categoria);
            record.put("conta", item.conta); record.put("origemDestino", item.origemDestino);
            record.put("data", item.data); records.put(record);
        }
        return new JSONObject().put("app", "FinanceFlow").put("version", 1).put("lancamentos", records).toString(2);
    }
    public static List<Lancamento> decode(String text) throws JSONException {
        JSONObject root = new JSONObject(text);
        if (!"FinanceFlow".equals(root.getString("app")) || root.getInt("version") != 1) throw new JSONException("Formato incompatível");
        JSONArray records = root.getJSONArray("lancamentos");
        if (records.length() > 100000) throw new JSONException("Arquivo muito grande");
        List<Lancamento> itens = new ArrayList<>();
        for (int i = 0; i < records.length(); i++) {
            JSONObject record = records.getJSONObject(i);
            String tipo = record.getString("tipo"), data = record.getString("data");
            if (!tipo.equals("ENTRADA") && !tipo.equals("SAIDA")) throw new JSONException("Tipo inválido");
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT); format.setLenient(false);
            ParsePosition position = new ParsePosition(0);
            if (!data.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}") || format.parse(data, position) == null || position.getIndex() != data.length()) throw new JSONException("Data inválida");
            double valor;
            try { valor = FinanceUtils.parseValor(record.getString("valor")); }
            catch (IllegalArgumentException ex) { throw new JSONException("Valor inválido"); }
            itens.add(new Lancamento(obrigatorio(record, "descricao"), valor, tipo, obrigatorio(record, "categoria"),
                    data, "CELULAR", "LOCAL", record.optString("origemDestino", ""), obrigatorio(record, "conta")));
        }
        return itens;
    }
    private static String obrigatorio(JSONObject record, String key) throws JSONException {
        String value = record.getString(key).trim();
        if (value.isEmpty() || value.length() > 10000) throw new JSONException("Campo inválido: " + key);
        return value;
    }
}
