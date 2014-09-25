package com.chitacan.bridge;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by chitacan on 2014. 9. 25..
 */
public class Util {
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
