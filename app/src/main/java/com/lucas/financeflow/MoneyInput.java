package com.lucas.financeflow;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import java.math.BigDecimal;
import java.text.NumberFormat;

public final class MoneyInput {
    private MoneyInput() { }
    public static String format(String text) {
        String digits = text.replaceAll("[^0-9]", "").replaceFirst("^0+(?!$)", "");
        if (digits.isEmpty()) digits = "0";
        if (digits.length() > 11) throw new IllegalArgumentException("Valor acima do limite");
        NumberFormat format = NumberFormat.getNumberInstance(FinanceUtils.BR);
        format.setMinimumFractionDigits(2); format.setMaximumFractionDigits(2);
        return format.format(new BigDecimal(digits).movePointLeft(2));
    }
    public static void attach(EditText field) {
        field.setKeyListener(android.text.method.DigitsKeyListener.getInstance("0123456789.,"));
        field.setRawInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        field.addTextChangedListener(new TextWatcher() {
            boolean changing;
            String previous = "0,00";
            public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            public void onTextChanged(CharSequence text, int start, int before, int count) { }
            public void afterTextChanged(Editable text) {
                if (changing) return;
                changing = true;
                String formatted;
                try { formatted = format(text.toString()); }
                catch (IllegalArgumentException e) { formatted = previous; field.setError("Máximo: 999.999.999,99"); }
                if (!formatted.equals(text.toString())) field.setText(formatted);
                field.setSelection(field.length());
                previous = formatted;
                changing = false;
            }
        });
    }
}
