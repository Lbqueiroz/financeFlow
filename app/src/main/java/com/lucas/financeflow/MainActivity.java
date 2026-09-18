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
    private TextView periodo, saldo, entradas, saidas;
    private LinearLayout ultimos;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) mes.setTimeInMillis(state.getLong("mes"));
        LinearLayout body = tela("Resumos mensais", "Uma visão simples do seu mês.", true);
        LinearLayout marca = new LinearLayout(this);
        marca.setGravity(android.view.Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.financeflow_mark);
        logo.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        marca.addView(logo, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView nome = new TextView(this);
        nome.setText(R.string.app_name); nome.setTextSize(22);
        nome.setTextColor(android.graphics.Color.rgb(23, 107, 83));
        nome.setTypeface(null, android.graphics.Typeface.BOLD);
        marca.addView(nome);
        LinearLayout.LayoutParams marcaParams = new LinearLayout.LayoutParams(-1, -2);
        marcaParams.setMargins(0, 0, 0, dp(12));
        body.addView(marca, 0, marcaParams);
        periodo = texto(body, "", 20);
        LinearLayout navegacao = new LinearLayout(this); body.addView(navegacao);
        Button anterior = secundario(navegacao, "‹ Anterior", v -> mudarMes(-1));
        Button proximo = secundario(navegacao, "Próximo ›", v -> mudarMes(1));
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, -2, 1); left.setMargins(0, 0, dp(6), 0); anterior.setLayoutParams(left);
        proximo.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        anterior.setContentDescription("Mês anterior"); proximo.setContentDescription("Próximo mês");
        LinearLayout destaque = card(body);
        android.graphics.drawable.GradientDrawable fundo = new android.graphics.drawable.GradientDrawable();
        fundo.setColor(android.graphics.Color.rgb(23,107,83)); fundo.setCornerRadius(dp(20)); destaque.setBackground(fundo);
        texto(destaque, "SALDO DO MÊS", 12).setTextColor(android.graphics.Color.rgb(212,239,224));
        saldo = texto(destaque, "", 34); saldo.setTextColor(android.graphics.Color.WHITE); saldo.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout totais = card(body);
        entradas = texto(totais, "", 17); entradas.setTextColor(android.graphics.Color.rgb(23,107,83));
        saidas = texto(totais, "", 17); saidas.setTextColor(android.graphics.Color.rgb(169,63,53));
        botao(body, "+ Novo lançamento", v -> startActivity(new Intent(this, AddLancamentoActivity.class)));
        secundario(body, "Ver lançamentos do mês", v -> startActivity(new Intent(this, LancamentosActivity.class).putExtra("mes", chaveMes())));
        rotulo(body, "ÚLTIMOS LANÇAMENTOS");
        ultimos = new LinearLayout(this); ultimos.setOrientation(LinearLayout.VERTICAL); body.addView(ultimos);
        new FinanceiroRepository(this).listarTodos().observe(this, lista -> { itens = lista; atualizar(); });
        atualizar();
    }
    private String chaveMes() { return new SimpleDateFormat("yyyy-MM", Locale.ROOT).format(mes.getTime()); }
    private void mudarMes(int delta) { mes.add(Calendar.MONTH, delta); atualizar(); }
    private void atualizar() {
        periodo.setText(new SimpleDateFormat("MMMM 'de' yyyy", FinanceUtils.BR).format(mes.getTime()));
        long entrada = 0, saida = 0;
        List<Lancamento> mensal = new ArrayList<>();
        for (Lancamento item : itens) {
            if (item.data == null || !item.data.startsWith(chaveMes())) continue;
            mensal.add(item);
            if ("ENTRADA".equals(item.tipo)) entrada += FinanceUtils.centavos(item.valor);
            else saida += FinanceUtils.centavos(item.valor);
        }
        saldo.setText(FinanceUtils.moeda(entrada - saida));
        entradas.setText("↗ Entradas     " + FinanceUtils.moeda(entrada));
        saidas.setText("↙ Saídas         " + FinanceUtils.moeda(saida));
        ultimos.removeAllViews();
        if (mensal.isEmpty()) texto(card(ultimos), "Nenhum lançamento neste mês.\nToque em + Novo lançamento para começar.", 16);
        for (int i = 0; i < Math.min(3, mensal.size()); i++) {
            Lancamento item = mensal.get(i);
            secundario(ultimos, item.descricao + " · " + ("ENTRADA".equals(item.tipo) ? "+ " : "− ") + FinanceUtils.moeda(FinanceUtils.centavos(item.valor)) + "\n" + FinanceUtils.dataVisivel(item.data),
                    v -> startActivity(new Intent(this, AddLancamentoActivity.class).putExtra("id", item.id)));
        }
    }
    @Override protected void onSaveInstanceState(Bundle out) { out.putLong("mes", mes.getTimeInMillis()); super.onSaveInstanceState(out); }
}
