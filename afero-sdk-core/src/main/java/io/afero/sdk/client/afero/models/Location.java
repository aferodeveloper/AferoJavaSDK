/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Location {

    public Location() {}

    public String latitude;
    public String longitude;
    public long lastUpdatedTimestamp;
    public String locationSourceType;
    public String[] formattedAddressLines;
}
