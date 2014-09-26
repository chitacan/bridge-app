package com.chitacan.bridge;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import java.util.ArrayList;

public class BridgeService extends Service {

    static final int MSG_REGISTER_CLIENT   = 0;
    static final int MSG_UNREGISTER_CLIENT = 1;
    static final int MSG_CREATE_BRIDGE     = 2;
    static final int MSG_REMOVE_BRIDGE     = 3;
    static final int MSG_STATUS_BRIDGE     = 4;

    class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    Log.d("chitacan", "register messenger");
                    break;
                case MSG_UNREGISTER_CLIENT:
                    Log.d("chitacan", "unregister messenger");
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_CREATE_BRIDGE:
                    Log.d("chitacan", "create bridge");
                    createBridge(msg.getData());
                    break;
                case MSG_REMOVE_BRIDGE:
                    Log.d("chitacan", "remove bridge");
                    removeBridge();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private final Messenger mMessenger = new Messenger(new IncomingHandler());
    private ArrayList<Messenger> mClients = new ArrayList();
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
    public IBinder onBind(Intent intent) {
        Log.d("chitacan", "onBind");
        return mMessenger.getBinder();
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
