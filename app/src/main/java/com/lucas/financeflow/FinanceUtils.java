package com.lucas.financeflow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class FinanceUtils {
    public static final Locale BR = new Locale("pt", "BR");
    private FinanceUtils() {}

    public static double parseValor(String texto) {
        String value = texto.trim();
        if (value.matches("[0-9]{1,3}(\\.[0-9]{3})+,[0-9]{1,2}")) value = value.replace(".", "");
        if (!value.matches("[0-9]+([,.][0-9]{1,2})?")) throw new IllegalArgumentException("Valor inválido");
        BigDecimal decimal = new BigDecimal(value.replace(',', '.'));
        if (decimal.signum() <= 0 || decimal.compareTo(new BigDecimal("999999999.99")) > 0)
            throw new IllegalArgumentException("Valor fora do limite");
        return decimal.doubleValue();
    }

    public static long centavos(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    public static String moeda(long centavos) {
        return NumberFormat.getCurrencyInstance(BR).format(BigDecimal.valueOf(centavos, 2));
    }

    public static String hoje() { return new SimpleDateFormat("yyyy-MM-dd", BR).format(new Date()); }

    public static String dataVisivel(String iso) {
        if (iso == null || !iso.matches("\\d{4}-\\d{2}-\\d{2}")) return iso == null ? "Sem data" : iso;
        return iso.substring(8) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4);
    }

    public static String normalizar(String texto) {
        return Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(BR);
    }

    public static String csv(String value) {
        String safe = value == null ? "" : value;
        if (safe.matches("^[\\s]*[=+@-].*")) safe = "'" + safe;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
