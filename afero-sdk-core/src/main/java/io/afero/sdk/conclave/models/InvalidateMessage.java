/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.conclave.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InvalidateMessage {

    public InvalidateMessage() {}

    public InvalidateMessage(String t, JsonNode j) {
        kind = t;
        json = j;
    }

    public String kind;
    public JsonNode json;
}
