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
    public SaveViewModel(Application app) { super(app); repository = new FinanceiroRepository(app); }
    public void salvar(Lancamento item) {
        if (Integer.valueOf(1).equals(estado.getValue()) || Integer.valueOf(2).equals(estado.getValue())) return;
        estado.setValue(1);
        repository.salvar(item, ok -> estado.setValue(ok ? 2 : 3));
    }
}
