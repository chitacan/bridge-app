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

    class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    mClients.add(msg.replyTo);
                    Log.d("chitacan", "register messenger");
                    break;
                case 1:
                    Log.d("chitacan", "unregister messenger");
                    mClients.remove(msg.replyTo);
                    break;
                case 2:
                    Log.d("chitacan", "msg");
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
        if (mBridge != null)
            mBridge.remove();

        Bundle bundle = intent.getBundleExtra("server");

        if (bundle != null) {
            mBridge = new Bridge();
            mBridge.create(bundle);
        }

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
        if (mBridge != null) {
            mBridge.remove();
            mBridge = null;
        }
        super.onDestroy();
    }
}
