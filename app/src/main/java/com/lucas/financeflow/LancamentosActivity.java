package com.lucas.financeflow;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.*;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.*;
import com.lucas.financeflow.adapter.LancamentoAdapter;
import com.lucas.financeflow.data.model.Lancamento;
import com.lucas.financeflow.data.repository.FinanceiroRepository;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

public class LancamentosActivity extends BaseActivity {
    @Override protected int tabAtual() { return R.id.nav_lancamentos; }
    private List<Lancamento> todos = new ArrayList<>(), visiveis = new ArrayList<>();
    private LancamentoAdapter adapter;
    private FinanceiroRepository repository;
    private EditText busca;
    private Spinner tipo;
    private TextView resumo;
    private String mes;
    private Button exportar;
    private Button filtroData;
    private DateRange intervalo;
    private final ActivityResultLauncher<String> arquivo = registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"), this::exportar);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        mes = getIntent().getStringExtra("mes");
        if (state != null) {
            mes = state.getString("mes");
            if (state.containsKey("inicio")) intervalo = new DateRange(state.getString("inicio"), state.getString("fim"));
        }
        repository = new FinanceiroRepository(this);
        LinearLayout body = tela("Lançamentos", "Seu histórico de entradas e saídas.", false);
        busca = campo(body, "Busca", "Nome, categoria, conta ou origem", R.id.lista_busca);
        tipo = seletor(body, new String[]{"Todos os tipos", "Entradas", "Saídas"}, R.id.lista_tipo);
        tipo.setContentDescription("Filtrar por tipo");
        filtroData = secundario(body, "", v -> escolherPeriodo()); filtroData.setId(R.id.filtro_data);
        atualizarPeriodo();
        resumo = texto(body, "Carregando…", 16);
        RecyclerView recycler = new RecyclerView(this); recycler.setId(R.id.lista_itens);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LancamentoAdapter(new LancamentoAdapter.OnLancamentoClickListener() {
            public void onEditar(Lancamento item) { startActivity(new Intent(LancamentosActivity.this, AddLancamentoActivity.class).putExtra("id", item.id)); }
            public void onExcluir(Lancamento item) { confirmarExclusao(item); }
        });
        recycler.setAdapter(adapter);
        body.addView(recycler, new LinearLayout.LayoutParams(-1, 0, 1));
        exportar = secundario(body, "Exportar lista em CSV", v -> arquivo.launch("financeflow-" + FinanceUtils.hoje() + ".csv"));
        exportar.setEnabled(false);
        botao(body, "+ Novo lançamento", v -> startActivity(new Intent(this, AddLancamentoActivity.class)));
        busca.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { filtrar(); }
            public void afterTextChanged(Editable text) { }
        });
        tipo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int position, long id) { filtrar(); }
            public void onNothingSelected(AdapterView<?> p) { }
        });
        repository.listarTodos().observe(this, items -> { todos = items; filtrar(); });
    }
    private void filtrar() {
        String query = FinanceUtils.normalizar(busca.getText().toString().trim());
        List<Lancamento> filtrados = new ArrayList<>(); long total = 0;
        for (Lancamento item : todos) {
            if (intervalo != null) { if (!intervalo.contem(item.data)) continue; }
            else if (mes != null && (item.data == null || !item.data.startsWith(mes))) continue;
            boolean entrada = "ENTRADA".equals(item.tipo);
            if (tipo.getSelectedItemPosition() == 1 && !entrada || tipo.getSelectedItemPosition() == 2 && entrada) continue;
            if (!FinanceUtils.normalizar(item.descricao + " " + item.categoria + " " + item.conta + " " + item.origemDestino).contains(query)) continue;
            filtrados.add(item); total += FinanceUtils.centavos(item.valor) * (entrada ? 1 : -1);
        }
        visiveis = filtrados; adapter.setLancamentos(filtrados);
        resumo.setText(filtrados.isEmpty() ? "Nenhum lançamento encontrado." : filtrados.size() + " lançamento(s) · Saldo " + FinanceUtils.moeda(total));
        if (exportar != null) exportar.setEnabled(!filtrados.isEmpty());
    }
    private void atualizarPeriodo() {
        if (intervalo != null) filtroData.setText(FinanceUtils.dataVisivel(intervalo.inicio) + (intervalo.inicio.equals(intervalo.fim) ? "" : " a " + FinanceUtils.dataVisivel(intervalo.fim)));
        else filtroData.setText(mes == null ? "Filtrar por data ou período" : "Período: " + mes.substring(5) + "/" + mes.substring(0, 4));
    }
    private void escolherPeriodo() {
        LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(dp(24), 0, dp(24), 0);
        texto(panel, "Para um único dia, preencha só a data inicial. Para um período, preencha as duas datas.", 15);
        EditText de = campo(panel, "Data inicial", "18/09/26", R.id.filtro_inicio);
        EditText ate = campo(panel, "Data final (opcional)", "30/09/26", R.id.filtro_fim);
        de.setInputType(android.text.InputType.TYPE_CLASS_DATETIME | android.text.InputType.TYPE_DATETIME_VARIATION_DATE);
        ate.setInputType(android.text.InputType.TYPE_CLASS_DATETIME | android.text.InputType.TYPE_DATETIME_VARIATION_DATE);
        if (intervalo != null) { de.setText(FinanceUtils.dataVisivel(intervalo.inicio)); ate.setText(FinanceUtils.dataVisivel(intervalo.fim)); }
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Pesquisar por data").setView(panel)
                .setNegativeButton("Cancelar", null).setNeutralButton("Limpar filtro", (d, w) -> {
                    intervalo = null; mes = null; atualizarPeriodo(); filtrar();
                }).setPositiveButton("Aplicar", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(-1).setOnClickListener(v -> {
            try { DateRange.iso(de.getText().toString()); }
            catch (IllegalArgumentException ex) { de.setError(ex.getMessage()); return; }
            try { intervalo = new DateRange(de.getText().toString(), ate.getText().toString()); }
            catch (IllegalArgumentException ex) { ate.setError(ex.getMessage()); return; }
            mes = null; atualizarPeriodo(); filtrar(); dialog.dismiss();
        }));
        dialog.show();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("mes", mes);
        if (intervalo != null) { out.putString("inicio", FinanceUtils.dataVisivel(intervalo.inicio)); out.putString("fim", FinanceUtils.dataVisivel(intervalo.fim)); }
        super.onSaveInstanceState(out);
    }
    private void confirmarExclusao(Lancamento item) {
        new AlertDialog.Builder(this).setTitle("Excluir lançamento?")
                .setMessage("\"" + item.descricao + "\" será removido do seu histórico.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (dialog, which) -> repository.excluir(item, ok -> {
                    if (!isDestroyed()) Toast.makeText(this, ok ? "Lançamento excluído" : "Não foi possível excluir. Tente novamente.", Toast.LENGTH_LONG).show();
                })).show();
    }
    private void exportar(Uri uri) {
        if (uri == null) return;
        List<Lancamento> copia = new ArrayList<>(visiveis);
        java.util.concurrent.ExecutorService worker = Executors.newSingleThreadExecutor();
        worker.execute(() -> {
            boolean ok = false;
            try (OutputStream stream = getContentResolver().openOutputStream(uri, "wt")) {
                if (stream == null) throw new IOException("Arquivo indisponível");
                try (Writer writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
                    writer.write("\uFEFFData;Nome;Tipo;Categoria;Conta;Origem/Destino;Valor\r\n");
                    for (Lancamento item : copia) {
                        writer.write(FinanceUtils.csv(FinanceUtils.dataVisivel(item.data)) + ";" + FinanceUtils.csv(item.descricao) + ";" + FinanceUtils.csv(item.tipo) + ";" + FinanceUtils.csv(item.categoria) + ";" + FinanceUtils.csv(item.conta) + ";" + FinanceUtils.csv(item.origemDestino) + ";" + String.format(FinanceUtils.BR, "%.2f", item.valor) + "\r\n");
                    }
                }
                ok = true;
            } catch (IOException | RuntimeException e) { /* Keep original data; report failure below. */ }
            boolean sucesso = ok;
            runOnUiThread(() -> { if (!isDestroyed()) Toast.makeText(this, sucesso ? "CSV exportado" : "Não foi possível exportar o arquivo", Toast.LENGTH_LONG).show(); });
            worker.shutdown();
        });
    }
}

