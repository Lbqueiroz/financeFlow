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
    @Test public void savesEditsAndFindsTransaction() throws Exception {
        AppDatabase db = AppDatabase.getInstance(RuntimeEnvironment.getApplication());
        CompletableFuture.runAsync(() -> db.lancamentoDao().limpar()).get();
        try (ActivityController<AddLancamentoActivity> controller = Robolectric.buildActivity(AddLancamentoActivity.class).setup()) {
            AddLancamentoActivity activity = controller.get();
            ((EditText) activity.findViewById(R.id.form_descricao)).setText("Compra de teste");
            ((EditText) activity.findViewById(R.id.form_valor)).setText("12,50");
            ((Spinner) activity.findViewById(R.id.form_categoria)).setSelection(3);
            ((Spinner) activity.findViewById(R.id.form_conta)).setSelection(0);
            button(activity.findViewById(android.R.id.content), "Salvar lançamento").performClick();
            aguardar(activity::isFinishing);
        }
        List<Lancamento> items = CompletableFuture.supplyAsync(() -> db.lancamentoDao().snapshot()).get();
        assertEquals(1, items.size()); assertEquals(12.5, items.get(0).valor, 0); assertEquals("INTER", items.get(0).conta);
        android.content.Intent intent = new android.content.Intent(RuntimeEnvironment.getApplication(), AddLancamentoActivity.class).putExtra("id", items.get(0).id);
        try (ActivityController<AddLancamentoActivity> controller = Robolectric.buildActivity(AddLancamentoActivity.class, intent).setup()) {
            EditText description = controller.get().findViewById(R.id.form_descricao);
            aguardar(() -> description.getText().length() > 0);
            description.setText("Mercado editado");
            button(controller.get().findViewById(android.R.id.content), "Salvar lançamento").performClick();
            aguardar(controller.get()::isFinishing);
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

