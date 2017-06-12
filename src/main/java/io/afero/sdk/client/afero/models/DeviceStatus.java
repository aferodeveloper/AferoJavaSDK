/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceStatus {
    public Boolean available;
    public Boolean visible;
    public Boolean direct;
    public Boolean connectable;
    public Boolean connected;
    public Boolean linked;
    public Integer rssi;

    @Override
    public String toString() {
        return "{ " +
                "available=" + available +
                ", visible=" + visible +
                ", direct=" + direct +
                ", connectable=" + connectable +
                ", connected=" + connected +
                ", linked=" + linked +
                ", rssi=" + rssi +
                " }";
    }
}
