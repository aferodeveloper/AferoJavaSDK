/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.conclave.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.afero.sdk.client.afero.models.DeviceStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceState {

    public DeviceState() {}

    public String id;
    public DeviceStatus status;

}
