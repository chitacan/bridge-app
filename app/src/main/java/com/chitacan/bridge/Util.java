package com.chitacan.bridge;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

/**
 * Created by chitacan on 2014. 9. 25..
 */
public class Util {
    static final String TAG = "chitacan";

    static final String createTag() {
        return createTag(null);
    }

    static final String createTag(String localTag) {
        String seprator = localTag == null ? "" : ":";
        return new StringBuffer(TAG).append(seprator).append(localTag).toString();
    }

    static final void Log(String message) {
        if (BuildConfig.DEBUG)
            Log.i(createTag(), message);
    }

    static final void Log(String tag, String message) {
        if (BuildConfig.DEBUG)
            Log.i(createTag(tag), message);
    }

    static String createUrl(String host, int port, String path) {
        if (path == null) path = "";
        try {
            URL url = new URL(
                    "http",
                    host,
                    port,
                    path
            );
            return url.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static HashMap<String, BroadcastReceiver> receivers = new HashMap<String, BroadcastReceiver>();

    static void registerRejectReceiver(Activity activity, String key) {
        IntentFilter filter = new IntentFilter("com.chitacan.bridge.notification");
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);

        BroadcastReceiver receiver;
        if (receivers.containsKey(key)) {
            receiver = receivers.get(key);
        } else {
            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    abortBroadcast();
                }
            };
            receivers.put(key, receiver);
        }

        activity.registerReceiver(receiver, filter);
    }

    static void unregisterRejectReceiver(Activity activity, String key) {
        if (receivers.containsKey(key)) {
            activity.unregisterReceiver(receivers.get(key));
            receivers.remove(key);
        }
    }
}
