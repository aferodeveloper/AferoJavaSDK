/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;
import java.util.Vector;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Scene {

    private String mSceneId;
    private long mLocalId;
    private String mLabel;
    private Vector<DeviceRules.DeviceAction> mDeviceActions;
    private String mDeviceGroupId;

    public Scene() {
        mLocalId = UUID.randomUUID().getLeastSignificantBits();
    }

    public Scene(Scene s) {
        mLocalId = UUID.randomUUID().getLeastSignificantBits();
        mSceneId = s.getSceneId();
        mLabel = s.getLabel();
        mDeviceGroupId = s.getDeviceGroupId();
        if (s.getDeviceActions() != null) {
            mDeviceActions = (Vector<DeviceRules.DeviceAction>)s.getDeviceActions().clone();
        }
    }

    @JsonProperty
    public void setSceneId(String id) {
        mSceneId = id;
    }

    public String getSceneId() {
        return mSceneId;
    }

    @JsonIgnore
    public boolean isSaved() {
        return mSceneId != null;
    }

    @JsonIgnore
    public void setLocalId(long id) {
        mLocalId = id;
    }

    @JsonIgnore
    public long getLocalId() {
        return mLocalId;
    }

    @JsonProperty
    public void setDeviceGroupId(String id) {
        mDeviceGroupId = id;
    }

    public String getDeviceGroupId() {
        return mDeviceGroupId;
    }

    @JsonProperty
    public void setLabel(String label) {
        mLabel = label;
    }

    public String getLabel() {
        return mLabel;
    }

    @JsonProperty
    public void setDeviceActions(Vector<DeviceRules.DeviceAction> actions) {
        mDeviceActions = actions;
    }

    public Vector<DeviceRules.DeviceAction> getDeviceActions() {
        return mDeviceActions;
    }

    public void addDeviceAction(DeviceRules.DeviceAction action) {
        if (mDeviceActions == null) {
            mDeviceActions = new Vector<DeviceRules.DeviceAction>();
        }

        mDeviceActions.add(action);
    }

    public DeviceRules.DeviceAction getActionForDevice(String deviceId) {
        if (mDeviceActions != null) {
            for (DeviceRules.DeviceAction action : mDeviceActions) {
                if (deviceId.equals(action.getDeviceId())) {
                    return action;
                }
            }
        }

        return null;
    }
}
