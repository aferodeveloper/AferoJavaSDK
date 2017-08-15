/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.conclave.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OTAInfo {

    public enum OtaState {
        START(0),
        ONGOING(1),
        STOP(2);

        private final int mValue;

        OtaState(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        static OtaState fromInt(int s) {
            switch (s) {
                case 0:
                    return OtaState.START;
                case 1:
                    return OtaState.ONGOING;
                case 2:
                    return OtaState.STOP;
            }

            return OtaState.STOP;
        }
    }

    public OTAInfo() {

    }

    public OTAInfo(OtaState s, String deviceId, int o, int t) {
        state = s;
        id = deviceId;
        offset = o;
        total = t;
    }

    public int getProgress() {
        float realTotal = total / 2.0f;
        float realOffset = offset < realTotal ? offset : offset - realTotal;
        float progress = realOffset / (realTotal - 1);

        return (int)Math.min(100f * progress, 100);
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

    public void setState(int s) {
        state = OtaState.fromInt(s);
    }

    public OtaState getState() {
        return state;
    }

    private OtaState state;

    public String id;
    public int offset;
    public int total;
}
