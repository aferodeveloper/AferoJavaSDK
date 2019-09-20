/*
 * Copyright (c) 2014-2019 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;


public class ResetPasswordBody {
    public String password;

    public ResetPasswordBody(String pw) {
        password = pw;
    }
}
