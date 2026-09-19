package com.lucas.financeflow;

import android.os.Bundle;
import android.widget.*;
import com.lucas.financeflow.data.model.Lancamento;
import com.lucas.financeflow.data.repository.FinanceiroRepository;
import java.util.*;
import java.text.SimpleDateFormat;

public abstract class AgrupamentoActivity extends BaseActivity {
    protected abstract boolean porConta();
    private final Calendar mes = Calendar.getInstance();
    private List<Lancamento> itens = new ArrayList<>();
    private LinearLayout lista;
    private TextView periodo;
    private List<com.lucas.financeflow.data.model.Cadastro> cadastros = new ArrayList<>();
    private List<com.lucas.financeflow.data.model.PlanItem> plans = new ArrayList<>();
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) mes.setTimeInMillis(state.getLong("mes"));
        LinearLayout body = tela(porConta() ? "Suas contas" : "Saídas", porConta() ? "Saldo acumulado de cada conta." : "Saídas por categoria\nVeja para onde seu dinheiro foi", true);
        if (porConta()) botao(body, "Cadastrar contas e origens", v -> startActivity(new android.content.Intent(this, CadastrosActivity.class)));
        if (porConta()) secundario(body, "Transferir entre contas", v -> startActivity(new android.content.Intent(this, PlanningActivity.class).putExtra("mode","TRANSFER")));
        else secundario(body, "Orçamentos por categoria", v -> startActivity(new android.content.Intent(this, PlanningActivity.class).putExtra("mode","BUDGET")));
        if (!porConta()) {
            periodo = texto(body, "", 20);
            LinearLayout controls = new LinearLayout(this); body.addView(controls);
            Button prev = secundario(controls, "‹ Anterior", v -> { mes.add(Calendar.MONTH, -1); atualizar(); });
            Button next = secundario(controls, "Próximo ›", v -> { mes.add(Calendar.MONTH, 1); atualizar(); });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1); params.setMargins(0, 0, dp(6), 0); prev.setLayoutParams(params);
            next.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        }
        lista = new LinearLayout(this); lista.setOrientation(LinearLayout.VERTICAL); body.addView(lista);
        if (porConta()) new FinanceiroRepository(this).cadastros().observe(this, dados -> { cadastros = dados; atualizar(); });
        if (porConta()) new com.lucas.financeflow.data.repository.PlanRepository(this).observe().observe(this, dados -> { plans = dados; atualizar(); });
        new FinanceiroRepository(this).listarTodos().observe(this, dados -> { itens = dados; atualizar(); });
        atualizar();
    }
    private void atualizar() {
        lista.removeAllViews();
        String month = new SimpleDateFormat("yyyy-MM", Locale.ROOT).format(mes.getTime());
        if (periodo != null) periodo.setText(new SimpleDateFormat("MMMM 'de' yyyy", FinanceUtils.BR).format(mes.getTime()));
        Map<String, Long> grupos = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (porConta()) for (com.lucas.financeflow.data.model.Cadastro cadastro : cadastros) {
            if ("CONTA".equals(cadastro.tipo)) grupos.put(cadastro.nome, 0L);
        }
        for (Lancamento item : itens) {
            boolean receita = "ENTRADA".equals(item.tipo);
            if (!porConta() && (receita || item.data == null || !item.data.startsWith(month))) continue;
            String key = porConta() ? item.conta : item.categoria;
            if (key == null || key.isEmpty()) key = "Outros";
            long valor = FinanceUtils.centavos(item.valor) * (porConta() && !receita ? -1 : 1);
            grupos.put(key, grupos.getOrDefault(key, 0L) + valor);
        }
        if (porConta()) Planning.adjustAccounts(grupos,plans);
        if (grupos.isEmpty()) texto(card(lista), porConta() ? "Suas contas aparecerão aqui quando você adicionar lançamentos." : "Nenhuma despesa neste mês.", 16);
        for (Map.Entry<String, Long> grupo : grupos.entrySet()) {
            LinearLayout panel = card(lista);
            texto(panel, grupo.getKey(), 16);
            TextView amount = texto(panel, FinanceUtils.moeda(grupo.getValue()), 26);
            amount.setTypeface(null, android.graphics.Typeface.BOLD);
            amount.setTextColor(android.graphics.Color.rgb(23,107,83));
        }
    }
    @Override protected void onSaveInstanceState(Bundle out) { out.putLong("mes", mes.getTimeInMillis()); super.onSaveInstanceState(out); }
}
