package com.lucas.financeflow;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.lucas.financeflow.data.model.Lancamento;
import com.lucas.financeflow.data.repository.FinanceiroRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
public class AddLancamentoActivity extends AppCompatActivity {

    private EditText edtDescricao, edtValor;
    private Spinner spinnerTipo, spinnerCategoria, spinnerOrigemDestino, spinnerConta;
    private Button btnSalvar;

    String origem = "CELULAR";
    String syncStatus = "PENDENTE";

    private FinanceiroRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_lancamento);

        repository = new FinanceiroRepository(this);

        edtDescricao = findViewById(R.id.edtDescricao);
        edtValor = findViewById(R.id.edtValor);
        spinnerTipo = findViewById(R.id.spinnerTipo);
        spinnerCategoria = findViewById(R.id.spinnerCategoria);
        btnSalvar = findViewById(R.id.btnSalvar);
        spinnerConta = findViewById(R.id.spinnerConta);
        spinnerOrigemDestino = findViewById(R.id.spinnerOrigemDestino);

        configurarSpinners();

        btnSalvar.setOnClickListener(v -> salvarLancamento());
    }

    private void configurarSpinners() {
        String[] tipos = {"ENTRADA", "SAIDA"};
        String[] categorias = {
                "SALARIO",
                "Renda Extra",
                "Conta fixa",
                "Gasto variavél",
                "Cartão",
                "Terceiros",
                "Outros"
        };

        String[] contas = {
                "INTER",
                "Crédito INTER",
                "NUBANK",
                "Crédito NUBANK",
                "SANTANDER",
                "Crédito SANTANDER",
                "CAIXA",
                "SHOPEE PAY",
                "MERCADO PAGO",
                "Crédito Mercado Pago",
                "OUTROS"
        };

        String[] origensDestinos = {
                "Shopee",
                "Mãe",
                "Amor",
                "Luiz",
                "Pai",
                "Eu",
                "OUTRO"
        };

        spinnerTipo.setAdapter(criarAdapter(tipos));
        spinnerCategoria.setAdapter(criarAdapter(categorias));
        spinnerConta.setAdapter(criarAdapter(contas));
        spinnerOrigemDestino.setAdapter(criarAdapter(origensDestinos));
    }

    private ArrayAdapter<String> criarAdapter(String[] itens) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                itens
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        return adapter;
    }
    private void salvarLancamento() {
        String descricao = edtDescricao.getText().toString().trim();
        String valorTexto = edtValor.getText().toString().trim();

        if (descricao.isEmpty() || valorTexto.isEmpty()) {
            Toast.makeText(this, "Preencha a descrição e o valor", Toast.LENGTH_SHORT).show();
            return;
        }

        double valor;

        try {
            valor = Double.parseDouble(valorTexto);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Valor inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        String tipo = spinnerTipo.getSelectedItem().toString();
        String categoria = spinnerCategoria.getSelectedItem().toString();
        String conta = spinnerConta.getSelectedItem().toString();
        String origemDestino = spinnerOrigemDestino.getSelectedItem().toString();

        String dataAtual = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                .format(new Date());

        Lancamento lancamento = new Lancamento(
                descricao,
                valor,
                tipo,
                categoria,
                conta,
                origemDestino,
                dataAtual,
                origem,
                syncStatus
        );

            repository.inserir(lancamento);

            Toast.makeText(this, "Lancamento salvo", Toast.LENGTH_SHORT).show();
            finish();
        }
   }


