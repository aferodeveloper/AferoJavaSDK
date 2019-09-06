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

    public String id;
    public String profileId;
    public int seq;
    public long createdTimeStamp;
    public DeviceTag[] tags;
    public boolean virtual;
    public AttributeEntry[] attributes;
    public AttributeEntry attribute;
    public DeviceStatus status;
    public String friendlyName;
    public Integer requestId;
    public Integer state;

    @Override
    public String toString() {
        return "{ " +
                "id='" + id + '\'' +
                ", profileId='" + profileId + '\'' +
                ", seq=" + seq +
                ", createdTimeStamp=" + createdTimeStamp +
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
}
