package com.lucas.financeflow.wearlink;
import com.lucas.financeflow.data.local.AppDatabase;
import com.lucas.financeflow.data.local.LancamentoDao;
import com.lucas.financeflow.data.model.*;

public final class WatchImporter {
    private WatchImporter() { }
    public static void accept(AppDatabase db, WearProtocol.Entry entry) {
        db.runInTransaction(() -> {
            LancamentoDao dao = db.lancamentoDao();
            if (dao.wearReceipt(entry.id) > 0) return;
            if (dao.contaExiste(entry.account) == 0) throw new IllegalArgumentException("Conta indisponível. Cadastre-a no celular e tente novamente.");
            dao.inserir(new Lancamento("ENTRADA".equals(entry.type) ? "Entrada pelo relógio" : "Gasto pelo relógio",
                    java.math.BigDecimal.valueOf(entry.cents,2).doubleValue(),entry.type,entry.category,entry.date,"RELOGIO","SINCRONIZADO","",entry.account));
            dao.registrarWearReceipt(new WearReceipt(entry.id));
        });
    }
}
