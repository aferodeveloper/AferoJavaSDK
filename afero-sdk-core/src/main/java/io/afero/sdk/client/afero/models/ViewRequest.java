/*
 * Copyright (c) 2014-2018 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;


public final class ViewRequest {
    public final String type = "notify_viewing";
    public final long seconds;

    private ViewRequest(long timeout) {
        seconds = timeout;
    }

    public static ViewRequest start(long durationInSeconds) {
        return new ViewRequest(durationInSeconds);
    }

    public static ViewRequest stop() {
        return new ViewRequest(0);
    }
}
