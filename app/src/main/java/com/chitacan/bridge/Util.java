package com.chitacan.bridge;

import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

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
}
