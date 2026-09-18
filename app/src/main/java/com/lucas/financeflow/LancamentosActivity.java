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
    private List<Lancamento> todos = new ArrayList<>(), visiveis = new ArrayList<>();
    private LancamentoAdapter adapter;
    private FinanceiroRepository repository;
    private EditText busca;
    private Spinner tipo;
    private TextView resumo;
    private String mes;
    private Button exportar;
    private final ActivityResultLauncher<String> arquivo = registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"), this::exportar);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        mes = getIntent().getStringExtra("mes");
        repository = new FinanceiroRepository(this);
        LinearLayout body = tela("Lançamentos", mes == null ? "Todo o histórico" : "Movimentações de " + mes.substring(5) + "/" + mes.substring(0, 4), false);
        busca = campo(body, "Busca", "Descrição, categoria, conta ou pessoa", R.id.lista_busca);
        tipo = seletor(body, new String[]{"Todos os tipos", "Entradas", "Saídas"}, R.id.lista_tipo);
        tipo.setContentDescription("Filtrar por tipo");
        resumo = texto(body, "Carregando…", 16);
        RecyclerView recycler = new RecyclerView(this); recycler.setId(R.id.lista_itens);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LancamentoAdapter(new LancamentoAdapter.OnLancamentoClickListener() {
            public void onEditar(Lancamento item) { startActivity(new Intent(LancamentosActivity.this, AddLancamentoActivity.class).putExtra("id", item.id)); }
            public void onExcluir(Lancamento item) { confirmarExclusao(item); }
        });
        recycler.setAdapter(adapter);
        body.addView(recycler, new LinearLayout.LayoutParams(-1, 0, 1));
        exportar = botao(body, "Exportar lista em CSV", v -> arquivo.launch("financeflow-" + FinanceUtils.hoje() + ".csv"));
        exportar.setEnabled(false);
        botao(body, "+ Novo lançamento", v -> startActivity(new Intent(this, AddLancamentoActivity.class)));
        botao(body, "Voltar", v -> finish());
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
            if (mes != null && (item.data == null || !item.data.startsWith(mes))) continue;
            boolean entrada = "ENTRADA".equals(item.tipo);
            if (tipo.getSelectedItemPosition() == 1 && !entrada || tipo.getSelectedItemPosition() == 2 && entrada) continue;
            if (!FinanceUtils.normalizar(item.descricao + " " + item.categoria + " " + item.conta + " " + item.origemDestino).contains(query)) continue;
            filtrados.add(item); total += FinanceUtils.centavos(item.valor) * (entrada ? 1 : -1);
        }
        visiveis = filtrados; adapter.setLancamentos(filtrados);
        resumo.setText(filtrados.isEmpty() ? "Nenhum lançamento encontrado." : filtrados.size() + " lançamento(s) · Saldo " + FinanceUtils.moeda(total));
        if (exportar != null) exportar.setEnabled(!filtrados.isEmpty());
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
                    writer.write("\uFEFFData;Descrição;Tipo;Categoria;Conta;Origem/Destino;Valor\r\n");
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

