package com.lucas.financeflow;

import android.os.Bundle;
import android.widget.LinearLayout;
import com.lucas.financeflow.data.model.Cadastro;
import com.lucas.financeflow.data.repository.FinanceiroRepository;

public class CadastrosActivity extends BaseActivity {
    @Override protected int tabAtual() { return 0; }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout body = tela("Seus cadastros", "Crie as contas e origens que você usa nos lançamentos.", true);
        botao(body, "+ Cadastrar conta", v -> cadastrar(Cadastro.CONTA, nome -> {}));
        secundario(body, "+ Cadastrar origem / destino", v -> cadastrar(Cadastro.ORIGEM, nome -> {}));
        LinearLayout lista = new LinearLayout(this); lista.setOrientation(LinearLayout.VERTICAL); body.addView(lista);
        new FinanceiroRepository(this).cadastros().observe(this, cadastros -> {
            lista.removeAllViews();
            for (String tipo : new String[]{Cadastro.CONTA, Cadastro.ORIGEM}) {
                rotulo(lista, tipo.equals(Cadastro.CONTA) ? "CONTAS" : "ORIGENS / DESTINOS");
                int count = 0;
                for (Cadastro item : cadastros) if (tipo.equals(item.tipo)) {
                    LinearLayout panel = card(lista);
                    texto(panel, item.nome, 17);
                    android.widget.Button excluir = secundario(panel, "Excluir", v -> confirmarExclusao(item));
                    excluir.setContentDescription("Excluir " + item.nome);
                    excluir.setTextColor(android.graphics.Color.rgb(169, 63, 53));
                    count++;
                }
                if (count == 0) texto(lista, "Nenhum cadastro ainda.", 15);
            }
        });
        secundario(body, "Voltar", v -> finish());
    }
    private void confirmarExclusao(Cadastro item) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(Cadastro.CONTA.equals(item.tipo) ? "Excluir conta?" : "Excluir origem / destino?")
                .setMessage("Excluir \"" + item.nome + "\" dos cadastros? Os lançamentos antigos e seus valores serão preservados. O nome ainda poderá aparecer no histórico e nos saldos existentes.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (dialog, which) -> new FinanceiroRepository(this).excluirCadastro(item, ok -> {
                    if (!isDestroyed()) android.widget.Toast.makeText(this,
                            ok ? "Cadastro excluído" : "Não foi possível excluir. Tente novamente.", android.widget.Toast.LENGTH_LONG).show();
                })).show();
    }
}
