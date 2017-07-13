/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

/*
 {
    "account": {
        "description": ""
    },
    "user": {
        "firstName": "",
        "lastName": ""
    },
    "credential": {
        "credentialId": "",
        "password": ""
    }
 }
 */
public class CreateAccountBody {

    public static class Account {
        public String description = "";
        public String type = "CUSTOMER";
    }

    public static class User {
        public String firstName;
        public String lastName;
    }

    public static class Credential {
        public String credentialId;
        public String password;
        public String type = "email";
    }

    public Account account = new Account();
    public User user = new User();
    public Credential credential = new Credential();

    public CreateAccountBody() {
    }
}
