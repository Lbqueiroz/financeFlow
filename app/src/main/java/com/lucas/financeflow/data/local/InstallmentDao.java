package com.lucas.financeflow.data.local;
import androidx.room.*;
import androidx.lifecycle.LiveData;
import com.lucas.financeflow.data.model.InstallmentPlan;
import java.util.List;
@Dao public interface InstallmentDao {
    @Query("SELECT * FROM installment_plans ORDER BY firstDate DESC, name") LiveData<List<InstallmentPlan>> observe();
    @Query("SELECT * FROM installment_plans ORDER BY firstDate DESC, name") List<InstallmentPlan> snapshot();
    @Query("SELECT * FROM installment_plans WHERE id=:id") InstallmentPlan get(String id);
    @Insert void insert(InstallmentPlan item);
    @Insert void insertAll(List<InstallmentPlan> items);
    @Query("DELETE FROM installment_plans WHERE id=:id") void delete(String id);
    @Query("DELETE FROM installment_plans") void clear();
}
