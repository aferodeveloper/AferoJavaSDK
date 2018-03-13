/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Device tags are a simple list of values associated with a specific device. When a device is
 * associated, this list is initially empty. When a device is disassociated, the tags are purged
 * from the device record, such that no information applied to that device via tags from a previous
 * owner are not retained for a new device owner.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceTag {

    public static final String TAG_TYPE_ACCOUNT = "ACCOUNT";
    public static final String TAG_TYPE_SYSTEM = "SYSTEM";

    /**
     * A unique UUID value that identifies this tag, such that it can be deleted in the future.
     */
    public String deviceTagId;

    /**
     * A free form field that can be used by clients for organizational purposes. Optional.
     */
    public String key;

    /**
     * This is the value of the tag. This is just a simple character string, so that it can be used
     * alone, or with a delimiter of the developers' choice to create a key/value pair.
     */
    public String value;

    /**
     * In the future, Afero may deploy different categories of tags. This field is not currently in
     * use, will always be 'ACCOUNT', and can be safely ignored by the developer.
     */
    public String deviceTagType = TAG_TYPE_ACCOUNT;

    /**
     * In the future, Afero may create the ability to localize these tags for different locales in
     * different global markets. This field is not currently in use and can be safely ignored by
     * the developer.
     */
    public String localizationKey;


    public DeviceTag() {
    }

    public DeviceTag(String tagKey, String tagValue) {
        key = tagKey;
        value = tagValue;
    }

    public DeviceTag(DeviceTag tag) {
        set(tag);
    }

    @JsonIgnore
    public void set(DeviceTag tag) {
        deviceTagId = tag.deviceTagId;
        key = tag.key;
        value = tag.value;
        deviceTagType = tag.deviceTagType;
        localizationKey = tag.localizationKey;
    }

    @JsonIgnore
    public boolean isEditable() {
        return deviceTagType != null && !deviceTagType.equalsIgnoreCase(TAG_TYPE_SYSTEM);
    }

    @Override
    public String toString() {
        return "{ " +
                "deviceTagId='" + deviceTagId + '\'' +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", deviceTagType='" + deviceTagType + '\'' +
                ", localizationKey='" + localizationKey + '\'' +
                " }";
    }
}
