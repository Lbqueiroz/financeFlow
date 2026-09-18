package com.lucas.financeflow;
public class ContasActivity extends AgrupamentoActivity {
    @Override protected boolean porConta() { return true; }
    @Override protected int tabAtual() { return R.id.nav_contas; }
}
