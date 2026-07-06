package com.lucas.financeflow;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lucas.financeflow.adapter.LancamentoAdapter;
import com.lucas.financeflow.data.model.Lancamento;
import com.lucas.financeflow.data.repository.FinanceiroRepository;

public class LancamentosActivity extends AppCompatActivity {

    private RecyclerView recyclerLancamentos;
    private EditText edtBuscaLancamento;
    private Button btnVoltarDashboard;

    private LancamentoAdapter adapter;
    private FinanceiroRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lancamentos);

        repository = new FinanceiroRepository(this);

        recyclerLancamentos = findViewById(R.id.recyclerLancamentos);
        edtBuscaLancamento = findViewById(R.id.edtBuscaLancamento);
        btnVoltarDashboard = findViewById(R.id.btnVoltarDashboard);

        configurarRecyclerView();
        observarLancamentos();

        btnVoltarDashboard.setOnClickListener(v -> finish());
    }

    private void configurarRecyclerView() {
        adapter = new LancamentoAdapter(new LancamentoAdapter.OnLancamentoClickListener() {
            @Override
            public void onEditar(Lancamento lancamento) {
                 Toast.makeText(
                        LancamentosActivity.this,
                        "Editar: " + lancamento.descricao,
                        Toast.LENGTH_SHORT
                ).show();
            }

            @Override
            public void onExcluir(Lancamento lancamento) {
                confirmarExclusao(lancamento);
            }
        });

        recyclerLancamentos.setLayoutManager(new LinearLayoutManager(this));

        recyclerLancamentos.setAdapter(adapter);
    }

    private void observarLancamentos() {
        repository.listarTodos().observe(this, lancamentos -> {
            adapter.setLancamentos(lancamentos);
        });
    }

    private void confirmarExclusao(Lancamento lancamento) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir lançamento")
                .setMessage("Deseja excluir \"" + lancamento.descricao + "\"?")
                .setPositiveButton("Excluir", (dialog, which) -> {
                    repository.deletar(lancamento);

                    Toast.makeText(
                            LancamentosActivity.this,
                            "Lançamento excluído",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}