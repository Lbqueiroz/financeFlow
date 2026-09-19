package com.lucas.financeflow;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import com.lucas.financeflow.data.local.AppDatabase;
import com.lucas.financeflow.data.model.Lancamento;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ScreenTest {
    private AppDatabase testDb;
    @org.junit.Before public void isolatedDatabase() throws Exception {
        testDb = androidx.room.Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase.class)
                .allowMainThreadQueries().setQueryExecutor(Runnable::run).setTransactionExecutor(Runnable::run).build();
        java.lang.reflect.Field instance = AppDatabase.class.getDeclaredField("instance"); instance.setAccessible(true); instance.set(null, testDb);
    }
    @org.junit.After public void closeDatabase() throws Exception {
        testDb.close();
        java.lang.reflect.Field instance = AppDatabase.class.getDeclaredField("instance"); instance.setAccessible(true); instance.set(null, null);
    }
    @Test public void createsAccountAndOriginInFormAndFormatsAmount() throws Exception {
        try (var controller = Robolectric.buildActivity(AddLancamentoActivity.class).setup()) {
            AddLancamentoActivity activity = controller.get();
            button(activity.findViewById(android.R.id.content), "+ Cadastrar conta").performClick();
            androidx.appcompat.app.AlertDialog dialog = ultimoDialogo();
            edit(dialog.findViewById(android.R.id.content)).setText("Minha carteira"); dialog.getButton(-1).performClick();
            aguardar(() -> !dialog.isShowing());
            assertEquals("Minha carteira", ((Spinner) activity.findViewById(R.id.form_conta)).getSelectedItem());
            button(activity.findViewById(android.R.id.content), "+ Cadastrar origem / destino").performClick();
            androidx.appcompat.app.AlertDialog origin = ultimoDialogo();
            edit(origin.findViewById(android.R.id.content)).setText("Meu trabalho"); origin.getButton(-1).performClick();
            aguardar(() -> !origin.isShowing());
            assertEquals("Meu trabalho", ((Spinner) activity.findViewById(R.id.form_pessoa)).getSelectedItem());
            EditText amount = activity.findViewById(R.id.form_valor);
            amount.setText("100000"); assertEquals("1.000,00", amount.getText().toString());
            amount.getText().delete(amount.length() - 1, amount.length()); assertEquals("100,00", amount.getText().toString());
            Bundle state = new Bundle(); controller.saveInstanceState(state);
            try (var restored = Robolectric.buildActivity(AddLancamentoActivity.class).setup(state)) {
                assertEquals("Minha carteira", ((Spinner) restored.get().findViewById(R.id.form_conta)).getSelectedItem());
                assertEquals("Meu trabalho", ((Spinner) restored.get().findViewById(R.id.form_pessoa)).getSelectedItem());
                assertEquals("100,00", ((EditText) restored.get().findViewById(R.id.form_valor)).getText().toString());
            }
            assertEquals(2, testDb.lancamentoDao().snapshotCadastros().size());
        }
    }

    @Test public void dateDialogFiltersInclusivelyAndRejectsImpossibleDate() throws Exception {
        for (String day : new String[]{"2026-07-31", "2026-08-01", "2026-09-18", "2026-09-30", "2026-10-01"}) {
            testDb.lancamentoDao().inserir(new Lancamento("Teste", 1, "ENTRADA", "Outros", day, "CELULAR", "LOCAL", "", "Conta"));
        }
        try (var controller = Robolectric.buildActivity(LancamentosActivity.class).setup()) {
            LancamentosActivity activity = controller.get();
            androidx.recyclerview.widget.RecyclerView recycler = activity.findViewById(R.id.lista_itens);
            aguardar(() -> recycler.getAdapter().getItemCount() == 5);
            activity.findViewById(R.id.filtro_data).performClick();
            androidx.appcompat.app.AlertDialog dialog = ultimoDialogo();
            ((EditText) dialog.findViewById(R.id.filtro_inicio)).setText("01/08/26");
            ((EditText) dialog.findViewById(R.id.filtro_fim)).setText("31/09/26");
            dialog.getButton(-1).performClick(); assertTrue(dialog.isShowing());
            assertNotNull(((EditText) dialog.findViewById(R.id.filtro_fim)).getError());
            ((EditText) dialog.findViewById(R.id.filtro_fim)).setText("30/09/26"); dialog.getButton(-1).performClick();
            assertEquals(3, recycler.getAdapter().getItemCount());
            activity.findViewById(R.id.filtro_data).performClick();
            dialog = ultimoDialogo();
            ((EditText) dialog.findViewById(R.id.filtro_inicio)).setText("18/09/26");
            ((EditText) dialog.findViewById(R.id.filtro_fim)).setText(""); dialog.getButton(-1).performClick();
            assertEquals(1, recycler.getAdapter().getItemCount());
            Bundle state = new Bundle(); controller.saveInstanceState(state);
            try (var restored = Robolectric.buildActivity(LancamentosActivity.class).setup(state)) {
                androidx.recyclerview.widget.RecyclerView list = restored.get().findViewById(R.id.lista_itens);
                aguardar(() -> list.getAdapter().getItemCount() == 1);
                restored.get().findViewById(R.id.filtro_data).performClick();
                (ultimoDialogo()).getButton(-3).performClick();
                aguardar(() -> list.getAdapter().getItemCount() == 5);
            }
        }
    }

    private androidx.appcompat.app.AlertDialog ultimoDialogo() {
        ShadowLooper.idleMainLooper();
        return (androidx.appcompat.app.AlertDialog) org.robolectric.shadows.ShadowDialog.getLatestDialog();
    }
    private EditText edit(View view) {
        if (view instanceof EditText) return (EditText) view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            EditText found = edit(((ViewGroup) view).getChildAt(i)); if (found != null) return found;
        }
        return null;
    }
    @Test public void savesEditsAndFindsTransaction() throws Exception {
        AppDatabase db = AppDatabase.getInstance(RuntimeEnvironment.getApplication());
        CompletableFuture.runAsync(() -> { db.lancamentoDao().limpar(); db.lancamentoDao().cadastrar(new com.lucas.financeflow.data.model.Cadastro("CONTA", "INTER")); }).get();
        try (ActivityController<AddLancamentoActivity> controller = Robolectric.buildActivity(AddLancamentoActivity.class).setup()) {
            AddLancamentoActivity activity = controller.get();
            ((EditText) activity.findViewById(R.id.form_descricao)).setText("Compra de teste");
            ((EditText) activity.findViewById(R.id.form_valor)).setText("12,50");
            ((Spinner) activity.findViewById(R.id.form_categoria)).setSelection(3);
            aguardar(() -> ((Spinner) activity.findViewById(R.id.form_conta)).getCount() > 1);
            ((Spinner) activity.findViewById(R.id.form_conta)).setSelection(1);
            button(activity.findViewById(android.R.id.content), "Salvar lançamento").performClick();
            aguardar(() -> button(activity.findViewById(android.R.id.content),"Concluir")!=null);
            button(activity.findViewById(android.R.id.content),"Concluir").performClick();
        }
        List<Lancamento> items = CompletableFuture.supplyAsync(() -> db.lancamentoDao().snapshot()).get();
        assertEquals(1, items.size()); assertEquals(12.5, items.get(0).valor, 0); assertEquals("INTER", items.get(0).conta);
        android.content.Intent intent = new android.content.Intent(RuntimeEnvironment.getApplication(), AddLancamentoActivity.class).putExtra("id", items.get(0).id);
        try (ActivityController<AddLancamentoActivity> controller = Robolectric.buildActivity(AddLancamentoActivity.class, intent).setup()) {
            EditText description = controller.get().findViewById(R.id.form_descricao);
            aguardar(() -> description.getText().length() > 0);
            description.setText("Mercado editado");
            button(controller.get().findViewById(android.R.id.content), "Salvar lançamento").performClick();
            aguardar(() -> button(controller.get().findViewById(android.R.id.content),"Concluir")!=null);
            button(controller.get().findViewById(android.R.id.content),"Concluir").performClick();
        }
        items = CompletableFuture.supplyAsync(() -> db.lancamentoDao().snapshot()).get();
        assertEquals(1, items.size()); assertEquals("Mercado editado", items.get(0).descricao);
        try (ActivityController<LancamentosActivity> controller = Robolectric.buildActivity(LancamentosActivity.class).setup()) {
            androidx.recyclerview.widget.RecyclerView recycler = controller.get().findViewById(R.id.lista_itens);
            aguardar(() -> recycler.getAdapter().getItemCount() == 1);
            EditText search = controller.get().findViewById(R.id.lista_busca);
            search.setText("alimentacao"); assertEquals(1, recycler.getAdapter().getItemCount());
            search.setText("inexistente"); assertEquals(0, recycler.getAdapter().getItemCount());
        }
    }

    private void aguardar(java.util.function.BooleanSupplier condition) throws Exception {
        long limit = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        while (!condition.getAsBoolean() && System.nanoTime() < limit) { ShadowLooper.idleMainLooper(); Thread.sleep(10); }
        assertTrue("Operação não concluiu", condition.getAsBoolean());
    }
    @Test public void formValidatesRequiredFieldsAndRestoresDraft() {
        try (ActivityController<AddLancamentoActivity> controller = Robolectric.buildActivity(AddLancamentoActivity.class).setup()) {
            AddLancamentoActivity activity = controller.get();
            Button save = button(activity.findViewById(android.R.id.content), "Salvar lançamento");
            assertNotNull(save); save.performClick();
            EditText description = activity.findViewById(R.id.form_descricao);
            assertNotNull(description.getError());
            description.setText("Supermercado");
            ((EditText) activity.findViewById(R.id.form_valor)).setText("12,50");
            Bundle state = new Bundle(); controller.saveInstanceState(state);
            try (ActivityController<AddLancamentoActivity> restored = Robolectric.buildActivity(AddLancamentoActivity.class).setup(state)) {
                assertEquals("Supermercado", ((EditText) restored.get().findViewById(R.id.form_descricao)).getText().toString());
                assertEquals("12,50", ((EditText) restored.get().findViewById(R.id.form_valor)).getText().toString());
            }
        }
    }
    @Test public void backupScreenHasBothActions() {
        try (ActivityController<BackupActivity> controller = Robolectric.buildActivity(BackupActivity.class).setup()) {
            assertNotNull(button(controller.get().findViewById(android.R.id.content), "Salvar backup completo"));
            assertNotNull(button(controller.get().findViewById(android.R.id.content), "Restaurar backup"));
        }
    }
    private Button button(View view, String text) {
        if (view instanceof Button && ((Button) view).getText().toString().equals(text)) return (Button) view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            Button found = button(((ViewGroup) view).getChildAt(i), text); if (found != null) return found;
        }
        return null;
    }
}
