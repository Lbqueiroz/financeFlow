package com.lucas.financeflow.wearlink;

/** One catalog for the phone, watch, budgets and recurring entries. */
public final class CategoryCatalog {
    private CategoryCatalog() { }
    public static String[] all() {
        return new String[]{"Salário", "Renda extra", "Moradia", "Alimentação", "Transporte", "Saúde", "Lazer", "Conta fixa", "Cartão", "Terceiros", "Outros"};
    }
}
