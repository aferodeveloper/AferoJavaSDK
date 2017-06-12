/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*
    {
        "status": "SUCCESS",
        "requestId": 3,
        "statusCode": 202,
        "timestampMs": 1486512901475
    }
 */

@JsonIgnoreProperties(ignoreUnknown=true)
public class RequestResponse {
    public String status;
    public int requestId;
    public int statusCode;
    public long timestampMs;

    @JsonIgnore
    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }
}
