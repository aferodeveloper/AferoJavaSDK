/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

import android.text.TextUtils;


public class LocationState {

    public enum State {
        Invalid,
        None,
        Valid
    }

    State state;
    Location location;

    public LocationState(Location location) {
        if (location == null) {
            this.state = State.None;
            return;
        }

        if (location.latitude == null ||
                location.longitude == null
                // || location.formattedAddressLines == null
                ) {
            this.state = State.Invalid;
            return;
        }

        this.state = State.Valid;
        this.location = location;
    }

    public LocationState(State state) {
        if (state.equals(State.Valid)) {
            throw new IllegalArgumentException("Valid LocationState must use LocationState(Location)");
        }
        this.state = state;
    }

    public State getState() {
        return state;
    }
    public Location getLocation() {
        if (state.equals(State.Valid)) {
            return location;
        }
        return null;
    }

    public String getAddress() {
        if (state.equals(State.Valid) && location.formattedAddressLines != null) {
            return TextUtils.join(System.getProperty("line.separator"), location.formattedAddressLines);
        }
        return "";
    }

}
