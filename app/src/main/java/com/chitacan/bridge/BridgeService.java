package com.chitacan.bridge;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

public class BridgeService extends Service implements Bridge.BridgeListener {

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
        if (mBridge != null && mBridge.isCreated()) {
            Intent intent = new Intent("com.chitacan.bridge.notification");
            intent.putExtra("bundle", bundle);
            sendOrderedBroadcast(intent, null);
        } else {
            Util.denotify(this);
        }
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

    public static class NotificationReceiver extends BroadcastReceiver {

        public static final int NOTIFICATION_STATUS_ID = 1111;

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getBundleExtra("bundle");
            int status = bundle.getInt("bridge_status");
            if (status == 1)
                notifyUser(context, bundle);
        }

        private void notifyUser(Context context, Bundle bundle) {
            String name   = bundle.getString("name");
            String endPoint = bundle.getString("server_endpoint");

            Notification.InboxStyle inbox = new Notification.InboxStyle();
            inbox.addLine("Server : " + name);
            inbox.addLine("EndPoint :" + endPoint);

            Notification.Builder builder = new Notification.Builder(context)
                    .setTicker("Bridge Crated")
                    .setSmallIcon(R.drawable.ic_fa_cloud)
                    .setContentTitle("Bridge Created")
                    .setContentIntent(createContentIntent(context))
                    .setContentText("Connected to " + name)
                    .setOngoing(true)
                    .setStyle(inbox);

            NotificationManager nm = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            nm.notify(NOTIFICATION_STATUS_ID, builder.build());
        }

        private PendingIntent createContentIntent(Context context) {
            Intent intent = new Intent(context, MainActivity.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addParentStack(MainActivity.class);
            stackBuilder.addNextIntent(intent);

            return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }
}
