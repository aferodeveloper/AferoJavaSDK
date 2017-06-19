/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*
    http://tools.corp.afero.io/swagger/#!/account-device/firmwareImageExists

    {
      "id": 0,
      "deviceId": "",
      "partnerId": "",
      "deviceTypeId": "",
      "bootloaderVersion": 0,
      "bootloaderVersionString": "",
      "applicationVersion": 0,
      "applicationVersionString": "",
      "deviceDescriptionVersion": 0,
      "deviceDescriptionVersionString": "",
      "softDeviceVersion": 0,
      "softDeviceVersionString": "",
      "hubVersion": 0,
      "hubVersionString": "",
      "wifiVersion": 0,
      "wifiVersionString": ""
    }
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceVersions {
    public int id;
    public String deviceId;
    public String partnerId;
    public String deviceTypeId;
    public int bootloaderVersion;
    public String bootloaderVersionString;
    public int applicationVersion;
    public String applicationVersionString;
    public int deviceDescriptionVersion;
    public String deviceDescriptionVersionString;
    public int softDeviceVersion;
    public String softDeviceVersionString;
    public int hubVersion;
    public String hubVersionString;
    public int wifiVersion;
    public String wifiVersionString;
}
