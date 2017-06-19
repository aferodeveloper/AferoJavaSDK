/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.android.clock;

import android.os.SystemClock;

import io.afero.sdk.utils.Clock;

public class AndroidClock implements Clock.ClockImpl {

    public static void init() {
        Clock.setClockImpl(new AndroidClock());
    }

    @Override
    public long getElapsedMillis() {
        return SystemClock.elapsedRealtime();
    }
}
