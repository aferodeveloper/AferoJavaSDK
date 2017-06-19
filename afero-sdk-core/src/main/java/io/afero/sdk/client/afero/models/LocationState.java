/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

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
            final StringBuilder sb = new StringBuilder();
            final String delim = System.getProperty("line.separator");
            int i = 0, n = location.formattedAddressLines.length;

            for (String s : location.formattedAddressLines) {
                sb.append(s);
                if (i < n) {
                    sb.append(delim);
                }
                ++i;
            }

            return sb.toString();
        }

        return "";
    }

}
