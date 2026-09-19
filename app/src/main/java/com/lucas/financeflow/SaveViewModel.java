package com.lucas.financeflow;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.lucas.financeflow.data.model.Lancamento;
import com.lucas.financeflow.data.repository.FinanceiroRepository;

/** Keep in-flight writes across rotation so a second tap cannot insert a duplicate. */
public class SaveViewModel extends AndroidViewModel {
    public final MutableLiveData<Integer> estado = new MutableLiveData<>(0);
    private final FinanceiroRepository repository;
    private Lancamento saved;
    private Lancamento previous;
    public void previous(Lancamento item) {if(saved==null) previous=item;}
    public SaveViewModel(Application app) { super(app); repository = new FinanceiroRepository(app); }
    public void salvar(Lancamento item) {
        if (!Integer.valueOf(0).equals(estado.getValue()) && !Integer.valueOf(3).equals(estado.getValue())) return;
        estado.setValue(1);
        repository.salvar(item, ok -> {if(ok) saved=item; estado.setValue(ok ? 2 : 3);});
    }
    public void undo(FinanceiroRepository.Resultado result) {
        if(saved==null || !Integer.valueOf(2).equals(estado.getValue())) return;
        estado.setValue(4);
        FinanceiroRepository.Resultado callback=ok -> {estado.setValue(ok?5:2); result.concluir(ok);};
        if(previous!=null) repository.salvar(previous,callback); else repository.excluir(saved,callback);
    }
}
