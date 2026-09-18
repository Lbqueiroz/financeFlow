package com.lucas.financeflow;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import com.lucas.financeflow.data.model.Lancamento;
import com.lucas.financeflow.data.repository.FinanceiroRepository;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends BaseActivity {
    private final Calendar mes = Calendar.getInstance();
    private List<Lancamento> itens = new ArrayList<>();
    private TextView periodo, saldo, entradas, saidas, categorias, contas;
    private LinearLayout ultimos;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) mes.setTimeInMillis(state.getLong("mes"));
        LinearLayout body = tela("FinanceFlow", "Seu dinheiro, com clareza. Tudo salvo neste aparelho.", true);
        periodo = texto(body, "", 22);
        LinearLayout navegacao = new LinearLayout(this); body.addView(navegacao);
        Button anterior = new Button(this); anterior.setText("‹ Anterior"); anterior.setContentDescription("Mês anterior");
        Button proximo = new Button(this); proximo.setText("Próximo ›"); proximo.setContentDescription("Próximo mês");
        navegacao.addView(anterior, new LinearLayout.LayoutParams(0, dp(52), 1));
        navegacao.addView(proximo, new LinearLayout.LayoutParams(0, dp(52), 1));
        anterior.setOnClickListener(v -> mudarMes(-1)); proximo.setOnClickListener(v -> mudarMes(1));
        saldo = texto(body, "", 30); saldo.setTypeface(null, android.graphics.Typeface.BOLD);
        entradas = texto(body, "", 18); saidas = texto(body, "", 18);
        botao(body, "+ Novo lançamento", v -> startActivity(new Intent(this, AddLancamentoActivity.class)));
        botao(body, "Ver lançamentos do mês", v -> startActivity(new Intent(this, LancamentosActivity.class).putExtra("mes", chaveMes())));
        botao(body, "Todo o histórico / exportar", v -> startActivity(new Intent(this, LancamentosActivity.class)));
        botao(body, "Backup e restauração", v -> startActivity(new Intent(this, BackupActivity.class)));
        texto(body, "Despesas por categoria", 22);
        categorias = texto(body, "", 16);
        texto(body, "Saldo por conta · todo o histórico", 22);
        contas = texto(body, "", 16);
        texto(body, "Últimos lançamentos do mês", 22);
        ultimos = new LinearLayout(this); ultimos.setOrientation(LinearLayout.VERTICAL); body.addView(ultimos);
        new FinanceiroRepository(this).listarTodos().observe(this, lista -> { itens = lista; atualizar(); });
        atualizar();
    }
    private String chaveMes() { return new SimpleDateFormat("yyyy-MM", Locale.ROOT).format(mes.getTime()); }
    private void mudarMes(int delta) { mes.add(Calendar.MONTH, delta); atualizar(); }
    private void atualizar() {
        periodo.setText(new SimpleDateFormat("MMMM 'de' yyyy", FinanceUtils.BR).format(mes.getTime()));
        long entrada = 0, saida = 0;
        Map<String, Long> porCategoria = new TreeMap<>(), porConta = new TreeMap<>();
        List<Lancamento> mensal = new ArrayList<>();
        for (Lancamento item : itens) {
            long valor = FinanceUtils.centavos(item.valor);
            boolean receita = "ENTRADA".equals(item.tipo);
            String conta = item.conta == null ? "Sem conta" : item.conta;
            porConta.put(conta, porConta.getOrDefault(conta, 0L) + (receita ? valor : -valor));
            if (item.data == null || !item.data.startsWith(chaveMes())) continue;
            mensal.add(item);
            if (receita) entrada += valor;
            else { saida += valor; String cat = item.categoria == null ? "Outros" : item.categoria; porCategoria.put(cat, porCategoria.getOrDefault(cat, 0L) + valor); }
        }
        saldo.setText("Saldo do mês\n" + FinanceUtils.moeda(entrada - saida));
        entradas.setText("Entradas  " + FinanceUtils.moeda(entrada));
        saidas.setText("Saídas  " + FinanceUtils.moeda(saida));
        categorias.setText(resumo(porCategoria, "Nenhuma despesa neste mês."));
        contas.setText(resumo(porConta, "Cadastre seu primeiro lançamento para começar."));
        ultimos.removeAllViews();
        if (mensal.isEmpty()) texto(ultimos, "Seu mês começa aqui. Adicione uma entrada ou saída.", 16);
        for (int i = 0; i < Math.min(5, mensal.size()); i++) {
            Lancamento item = mensal.get(i);
            botao(ultimos, item.descricao + " · " + ("ENTRADA".equals(item.tipo) ? "+ " : "− ") + FinanceUtils.moeda(FinanceUtils.centavos(item.valor)) + "\n" + FinanceUtils.dataVisivel(item.data),
                    v -> startActivity(new Intent(this, AddLancamentoActivity.class).putExtra("id", item.id)));
        }
    }
    private String resumo(Map<String, Long> dados, String vazio) {
        if (dados.isEmpty()) return vazio;
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, Long> item : dados.entrySet()) text.append(item.getKey()).append("   ").append(FinanceUtils.moeda(item.getValue())).append('\n');
        return text.toString().trim();
    }
    @Override protected void onSaveInstanceState(Bundle out) { out.putLong("mes", mes.getTimeInMillis()); super.onSaveInstanceState(out); }
}
