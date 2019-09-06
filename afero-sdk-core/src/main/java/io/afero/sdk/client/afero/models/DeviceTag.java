/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceTag {
    public String deviceTagId;
    public String deviceTagType;
    public String localizationKey;
    public String value;

    @Override
    public String toString() {
        return "{" +
                "deviceTagId='" + deviceTagId + '\'' +
                ", deviceTagType='" + deviceTagType + '\'' +
                ", localizationKey='" + localizationKey + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
