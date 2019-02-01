/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*
    {
      "users": [
        {
          "userId": "",
          "firstName": "",
          "lastName": "",
          "userAccountAccess": {
            "accountId": "",
            "userId": "",
            "privileges": {
              "canWrite": false,
              "owner": false
            },
            "startAccessTimestamp": 0,
            "endAccessTimestamp": 0
          },
          "credentialId": ""
        }
      ],
      "invitations": [
        {
          "value": "",
          "credentialId": "",
          "type": "",
          "createdTimestamp": 0,
          "expiresTimestamp": 0,
          "tokenParams": "",
          "accountId": ""
        }
      ]
    }
*/


@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountUserSummary {
    public SimpleUser[] users;
    public Token[] invitations;
}
