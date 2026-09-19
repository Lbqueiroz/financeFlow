package com.lucas.financeflow;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.*;
import com.lucas.financeflow.data.model.Lancamento;
import com.lucas.financeflow.data.repository.FinanceiroRepository;
import java.util.*;

public class AddLancamentoActivity extends BaseActivity {
    private EditText descricao, valor;
    private Spinner tipo, categoria, conta, pessoa;
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
        descricao = campo(body, "Nome", "Ex.: supermercado", R.id.form_descricao);
        descricao.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        valor = campo(body, "Valor (R$)", "0,00", R.id.form_valor);
        MoneyInput.attach(valor);
        rotulo(body, "Tipo");
        tipo = seletor(body, new String[]{"Entrada", "Saída"}, R.id.form_tipo);
        categoria = opcoes(body, "Categoria", com.lucas.financeflow.wearlink.CategoryCatalog.all(), R.id.form_categoria);
        conta = opcoes(body, "Conta", new String[]{"Selecione uma conta"}, R.id.form_conta);
        secundario(body, "+ Cadastrar conta", v -> cadastrar("CONTA", nome -> selecionar(conta, nome)));
        pessoa = opcoes(body, "Origem / destino (opcional)", new String[]{"Não informado"}, R.id.form_pessoa);
        conta.setSaveEnabled(false); pessoa.setSaveEnabled(false); categoria.setSaveEnabled(false);
        secundario(body, "+ Cadastrar origem / destino", v -> cadastrar("ORIGEM", nome -> selecionar(pessoa, nome)));
        repository.cadastros().observe(this, itens -> {
            for (com.lucas.financeflow.data.model.Cadastro item : itens) {
                Spinner spinner = "CONTA".equals(item.tipo) ? conta : pessoa;
                ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
                if (posicao(adapter, item.nome) < 0) adapter.add(item.nome);
            }
        });
        if (state != null) {
            selecionar(categoria, state.getString("categoria"));
            selecionar(conta, state.getString("conta"));
            selecionar(pessoa, state.getString("pessoa"));
        }
        dataIso = state == null ? FinanceUtils.hoje() : state.getString("data", FinanceUtils.hoje());
        data = secundario(body, "Data: " + FinanceUtils.dataVisivel(dataIso), v -> escolherData());
        salvar = botao(body, "Salvar lançamento", v -> salvar());
        secundario(body, "Cancelar", v -> finish());
        saveModel.estado.observe(this, status -> {
            salvar.setEnabled((status == 0 || status == 3) && (id == 0 || original != null));
            if (status == 2) {
                LinearLayout done=tela("Lançamento salvo", "Você pode desfazer se salvou por engano.", true);
                botao(done,"Concluir",v -> finish());
                secundario(done,"Desfazer",v -> {v.setEnabled(false); saveModel.undo(ok -> {if(isDestroyed()) return; if(ok) {Toast.makeText(this,"Alteração desfeita",Toast.LENGTH_SHORT).show(); finish();} else Toast.makeText(this,"Não foi possível desfazer. Tente novamente.",Toast.LENGTH_LONG).show();});});
            }
            if (status == 5) finish();
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
                saveModel.previous(item);
                if (state == null) {
                    descricao.setText(item.descricao);
                    valor.setText(String.format(FinanceUtils.BR, "%.2f", item.valor));
                    tipo.setSelection("ENTRADA".equals(item.tipo) ? 0 : 1);
                    selecionar(categoria, item.categoria); selecionar(conta, item.conta); selecionar(pessoa, item.origemDestino);
                    dataIso = item.data;
                    data.setText("Data: " + FinanceUtils.dataVisivel(dataIso));
                }
                salvar.setEnabled(!Integer.valueOf(1).equals(saveModel.estado.getValue()));
            });
        }
    }

    @Override protected int tabAtual() { return 0; }

    private void selecionar(Spinner spinner, String value) {
        if (value == null || value.isEmpty()) return;
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
        int position = posicao(adapter, value);
        if (position < 0) { adapter.add(value); position = adapter.getPosition(value); }
        spinner.setSelection(position);
    }

    private int posicao(ArrayAdapter<String> adapter, String value) {
        for (int i = 0; i < adapter.getCount(); i++) if (value.equalsIgnoreCase(adapter.getItem(i))) return i;
        return -1;
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
        if (!obrigatorio(descricao)) return;
        if (conta.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Cadastre e selecione uma conta", Toast.LENGTH_LONG).show(); return;
        }
        double quantia;
        try { quantia = FinanceUtils.parseValor(valor.getText().toString()); }
        catch (IllegalArgumentException ex) { valor.setError("Informe de R$ 0,01 a R$ 999.999.999,99, com até 2 casas decimais"); valor.requestFocus(); return; }
        Lancamento item = new Lancamento(descricao.getText().toString().trim(), quantia,
                tipo.getSelectedItemPosition() == 0 ? "ENTRADA" : "SAIDA",
                categoria.getSelectedItem().toString(), dataIso, "CELULAR", "LOCAL",
                pessoa.getSelectedItemPosition() == 0 ? "" : pessoa.getSelectedItem().toString(), conta.getSelectedItem().toString());
        if (original != null) item.id = original.id;
        saveModel.salvar(item);
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("data", dataIso);
        out.putString("categoria", categoria.getSelectedItem().toString());
        out.putString("conta", conta.getSelectedItem().toString());
        out.putString("pessoa", pessoa.getSelectedItem().toString());
        super.onSaveInstanceState(out);
    }
}


