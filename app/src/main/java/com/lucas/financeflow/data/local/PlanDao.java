package com.lucas.financeflow.data.local;
import androidx.room.*;
import androidx.lifecycle.LiveData;
import com.lucas.financeflow.data.model.PlanItem;
import java.util.List;
@Dao public interface PlanDao {
    @Query("SELECT * FROM planning ORDER BY created, id") List<PlanItem> snapshot();
    @Query("SELECT * FROM planning ORDER BY created, id") LiveData<List<PlanItem>> observe();
    @Query("SELECT * FROM planning WHERE id=:id") PlanItem get(String id);
    @Insert void insert(PlanItem item);
    @Update void update(PlanItem item);
    @Query("DELETE FROM planning WHERE id=:id") void delete(String id);
    @Query("DELETE FROM planning") void clear();
    @Insert void insertAll(List<PlanItem> items);
}
