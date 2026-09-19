package com.lucas.financeflow.watch;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import android.text.*;
import com.lucas.financeflow.wearlink.WearProtocol;
import org.json.*;

public class WatchActivity extends androidx.activity.ComponentActivity implements android.content.SharedPreferences.OnSharedPreferenceChangeListener {
    private LinearLayout content;
    private String screen="home",type="SAIDA",account="",category="Outros",digits="";
    private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getOnBackPressedDispatcher().addCallback(this,new androidx.activity.OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { if(screen.equals("home")) finish(); else home(); }
        });
        if(state!=null) { type=state.getString("type","SAIDA"); account=state.getString("account",""); category=state.getString("category","Outros"); digits=state.getString("digits",""); screen=state.getString("screen","home"); }
        if(screen.equals("form") || screen.equals("review")) form(); else home();
    }
    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); out.putString("screen",screen); out.putString("type",type); out.putString("account",account); out.putString("category",category); out.putString("digits",digits); }
    @Override protected void onResume() { super.onResume(); getSharedPreferences("watch",MODE_PRIVATE).registerOnSharedPreferenceChangeListener(this); WatchSync.refresh(this); }
    @Override protected void onPause() { getSharedPreferences("watch",MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(this); super.onPause(); }
    @Override public void onSharedPreferenceChanged(android.content.SharedPreferences prefs,String key) { handler.post(() -> { if(screen.equals("home")) home(); else if(screen.equals("pending")) pending(); }); }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    private void page(String title) {
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setGravity(Gravity.CENTER_HORIZONTAL); content.setPadding(dp(24),dp(30),dp(24),dp(36));
        scroll.addView(content); setContentView(scroll); label(title,16,0xff8bd6ba);
    }
    private void label(String text,int size,int color) { TextView view=new TextView(this); view.setText(text); view.setTextSize(size); view.setTextColor(color); view.setGravity(Gravity.CENTER); view.setPadding(0,dp(5),0,dp(5)); content.addView(view,new LinearLayout.LayoutParams(-1,-2)); }
    private void button(String text,Runnable action) {
        Button b=new Button(this); b.setText(text); b.setAllCaps(false); b.setTextSize(14); b.setTextColor(0xffecfff5); b.setMinHeight(dp(48));
        android.graphics.drawable.GradientDrawable shape=new android.graphics.drawable.GradientDrawable(); shape.setColor(0xff234b3b); shape.setCornerRadius(dp(28));
        b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0xff52866f),shape,null));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2); params.topMargin=dp(6); content.addView(b,params); b.setOnClickListener(v -> action.run());
    }
    private WatchStore store() { return new WatchStore(this); }
    private void home() {
        screen="home"; page("FinanceFlow"); JSONObject summary=store().summary();
        String month=summary.optString("month");
        if(month.equals(WearProtocol.month())) { label("Saldo do mês",13,Color.WHITE); label(WearProtocol.money(summary.optLong("balanceCents")),24,0xff8bd6ba); }
        else { label("Aguardando saldo do mês",17,Color.WHITE); if(!month.isEmpty()) label("Último saldo: "+month+"\n"+WearProtocol.money(summary.optLong("balanceCents")),12,Color.LTGRAY); }
        long updated=summary.optLong("updatedAt");
        label(updated==0 ? "Abra o FinanceFlow no celular pareado para sincronizar." : "Atualizado "+new java.text.SimpleDateFormat("dd/MM HH:mm",WearProtocol.BR).format(new java.util.Date(updated)),11,Color.LTGRAY);
        button("− Saída",() -> start("SAIDA")); button("+ Entrada",() -> start("ENTRADA"));
        button("Pendentes ("+store().pending().length()+")",this::pending);
        button("Atualizar",() -> { WatchSync.refresh(this); Toast.makeText(this,"Atualização solicitada",Toast.LENGTH_SHORT).show(); });
    }
    private void start(String nextType) {
        JSONArray accounts=store().summary().optJSONArray("accounts");
        if(accounts==null || accounts.length()==0) { Toast.makeText(this,"Cadastre uma conta no celular e sincronize",Toast.LENGTH_LONG).show(); return; }
        type=nextType; digits=""; category="Outros";
        String last=getSharedPreferences("watch",MODE_PRIVATE).getString("lastAccount",""); account=accounts.optString(0);
        for(int i=0;i<accounts.length();i++) if(last.equals(accounts.optString(i))) account=last;
        form();
    }
    private void form() {
        screen="form"; page(type.equals("SAIDA")?"Nova saída":"Nova entrada");
        EditText amount=new EditText(this); amount.setSingleLine(true); amount.setTextColor(Color.WHITE); amount.setTextSize(22); amount.setGravity(Gravity.CENTER); amount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); amount.setHint("0,00"); amount.setContentDescription("Valor em reais");
        if(!digits.isEmpty()) amount.setText(WearProtocol.amount(Long.parseLong(digits)));
        content.addView(amount,new LinearLayout.LayoutParams(-1,dp(54)));
        amount.addTextChangedListener(new TextWatcher() { boolean changing;
            public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            public void onTextChanged(CharSequence s,int start,int before,int count) { }
            public void afterTextChanged(Editable value) {
                if(changing) return; changing=true; String raw=value.toString().replaceAll("[^0-9]",""); if(raw.length()>11) raw=raw.substring(0,11);
                digits=raw.isEmpty()?"":Long.toString(Long.parseLong(raw));
                String formatted=digits.isEmpty()?"":WearProtocol.amount(Long.parseLong(digits)); amount.setText(formatted); amount.setSelection(formatted.length()); changing=false;
            }
        });
        button(account,() -> { page("Conta"); JSONArray list=store().summary().optJSONArray("accounts"); if(list!=null) for(int i=0;i<list.length();i++) { String name=list.optString(i); button(name,() -> {account=name; form();}); } button("Voltar",this::form); });
        button(category,() -> { page("Categoria"); String[] categories=type.equals("SAIDA")?new String[]{"Alimentação","Transporte","Moradia","Saúde","Lazer","Outros"}:new String[]{"Salário","Freelance","Investimentos","Outros"}; for(String name:categories) button(name,() -> {category=name; form();}); button("Voltar",this::form); });
        button("Revisar",this::review); button("Cancelar",this::home);
    }
    private void review() {
        if(digits.isEmpty() || Long.parseLong(digits)==0) { Toast.makeText(this,"Informe o valor",Toast.LENGTH_SHORT).show(); return; }
        screen="review"; page("Confirmar "+(type.equals("SAIDA")?"saída":"entrada")); label(WearProtocol.money(Long.parseLong(digits)),24,Color.WHITE); label(account+"\n"+category,14,Color.LTGRAY);
        button("Salvar",() -> {
            try {
                synchronized(WatchSync.LOCK) { store().add(new WearProtocol.Entry(java.util.UUID.randomUUID().toString(),type,account,category,WearProtocol.today(),Long.parseLong(digits))); }
                getSharedPreferences("watch",MODE_PRIVATE).edit().putString("lastAccount",account).apply();
                digits=""; home(); WatchSync.flush(this); Toast.makeText(this,"Salvo. Aguardando o celular.",Toast.LENGTH_LONG).show();
            } catch(Exception e) { Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show(); }
        }); button("Voltar",this::form);
    }
    private void pending() {
        screen="pending"; page("Pendentes"); JSONArray list=store().pending();
        if(list.length()==0) label("Tudo sincronizado",16,Color.WHITE);
        for(int i=0;i<list.length();i++) { JSONObject item=list.optJSONObject(i); if(item!=null) { label(("SAIDA".equals(item.optString("type"))?"− ":"+ ")+WearProtocol.money(item.optLong("cents"))+"\n"+item.optString("account"),16,Color.WHITE); label(item.optString("error","Aguardando confirmação do celular"),12,Color.LTGRAY); } }
        if(list.length()>0) { label("O saldo só muda após a confirmação. Não repita o lançamento no celular.",12,Color.LTGRAY); button("Tentar novamente",() -> { WatchSync.refresh(this); Toast.makeText(this,"Sincronização solicitada",Toast.LENGTH_SHORT).show(); }); }
        button("Voltar",this::home);
    }
}
