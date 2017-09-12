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
    },
    {
        "status": "FAILURE",
        "statusCode": 400
    },
    {
        "status": "NOT_ATTEMPTED"
    }
 */

@JsonIgnoreProperties(ignoreUnknown=true)
public class WriteResponse {

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILURE = "FAILURE";
    public static final String STATUS_NOT_ATTEMPTED = "NOT_ATTEMPTED";

    public String status;
    public int requestId;
    public int statusCode;
    public long timestampMs;

    @JsonIgnore
    public boolean isSuccess() {
        return STATUS_SUCCESS.equals(status);
    }
}
