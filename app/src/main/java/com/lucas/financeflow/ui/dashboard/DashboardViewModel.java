package com.lucas.financeflow.ui.dashboard;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;


import com.lucas.financeflow.data.repository.FinanceiroRepository;

public class DashboardViewModel extends AndroidViewModel {

    private final FinanceiroRepository repository;

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        repository = new FinanceiroRepository(application);
    }

    public LiveData<Double> getTotalEntradas() {
        return repository.totalEntradas();
    }

    public LiveData<Double> getTotalSaidas() {
        return repository.totalSaidas();
    }


}
