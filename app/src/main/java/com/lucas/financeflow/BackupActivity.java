package com.lucas.financeflow;

import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import com.lucas.financeflow.data.local.AppDatabase;
import com.lucas.financeflow.data.model.Lancamento;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class BackupActivity extends BaseActivity {
    @Override protected int tabAtual() { return R.id.nav_backup; }
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private Button salvar, restaurar;
    private final ActivityResultLauncher<String> destino = registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), this::salvarBackup);
    private final ActivityResultLauncher<String[]> origem = registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::lerBackup);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout body = tela("Backup dos seus dados", "Leve seu histórico com você.", true);
        texto(body, "Salve uma cópia em uma pasta de sua escolha. Para recuperar seus dados em outro aparelho, abra o arquivo pela opção Restaurar backup.", 17);
        texto(body, "O arquivo contém suas movimentações financeiras. Guarde-o em um local privado.", 15);
        salvar = botao(body, "Salvar backup completo", v -> destino.launch("financeflow-backup-" + FinanceUtils.hoje() + ".json"));
        restaurar = secundario(body, "Restaurar backup", v -> origem.launch(new String[]{"application/json", "text/plain", "application/octet-stream"}));
        texto(body, "A restauração substitui todo o histórico atual após sua confirmação. A exportação PDF fica na tela de lançamentos e serve para consultar ou compartilhar relatórios.", 15);
    }
    private void ocupado(boolean busy) { salvar.setEnabled(!busy); restaurar.setEnabled(!busy); }
    private void resposta(String text) { if (!isDestroyed()) { ocupado(false); Toast.makeText(this, text, Toast.LENGTH_LONG).show(); } }
    private void salvarBackup(Uri uri) {
        if (uri == null) return;
        ocupado(true);
        IO.execute(() -> {
            String mensagem;
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                BackupCodec.Documento snapshot = db.runInTransaction(() -> new BackupCodec.Documento(db.lancamentoDao().snapshot(), db.lancamentoDao().snapshotCadastros(),db.planDao().snapshot()));
                String content = BackupCodec.encode(snapshot.itens, snapshot.cadastros,snapshot.plans);
                try (OutputStream stream = getContentResolver().openOutputStream(uri, "wt")) {
                    if (stream == null) throw new IOException();
                    stream.write(content.getBytes(StandardCharsets.UTF_8));
                }
                mensagem = "Backup salvo";
            } catch (Exception ex) { mensagem = "Não foi possível salvar o backup. Tente novamente."; }
            String result = mensagem; runOnUiThread(() -> resposta(result));
        });
    }
    private void lerBackup(Uri uri) {
        if (uri == null) return;
        ocupado(true);
        IO.execute(() -> {
            try (InputStream stream = getContentResolver().openInputStream(uri)) {
                if (stream == null) throw new IOException();
                ByteArrayOutputStream bytes = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int count;
                while ((count = stream.read(buffer)) != -1) {
                    if (bytes.size() + count > 20 * 1024 * 1024) throw new IOException("Arquivo muito grande");
                    bytes.write(buffer, 0, count);
                }
                BackupCodec.Documento itens = BackupCodec.decodeCompleto(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
                runOnUiThread(() -> confirmar(itens));
            } catch (Exception ex) { runOnUiThread(() -> resposta("Backup inválido ou inacessível. Nenhum dado foi alterado.")); }
        });
    }
    private void confirmar(BackupCodec.Documento itens) {
        if (isDestroyed() || isFinishing()) return;
        new AlertDialog.Builder(this).setTitle("Substituir o histórico?")
                .setMessage("O backup contém " + itens.itens.size() + " lançamento(s) e "+itens.plans.size()+" registro(s) de planejamento. Histórico, cadastros, investimentos, transferências, recorrências e orçamentos serão substituídos. Backups antigos não contêm planejamento. Salve um backup antes de continuar.")
                .setNegativeButton("Cancelar", (d, w) -> ocupado(false))
                .setOnCancelListener(d -> ocupado(false))
                .setPositiveButton("Restaurar", (d, w) -> IO.execute(() -> {
                    try {
                        AppDatabase db=AppDatabase.getInstance(this);
                        db.runInTransaction(() -> {db.lancamentoDao().restaurarCompleto(itens.itens, itens.cadastros); db.planDao().clear(); db.planDao().insertAll(itens.plans);});
                        runOnUiThread(() -> resposta("Backup restaurado"));
                    } catch (RuntimeException ex) { runOnUiThread(() -> resposta("Falha na restauração. Seu histórico foi preservado.")); }
                })).show();
    }
}

