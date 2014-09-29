package com.chitacan.bridge;

import com.squareup.otto.Bus;

/**
 * Created by chitacan on 2014. 9. 29..
 */
public class BusProvider {
    private static final Bus BUS = new Bus();

    public static Bus getInstance() {
        return BUS;
    }

    private BusProvider() {}
}
