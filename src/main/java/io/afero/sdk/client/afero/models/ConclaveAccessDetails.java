/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;

/*
    {
      "conclave": {
        "host": "",
        "compression": false,
        "port": 0
      },
      "tokens": [
        {
          "token": "",
          "channelId": "",
          "expiresTimestamp": 0,
          "client": {}
        }
      ]
    }
*/

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConclaveAccessDetails {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConclaveHost {
        public String type;
        public String host;
        public boolean compression;
        public boolean ssl;
        public int port;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConclaveAccess {
        public String token;
        public String channelId;
        public long expiresTimestamp;
        public HashMap<String,String> client;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConclaveClient {
        // this space intentionally left blank
    }

    public ConclaveHost conclave;
    public ConclaveAccess[] tokens;
    public ConclaveHost[] conclaveHosts;

}
