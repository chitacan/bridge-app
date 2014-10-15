package com.chitacan.bridge;

import android.os.Bundle;

/**
 * Created by chitacan on 2014. 10. 15..
 */
public class StatusItem {
    String title;
    String value;
    String bundleKey;
    boolean isHeader = false;
    boolean hasPref = false;

    private StatusItem() {}

    public static StatusItem create() {
        return new StatusItem();
    }

    public StatusItem title(String title) {
        this.title = title;
        return this;
    }

    public StatusItem value(Bundle bundle) {
        if (!bundle.containsKey(bundleKey)) return this;

        this.value = String.valueOf(bundle.get(bundleKey));
        return this;
    }

    public StatusItem header() {
        this.isHeader = true;
        return this;
    }

    public StatusItem pref() {
        this.hasPref = true;
        return this;
    }

    public StatusItem key(String key) {
        this.bundleKey = key;
        return this;
    }
}
