package com.chitacan.bridge;

import android.os.Bundle;

/**
 * Created by chitacan on 2014. 9. 29..
 */
public class BridgeEvent {

    public static final int CREATE  = 0;
    public static final int REMOVE  = 1;
    public static final int STATUS  = 2;
    public static final int CREATED = 3;
    public static final int REMOVED = 4;
    public static final int ERROR   = 5;

    public final int type;
    public final Bundle bundle;

    public BridgeEvent(int type) {
        this(type, null);
    }

    public BridgeEvent(int type, Bundle bundle) {
        this.type = type;
        this.bundle = bundle;
    }
}
