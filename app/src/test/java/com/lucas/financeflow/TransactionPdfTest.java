package com.lucas.financeflow;

import com.lucas.financeflow.data.model.Lancamento;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import java.util.*;
import java.io.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class TransactionPdfTest {
    @Test public void createsReadableMultipageReportWithLongNames() throws Exception {
        List<Lancamento> items = new ArrayList<>();
        for (int i = 0; i < 35; i++) items.add(new Lancamento(i == 0 ? "Salário de setembro" : "Compra de supermercado e itens para a família - lançamento " + i,
                i == 0 ? 5000 : 125.49, i == 0 ? "ENTRADA" : "SAIDA", i == 0 ? "Salário" : "Alimentação",
                "2026-09-18", "CELULAR", "LOCAL", "Família", "Minha conta principal"));
        items.add(new Lancamento("Texto extenso para verificar a quebra de linhas e de páginas. ".repeat(140), 999999999.99,
                "SAIDA", "Outros", "2026-09-01", "CELULAR", "LOCAL", "Teste de texto longo", "Reserva"));
        File folder = new File("build/previews"); folder.mkdirs();
        int pages = TransactionPdf.render(RuntimeEnvironment.getApplication(), items, "Período: 01/09/2026 a 30/09/2026", "Tipo: Todos os tipos", new TransactionPdf.PageWriter() {
            android.graphics.Bitmap bitmap; int number;
            public android.graphics.Canvas begin(int n) {
                number = n; bitmap = android.graphics.Bitmap.createBitmap(1190, 1684, android.graphics.Bitmap.Config.ARGB_8888);
                android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap); canvas.scale(2, 2); return canvas;
            }
            public void end() {
                try (OutputStream file = new FileOutputStream(new File(folder, "pdf-page-" + number + ".png"))) {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, file);
                } catch (IOException e) { throw new RuntimeException(e); }
                bitmap.recycle();
            }
        });
        assertTrue(pages > 2);
        java.nio.file.Files.writeString(new File(folder, "pdf-page-count.txt").toPath(), Integer.toString(pages));
    }
}
