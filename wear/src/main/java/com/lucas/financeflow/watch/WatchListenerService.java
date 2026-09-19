package com.lucas.financeflow.watch;
import com.google.android.gms.wearable.*;
public class WatchListenerService extends WearableListenerService {
    @Override public void onDataChanged(DataEventBuffer events) {
        for(DataEvent event:events) if(event.getType()==DataEvent.TYPE_CHANGED) WatchSync.receive(this,event.getDataItem());
    }
}
