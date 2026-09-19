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

import java.util.*;
import java.util.concurrent.Executors;

public class LancamentosActivity extends BaseActivity {
    public static final String EXTRA_CONTA = "contaSelecionada";
    private String contaSelecionada;
    @Override protected int tabAtual() { return contaSelecionada == null ? R.id.nav_lancamentos : 0; }
    private List<Lancamento> todos = new ArrayList<>(), visiveis = new ArrayList<>();
    private LancamentoAdapter adapter;
    private FinanceiroRepository repository;
    private EditText busca;
    private Spinner tipo;
    private Spinner filtroConta;
    private String contaFiltro;
    private List<com.lucas.financeflow.data.model.Cadastro> cadastros=new ArrayList<>();
    private TextView resumo;
    private String mes;
    private Button exportar;
    private Button filtroData;
    private Button novo,voltar;
    private DateRange intervalo;
    private final ActivityResultLauncher<String> arquivo = registerForActivityResult(new ActivityResultContracts.CreateDocument("application/pdf"), this::exportar);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        contaSelecionada = getIntent().getStringExtra(EXTRA_CONTA);
        if (contaSelecionada != null && contaSelecionada.trim().isEmpty()) contaSelecionada = null;
        mes = getIntent().getStringExtra("mes");
        if (state != null) {
            mes = state.getString("mes");
            contaFiltro=state.getString("contaFiltro");
            if (state.containsKey("inicio")) intervalo = new DateRange(state.getString("inicio"), state.getString("fim"));
        }
        repository = new FinanceiroRepository(this);
        LinearLayout body = tela(contaSelecionada == null ? "Lançamentos" : contaSelecionada,
                contaSelecionada == null ? "Seu histórico de entradas e saídas." : "Entradas e saídas desta conta. Novos lançamentos já vêm com ela preenchida.", false);
        if (contaSelecionada != null) voltar=secundario(body,"‹ Voltar às contas",v -> finish());
        busca = campo(body, "Busca", "Nome, categoria, conta ou origem", R.id.lista_busca);
        busca.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH | android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI | android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN);
        busca.setOnEditorActionListener((view,action,event) -> {
            if(action!=android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) return false;
            androidx.core.view.WindowCompat.getInsetsController(getWindow(),view).hide(androidx.core.view.WindowInsetsCompat.Type.ime());
            busca.clearFocus(); return true;
        });
        tipo = seletor(body, new String[]{"Todos os tipos", "Entradas", "Saídas"}, R.id.lista_tipo);
        tipo.setContentDescription("Filtrar por tipo");
        if(contaSelecionada==null) {
            filtroConta=seletor(body,new String[]{"Todas as contas"},R.id.filtro_conta);
            filtroConta.setContentDescription("Filtrar por conta"); filtroConta.setSaveEnabled(false);
        }
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
        exportar = secundario(body, "Exportar relatório em PDF", v -> prepararExportacao());
        exportar.setEnabled(false);
        novo=botao(body, "+ Novo lançamento", v -> startActivity(new Intent(this, AddLancamentoActivity.class).putExtra(EXTRA_CONTA,contaEfetiva())));
        busca.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { filtrar(); }
            public void afterTextChanged(Editable text) { }
        });
        tipo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int position, long id) { filtrar(); }
            public void onNothingSelected(AdapterView<?> p) { }
        });
        if(filtroConta!=null) {
            atualizarContas();
            filtroConta.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent,View view,int position,long id) {
                    contaFiltro=position==0?null:parent.getItemAtPosition(position).toString(); filtrar();
                }
                public void onNothingSelected(AdapterView<?> parent) { }
            });
            repository.cadastros().observe(this,items -> {cadastros=items; atualizarContas();});
        }
        repository.listarTodos().observe(this, items -> { todos = items; atualizarContas(); filtrar(); });
    }
    private String contaEfetiva() {return contaSelecionada!=null?contaSelecionada:contaFiltro;}
    @Override protected void onKeyboardVisibilityChanged(boolean visible) {
        showHeading(!visible);
        for(View view:new View[]{tipo,filtroConta,filtroData,exportar,novo,voltar}) if(view!=null) view.setVisibility(visible?View.GONE:View.VISIBLE);
    }
    private void atualizarContas() {
        if(filtroConta==null) return;
        Set<String> names=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for(com.lucas.financeflow.data.model.Cadastro item:cadastros) if("CONTA".equals(item.tipo)) names.add(item.nome);
        for(Lancamento item:todos) if(item.conta!=null && !item.conta.isEmpty()) names.add(item.conta);
        if(contaFiltro!=null) names.add(contaFiltro);
        List<String> values=new ArrayList<>(); values.add("Todas as contas"); values.addAll(names);
        ArrayAdapter<String> options=new ArrayAdapter<>(this,R.layout.select_value,values); options.setDropDownViewResource(R.layout.select_option);
        filtroConta.setAdapter(options); int selected=0;
        if(contaFiltro!=null) for(int i=1;i<values.size();i++) if(contaFiltro.equalsIgnoreCase(values.get(i))) selected=i;
        filtroConta.setSelection(selected);
    }
    private void filtrar() {
        String query = FinanceUtils.normalizar(busca.getText().toString().trim());
        List<Lancamento> filtrados = new ArrayList<>(); long total = 0;
        for (Lancamento item : todos) {
            if (contaEfetiva() != null && !contaEfetiva().equalsIgnoreCase(item.conta)) continue;
            if (intervalo != null) { if (!intervalo.contem(item.data)) continue; }
            else if (mes != null && (item.data == null || !item.data.startsWith(mes))) continue;
            boolean entrada = "ENTRADA".equals(item.tipo);
            if (tipo.getSelectedItemPosition() == 1 && !entrada || tipo.getSelectedItemPosition() == 2 && entrada) continue;
            if (!FinanceUtils.normalizar(item.descricao + " " + item.categoria + " " + item.conta + " " + item.origemDestino).contains(query)) continue;
            filtrados.add(item); total += FinanceUtils.centavos(item.valor) * (entrada ? 1 : -1);
        }
        visiveis = filtrados; adapter.setLancamentos(filtrados);
        resumo.setText(filtrados.isEmpty() ? "Nenhum lançamento encontrado." : filtrados.size() + " lançamento(s) · " + (contaSelecionada == null ? "Saldo " : "Resultado dos lançamentos ") + FinanceUtils.moeda(total));
        if (exportar != null) exportar.setEnabled(!filtrados.isEmpty());
    }
    private void atualizarPeriodo() {
        if (intervalo != null) filtroData.setText(FinanceUtils.dataVisivel(intervalo.inicio) + (intervalo.inicio.equals(intervalo.fim) ? "" : " a " + FinanceUtils.dataVisivel(intervalo.fim)));
        else filtroData.setText(mes == null ? "Filtrar por data ou período" : "Período: " + mes.substring(5) + "/" + mes.substring(0, 4));
    }
    private void escolherPeriodo() {
        LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(dp(24), 0, dp(24), 0);
        texto(panel, "Escolha um dia no calendário. Para um intervalo, escolha também a data final.", 15);
        String[] dates={intervalo==null?null:intervalo.inicio,intervalo==null || intervalo.inicio.equals(intervalo.fim)?null:intervalo.fim};
        Button de=secundario(panel,"",null); de.setId(R.id.filtro_inicio);
        Button ate=secundario(panel,"",null); ate.setId(R.id.filtro_fim);
        Runnable labels=() -> {de.setText(dates[0]==null?"Selecionar data inicial":"De: "+FinanceUtils.dataVisivel(dates[0])); ate.setText(dates[1]==null?"Adicionar data final (opcional)":"Até: "+FinanceUtils.dataVisivel(dates[1]));};
        labels.run();
        de.setOnClickListener(v -> escolherDia(dates[0],iso -> {dates[0]=iso; labels.run();}));
        ate.setOnClickListener(v -> escolherDia(dates[1]==null?dates[0]:dates[1],iso -> {dates[1]=iso; labels.run();}));
        secundario(panel,"Usar só a data inicial",v -> {dates[1]=null; labels.run();});
        TextView error=texto(panel,"",14); error.setTextColor(0xffa93f35);
        error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Pesquisar por data").setView(panel)
                .setNegativeButton("Cancelar", null).setNeutralButton("Limpar filtro", (d, w) -> {
                    intervalo = null; mes = null; atualizarPeriodo(); filtrar();
                }).setPositiveButton("Aplicar", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(-1).setOnClickListener(v -> {
            if(dates[0]==null) {error.setText("Escolha a data inicial no calendário"); return;}
            try { intervalo = new DateRange(FinanceUtils.dataVisivel(dates[0]),dates[1]==null?"":FinanceUtils.dataVisivel(dates[1])); }
            catch (IllegalArgumentException ex) { error.setText(ex.getMessage()); return; }
            mes = null; atualizarPeriodo(); filtrar(); dialog.dismiss();
        }));
        dialog.show();
    }
    private void escolherDia(String initial,java.util.function.Consumer<String> chosen) {
        Calendar cal=Calendar.getInstance(); if(initial!=null) cal.setTime(Planning.strictDate(initial));
        new android.app.DatePickerDialog(this,(picker,year,month,day) -> chosen.accept(String.format(Locale.ROOT,"%04d-%02d-%02d",year,month+1,day)),cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH)).show();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("mes", mes);
        out.putString("contaFiltro",contaFiltro);
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
    public static class PdfExportState extends androidx.lifecycle.ViewModel {
        List<Lancamento> items;
        String period, filters;
    }
    private void prepararExportacao() {
        PdfExportState state = new androidx.lifecycle.ViewModelProvider(this).get(PdfExportState.class);
        state.items = new ArrayList<>(visiveis);
        state.period = intervalo != null ? "Período: " + FinanceUtils.dataVisivel(intervalo.inicio) + " a " + FinanceUtils.dataVisivel(intervalo.fim)
                : mes != null ? "Período: " + mes.substring(5) + "/" + mes.substring(0, 4) : "Período: todo o histórico";
        state.filters = (contaEfetiva() == null ? "" : "Conta: " + contaEfetiva() + " | ") + "Tipo: " + tipo.getSelectedItem() + (busca.getText().toString().trim().isEmpty() ? "" : " | Busca: " + busca.getText().toString().trim());
        arquivo.launch("financeflow-" + FinanceUtils.hoje() + ".pdf");
    }
    private void exportar(Uri uri) {
        PdfExportState state = new androidx.lifecycle.ViewModelProvider(this).get(PdfExportState.class);
        if (uri == null) { state.items = null; return; }
        if (state.items == null) {
            Toast.makeText(this, "A exportação foi interrompida. Toque em exportar novamente.", Toast.LENGTH_LONG).show(); return;
        }
        List<Lancamento> copia = state.items;
        String period = state.period, filters = state.filters; state.items = null;
        java.util.concurrent.ExecutorService worker = Executors.newSingleThreadExecutor();
        worker.execute(() -> {
            boolean ok = false;
            try (OutputStream stream = getContentResolver().openOutputStream(uri, "wt")) {
                if (stream == null) throw new IOException("Arquivo indisponível");
                TransactionPdf.write(getApplicationContext(), copia, period, filters, stream);
                ok = true;
            } catch (IOException | RuntimeException e) { /* Original records remain intact. */ }
            boolean sucesso = ok;
            runOnUiThread(() -> { if (!isDestroyed()) Toast.makeText(this, sucesso ? "PDF exportado" : "Não foi possível exportar o PDF. Tente novamente.", Toast.LENGTH_LONG).show(); });
            worker.shutdown();
        });
    }
}
