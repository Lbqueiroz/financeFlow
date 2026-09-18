package com.lucas.financeflow;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.*;
import com.lucas.financeflow.data.model.Lancamento;
import com.lucas.financeflow.data.repository.FinanceiroRepository;
import java.util.*;

public class AddLancamentoActivity extends BaseActivity {
    private EditText descricao, valor, categoria, conta, pessoa;
    private Spinner tipo;
    private Button salvar, data;
    private String dataIso;
    private Lancamento original;
    private boolean carregado;
    private FinanceiroRepository repository;
    private SaveViewModel saveModel;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        repository = new FinanceiroRepository(this);
        saveModel = new androidx.lifecycle.ViewModelProvider(this).get(SaveViewModel.class);
        int id = getIntent().getIntExtra("id", 0);
        LinearLayout body = tela(id == 0 ? "Novo lançamento" : "Editar lançamento", "Organize os detalhes da sua movimentação.", true);
        descricao = campo(body, "Descrição", "Ex.: supermercado", R.id.form_descricao);
        descricao.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        valor = campo(body, "Valor (R$)", "0,00", R.id.form_valor);
        valor.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        rotulo(body, "Tipo");
        tipo = seletor(body, new String[]{"Entrada", "Saída"}, R.id.form_tipo);
        categoria = sugestoes(body, "Categoria", new String[]{"Salário", "Renda extra", "Moradia", "Alimentação", "Transporte", "Saúde", "Lazer", "Conta fixa", "Cartão", "Terceiros", "Outros"}, R.id.form_categoria);
        conta = sugestoes(body, "Conta", new String[]{"INTER", "Crédito INTER", "NUBANK", "Crédito NUBANK", "SANTANDER", "CAIXA", "SHOPEE PAY", "MERCADO PAGO", "Dinheiro", "Outros"}, R.id.form_conta);
        pessoa = sugestoes(body, "Origem / destino (opcional)", new String[]{"Eu", "Mãe", "Pai", "Amor", "Luiz", "Shopee"}, R.id.form_pessoa);
        dataIso = state == null ? FinanceUtils.hoje() : state.getString("data", FinanceUtils.hoje());
        data = botao(body, "Data: " + FinanceUtils.dataVisivel(dataIso), v -> escolherData());
        salvar = botao(body, "Salvar lançamento", v -> salvar());
        botao(body, "Cancelar", v -> finish());
        saveModel.estado.observe(this, status -> {
            salvar.setEnabled(status != 1 && (id == 0 || original != null));
            if (status == 2) { Toast.makeText(this, "Lançamento salvo", Toast.LENGTH_SHORT).show(); finish(); }
            if (status == 3) {
                Toast.makeText(this, "Não foi possível salvar. Tente novamente.", Toast.LENGTH_LONG).show();
                saveModel.estado.setValue(0);
            }
        });
        if (id != 0) {
            salvar.setEnabled(false);
            repository.porId(id).observe(this, item -> {
                if (carregado) return;
                carregado = true;
                if (item == null) { Toast.makeText(this, "Lançamento não encontrado", Toast.LENGTH_LONG).show(); finish(); return; }
                original = item;
                if (state == null) {
                    descricao.setText(item.descricao);
                    valor.setText(String.format(FinanceUtils.BR, "%.2f", item.valor));
                    tipo.setSelection("ENTRADA".equals(item.tipo) ? 0 : 1);
                    categoria.setText(item.categoria); conta.setText(item.conta); pessoa.setText(item.origemDestino);
                    dataIso = item.data;
                    data.setText("Data: " + FinanceUtils.dataVisivel(dataIso));
                }
                salvar.setEnabled(!Integer.valueOf(1).equals(saveModel.estado.getValue()));
            });
        }
    }

    private void escolherData() {
        Calendar cal = Calendar.getInstance();
        try {
            String[] parts = dataIso.split("-");
            cal.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
        } catch (RuntimeException ignored) { }
        new DatePickerDialog(this, (picker, year, month, day) -> {
            dataIso = String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, day);
            data.setText("Data: " + FinanceUtils.dataVisivel(dataIso));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private boolean obrigatorio(EditText campo) {
        if (!campo.getText().toString().trim().isEmpty()) return true;
        campo.setError("Preencha este campo"); campo.requestFocus(); return false;
    }

    private void salvar() {
        if (!obrigatorio(descricao) || !obrigatorio(categoria) || !obrigatorio(conta)) return;
        double quantia;
        try { quantia = FinanceUtils.parseValor(valor.getText().toString()); }
        catch (IllegalArgumentException ex) { valor.setError("Informe de R$ 0,01 a R$ 999.999.999,99, com até 2 casas decimais"); valor.requestFocus(); return; }
        Lancamento item = new Lancamento(descricao.getText().toString().trim(), quantia,
                tipo.getSelectedItemPosition() == 0 ? "ENTRADA" : "SAIDA",
                categoria.getText().toString().trim(), dataIso, "CELULAR", "LOCAL",
                pessoa.getText().toString().trim(), conta.getText().toString().trim());
        if (original != null) item.id = original.id;
        saveModel.salvar(item);
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("data", dataIso);
        super.onSaveInstanceState(out);
    }
}

