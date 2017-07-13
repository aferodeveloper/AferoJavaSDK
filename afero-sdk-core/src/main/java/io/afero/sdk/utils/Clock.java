/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.utils;

import java.util.concurrent.TimeUnit;

public class Clock {

    public interface ClockImpl {
        long getElapsedMillis();
    }

    private static ClockImpl sClockImpl = new ClockImpl() {
        @Override
        public long getElapsedMillis() {
            return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
        }
    };

    public static long getElapsedMillis() {
        return sClockImpl.getElapsedMillis();
    }

    public static void setClockImpl(ClockImpl clock) {
        sClockImpl = clock;
    }
}
