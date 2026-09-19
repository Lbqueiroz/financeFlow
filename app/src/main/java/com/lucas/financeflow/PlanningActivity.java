package com.lucas.financeflow;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import com.lucas.financeflow.data.model.*;
import com.lucas.financeflow.data.repository.*;
import com.lucas.financeflow.wearlink.CategoryCatalog;
import java.util.*;

/** Independent screens reached through Mais, with a common, small form toolkit. */
public class PlanningActivity extends BaseActivity {
    private String mode;
    private List<PlanItem> plans=new ArrayList<>();
    private List<Lancamento> entries=new ArrayList<>();
    private List<Cadastro> registrations=new ArrayList<>();
    private LinearLayout list;
    private PlanRepository repository;
    private final Calendar month=Calendar.getInstance();
    private TextView period;
    @Override protected int tabAtual() { return R.id.nav_backup; }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); mode=getIntent().getStringExtra("mode"); if(mode==null) mode="ASSET";
        if(state!=null) month.setTimeInMillis(state.getLong("month",month.getTimeInMillis()));
        repository=new PlanRepository(this);
        String title=mode.equals("ASSET")?"Investimentos":mode.equals("TRANSFER")?"Transferências":mode.equals("RULE")?"Recorrências":"Orçamentos";
        String subtitle=mode.equals("ASSET")?"Seu dinheiro aplicado, separado dos gastos.":mode.equals("TRANSFER")?"Entre contas, sem alterar receitas ou despesas.":mode.equals("RULE")?"Confirme cada pagamento ou recebimento.":"Um limite para cada categoria do mês.";
        LinearLayout body=tela(title,subtitle,true);
        secundario(body,"‹ Mais opções",v -> finish());
        if(mode.equals("BUDGET")) {
            period=texto(body,"",20); LinearLayout controls=new LinearLayout(this); body.addView(controls);
            Button prev=secundario(controls,"‹ Anterior",v -> {month.add(Calendar.MONTH,-1); render();});
            Button next=secundario(controls,"Próximo ›",v -> {month.add(Calendar.MONTH,1); render();});
            prev.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1)); next.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
        }
        botao(body,mode.equals("ASSET")?"+ Novo investimento":mode.equals("TRANSFER")?"+ Transferir":mode.equals("RULE")?"+ Nova recorrência":"+ Definir limite",v -> form(mode,null,null));
        list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); body.addView(list);
        repository.observe().observe(this,items -> {plans=items; render();});
        FinanceiroRepository finance=new FinanceiroRepository(this);
        finance.listarTodos().observe(this,items -> {entries=items; render();});
        finance.cadastros().observe(this,items -> registrations=items);
        render();
    }
    private String monthKey() { return new java.text.SimpleDateFormat("yyyy-MM",Locale.ROOT).format(month.getTime()); }
    private void render() {
        list.removeAllViews();
        if(period!=null) period.setText(new java.text.SimpleDateFormat("MMMM 'de' yyyy",FinanceUtils.BR).format(month.getTime()));
        if(mode.equals("ASSET")) {
            long value=0,net=0; for(PlanItem p:plans) if(p.kind.equals("ASSET")) {value+=Planning.assetValue(plans,p.id); net+=Planning.netContributions(plans,p.id);}
            LinearLayout totals=card(list); texto(totals,"PATRIMÔNIO INVESTIDO",12); texto(totals,FinanceUtils.moeda(value),30);
            texto(totals,"Aportes líquidos: "+FinanceUtils.moeda(net)+"\nResultado acumulado: "+FinanceUtils.moeda(value-net),16);
            texto(totals,"Valores atualizados manualmente. O resultado inclui resgates e não representa uma taxa de rentabilidade.",13);
        }
        int count=0;
        for(PlanItem p:plans) {
            if(!p.kind.equals(mode) || (mode.equals("BUDGET") && !p.date.startsWith(monthKey()+"-"))) continue;
            count++; LinearLayout panel=card(list);
            if(mode.equals("ASSET")) {
                texto(panel,p.name,22); texto(panel,p.account,14);
                texto(panel,FinanceUtils.moeda(Planning.assetValue(plans,p.id)),28);
                texto(panel,"Resultado: "+FinanceUtils.moeda(Planning.assetValue(plans,p.id)-Planning.netContributions(plans,p.id)),15);
                botao(panel,"Aplicar",v -> form("APPLY",null,p));
                secundario(panel,"Resgatar",v -> form("REDEEM",null,p));
                secundario(panel,"Atualizar valor atual",v -> form("VALUE",null,p));
                secundario(panel,"Histórico",v -> history(p));
                boolean used=false; for(PlanItem other:plans) if(other.target.equals(p.id)) used=true;
                if(!used) removeButton(panel,p);
            } else if(mode.equals("TRANSFER")) {
                texto(panel,p.account+" → "+p.target,20); texto(panel,FinanceUtils.moeda(p.cents),26); texto(panel,FinanceUtils.dataVisivel(p.date),14); removeButton(panel,p);
            } else if(mode.equals("RULE")) {
                texto(panel,p.name,22); texto(panel,(p.type.equals("ENTRADA")?"Receber ":"Pagar ")+FinanceUtils.moeda(p.cents),24);
                texto(panel,p.account+" · "+p.category+"\nPróximo: "+FinanceUtils.dataVisivel(p.date)+" · mensal, dia "+p.day,15);
                boolean due=p.date.compareTo(FinanceUtils.hoje())<=0;
                Button confirm=botao(panel,due?(p.type.equals("ENTRADA")?"Confirmar recebimento":"Confirmar pagamento"):"Aguardando vencimento",v -> {
                    new AlertDialog.Builder(this).setTitle("Confirmar lançamento?").setMessage(p.name+"\n"+FinanceUtils.moeda(p.cents)+" · "+FinanceUtils.dataVisivel(p.date))
                        .setNegativeButton("Cancelar",null).setPositiveButton("Confirmar",(d,w) -> {v.setEnabled(false); repository.confirm(p.id,p.date,error -> {if(!isDestroyed()) {v.setEnabled(true); message(error==null?"Lançamento registrado. Próximo mês agendado.":error);}});}).show();
                }); confirm.setEnabled(due);
                secundario(panel,"Editar",v -> form("RULE",p,null)); removeButton(panel,p);
            } else {
                long used=Planning.spent(entries,monthKey(),p.category), remaining=p.cents-used;
                texto(panel,p.category,22); texto(panel,FinanceUtils.moeda(used)+" de "+FinanceUtils.moeda(p.cents),20);
                ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); progress.setMax(100); progress.setProgress((int)Math.min(100,used*100/p.cents)); panel.addView(progress,new LinearLayout.LayoutParams(-1,dp(10)));
                TextView status=texto(panel,(remaining>=0?"Disponível: ":"Acima do limite: ")+FinanceUtils.moeda(Math.abs(remaining)),16); if(remaining<0) status.setTextColor(0xffa93f35);
                secundario(panel,"Editar limite",v -> form("BUDGET",p,null)); removeButton(panel,p);
            }
        }
        if(count==0) texto(card(list),"Nada cadastrado aqui ainda. Use o botão acima para começar.",16);
    }
    private void removeButton(LinearLayout panel,PlanItem item) {
        secundario(panel,item.kind.equals("TRANSFER")?"Desfazer transferência":"Excluir",v -> new AlertDialog.Builder(this).setTitle("Excluir registro?")
            .setMessage(item.kind.equals("RULE")?"Os lançamentos já confirmados serão mantidos.":"Os saldos serão recalculados. Esta ação não pode ser desfeita.")
            .setNegativeButton("Cancelar",null).setPositiveButton("Excluir",(d,w) -> repository.delete(item.id,error -> message(error==null?"Registro excluído":error))).show());
    }
    private void history(PlanItem asset) {
        LinearLayout panel=new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(dp(16),dp(8),dp(16),dp(8));
        int count=0;
        for(PlanItem p:plans) if(p.target.equals(asset.id)) {
            count++; LinearLayout row=card(panel);
            texto(row,(p.kind.equals("APPLY")?"Aplicação":p.kind.equals("REDEEM")?"Resgate":"Valor atualizado")+" · "+FinanceUtils.moeda(p.cents),17);
            texto(row,FinanceUtils.dataVisivel(p.date)+(p.account.isEmpty()?"":" · "+p.account),14);
            removeButton(row,p);
        }
        if(count==0) texto(panel,"Sem movimentações",16);
        ScrollView scroll=new ScrollView(this); scroll.addView(panel);
        new AlertDialog.Builder(this).setTitle(asset.name).setView(scroll).setPositiveButton("Fechar",null).show();
    }
    private Spinner select(LinearLayout body,String label,String[] values,String selected) {
        Spinner field=opcoes(body,label,values,View.generateViewId());
        if(selected!=null) for(int i=0;i<values.length;i++) if(selected.equals(values[i])) field.setSelection(i);
        return field;
    }
    private String[] accounts(String existing) {
        ArrayList<String> names=new ArrayList<>(); for(Cadastro p:registrations) if(p.tipo.equals("CONTA")) names.add(p.nome);
        if(existing!=null && !existing.isEmpty() && !names.contains(existing)) names.add(existing);
        return names.toArray(new String[0]);
    }
    private String value(Spinner spinner) { if(spinner==null || spinner.getSelectedItem()==null) throw new IllegalArgumentException("Cadastre uma conta na aba Contas primeiro"); return spinner.getSelectedItem().toString(); }
    private void form(String kind,PlanItem existing,PlanItem asset) {
        LinearLayout body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(20),0,dp(20),dp(12));
        PlanItem item=new PlanItem(); item.kind=kind; item.date=FinanceUtils.hoje();
        if(existing!=null) {item.id=existing.id; item.created=existing.created; item.date=existing.date;}
        if(kind.equals("BUDGET")) item.date=monthKey()+"-01";
        if(asset!=null) item.target=asset.id;
        String title=kind.equals("ASSET")?"Novo investimento":kind.equals("TRANSFER")?"Transferir":kind.equals("RULE")?"Recorrência mensal":kind.equals("BUDGET")?"Limite mensal":kind.equals("APPLY")?"Aplicar":kind.equals("REDEEM")?"Resgatar":"Atualizar valor";
        if(asset!=null) texto(body,asset.name,20);
        final EditText name=(kind.equals("ASSET")||kind.equals("RULE"))?campo(body,"Nome",kind.equals("ASSET")?"Ex.: CDB reserva":"Ex.: aluguel",View.generateViewId()):null;
        if(name!=null) { name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(120)}); if(existing!=null) name.setText(existing.name); }
        final EditText institution=kind.equals("ASSET")?campo(body,"Banco / corretora","Ex.: minha corretora",View.generateViewId()):null;
        if(institution!=null) institution.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(120)});
        final EditText amount=!kind.equals("ASSET")?campo(body,kind.equals("VALUE")?"Valor total atual (R$)":"Valor (R$)","0,00",View.generateViewId()):null;
        if(amount!=null) {MoneyInput.attach(amount); if(existing!=null) amount.setText(com.lucas.financeflow.wearlink.WearProtocol.amount(existing.cents));}
        final Spinner source=Arrays.asList("TRANSFER","APPLY","REDEEM","RULE").contains(kind)?select(body,kind.equals("REDEEM")?"Receber na conta":"Conta",accounts(existing==null?null:existing.account),existing==null?null:existing.account):null;
        final Spinner target=kind.equals("TRANSFER")?select(body,"Destino",accounts(null),null):null;
        final Spinner category=Arrays.asList("RULE","BUDGET").contains(kind)?select(body,"Categoria",CategoryCatalog.all(),existing==null?"Outros":existing.category):null;
        if(kind.equals("BUDGET") && existing!=null) category.setEnabled(false);
        final Spinner type=kind.equals("RULE")?select(body,"Tipo",new String[]{"Saída","Entrada"},existing!=null && existing.type.equals("ENTRADA")?"Entrada":"Saída"):null;
        if(kind.equals("RULE")) {
            Button date=secundario(body,"Próximo vencimento: "+FinanceUtils.dataVisivel(item.date),null);
            date.setOnClickListener(v -> {Calendar cal=Calendar.getInstance(); cal.setTime(Planning.strictDate(item.date)); new android.app.DatePickerDialog(this,(picker,y,m,d) -> {
                item.date=String.format(Locale.ROOT,"%04d-%02d-%02d",y,m+1,d); date.setText("Próximo vencimento: "+FinanceUtils.dataVisivel(item.date));
            },cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH)).show();});
        }
        if(kind.equals("VALUE")) texto(body,"Informe quanto o investimento vale agora. Não movimenta dinheiro nas contas. Zero é permitido.",14);
        if(kind.equals("ASSET")) texto(body,"Depois de cadastrar, registre sua primeira aplicação.",14);
        ScrollView scroll=new ScrollView(this); scroll.addView(body);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(title).setView(scroll).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",null).create();
        dialog.setOnShowListener(d -> dialog.getButton(-1).setOnClickListener(v -> {
            try {
                if(name!=null) item.name=name.getText().toString().trim();
                if(institution!=null) item.account=institution.getText().toString().trim();
                if(amount!=null) {
                    String text=amount.getText().toString();
                    if(kind.equals("VALUE") && text.matches("0+,00")) item.cents=0;
                    else item.cents=FinanceUtils.centavos(FinanceUtils.parseValor(text));
                }
                if(source!=null) item.account=value(source); if(target!=null) item.target=value(target);
                if(category!=null) item.category=value(category); if(type!=null) item.type=type.getSelectedItemPosition()==0?"SAIDA":"ENTRADA";
                if(kind.equals("BUDGET")) item.id="budget:"+item.date.substring(0,7)+":"+item.category;
                if(kind.equals("RULE")) {Calendar due=Calendar.getInstance(); due.setTime(Planning.strictDate(item.date)); item.day=existing!=null && existing.date.equals(item.date)?existing.day:due.get(Calendar.DAY_OF_MONTH);}
                Planning.validate(item); dialog.getButton(-1).setEnabled(false);
                repository.save(item,error -> {if(isDestroyed()) return; if(error==null) {dialog.dismiss(); message("Salvo");} else {dialog.getButton(-1).setEnabled(true); message(error);}});
            } catch(IllegalArgumentException error) {message(error.getMessage());}
        })); dialog.show();
    }
    private void message(String text) { if(!isDestroyed()) Toast.makeText(this,text,Toast.LENGTH_LONG).show(); }
    @Override protected void onSaveInstanceState(Bundle out) {out.putLong("month",month.getTimeInMillis()); super.onSaveInstanceState(out);}
}
