package com.chitacan.bridge;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

public class BridgeService extends Service implements Bridge.BridgeListener {

    private Bridge mBridge;
    private boolean mIsRegistered = false;

    public BridgeService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mIsRegistered) {
            BusProvider.getInstance().register(this);
            mIsRegistered = true;
        }
        return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        if (mIsRegistered) {
            BusProvider.getInstance().unregister(this);
            mIsRegistered = false;
        }
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
            bundle.putInt("adbport", getPortNumber());

            mBridge = new Bridge(this, this);
            mBridge.create(bundle);
        }
    }

    private int getPortNumber() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        return Integer.parseInt( pref.getString(
                getString(R.string.pref_key_adb_port),
                getString(R.string.pref_default_adbd_port)
        ));
    }

    private void removeBridge() {
        if (mBridge == null)
            BusProvider.getInstance().post(new BridgeEvent(BridgeEvent.STATUS));

        if (mBridge != null) {
            mBridge.remove();
            mBridge = null;
        }
    }

    @Subscribe
    public void bridgeEvent(BridgeEvent event) {
        switch (event.type) {
            case BridgeEvent.CREATE:
                createBridge(event.bundle);
                break;
            case BridgeEvent.REMOVE:
                removeBridge();
                break;
        }
    }

    @Override
    public void onStatusUpdate(Bundle bundle) {
        BusProvider.getInstance().post(new BridgeEvent(BridgeEvent.STATUS, bundle));
    }

    @Override
    public void onBridgeError(Bundle bundle) {
        BusProvider.getInstance().post(new BridgeEvent(BridgeEvent.ERROR, bundle));
    }

    @Produce
    public BridgeEvent produceBridgeStatus() {
        return new BridgeEvent(BridgeEvent.STATUS, mBridge == null ? null : mBridge.getStatus());
    }
}
