/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

public class LocationState {

    public enum State {
        INVALID,
        NONE,
        VALID
    }

    private State state;
    private Location location;

    public LocationState(Location location) {
        if (location == null) {
            this.state = State.NONE;
            return;
        }

        if (location.latitude == null || location.longitude == null) {
            this.state = State.INVALID;
            return;
        }

        this.state = State.VALID;
        this.location = location;
    }

    public LocationState(State newState) {
        if (newState.equals(State.VALID)) {
            throw new IllegalArgumentException("Valid LocationState must use LocationState(Location)");
        }

        state = newState;
    }

    public State getState() {
        return state;
    }

    public Location getLocation() {
        if (state.equals(State.VALID)) {
            return location;
        }

        return null;
    }

    public String getAddress() {
        if (state.equals(State.VALID) && location.formattedAddressLines != null) {
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
