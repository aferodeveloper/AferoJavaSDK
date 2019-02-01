/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

/*
    {
      "targetEmail": "",
      "customMessage": "",
      "targetLocale": "",
      "sourceAccountId": "",
      "sourceUserId": "",
      "startAccessTimestamp": 0,
      "endAccessTimestamp": 0,
      "accountPrivilegesDto": {
        "canWrite": false,
        "owner": false
      },
      "params": {}
    }
*/

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InvitationDetails {
    public String targetEmail;
    public String customMessage;
    public String targetLocale;
    public String sourceAccountId;
    public String sourceUserId;
    public long startAccessTimestamp;
    public long endAccessTimestamp;
    public AccountPrivileges accountPrivilegesDto;
    public HashMap<String,String> params;
}
