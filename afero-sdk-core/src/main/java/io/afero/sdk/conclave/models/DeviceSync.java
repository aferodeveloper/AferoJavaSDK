/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.conclave.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.HashMap;

import io.afero.sdk.client.afero.models.DeviceStatus;
import io.afero.sdk.client.afero.models.DeviceTag;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceSync {

    /*
         // Success states
        #define UPDATE_STATE_UPDATED                0

        // Failure states
        #define UPDATE_STATE_INTERRUPTED            1
        #define UPDATE_STATE_UNKNOWN_UUID           2
        #define UPDATE_STATE_LENGTH_EXCEEDED        3
        #define UPDATE_STATE_CONFLICT               4
        #define UPDATE_STATE_TIMEOUT                5
     */

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AttributeEntry {

        public int id;
        public String value;
        public long updatedTimestamp;

        public AttributeEntry() {}
        public AttributeEntry(int id, String value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public String toString() {
            return "{ " +
                    "id=" + id +
                    ", value='" + value + '\'' +
                    ", updatedTimestamp=" + updatedTimestamp +
                    " }";
        }
    }

    public static final int UPDATE_STATE_UPDATED = 0;
    public static final int UPDATE_STATE_INTERRUPTED = 1;
    public static final int UPDATE_STATE_UNKNOWN_UUID = 2;
    public static final int UPDATE_STATE_LENGTH_EXCEEDED = 3;
    public static final int UPDATE_STATE_CONFLICT = 4;
    public static final int UPDATE_STATE_TIMEOUT = 5;

    public String deviceId;
    public String profileId;
    public int seq;
    public long createdTimestamp;
    public DeviceTag[] tags;
    public boolean virtual;
    public AttributeEntry[] attributes;
    public AttributeEntry attribute;
    public DeviceStatus status;
    public String friendlyName;
    public Integer requestId;
    public Integer state;
    public DeviceTimeZone timezone;

    public boolean hasValidAttributeValues() {
        // See https://kibanlabs.atlassian.net/browse/ANDROID-606
        // "For states 0, 1, 4, and 5 your going to want to update the UI with the value that is returned.
        // In each of these cases the device is going to be returning the current value. In the failure"
        // cases this value will not be the value that you attempted to set. It will likely be the
        // previous value of that attribute.
        // For states 2 and 3 the device is going to return 0 length for the value, so we probably
        // don't want to update the UI with that value." --lucas
        boolean hasValidValues = true;
        if (state != null) {
            switch (state) {
                case DeviceSync.UPDATE_STATE_UNKNOWN_UUID:
                case DeviceSync.UPDATE_STATE_LENGTH_EXCEEDED:
                    return false;
            }
        }

        return true;
    }

    public boolean hasRequestId() {
        return requestId != null && requestId != 0;
    }

    public void setDeviceState(DeviceStatus s) {
        status = s;
    }

    public void setId(String id) {
        deviceId = id;
    }

    public void setDeviceId(String id) {
        deviceId = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    @Override
    public String toString() {
        return "{ " +
                "deviceId='" + deviceId + '\'' +
                ", profileId='" + profileId + '\'' +
                ", seq=" + seq +
                ", createdTimeStamp=" + createdTimestamp +
                ", tags=" + Arrays.toString(tags) +
                ", virtual=" + virtual +
                ", attributes=" + Arrays.toString(attributes) +
                ", attribute=" + attribute +
                ", status=" + status +
                ", friendlyName='" + friendlyName + '\'' +
                ", requestId=" + requestId +
                ", state=" + state +
                " }";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public class DeviceTimeZone {
        public Boolean userOverride;
        public String timezone;
    }
}
