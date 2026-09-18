package com.lucas.financeflow;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Locale;

public final class DateRange {
    public final String inicio, fim;
    public DateRange(String de, String ate) {
        inicio = iso(de);
        fim = ate.trim().isEmpty() ? inicio : iso(ate);
        if (inicio.compareTo(fim) > 0) throw new IllegalArgumentException("A data final deve ser igual ou posterior à inicial");
    }
    public boolean contem(String data) { return data != null && data.compareTo(inicio) >= 0 && data.compareTo(fim) <= 0; }
    public static String iso(String input) {
        String text = input.trim();
        if (!text.matches("[0-9]{2}/[0-9]{2}/([0-9]{2}|[0-9]{4})")) throw new IllegalArgumentException("Use dd/MM/aa ou dd/MM/aaaa");
        if (text.length() == 8) text = text.substring(0, 6) + "20" + text.substring(6);
        SimpleDateFormat parser = new SimpleDateFormat("dd/MM/yyyy", Locale.ROOT); parser.setLenient(false);
        ParsePosition pos = new ParsePosition(0);
        if (parser.parse(text, pos) == null || pos.getIndex() != text.length()) throw new IllegalArgumentException("Data inexistente. Confira dia, mês e ano");
        return text.substring(6) + "-" + text.substring(3, 5) + "-" + text.substring(0, 2);
    }
}
