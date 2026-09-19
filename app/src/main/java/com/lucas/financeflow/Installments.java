package com.lucas.financeflow;
import com.lucas.financeflow.data.model.*;
import java.util.*;

public final class Installments {
    private Installments() { }
    public static void validate(InstallmentPlan plan) {
        if(!plan.id.matches("[a-zA-Z0-9-]{1,80}")) throw new IllegalArgumentException("Identificador inválido");
        if(plan.name.trim().isEmpty() || plan.name.length()>120) throw new IllegalArgumentException("Informe o nome da compra (até 120 caracteres)");
        if(plan.account.trim().isEmpty() || plan.account.length()>120 || plan.category.trim().isEmpty() || plan.category.length()>120) throw new IllegalArgumentException("Selecione conta e categoria");
        if(plan.count<2 || plan.count>120) throw new IllegalArgumentException("Informe de 2 a 120 parcelas");
        if(plan.totalCents<plan.count || plan.totalCents>Planning.MAX) throw new IllegalArgumentException("O total deve permitir ao menos R$ 0,01 por parcela");
        Planning.strictDate(plan.firstDate);
    }
    public static List<Lancamento> schedule(InstallmentPlan plan) {
        validate(plan);
        List<Lancamento> entries=new ArrayList<>();
        Calendar first=Calendar.getInstance(); first.setTime(Planning.strictDate(plan.firstDate)); int day=first.get(Calendar.DAY_OF_MONTH);
        String date=plan.firstDate; long base=plan.totalCents/plan.count;
        for(int number=1;number<=plan.count;number++) {
            long cents=number==plan.count?plan.totalCents-base*(plan.count-1):base;
            Lancamento entry=new Lancamento(plan.name+" · "+number+"/"+plan.count,java.math.BigDecimal.valueOf(cents,2).doubleValue(),"SAIDA",plan.category,date,"CELULAR","LOCAL","",plan.account);
            entry.installmentPlanId=plan.id; entry.installmentNumber=number; entries.add(entry);
            if(number<plan.count) date=Planning.nextDate(date,day);
        }
        return entries;
    }
    public static void validateLinks(List<InstallmentPlan> plans,List<Lancamento> entries) {
        Map<String,InstallmentPlan> map=new HashMap<>(); Set<String> numbers=new HashSet<>();
        for(InstallmentPlan p:plans) {validate(p); if(map.put(p.id,p)!=null) throw new IllegalArgumentException("Parcelamento repetido");}
        for(Lancamento entry:entries) {
            if(entry.installmentPlanId==null) {if(entry.installmentNumber!=0) throw new IllegalArgumentException("Parcela sem compra"); continue;}
            InstallmentPlan plan=map.get(entry.installmentPlanId);
            if(plan==null || entry.installmentNumber<1 || entry.installmentNumber>plan.count || !numbers.add(plan.id+":"+entry.installmentNumber)) throw new IllegalArgumentException("Vínculo de parcela inválido");
        }
    }
}
