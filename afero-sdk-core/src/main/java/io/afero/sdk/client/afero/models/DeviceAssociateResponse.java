/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.afero.sdk.client.afero.models.AttributeBody;
import io.afero.sdk.client.afero.models.DeviceStatus;
import io.afero.sdk.device.DeviceProfile;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceAssociateResponse {

    public DeviceAssociateResponse() {}

    public String deviceId;
    public String profileId;
    public boolean updating;
    public DeviceStatus deviceState;
    public DeviceProfile profile;
    public boolean virtual;
    public String disconnectNotificationLevel;
    public AttributeBody[] attributes;
    public boolean developerDevice;
}
