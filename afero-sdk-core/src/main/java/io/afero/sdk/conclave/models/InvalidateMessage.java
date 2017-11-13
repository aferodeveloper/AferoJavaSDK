/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.conclave.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import io.afero.sdk.client.afero.models.DeviceTag;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InvalidateMessage {

    public InvalidateMessage() {}

    public InvalidateMessage(String t, JsonNode j) {
        kind = t;
    }

    public String kind;

    public String deviceId;

    // kind: profiles
    public String profileId;

    // kind: tags
    public String deviceTagAction;
    public DeviceTag deviceTag;
}
