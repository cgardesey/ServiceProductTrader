package com.service.provision.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class GuidChangeListenerService extends Service {
    public GuidChangeListenerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}