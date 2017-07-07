/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.retrofit2.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceTimezone {

    public DeviceTimezone() {}
    public DeviceTimezone(String tz) {
        timezone = tz;
    }

    public boolean userOverride = false;
    public String timezone;
}
