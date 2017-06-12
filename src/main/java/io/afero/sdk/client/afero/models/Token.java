/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Token {
    public String value;
    public String credentialId;
    public String type;
    public long createdTimestamp;
    public long expiresTimestamp;
    public String tokenParams;
    public String accountId;
}
