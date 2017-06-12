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

    public DeviceAssociateBody(String id, android.location.Location loc) {
        associationId = id;
        if (loc != null) {
            location = new Location();
            location.latitude = String.valueOf(loc.getLatitude());
            location.longitude = String.valueOf(loc.getLongitude());
            location.locationSourceType = "INITIAL_DEVICE_ASSOCIATE";
        }
    }
}
