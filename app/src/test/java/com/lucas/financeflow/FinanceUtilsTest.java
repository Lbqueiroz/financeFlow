package com.lucas.financeflow;

import org.junit.Test;
import static org.junit.Assert.*;

public class FinanceUtilsTest {
    @Test public void aceitaValoresBrasileiros() {
        assertEquals(1234.56, FinanceUtils.parseValor("1.234,56"), 0);
        assertEquals(12.30, FinanceUtils.parseValor("12,30"), 0);
        assertEquals(12.30, FinanceUtils.parseValor("12.30"), 0);
    }
    @Test public void rejeitaValoresInvalidos() {
        for (String input : new String[]{"0", "-1", "NaN", "Infinity", "1e5", "1,234", "1.234", "", "1000000000"}) {
            try { FinanceUtils.parseValor(input); fail(input); } catch (IllegalArgumentException expected) { }
        }
    }
    @Test public void somaSemErroDePontoFlutuante() {
        assertEquals(30, FinanceUtils.centavos(0.1) + FinanceUtils.centavos(0.2));
    }
    @Test public void buscaIgnoraAcentos() { assertEquals("salario", FinanceUtils.normalizar("SALÁRIO")); }
    @Test public void csvEscapaAspasEFormulas() {
        assertEquals("\"a\"\"b\"", FinanceUtils.csv("a\"b"));
        assertEquals("\"'=1+1\"", FinanceUtils.csv("=1+1"));
    }
}
