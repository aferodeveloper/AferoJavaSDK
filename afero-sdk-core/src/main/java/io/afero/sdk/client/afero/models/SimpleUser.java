/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleUser {
    public String userId;
    public String firstName;
    public String lastName;
    public String credentialId;
    public UserAccountAccess userAccountAccess;
}
