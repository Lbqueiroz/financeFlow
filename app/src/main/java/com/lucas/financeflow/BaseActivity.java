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
    protected int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }

    protected LinearLayout tela(String titulo, String subtitulo, boolean rolar) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(16), dp(20), dp(16));
        setContentView(root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(dp(20) + bars.left, dp(16) + bars.top, dp(20) + bars.right, dp(16) + bars.bottom);
            return insets;
        });
        LinearLayout body = root;
        if (rolar) {
            ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
            root.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
            body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(body);
        }
        TextView heading = texto(body, titulo, 30); heading.setTypeface(null, android.graphics.Typeface.BOLD);
        ViewCompat.setAccessibilityHeading(heading, true);
        texto(body, subtitulo, 15).setPadding(0, dp(4), 0, dp(20));
        return body;
    }

    protected TextView texto(LinearLayout body, String text, int size) {
        TextView view = new TextView(this); view.setText(text); view.setTextSize(size);
        view.setPadding(0, dp(6), 0, dp(6)); body.addView(view, new LinearLayout.LayoutParams(-1, -2)); return view;
    }
    protected void rotulo(LinearLayout body, String text) { texto(body, text, 14); }
    protected EditText campo(LinearLayout body, String label, String hint, int id) {
        rotulo(body, label);
        EditText field = new EditText(this); field.setId(id); field.setHint(hint); field.setContentDescription(label);
        field.setSingleLine(true); field.setMinHeight(dp(52)); body.addView(field, new LinearLayout.LayoutParams(-1, -2)); return field;
    }
    protected EditText sugestoes(LinearLayout body, String label, String[] options, int id) {
        rotulo(body, label);
        AutoCompleteTextView field = new AutoCompleteTextView(this); field.setId(id); field.setHint("Escolha ou digite");
        field.setContentDescription(label); field.setSingleLine(true); field.setMinHeight(dp(52)); field.setThreshold(0);
        field.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, options));
        field.setOnClickListener(v -> field.showDropDown());
        body.addView(field, new LinearLayout.LayoutParams(-1, -2)); return field;
    }
    protected Spinner seletor(LinearLayout body, String[] options, int id) {
        Spinner spinner = new Spinner(this); spinner.setId(id);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); spinner.setAdapter(adapter);
        body.addView(spinner, new LinearLayout.LayoutParams(-1, dp(52))); return spinner;
    }
    protected Button botao(LinearLayout body, String text, View.OnClickListener action) {
        MaterialButton button = new MaterialButton(this); button.setText(text); button.setMinHeight(dp(52));
        button.setOnClickListener(action); body.addView(button, new LinearLayout.LayoutParams(-1, -2)); return button;
    }
}
