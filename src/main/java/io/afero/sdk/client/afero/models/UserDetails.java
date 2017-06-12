/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*
    Response Schema:
    {
      "userId": "",
      "firstName": "",
      "lastName": "",
      "lastUsedTimestamp": 0,
      "accountAccess": [
        {
          "account": {
            "accountId": "",
            "type": "",
            "description": "",
            "createdTimestamp": 0
          },
          "privileges": {
            "owner": false
          }
        }
      ],
      "partnerAccess": [
        {
          "partner": {
            "name": "",
            "partnerId": "",
            "createdTimestamp": 0
          },
          "privileges": {
            "owner": false,
            "inviteUsers": false,
            "manageDeviceProfiles": false,
            "viewDeviceInfo": false
          }
        }
      ],
        "credential": {
            "credentialId": "",
            "userId": "",
            "verified": false,
            "password": "",
            "lastUsedTimestamp": 0,
            "type": "",
            "failedAttempts": 0
        },
    	"tos": [{
            "currentVersion": 1,
            "userVersion": 1,
            "tosType": "user",
            "url": "https://cdn.dev.afero.io/tos/user/v1/user.html",
            "needsAcceptance": false
        }, {
            "currentVersion": 1,
            "userVersion": 0,
            "tosType": "developer",
            "url": "https://cdn.dev.afero.io/tos/user/v1/developer.html",
            "needsAcceptance": true
        }, {
            "currentVersion": 1,
            "userVersion": 0,
            "tosType": "general",
            "url": "https://cdn.dev.afero.io/tos/user/v1/general.html",
            "needsAcceptance": true
        }]
    }
*/
@JsonIgnoreProperties(ignoreUnknown=true)
public class UserDetails {

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class AuthUserAccountAccess {
        public AuthAccount account;
        public AccountPrivileges privileges;
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class AuthAccount {
        public String accountId;
        public String type;
        public String description;
        public int createdTimeStamp;
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class AccountPrivileges {
        public boolean owner;
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class TermsOfService {
        public int currentVersion;
        public int userVersion;
        public String tosType;
        public String url;
        public boolean needsAcceptance;
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class Credential {
        public String credentialId;
        public String userId;
        public String password;
        public boolean verified;
        public long createdTimeStamp;
        public String type;
        public int failedAttempts;

        public boolean isTypeEmail() {
            return "EMAIL".equalsIgnoreCase(type);
        }
    }

    public String userId;
    public String firstName;
    public String lastName;
    public long lastUsedTimeStamp;
    public AuthUserAccountAccess[] accountAccess;
    public TermsOfService[] tos;
    public Credential credential;

}

