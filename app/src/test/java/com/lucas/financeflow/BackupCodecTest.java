package com.lucas.financeflow;

import com.lucas.financeflow.data.model.Lancamento;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class BackupCodecTest {
    @Test public void preservesUnusedRegistrationsAndReadsLegacyBackups() throws Exception {
        String text = BackupCodec.encode(Collections.emptyList(), Arrays.asList(
                new com.lucas.financeflow.data.model.Cadastro("CONTA", "Reserva"),
                new com.lucas.financeflow.data.model.Cadastro("ORIGEM", "Freelance")));
        BackupCodec.Documento copy = BackupCodec.decodeCompleto(text);
        assertEquals(2, copy.cadastros.size()); assertTrue(copy.itens.isEmpty());
        assertEquals("Reserva", copy.cadastros.get(0).nome);
        assertTrue(BackupCodec.decodeCompleto("{\"app\":\"FinanceFlow\",\"version\":1,\"lancamentos\":[]}").cadastros.isEmpty());
    }
    @Test public void roundTripPreservesAllUserFieldsAndLargeAmounts() throws Exception {
        Lancamento item = new Lancamento("Compra \"especial\"", 999999999.99, "SAIDA", "Alimentação", "2026-09-18", "CELULAR", "LOCAL", "Mãe", "INTER");
        Lancamento copy = BackupCodec.decode(BackupCodec.encode(Collections.singletonList(item))).get(0);
        assertEquals(item.descricao, copy.descricao); assertEquals(item.valor, copy.valor, 0);
        assertEquals(item.tipo, copy.tipo); assertEquals(item.categoria, copy.categoria);
        assertEquals(item.data, copy.data); assertEquals(item.conta, copy.conta); assertEquals(item.origemDestino, copy.origemDestino);
    }
    @Test public void rejectsInvalidDateBeforeRestore() throws Exception {
        Lancamento item = new Lancamento("Teste", 10, "SAIDA", "Outros", "2026-02-30", "CELULAR", "LOCAL", "Eu", "INTER");
        try { BackupCodec.decode(BackupCodec.encode(Collections.singletonList(item))); fail(); }
        catch (org.json.JSONException expected) { }
    }
    @Test public void rejectsUnknownFormat() throws Exception {
        try { BackupCodec.decode("{\"app\":\"Outro\",\"version\":1,\"lancamentos\":[]}"); fail(); }
        catch (org.json.JSONException expected) { }
    }
}
