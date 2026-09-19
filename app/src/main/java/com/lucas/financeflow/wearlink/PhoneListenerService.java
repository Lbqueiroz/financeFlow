package com.lucas.financeflow.wearlink;
import com.google.android.gms.wearable.*;
public class PhoneListenerService extends WearableListenerService {
    @Override public void onDataChanged(DataEventBuffer events) {
        for (DataEvent event:events) {
            String path=event.getDataItem().getUri().getPath();
            if(event.getType()==DataEvent.TYPE_CHANGED && path!=null && path.startsWith(WearProtocol.TX))
                PhoneSync.receive(this,path,DataMapItem.fromDataItem(event.getDataItem()).getDataMap().getString("json"));
        }
    }
    @Override public void onMessageReceived(MessageEvent event) {
        if(WearProtocol.REFRESH.equals(event.getPath())) PhoneSync.publish(this);
    }
}
