/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;

/*
    {
        "accountId": "",
        "createdTimestamp": 0,
        "deviceGroupId": "",
        "devices": [
            {
                "clientMetadata": "object",
                "deviceId": ""
            }
        ],
        "label": ""
    }
*/

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceGroup {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Device {
        public Device() {}

        public Device(String id) {
            deviceId = id;
        }

        public String deviceId;
        public String clientMetaData;
    }

    private long mLocalId;
    private String mDeviceGroupId;
    private String mLabel;
    private long mCreatedTimestamp;
    private String mAccountId;
    private Vector<Device> mDevices;
    private String mClientMetaData;
    private DeviceRules.Rule[] mRules;

    public DeviceGroup() {
        mLocalId = UUID.randomUUID().getLeastSignificantBits();
    }

    @JsonProperty
    public void setDeviceGroupId(String id) {
        mDeviceGroupId = id;
    }

    public String getDeviceGroupId() {
        return mDeviceGroupId;
    }

    @JsonProperty
    public void setClientMetaData(String s) {
        mClientMetaData = s;
    }

    public String getClientMetaData() {
        return mClientMetaData;
    }

    @JsonIgnore
    public boolean isSaved() {
        return mDeviceGroupId != null;
    }

    @JsonProperty
    public void setCreatedTimestamp(long t) {
        mCreatedTimestamp = t;
    }

    public long getCreatedTimestamp() {
        return mCreatedTimestamp;
    }

    @JsonIgnore
    public void setLocalId(long id) {
        mLocalId = id;
    }

    public long getLocalId() {
        return mLocalId;
    }

    @JsonProperty
    public void setAccountId(String id) {
        mAccountId = id;
    }

    public String getAccountId() {
        return mAccountId;
    }

    @JsonProperty
    public void setLabel(String label) {
        mLabel = label;
    }

    public String getLabel() {
        return mLabel;
    }

    @JsonProperty
    public void setDevices(Vector<Device> deviceList) {
        mDevices = deviceList;
    }

    public Vector<Device> getDevices() {
        return mDevices;
    }

    @JsonIgnore
    public void addDevice(Device d) {
        mDevices.add(d);
    }

    @JsonIgnore
    public void removeDeviceById(String id) {
        Iterator<Device> iter = mDevices.iterator();
        while (iter.hasNext()) {
            Device d = iter.next();
            if (d.deviceId.equals(id)) {
                iter.remove();
                return;
            }
        }
    }

    @JsonIgnore
    public Device findDeviceById(String id) {
        for (Device d: mDevices) {
            if (d.deviceId.equals(id)) {
                return d;
            }
        }

        return null;
    }

    public void setRules(DeviceRules.Rule[] rules) {
        mRules = rules;
    }

    public DeviceRules.Rule[] getRules() {
        return mRules;
    }
}
