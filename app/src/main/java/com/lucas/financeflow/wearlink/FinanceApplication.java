package com.lucas.financeflow.wearlink;
import android.app.Application;
import androidx.room.InvalidationTracker;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.wearable.*;
import com.lucas.financeflow.data.local.AppDatabase;
public class FinanceApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        if(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)!=ConnectionResult.SUCCESS) return;
        AppDatabase.getInstance(this).getInvalidationTracker().addObserver(new InvalidationTracker.Observer("lancamentos","cadastros") {
            @Override public void onInvalidated(java.util.Set<String> tables) { PhoneSync.publish(FinanceApplication.this); }
        });
        PhoneSync.publish(this);
        Wearable.getDataClient(this).getDataItems().addOnSuccessListener(items -> {
            try { for(DataItem item:items) {
                String path=item.getUri().getPath();
                if(path!=null && path.startsWith(WearProtocol.TX)) PhoneSync.receive(this,path,DataMapItem.fromDataItem(item).getDataMap().getString("json"));
            } } finally { items.release(); }
        });
    }
}
