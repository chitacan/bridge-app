package com.chitacan.bridge;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class BridgeService extends Service {

    private Bridge mBridge;

    public BridgeService() {
    }

    @Override
    public void onCreate() {
        Log.d("chitacan", "onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("chitacan", "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("chitacan", "onUnBind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d("chitacan", "onDestroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createBridge(Bundle bundle) {
        if (mBridge != null)
            mBridge.remove();

        if (bundle != null) {
            if (!bundle.containsKey("adbport"))
                bundle.putInt("adbport", 6666);

            mBridge = new Bridge();
            mBridge.create(bundle);
        }
    }

    private void removeBridge() {
        if (mBridge != null) {
            mBridge.remove();
            mBridge = null;
        }
    }
}
