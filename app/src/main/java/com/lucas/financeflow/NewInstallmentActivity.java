package com.lucas.financeflow;

import android.app.Application;
import android.os.Bundle;
import android.widget.*;
import com.lucas.financeflow.data.model.*;
import com.lucas.financeflow.data.repository.*;
import java.util.*;

public class NewInstallmentActivity extends BaseActivity {
    private EditText name,total,count;
    private Spinner account,category;
    private String firstDate;
    private TextView preview;
    private Button save,date;
    private SaveState model;
    @Override protected int tabAtual() {return 0;}
    public static class SaveState extends androidx.lifecycle.AndroidViewModel {
        public String id=UUID.randomUUID().toString();
        public final androidx.lifecycle.MutableLiveData<Integer> status=new androidx.lifecycle.MutableLiveData<>(0);
        public String error;
        public SaveState(Application app) {super(app);}
        public void save(InstallmentPlan plan) {
            if(!Integer.valueOf(0).equals(status.getValue())) return;
            status.setValue(1); new InstallmentRepository(getApplication()).save(plan,message -> {error=message; status.setValue(message==null?2:0);});
        }
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); model=new androidx.lifecycle.ViewModelProvider(this).get(SaveState.class);
        if(state!=null) model.id=state.getString("planId",model.id);
        firstDate=state==null?FinanceUtils.hoje():state.getString("firstDate",FinanceUtils.hoje());
        LinearLayout body=tela("Nova compra parcelada","Informe o total final da compra, já com juros se houver.",true);
        name=campo(body,"Nome da compra","Ex.: celular",R.id.form_descricao); name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(120)});
        total=campo(body,"Valor total (R$)","0,00",R.id.form_valor); MoneyInput.attach(total);
        count=campo(body,"Quantidade de parcelas","Ex.: 3",R.id.installment_count); count.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); count.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(3)});
        account=opcoes(body,"Conta / cartão",new String[]{"Selecione uma conta"},R.id.form_conta); account.setSaveEnabled(false);
        category=opcoes(body,"Categoria",com.lucas.financeflow.wearlink.CategoryCatalog.all(),R.id.form_categoria);
        category.setSelection(Arrays.asList(com.lucas.financeflow.wearlink.CategoryCatalog.all()).indexOf("Outros"));
        String restored=state==null?null:state.getString("account");
        new FinanceiroRepository(this).cadastros().observe(this,items -> {
            String selected=account.getSelectedItemPosition()>0?account.getSelectedItem().toString():restored;
            List<String> names=new ArrayList<>(); names.add("Selecione uma conta"); for(Cadastro item:items) if("CONTA".equals(item.tipo)) names.add(item.nome);
            ArrayAdapter<String> adapter=new ArrayAdapter<>(this,R.layout.select_value,names); adapter.setDropDownViewResource(R.layout.select_option); account.setAdapter(adapter);
            if(selected!=null) for(int i=1;i<names.size();i++) if(names.get(i).equalsIgnoreCase(selected)) account.setSelection(i);
        });
        date=secundario(body,"Primeira parcela: "+FinanceUtils.dataVisivel(firstDate),v -> {
            Calendar cal=Calendar.getInstance(); cal.setTime(Planning.strictDate(firstDate));
            new android.app.DatePickerDialog(this,(picker,y,m,d) -> {firstDate=String.format(Locale.ROOT,"%04d-%02d-%02d",y,m+1,d); date.setText("Primeira parcela: "+FinanceUtils.dataVisivel(firstDate)); updatePreview();},cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH)).show();
        });
        preview=texto(card(body),"Confira aqui os valores e o período antes de salvar.",16);
        texto(body,"A primeira data é a do lançamento na fatura. Não calculamos automaticamente o fechamento do cartão. Cada parcela entra como saída no seu mês, inclusive nos meses futuros.",14);
        save=botao(body,"Criar parcelas",v -> submit()); secundario(body,"Cancelar",v -> finish());
        android.text.TextWatcher watcher=new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int before,int after) { }
            public void onTextChanged(CharSequence s,int start,int before,int after) {updatePreview();}
            public void afterTextChanged(android.text.Editable e) { }
        }; total.addTextChangedListener(watcher); count.addTextChangedListener(watcher);
        model.status.observe(this,status -> {
            save.setEnabled(status==0);
            if(status==2) {Toast.makeText(this,"Compra e parcelas salvas",Toast.LENGTH_SHORT).show(); finish();}
            else if(model.error!=null) {Toast.makeText(this,model.error,Toast.LENGTH_LONG).show(); model.error=null;}
        });
    }
    private InstallmentPlan draft(boolean validateFields) {
        InstallmentPlan plan=new InstallmentPlan(); plan.id=model.id; plan.name=validateFields?name.getText().toString().trim():"Compra";
        if(validateFields && account.getSelectedItemPosition()==0) throw new IllegalArgumentException("Cadastre e selecione uma conta na aba Contas");
        plan.account=validateFields?account.getSelectedItem().toString():"Conta"; plan.category=category.getSelectedItem().toString(); plan.firstDate=firstDate;
        try {plan.count=Integer.parseInt(count.getText().toString());} catch(NumberFormatException e) {throw new IllegalArgumentException("Informe de 2 a 120 parcelas");}
        plan.totalCents=FinanceUtils.centavos(FinanceUtils.parseValor(total.getText().toString())); Installments.validate(plan); return plan;
    }
    private void updatePreview() {
        if(preview==null) return;
        try {
            InstallmentPlan plan=draft(false); List<Lancamento> items=Installments.schedule(plan); Lancamento last=items.get(items.size()-1);
            preview.setText((plan.count-1)+" × "+FinanceUtils.moeda(FinanceUtils.centavos(items.get(0).valor))+"\nÚltima parcela: "+FinanceUtils.moeda(FinanceUtils.centavos(last.valor))+"\nTotal: "+FinanceUtils.moeda(plan.totalCents)+"\nDe "+FinanceUtils.dataVisivel(firstDate)+" até "+FinanceUtils.dataVisivel(last.data));
        } catch(IllegalArgumentException e) {preview.setText("Preencha o total e a quantidade (2 a 120) para conferir as parcelas.");}
    }
    private void submit() {
        try {
            InstallmentPlan plan=draft(true);
            new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Criar "+plan.count+" parcelas?")
                .setMessage(plan.name+"\n"+plan.account+"\nTotal: "+FinanceUtils.moeda(plan.totalCents)+"\nPrimeira: "+FinanceUtils.dataVisivel(firstDate))
                .setNegativeButton("Revisar",null).setPositiveButton("Criar",(dialog,which) -> model.save(plan)).show();
        } catch(IllegalArgumentException e) {Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("planId",model.id); out.putString("firstDate",firstDate); out.putString("account",account.getSelectedItemPosition()==0?null:account.getSelectedItem().toString()); super.onSaveInstanceState(out);
    }
}
