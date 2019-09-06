/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.conclave.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.afero.sdk.client.afero.models.AferoError;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceError implements AferoError {

    public DeviceError() {}

    public String event;
    public String status;
    public String id;
    public Long channelId;
    public int requestId;
}
