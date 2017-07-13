/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

/*
{
  "deviceId": "",
  "location": {
    "latitude": "",
    "longitude": ""
  }
}
*/

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceAssociateBody {

    public String associationId;
    public Location location;

    public DeviceAssociateBody() {
    }

    public DeviceAssociateBody(String id) {
        associationId = id;
    }
}
