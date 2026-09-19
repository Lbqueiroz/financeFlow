package com.lucas.financeflow;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import com.lucas.financeflow.data.model.*;
import com.lucas.financeflow.data.repository.*;
import java.util.*;

public class InstallmentsActivity extends BaseActivity {
    private LinearLayout list;
    private List<InstallmentPlan> plans=new ArrayList<>();
    private List<Lancamento> entries=new ArrayList<>();
    private InstallmentRepository repository;
    @Override protected int tabAtual() {return R.id.nav_backup;}
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); repository=new InstallmentRepository(this);
        LinearLayout body=tela("Parcelamentos","Compras divididas em parcelas mensais, com início e fim definidos.",true);
        secundario(body,"‹ Mais opções",v -> finish());
        botao(body,"+ Nova compra parcelada",v -> startActivity(new Intent(this,NewInstallmentActivity.class)));
        texto(body,"As parcelas entram no histórico da conta e no resumo de cada mês. Assinaturas ficam separadas em Mais → Recorrências.",14);
        list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); body.addView(list);
        repository.observe().observe(this,items -> {plans=items; render();});
        new FinanceiroRepository(this).listarTodos().observe(this,items -> {entries=items; render();});
    }
    private List<Lancamento> linked(InstallmentPlan plan) {
        List<Lancamento> result=new ArrayList<>(); for(Lancamento entry:entries) if(plan.id.equals(entry.installmentPlanId)) result.add(entry);
        result.sort(Comparator.comparingInt(p -> p.installmentNumber)); return result;
    }
    private void render() {
        list.removeAllViews(); if(plans.isEmpty()) texto(card(list),"Nenhuma compra parcelada. Cadastre o valor total e escolha em quantas vezes vai pagar.",16);
        for(InstallmentPlan plan:plans) {
            LinearLayout panel=card(list); texto(panel,plan.name,22); texto(panel,plan.account+" · "+plan.category,15);
            texto(panel,"Total da compra: "+FinanceUtils.moeda(plan.totalCents),22);
            texto(panel,plan.count+" parcelas · primeira em "+FinanceUtils.dataVisivel(plan.firstDate),15);
            List<Lancamento> linked=linked(plan); long current=0; for(Lancamento p:linked) current+=FinanceUtils.centavos(p.valor)*("SAIDA".equals(p.tipo)?1:-1);
            if(linked.size()!=plan.count || current!=plan.totalCents) texto(panel,linked.size()+" parcela(s) no histórico · resultado atual "+FinanceUtils.moeda(current)+"\nHá parcelas editadas ou excluídas individualmente.",14);
            botao(panel,"Ver parcelas",v -> details(plan));
            secundario(panel,"Excluir compra e parcelas",v -> new AlertDialog.Builder(this).setTitle("Excluir esta compra?")
                .setMessage("A compra e todas as suas parcelas, inclusive as de meses anteriores, serão removidas. Outros lançamentos não serão alterados.")
                .setNegativeButton("Cancelar",null).setPositiveButton("Excluir",(d,w) -> {v.setEnabled(false); repository.delete(plan.id,error -> {if(!isDestroyed()) {v.setEnabled(true); Toast.makeText(this,error==null?"Compra excluída":error,Toast.LENGTH_LONG).show();}});}).show());
        }
    }
    private void details(InstallmentPlan plan) {
        LinearLayout panel=new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(dp(20),0,dp(20),dp(12));
        List<Lancamento> linked=linked(plan); if(linked.isEmpty()) texto(panel,"Todas as parcelas foram excluídas do histórico.",16);
        ScrollView scroll=new ScrollView(this); scroll.addView(panel);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(plan.name).setView(scroll).setPositiveButton("Fechar",null).create();
        for(Lancamento entry:linked) secundario(panel,entry.installmentNumber+"/"+plan.count+" · "+FinanceUtils.moeda(FinanceUtils.centavos(entry.valor))+"\n"+FinanceUtils.dataVisivel(entry.data)+" · "+entry.conta,
            v -> {dialog.dismiss(); startActivity(new Intent(this,AddLancamentoActivity.class).putExtra("id",entry.id));});
        dialog.show();
    }
}
