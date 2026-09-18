package com.lucas.financeflow;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;

/** Shared spacing, accessible touch targets and system-bar insets for all screens. */
public abstract class BaseActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        getDelegate().setLocalNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(state);
    }
    protected int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    protected int tabAtual() { return R.id.nav_inicio; }
    protected void cadastrar(String tipo, java.util.function.Consumer<String> onSaved) {
        boolean conta = com.lucas.financeflow.data.model.Cadastro.CONTA.equals(tipo);
        LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(24), 0, dp(24), dp(8));
        EditText nome = campo(panel, "Nome", conta ? "Ex.: Minha conta" : "Ex.: Trabalho", View.generateViewId());
        nome.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        nome.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(80)});
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(conta ? "Cadastrar conta" : "Cadastrar origem / destino")
                .setView(panel).setNegativeButton("Cancelar", null).setPositiveButton("Cadastrar", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(-1).setOnClickListener(v -> {
            String value = nome.getText().toString().trim().replaceAll("\\s+", " ");
            if (value.isEmpty()) { nome.setError("Informe um nome"); return; }
            dialog.getButton(-1).setEnabled(false);
            new com.lucas.financeflow.data.repository.FinanceiroRepository(this).cadastrar(tipo, value, ok -> {
                if (isDestroyed()) return;
                if (ok) { onSaved.accept(value); dialog.dismiss(); }
                else { dialog.getButton(-1).setEnabled(true); nome.setError("Não foi possível cadastrar. Tente novamente."); }
            });
        }));
        dialog.show();
    }

    protected LinearLayout tela(String titulo, String subtitulo, boolean rolar) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        setContentView(root);
        androidx.core.view.WindowCompat.getInsetsController(getWindow(), root).setAppearanceLightNavigationBars(true);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(12));
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout body = content;
        if (rolar) {
            ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
            content.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
            body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(body);
        }
        TextView heading = texto(body, titulo, 30); heading.setTypeface(null, android.graphics.Typeface.BOLD);
        ViewCompat.setAccessibilityHeading(heading, true);
        texto(body, subtitulo, 15).setPadding(0, dp(4), 0, dp(20));
        if (tabAtual() != 0) {
            com.google.android.material.bottomnavigation.BottomNavigationView nav = new com.google.android.material.bottomnavigation.BottomNavigationView(this);
            nav.setBackgroundColor(android.graphics.Color.WHITE);
            nav.setItemActiveIndicatorColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.rgb(219,239,227)));
            android.content.res.ColorStateList navColors = new android.content.res.ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{android.graphics.Color.rgb(23,107,83), android.graphics.Color.rgb(93,114,105)});
            nav.setItemIconTintList(navColors); nav.setItemTextColor(navColors);
            nav.inflateMenu(R.menu.navigation);
            nav.setLabelVisibilityMode(com.google.android.material.navigation.NavigationBarView.LABEL_VISIBILITY_LABELED);
            nav.setSelectedItemId(tabAtual());
            root.addView(nav, new LinearLayout.LayoutParams(-1, -2));
            nav.setOnItemSelectedListener(item -> {
                if (item.getItemId() == tabAtual()) return true;
                Class<?> destino = MainActivity.class;
                if (item.getItemId() == R.id.nav_lancamentos) destino = LancamentosActivity.class;
                else if (item.getItemId() == R.id.nav_contas) destino = ContasActivity.class;
                else if (item.getItemId() == R.id.nav_categorias) destino = CategoriasActivity.class;
                else if (item.getItemId() == R.id.nav_backup) destino = BackupActivity.class;
                startActivity(new android.content.Intent(this, destino).addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP));
                if (!(this instanceof MainActivity)) finish();
                return false;
            });
        }
        return body;
    }

    protected TextView texto(LinearLayout body, String text, int size) {
        TextView view = new TextView(this); view.setText(text); view.setTextSize(size);
        view.setPadding(0, dp(6), 0, dp(6)); body.addView(view, new LinearLayout.LayoutParams(-1, -2)); return view;
    }
    protected void rotulo(LinearLayout body, String text) {
        TextView label = texto(body, text, 14);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        label.setPadding(0, dp(16), 0, dp(8));
    }
    protected LinearLayout card(LinearLayout parent) {
        LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundResource(R.drawable.card_background);
        panel.setPadding(dp(18), dp(12), dp(18), dp(16));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(8), 0, dp(12)); parent.addView(panel, params);
        return panel;
    }
    protected EditText campo(LinearLayout body, String label, String hint, int id) {
        rotulo(body, label);
        EditText field = new EditText(this); field.setId(id); field.setHint(hint); field.setContentDescription(label);
        field.setSingleLine(true); field.setMinHeight(dp(56));
        field.setBackgroundResource(R.drawable.card_background);
        field.setPadding(dp(16), dp(12), dp(16), dp(12));
        field.setTextSize(16);
        body.addView(field, new LinearLayout.LayoutParams(-1, -2)); return field;
    }
    protected Spinner opcoes(LinearLayout body, String label, String[] options, int id) {
        rotulo(body, label);
        Spinner field = seletor(body, options, id); field.setContentDescription(label); return field;
    }
    protected Spinner seletor(LinearLayout body, String[] options, int id) {
        Spinner spinner = new Spinner(this, Spinner.MODE_DROPDOWN); spinner.setId(id);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.select_value, new java.util.ArrayList<>(java.util.Arrays.asList(options)));
        adapter.setDropDownViewResource(R.layout.select_option); spinner.setAdapter(adapter);
        spinner.setBackgroundResource(R.drawable.card_background);
        spinner.setPopupBackgroundResource(R.drawable.card_background);
        body.addView(spinner, new LinearLayout.LayoutParams(-1, -2)); return spinner;
    }
    protected Button botao(LinearLayout body, String text, View.OnClickListener action) {
        MaterialButton button = new MaterialButton(this); button.setText(text); button.setMinHeight(dp(52));
        button.setCornerRadius(dp(14)); button.setAllCaps(false); button.setTextSize(15);
        button.setOnClickListener(action); body.addView(button, new LinearLayout.LayoutParams(-1, -2)); return button;
    }
    protected Button secundario(LinearLayout body, String text, View.OnClickListener action) {
        MaterialButton button = (MaterialButton) botao(body, text, action);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
        button.setTextColor(android.graphics.Color.rgb(23,107,83));
        button.setStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.rgb(218,230,222)));
        button.setStrokeWidth(dp(1)); return button;
    }
}
