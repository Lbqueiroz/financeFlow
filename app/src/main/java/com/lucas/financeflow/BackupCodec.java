package com.lucas.financeflow;

import com.lucas.financeflow.data.model.Lancamento;
import com.lucas.financeflow.data.model.Cadastro;
import org.json.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;

/** Versioned, portable backup. Validate the complete file before changing the database. */
public final class BackupCodec {
    private BackupCodec() { }
    public static String encode(List<Lancamento> itens) throws JSONException {
        return encode(itens, Collections.emptyList());
    }
    public static String encode(List<Lancamento> itens, List<Cadastro> cadastros) throws JSONException {
        JSONArray records = new JSONArray();
        for (Lancamento item : itens) {
            JSONObject record = new JSONObject();
            record.put("descricao", item.descricao); record.put("valor", java.math.BigDecimal.valueOf(item.valor).toPlainString());
            record.put("tipo", item.tipo); record.put("categoria", item.categoria);
            record.put("conta", item.conta); record.put("origemDestino", item.origemDestino);
            record.put("data", item.data); records.put(record);
        }
        JSONArray names = new JSONArray();
        for (Cadastro item : cadastros) names.put(new JSONObject().put("tipo", item.tipo).put("nome", item.nome));
        return new JSONObject().put("app", "FinanceFlow").put("version", 2).put("lancamentos", records).put("cadastros", names).toString(2);
    }
    public static List<Lancamento> decode(String text) throws JSONException {
        return decodeCompleto(text).itens;
    }
    public static class Documento {
        public final List<Lancamento> itens;
        public final List<Cadastro> cadastros;
        public Documento(List<Lancamento> itens, List<Cadastro> cadastros) { this.itens = itens; this.cadastros = cadastros; }
    }
    public static Documento decodeCompleto(String text) throws JSONException {
        JSONObject root = new JSONObject(text);
        int version = root.getInt("version");
        if (!"FinanceFlow".equals(root.getString("app")) || (version != 1 && version != 2)) throw new JSONException("Formato incompatível");
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
        List<Cadastro> cadastros = new ArrayList<>();
        if (version == 2) {
            JSONArray names = root.getJSONArray("cadastros");
            if (names.length() > 100000) throw new JSONException("Arquivo muito grande");
            for (int i = 0; i < names.length(); i++) {
                JSONObject name = names.getJSONObject(i);
                String tipo = name.getString("tipo");
                if (!Cadastro.CONTA.equals(tipo) && !Cadastro.ORIGEM.equals(tipo)) throw new JSONException("Cadastro inválido");
                cadastros.add(new Cadastro(tipo, obrigatorio(name, "nome")));
            }
        }
        return new Documento(itens, cadastros);
    }
    private static String obrigatorio(JSONObject record, String key) throws JSONException {
        String value = record.getString(key).trim();
        if (value.isEmpty() || value.length() > 10000) throw new JSONException("Campo inválido: " + key);
        return value;
    }
}
