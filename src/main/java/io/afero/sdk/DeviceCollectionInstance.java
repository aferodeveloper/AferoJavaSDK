/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk;

import io.afero.sdk.device.DeviceCollection;

public class DeviceCollectionInstance {

    private static DeviceCollection sInstance;

    public static void set(DeviceCollection dc) {
        sInstance = dc;
    }

    public static DeviceCollection get() {
        return sInstance;
    }
}
