/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserAccountAccess {
    public String accountId;
    public String userId;
    public AccountPrivileges privileges;
    public long startAccessTimeStamp;
    public long endAccessTimestamp;
}
