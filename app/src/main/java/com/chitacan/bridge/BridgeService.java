package com.chitacan.bridge;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

public class BridgeService extends Service implements Bridge.BridgeListener {

    private static final int mNotificationID = 1111;
    private Bridge mBridge;
    private boolean mIsRegistered = false;
    private PowerManager.WakeLock mWakeLock;

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
            bundle.putInt("adbd_port", getPortNumber());

            mBridge = new Bridge(this, this);
            mBridge.create(bundle);
        }
    }

    private int getPortNumber() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        return Integer.parseInt(pref.getString(
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
                wakeLock(true);
                createBridge(event.bundle);
                break;
            case BridgeEvent.REMOVE:
                removeBridge();
                wakeLock(false);
                break;
        }
    }

    private void notifyUser(Bundle bundle) {
        String name     = bundle.getString("name");
        String endPoint = bundle.getString("server_endpoint");

        Notification.InboxStyle inbox = new Notification.InboxStyle();
        inbox.addLine("Server : " + name);
        inbox.addLine("EndPoint :" + endPoint);

        Notification.Builder builder = new Notification.Builder(this)
                .setTicker("Bridge Crated")
                .setSmallIcon(R.drawable.ic_fa_cloud)
                .setContentTitle("Bridge Created")
                .setContentIntent(createContentIntent())
                .setContentText("Connected to " + name)
                .setStyle(inbox);

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(mNotificationID, builder.build());
    }

    private void denotify() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(mNotificationID);
    }

    private PendingIntent createContentIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(intent);

        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void wakeLock(boolean isAquire) {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);

        if (isAquire) {
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "bridge_wakelock");
            mWakeLock.acquire();
            Util.Log("AQUIRE_WAKELOCK");
        } else {
            mWakeLock.release();
            Util.Log("RELEASE_WAKELOCK");
        }
    }

    @Override
    public void onStatusUpdate(Bundle bundle) {
        if (mBridge != null && mBridge.isCreated())
            notifyUser(bundle);
        else
            denotify();

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
