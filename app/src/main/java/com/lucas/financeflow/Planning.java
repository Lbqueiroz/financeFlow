package com.lucas.financeflow;

import com.lucas.financeflow.data.model.*;
import java.util.*;

/** Pure calculations shared by all views and by transactional validation. */
public final class Planning {
    private Planning() { }
    public static final long MAX = 99999999999L;
    public static void validate(PlanItem p) {
        if(p.id.isEmpty() || p.id.length()>150 || p.name.length()>120 || p.account.length()>120 || p.target.length()>150 || p.category.length()>120) throw new IllegalArgumentException("Campo inválido");
        if(!Arrays.asList("TRANSFER","ASSET","APPLY","REDEEM","VALUE","RULE","BUDGET","CONFIRMED").contains(p.kind)) throw new IllegalArgumentException("Operação inválida");
        if(p.cents<0 || p.cents>MAX || (!Arrays.asList("ASSET","VALUE","CONFIRMED").contains(p.kind) && p.cents==0)) throw new IllegalArgumentException("Informe um valor maior que zero");
        strictDate(p.date);
        if("RULE".equals(p.kind) && (p.day<1 || p.day>31 || p.name.trim().isEmpty() || !(p.type.equals("ENTRADA") || p.type.equals("SAIDA")))) throw new IllegalArgumentException("Recorrência inválida");
        if(Arrays.asList("TRANSFER","APPLY","REDEEM","RULE").contains(p.kind) && p.account.trim().isEmpty()) throw new IllegalArgumentException("Selecione uma conta");
        if(Arrays.asList("TRANSFER","APPLY","REDEEM","VALUE","CONFIRMED").contains(p.kind) && p.target.isEmpty()) throw new IllegalArgumentException("Selecione o destino");
        if("ASSET".equals(p.kind) && p.name.trim().isEmpty()) throw new IllegalArgumentException("Informe o nome do investimento");
        if("TRANSFER".equals(p.kind) && p.account.equalsIgnoreCase(p.target)) throw new IllegalArgumentException("Escolha contas diferentes");
        if("BUDGET".equals(p.kind) && p.category.trim().isEmpty()) throw new IllegalArgumentException("Selecione uma categoria");
    }
    public static String nextDate(String current,int day) {
        Calendar next=Calendar.getInstance(); next.setTime(strictDate(current)); next.set(Calendar.DAY_OF_MONTH,1); next.add(Calendar.MONTH,1);
        next.set(Calendar.DAY_OF_MONTH,Math.min(day,next.getActualMaximum(Calendar.DAY_OF_MONTH)));
        return new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(next.getTime());
    }
    public static Date strictDate(String value) {
        java.text.SimpleDateFormat format=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.ROOT); format.setLenient(false);
        java.text.ParsePosition position=new java.text.ParsePosition(0); Date date=format.parse(value,position);
        if(!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}") || date==null || position.getIndex()!=value.length()) throw new IllegalArgumentException("Data inválida");
        return date;
    }
    public static long assetValue(List<PlanItem> items,String id) {
        long value=0;
        for(PlanItem p:items) if(id.equals(p.target)) {
            if(p.kind.equals("APPLY")) value+=p.cents;
            if(p.kind.equals("REDEEM")) value-=p.cents;
            if(p.kind.equals("VALUE")) value=p.cents;
        }
        return value;
    }
    public static long netContributions(List<PlanItem> items,String id) {
        long value=0;
        for(PlanItem p:items) if(id.equals(p.target)) { if(p.kind.equals("APPLY")) value+=p.cents; if(p.kind.equals("REDEEM")) value-=p.cents; }
        return value;
    }
    public static void adjustAccounts(Map<String,Long> balances,List<PlanItem> items) {
        for(PlanItem p:items) {
            if(p.kind.equals("TRANSFER")) { change(balances,p.account,-p.cents); change(balances,p.target,p.cents); }
            if(p.kind.equals("APPLY")) change(balances,p.account,-p.cents);
            if(p.kind.equals("REDEEM")) change(balances,p.account,p.cents);
        }
    }
    private static void change(Map<String,Long> balances,String key,long delta) {
        String actual=key; for(String existing:balances.keySet()) if(existing.equalsIgnoreCase(key)) { actual=existing; break; }
        balances.put(actual,balances.getOrDefault(actual,0L)+delta);
    }
    public static long spent(List<Lancamento> items,String month,String category) {
        long sum=0; for(Lancamento p:items) if("SAIDA".equals(p.tipo) && p.data.startsWith(month+"-") && category.equals(p.categoria)) sum+=FinanceUtils.centavos(p.valor); return sum;
    }
    public static void validateLedger(List<PlanItem> items) {
        Set<String> ids=new HashSet<>(); Map<String,Long> values=new HashMap<>();
        for(PlanItem p:items) { validate(p); if(!ids.add(p.id)) throw new IllegalArgumentException("Identificador repetido"); if(p.kind.equals("ASSET")) values.put(p.id,0L); }
        for(PlanItem p:items) if(Arrays.asList("APPLY","REDEEM","VALUE").contains(p.kind)) {
            if(!values.containsKey(p.target)) throw new IllegalArgumentException("Investimento inexistente");
            long value=values.get(p.target);
            value=p.kind.equals("VALUE")?p.cents:value+(p.kind.equals("APPLY")?p.cents:-p.cents);
            if(value<0) throw new IllegalArgumentException("O resgate ultrapassa o valor do investimento"); values.put(p.target,value);
        }
    }
}
