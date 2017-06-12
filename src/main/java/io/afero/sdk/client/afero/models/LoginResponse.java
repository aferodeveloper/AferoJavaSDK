/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.afero.sdk.client.AccountServiceClient;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginResponse {
    public AccessToken accessToken;
    public UserDetails userDetails;
    public AccountServiceClient.Profile userProfile;

    public LoginResponse(AccessToken at, UserDetails ud, AccountServiceClient.Profile up) {
        accessToken = at;
        userDetails = ud;
        userProfile = up;
    }
}
