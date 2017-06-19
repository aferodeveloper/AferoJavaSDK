/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*
{
    "accountId": "",
    "type": "",
    "description": "",
    "createdTimestamp": 0
}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateAccountResponse extends BaseResponse {

    public String accountId;
    public String type;
    public String description;
    public long createdTimestamp;

    public CreateAccountResponse() {}
}
