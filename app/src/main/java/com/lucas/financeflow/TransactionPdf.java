package com.lucas.financeflow;

import android.content.Context;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import com.lucas.financeflow.data.model.Lancamento;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** A4 report with measured wrapping, repeated table headings and complete multi-page rows. */
public final class TransactionPdf {
    private static final int WIDTH = 595, HEIGHT = 842, LEFT = 36, RIGHT = 559, BOTTOM = 784;
    private static final int GREEN = Color.rgb(23,107,83), INK = Color.rgb(25,56,46), MUTED = Color.rgb(93,114,105);
    private static final float LINE = 13;
    private TransactionPdf() { }

    private static Paint paint(float size, int color, boolean bold) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setTextSize(size); p.setColor(color);
        p.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL)); return p;
    }
    static List<String> wrap(String value, Paint p, float width) {
        List<String> result = new ArrayList<>();
        String remaining = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        if (remaining.isEmpty()) { result.add("-"); return result; }
        while (!remaining.isEmpty()) {
            int count = p.breakText(remaining, true, width, null);
            if (count <= 0) count = Character.charCount(remaining.codePointAt(0));
            if (count < remaining.length() && Character.isHighSurrogate(remaining.charAt(count - 1))) count--;
            if (count < remaining.length()) {
                int space = remaining.lastIndexOf(' ', count);
                if (space > 0) count = space;
            }
            result.add(remaining.substring(0, count).trim()); remaining = remaining.substring(count).trim();
        }
        return result;
    }
    private static class Row {
        Lancamento item;
        List<String> lines = new ArrayList<>();
        int nameLines;
        Row(Lancamento item) {
            this.item = item;
            lines.addAll(wrap(item.descricao, paint(10, INK, true), 302)); nameLines = lines.size();
            lines.addAll(wrap("Categoria: " + safe(item.categoria) + "  |  Conta: " + safe(item.conta), paint(9, MUTED, false), 302));
            if (item.origemDestino != null && !item.origemDestino.isEmpty()) lines.addAll(wrap("Origem / destino: " + item.origemDestino, paint(9, MUTED, false), 302));
        }
    }
    private static class Part {
        Row row; int start, count; float y;
        Part(Row row, int start, int count, float y) { this.row = row; this.start = start; this.count = count; this.y = y; }
    }
    private static String safe(String text) { return text == null || text.isEmpty() ? "-" : text; }
    private static void text(Canvas c, String text, float x, float y, float size, int color, boolean bold) { c.drawText(text, x, y, paint(size, color, bold)); }

    public static int write(Context context, List<Lancamento> items, String period, String filters, OutputStream output) throws IOException {
        PdfDocument document = new PdfDocument();
        final PdfDocument.Page[] current = new PdfDocument.Page[1];
        try {
            int pages = render(context, items, period, filters, new PageWriter() {
                public Canvas begin(int number) {
                    current[0] = document.startPage(new PdfDocument.PageInfo.Builder(WIDTH, HEIGHT, number).create());
                    return current[0].getCanvas();
                }
                public void end() { document.finishPage(current[0]); current[0] = null; }
            });
            document.writeTo(output);
            return pages;
        } finally {
            if (current[0] != null) document.finishPage(current[0]);
            document.close();
        }
    }
    interface PageWriter { Canvas begin(int number); void end(); }
    static int render(Context context, List<Lancamento> items, String period, String filters, PageWriter writer) {
        long income = 0, expense = 0;
        for (Lancamento item : items) {
            if ("ENTRADA".equals(item.tipo)) income += FinanceUtils.centavos(item.valor);
            else expense += FinanceUtils.centavos(item.valor);
        }
        String compactFilters = filters.length() > 220 ? filters.substring(0, 220) + "…" : filters;
        List<String> filterLines = wrap(compactFilters, paint(9, MUTED, false), RIGHT - LEFT);
        float summaryTop = 140 + filterLines.size() * 12;
        float firstRowsTop = summaryTop + 108;
        List<List<Part>> pages = new ArrayList<>(); pages.add(new ArrayList<>());
        float y = firstRowsTop;
        for (Lancamento item : items) {
            Row row = new Row(item);
            float fullHeight = row.lines.size() * LINE + 20;
            float pageStart = pages.size() == 1 ? firstRowsTop : 116;
            if (y > pageStart && y + fullHeight > BOTTOM) { pages.add(new ArrayList<>()); y = 116; }
            int start = 0;
            while (start < row.lines.size()) {
                int available = (int) ((BOTTOM - y - 20) / LINE);
                if (available < 1) { pages.add(new ArrayList<>()); y = 116; continue; }
                int count = Math.min(available, row.lines.size() - start);
                pages.get(pages.size() - 1).add(new Part(row, start, count, y));
                y += count * LINE + 20; start += count;
                if (start < row.lines.size()) { pages.add(new ArrayList<>()); y = 116; }
            }
        }
        Bitmap logo = BitmapFactory.decodeResource(context.getResources(), R.drawable.financeflow_mark);
        String generated = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", FinanceUtils.BR).format(new Date());
        try {
            for (int i = 0; i < pages.size(); i++) {
                Canvas c = writer.begin(i + 1); c.drawColor(Color.WHITE);
                if (logo != null) c.drawBitmap(logo, null, new Rect(LEFT - 4, 26, LEFT + 38, 68), paint(10, INK, false));
                text(c, "FinanceFlow", LEFT + 42, 53, 20, GREEN, true);
                Paint right = paint(8, MUTED, false); right.setTextAlign(Paint.Align.RIGHT);
                c.drawText("Emitido em " + generated, RIGHT, 48, right);
                float headerY;
                if (i == 0) {
                    text(c, "Relatório de lançamentos", LEFT, 99, 22, INK, true);
                    text(c, period, LEFT, 121, 10, MUTED, false);
                    for (int n = 0; n < filterLines.size(); n++) text(c, filterLines.get(n), LEFT, 139 + n * 12, 9, MUTED, false);
                    summary(c, LEFT, summaryTop, "ENTRADAS", income, GREEN);
                    summary(c, LEFT + 178, summaryTop, "SAÍDAS", expense, Color.rgb(169,63,53));
                    summary(c, LEFT + 356, summaryTop, "SALDO", income - expense, GREEN);
                    text(c, items.size() + " lançamento(s) - valores em reais (BRL)", LEFT, summaryTop + 80, 10, MUTED, false);
                    headerY = firstRowsTop - 24;
                } else {
                    text(c, "Lançamentos - continuação", LEFT, 84, 13, INK, true); headerY = 92;
                }
                c.drawRect(LEFT, headerY, RIGHT, headerY + 23, paint(10, Color.rgb(234,242,238), false));
                text(c, "DATA", LEFT + 7, headerY + 15, 8, GREEN, true);
                text(c, "NOME E DETALHES", 112, headerY + 15, 8, GREEN, true);
                right = paint(8, GREEN, true); right.setTextAlign(Paint.Align.RIGHT); c.drawText("VALOR", RIGHT - 7, headerY + 15, right);
                int index = 0;
                for (Part part : pages.get(i)) {
                    float height = part.count * LINE + 20;
                    if (index++ % 2 == 1) c.drawRect(LEFT, part.y, RIGHT, part.y + height, paint(10, Color.rgb(248,250,249), false));
                    text(c, FinanceUtils.dataVisivel(part.row.item.data), LEFT + 7, part.y + 16, 8, MUTED, false);
                    boolean entrada = "ENTRADA".equals(part.row.item.tipo);
                    right = paint(10, entrada ? GREEN : Color.rgb(169,63,53), true); right.setTextAlign(Paint.Align.RIGHT);
                    String amount = (entrada ? "+ " : "- ") + FinanceUtils.moeda(FinanceUtils.centavos(part.row.item.valor));
                    if (part.start == 0) c.drawText(amount, RIGHT - 7, part.y + 16, right);
                    text(c, part.start == 0 ? (entrada ? "Entrada" : "Saída") : "continuação", LEFT + 7, part.y + 28, 8, MUTED, false);
                    for (int n = 0; n < part.count; n++) {
                        int line = part.start + n; boolean name = line < part.row.nameLines;
                        text(c, part.row.lines.get(line), 112, part.y + 16 + n * LINE, name ? 10 : 9, name ? INK : MUTED, name);
                    }
                    c.drawLine(LEFT, part.y + height, RIGHT, part.y + height, paint(1, Color.rgb(225,233,228), false));
                }
                text(c, "FinanceFlow | Totais referentes aos filtros selecionados", LEFT, 816, 8, MUTED, false);
                right = paint(8, MUTED, false); right.setTextAlign(Paint.Align.RIGHT); c.drawText("Página " + (i + 1) + " de " + pages.size(), RIGHT, 816, right);
                writer.end();
            }
        } finally { if (logo != null) logo.recycle(); }
        return pages.size();
    }
    private static void summary(Canvas c, float x, float y, String label, long cents, int color) {
        c.drawRoundRect(x, y, x + 167, y + 56, 8, 8, paint(10, Color.rgb(244,247,245), false));
        text(c, label, x + 12, y + 19, 8, MUTED, true);
        String amount = FinanceUtils.moeda(cents);
        Paint amountPaint = paint(16, color, true);
        while (amountPaint.measureText(amount) > 143 && amountPaint.getTextSize() > 9) amountPaint.setTextSize(amountPaint.getTextSize() - 1);
        c.drawText(amount, x + 12, y + 41, amountPaint);
    }
}
