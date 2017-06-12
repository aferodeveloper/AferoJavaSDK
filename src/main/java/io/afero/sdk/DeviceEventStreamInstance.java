/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk;

import io.afero.sdk.device.DeviceEventStream;

public class DeviceEventStreamInstance {

    private static DeviceEventStream sInstance;

    public static void set(DeviceEventStream store) {
        sInstance = store;
    }

    public static DeviceEventStream get() {
        return sInstance;
    }
}
