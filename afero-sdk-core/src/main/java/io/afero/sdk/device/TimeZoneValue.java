/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;


import java.util.TimeZone;

final class TimeZoneValue {

    enum State {
        NOT_SET,
        INVALIDATED,
        SET
    }

    private State mState;
    private TimeZone mTimeZone;

    TimeZoneValue() {
        mState = State.NOT_SET;
    }

    synchronized void setTimeZone(TimeZone timeZone) {
        mTimeZone = timeZone;
        mState = State.SET;
    }

    synchronized TimeZone getTimeZone() {
        return mTimeZone;
    }

    synchronized State getState() {
        return mState;
    }

    synchronized void invalidate() {
        mState = State.INVALIDATED;
        mTimeZone = null;
    }

}
