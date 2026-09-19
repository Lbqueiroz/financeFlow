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
    @Test public void accountSelectorCombinesWithSearchTypeDateAndPdfAndSurvivesRotation() throws Exception {
        testDb.lancamentoDao().cadastrar(new com.lucas.financeflow.data.model.Cadastro("CONTA","Inter"));
        for(String[] row:new String[][]{{"Inter","SAIDA","2026-09-18"},{"Inter","ENTRADA","2026-09-18"},{"Inter","SAIDA","2026-08-18"},{"Inter extra","SAIDA","2026-09-18"}})
            testDb.lancamentoDao().inserir(new Lancamento("Almoço",10,row[1],"Alimentação",row[2],"CELULAR","LOCAL","",row[0]));
        Bundle saved=new Bundle();
        try(var controller=Robolectric.buildActivity(LancamentosActivity.class).setup()) {
            var activity=controller.get(); var list=(androidx.recyclerview.widget.RecyclerView)activity.findViewById(R.id.lista_itens);
            Spinner account=activity.findViewById(R.id.filtro_conta); aguardar(() -> account.getCount()==3);
            assertEquals("Inter extra",account.getItemAtPosition(2)); // historical account without registration
            account.setSelection(1); aguardar(() -> list.getAdapter().getItemCount()==3);
            ((Spinner)activity.findViewById(R.id.lista_tipo)).setSelection(2); aguardar(() -> list.getAdapter().getItemCount()==2);
            ((EditText)activity.findViewById(R.id.lista_busca)).setText("almoco"); assertEquals(2,list.getAdapter().getItemCount());
            activity.findViewById(R.id.filtro_data).performClick(); var dialog=ultimoDialogo();
            escolherDia(dialog,R.id.filtro_inicio,2026,9,18); dialog.getButton(-1).performClick(); assertEquals(1,list.getAdapter().getItemCount());
            button(activity.findViewById(android.R.id.content),"Exportar relatório em PDF").performClick();
            var pdf=new androidx.lifecycle.ViewModelProvider(activity).get(LancamentosActivity.PdfExportState.class);
            assertEquals(1,pdf.items.size()); assertTrue(pdf.filters.contains("Conta: Inter |")); assertEquals("Inter",pdf.items.get(0).conta);
            controller.saveInstanceState(saved);
        }
        try(var restored=Robolectric.buildActivity(LancamentosActivity.class).setup(saved)) {
            var activity=restored.get(); var list=(androidx.recyclerview.widget.RecyclerView)activity.findViewById(R.id.lista_itens);
            aguardar(() -> list.getAdapter().getItemCount()==1); assertEquals("Inter",((Spinner)activity.findViewById(R.id.filtro_conta)).getSelectedItem());
            activity.findViewById(R.id.filtro_data).performClick(); ultimoDialogo().getButton(-3).performClick();
            ShadowLooper.idleMainLooper();
            assertEquals(2,list.getAdapter().getItemCount());
            ((Spinner)activity.findViewById(R.id.filtro_conta)).setSelection(0); aguardar(() -> list.getAdapter().getItemCount()==3);
        }
    }
    @Test public void accountHistoryScopesExactlyAndPrefillsRepeatedExpenses() throws Exception {
        testDb.lancamentoDao().cadastrar(new com.lucas.financeflow.data.model.Cadastro("CONTA","Fatura Nubank"));
        testDb.lancamentoDao().cadastrar(new com.lucas.financeflow.data.model.Cadastro("CONTA","Fatura Nubank extra"));
        for(String account:new String[]{"Fatura Nubank","fatura nubank","Fatura Nubank extra"})
            testDb.lancamentoDao().inserir(new Lancamento("Compra",10,"SAIDA","Outros","2026-09-19","CELULAR","LOCAL","",account));
        android.content.Intent historyIntent;
        try(var accounts=Robolectric.buildActivity(ContasActivity.class).setup()) {
            aguardar(() -> described(accounts.get().findViewById(android.R.id.content),"Abrir conta Fatura Nubank")!=null);
            described(accounts.get().findViewById(android.R.id.content),"Abrir conta Fatura Nubank").performClick();
            historyIntent=org.robolectric.Shadows.shadowOf(accounts.get()).getNextStartedActivity();
            assertEquals("Fatura Nubank",historyIntent.getStringExtra(LancamentosActivity.EXTRA_CONTA));
        }
        try(var history=Robolectric.buildActivity(LancamentosActivity.class,historyIntent).setup()) {
            var activity=history.get(); var list=(androidx.recyclerview.widget.RecyclerView)activity.findViewById(R.id.lista_itens);
            aguardar(() -> list.getAdapter().getItemCount()==2);
            activity.findViewById(R.id.filtro_data).performClick(); ultimoDialogo().getButton(-3).performClick(); assertEquals(2,list.getAdapter().getItemCount());
            Bundle state=new Bundle(); history.saveInstanceState(state);
            try(var restored=Robolectric.buildActivity(LancamentosActivity.class,historyIntent).setup(state)) {
                var restoredList=(androidx.recyclerview.widget.RecyclerView)restored.get().findViewById(R.id.lista_itens);
                aguardar(() -> restoredList.getAdapter().getItemCount()==2);
            }
            // Saving instance state lowers the LifecycleRegistry state; simulate returning to this screen.
            history.pause().resume();
            button(activity.findViewById(android.R.id.content),"+ Novo lançamento").performClick();
            android.content.Intent formIntent=org.robolectric.Shadows.shadowOf(activity).getNextStartedActivity();
            try(var form=Robolectric.buildActivity(AddLancamentoActivity.class,formIntent).setup()) {
                var editor=form.get(); Spinner account=editor.findViewById(R.id.form_conta);
                aguardar(() -> "Fatura Nubank".equals(account.getSelectedItem()));
                assertEquals(1,((Spinner)editor.findViewById(R.id.form_tipo)).getSelectedItemPosition());
                ((EditText)editor.findViewById(R.id.form_descricao)).setText("Compra nova");
                ((EditText)editor.findViewById(R.id.form_valor)).setText("1250");
                button(editor.findViewById(android.R.id.content),"Salvar lançamento").performClick();
                aguardar(() -> button(editor.findViewById(android.R.id.content),"+ Outro nesta conta")!=null);
                button(editor.findViewById(android.R.id.content),"+ Outro nesta conta").performClick();
                assertEquals("Fatura Nubank",org.robolectric.Shadows.shadowOf(editor).getNextStartedActivity().getStringExtra(LancamentosActivity.EXTRA_CONTA));
            }
            aguardar(() -> list.getAdapter().getItemCount()==3);
            assertEquals(4,testDb.lancamentoDao().snapshot().size());
        }
    }
    private View described(View view,String description) {
        if(description.contentEquals(view.getContentDescription()==null?"":view.getContentDescription())) return view;
        if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) {View found=described(((ViewGroup)view).getChildAt(i),description); if(found!=null) return found;}
        return null;
    }
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

    @Test public void calendarFiltersInclusivelyAndRejectsReversedRange() throws Exception {
        for (String day : new String[]{"2026-07-31", "2026-08-01", "2026-09-18", "2026-09-30", "2026-10-01"}) {
            testDb.lancamentoDao().inserir(new Lancamento("Teste", 1, "ENTRADA", "Outros", day, "CELULAR", "LOCAL", "", "Conta"));
        }
        try (var controller = Robolectric.buildActivity(LancamentosActivity.class).setup()) {
            LancamentosActivity activity = controller.get();
            androidx.recyclerview.widget.RecyclerView recycler = activity.findViewById(R.id.lista_itens);
            aguardar(() -> recycler.getAdapter().getItemCount() == 5);
            activity.findViewById(R.id.filtro_data).performClick();
            androidx.appcompat.app.AlertDialog dialog = ultimoDialogo();
            assertTrue(dialog.findViewById(R.id.filtro_inicio) instanceof Button);
            escolherDia(dialog,R.id.filtro_inicio,2026,8,1);
            escolherDia(dialog,R.id.filtro_fim,2026,7,31);
            dialog.getButton(-1).performClick(); assertTrue(dialog.isShowing());
            escolherDia(dialog,R.id.filtro_fim,2026,9,30); dialog.getButton(-1).performClick();
            assertEquals(3, recycler.getAdapter().getItemCount());
            activity.findViewById(R.id.filtro_data).performClick();
            dialog = ultimoDialogo();
            escolherDia(dialog,R.id.filtro_inicio,2026,9,18);
            button(dialog.findViewById(android.R.id.content),"Usar só a data inicial").performClick(); dialog.getButton(-1).performClick();
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
    private void escolherDia(androidx.appcompat.app.AlertDialog dialog,int field,int year,int month,int day) {
        dialog.findViewById(field).performClick(); ShadowLooper.idleMainLooper();
        android.app.DatePickerDialog picker=(android.app.DatePickerDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
        picker.updateDate(year,month-1,day); picker.getButton(-1).performClick(); ShadowLooper.idleMainLooper();
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
