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
                for (Cadastro item : cadastros) if (tipo.equals(item.tipo)) { texto(card(lista), item.nome, 17); count++; }
                if (count == 0) texto(lista, "Nenhum cadastro ainda.", 15);
            }
        });
        secundario(body, "Voltar", v -> finish());
    }
}
