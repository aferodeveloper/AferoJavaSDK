/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

/*
    {
      "type": "attribute_write",
      "attrId": 1024,
      "value": "true"
    }
 */

public class DeviceRequest {
    public String type = "attribute_write";
    public int attrId;
    public String value;

    public DeviceRequest(int id, String v) {
        attrId = id;
        value = v;
    }
}
