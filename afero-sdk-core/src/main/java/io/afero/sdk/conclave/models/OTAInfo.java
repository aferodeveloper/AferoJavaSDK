/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.conclave.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OTAInfo {

    public OTAInfo() {

    }

    public OTAInfo(int s, String deviceId, int o, int t) {
        state = s;
        id = deviceId;
        offset = o;
        total = t;
    }

    public int getProgress() {
        float realTotal = total / 2.0f;
        float progress;

        if (offset <= realTotal) {
            progress = offset / realTotal;
        } else {
            progress = (offset - realTotal) / realTotal;
        }

        return (int)(100f * progress);
    }

    @Override
    public String toString() {
        return "OTAInfo { " +
                "state=" + state +
                ", id='" + id + '\'' +
                ", offset=" + offset +
                ", total=" + total +
                " }";
    }

    public int state;
    public String id;
    public int offset;
    public int total;
}
