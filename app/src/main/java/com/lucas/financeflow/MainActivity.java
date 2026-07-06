package com.lucas.financeflow;

import android.os.Bundle;
import android.widget.TextView;
import android.content.Intent;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.lucas.financeflow.ui.dashboard.DashboardViewModel;



public class MainActivity extends AppCompatActivity {

    private TextView txtEntradas, txtSaidas, txtSaldo;

    private Button btnAdicionar;
    private double totalEntradas = 0;
    private double totalSaidas = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtEntradas = findViewById(R.id.txtEntradas);
        txtSaidas = findViewById(R.id.txtSaidas);
        txtSaldo = findViewById(R.id.txtSaldo);
        btnAdicionar = findViewById(R.id.btnAdicionar);

        btnAdicionar.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddLancamentoActivity.class);
            startActivity(intent);
        });

        DashboardViewModel viewModel = new ViewModelProvider(this)
            .get(DashboardViewModel.class);

        viewModel.getTotalEntradas().observe(this, valor -> {
            totalEntradas = valor != null ? valor : 0.0;
            atualizarDashboard();
        });

        viewModel.getTotalSaidas().observe(this, valor -> {
            totalSaidas = valor != null ? valor : 0.0;
            atualizarDashboard();
        });
    }

    private void atualizarDashboard() {
        double saldo = totalEntradas - totalSaidas;

        txtEntradas.setText("Entradas: R$ " + String.format("%.2f", totalEntradas));
        txtSaidas.setText("Saídas: R$ " + String.format("%.2f", totalSaidas));
        txtSaldo.setText("Saldo: R$ " + String.format("%.2f", saldo));
    }
}