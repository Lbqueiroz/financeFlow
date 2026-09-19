package com.lucas.financeflow;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
public class MoreActivity extends BaseActivity {
    @Override protected int tabAtual() { return R.id.nav_backup; }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); LinearLayout body=tela("Mais", "Cuide dos próximos passos do seu dinheiro.",true);
        section(body,"Investimentos","Aplicações, resgates e evolução do patrimônio.","ASSET");
        section(body,"Transferências","Mova dinheiro entre suas contas.","TRANSFER");
        LinearLayout installments=card(body); texto(installments,"Compras com quantidade de parcelas e data final.",15);
        botao(installments,"Parcelamentos",v -> startActivity(new Intent(this,InstallmentsActivity.class)));
        section(body,"Recorrências","Confirme salários e contas mensais no vencimento.","RULE");
        section(body,"Orçamentos","Acompanhe limites mensais por categoria.","BUDGET");
        secundario(body,"Backup e restauração",v -> startActivity(new Intent(this,BackupActivity.class)));
    }
    private void section(LinearLayout body,String title,String subtitle,String mode) {
        LinearLayout panel=card(body); texto(panel,subtitle,15); botao(panel,title,v -> startActivity(new Intent(this,PlanningActivity.class).putExtra("mode",mode)));
    }
}
