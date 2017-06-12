/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*
    {
        "attributeLabel": "Device Disassociated from Account",
        "attributeValue": "Disassociated",
        "controlIcon": "https://cdn.dev.afero.io/defaults/activity_feed_icon_disassociated.png",
        "createdTimestamp": 1447946616787,
        "deviceFriendlyName": "GPIO-Button-LED",
        "deviceIcon": "https://cdn.dev.afero.io/e3dd408d-b13e-47a1-abab-8e6a3a4575cb/14442506334111444250323579_xxhdpi_device_modulo_icon.png",
        "deviceId": "01235d26d62ca571",
        "historyId": "c93d9c5d-a603-4849-84f3-de0054b2e8d8",
        "historyType": "DISASSOCIATE",
        "icon": "https://cdn.dev.afero.io/defaults/activity_feed_icon_disassociated.png",
        "message": "The Device was disassociated from Account ID: ea92be3a-dfea-44f0-9eee-e41cdb5e95ba"
    },
    {
        "attributeLabel": "Version Change",
        "attributeValue": "Updated",
        "controlIcon": "https://cdn.dev.afero.io/defaults/question.png",
        "createdTimestamp": 1447946598570,
        "deviceFriendlyName": "GPIO-Button-LED",
        "deviceIcon": "https://cdn.dev.afero.io/e3dd408d-b13e-47a1-abab-8e6a3a4575cb/14442506334111444250323579_xxhdpi_device_modulo_icon.png",
        "deviceId": "01235d26d62ca571",
        "historyId": "28199764-c3da-4bc3-8cb5-f7e11fbe0b93",
        "historyType": "VERSION",
        "icon": "https://cdn.dev.afero.io/defaults/question.png",
        "message": "The Profile ID for this device was changed from 36a8fa4c-a614-498d-a44e-daf6473f20c4 to 90b13dae-823b-4dc1-ad5b-0ae8577735eb"
    },
    {
        "attributeLabel": "Device Availability Changed",
        "attributeValue": "Unavailable",
        "controlIcon": "https://cdn.dev.afero.io/defaults/question.png",
        "createdTimestamp": 1447283321179,
        "deviceFriendlyName": "Modder",
        "deviceIcon": "https://cdn.dev.afero.io/b55b0e6e-18b1-4040-99bf-e173fc712ec0/14412295316571441228108597_afero_logo_letter_mark.png",
        "deviceId": "01233368d72ca571",
        "historyId": "ede8df20-9532-45db-9220-b751042376a3",
        "historyType": "STATE",
        "icon": "https://cdn.dev.afero.io/defaults/question.png",
        "message": "Device Availability Changed"
    },
    {
        "attributeLabel": "Device Updated",
        "attributeValue": "Updated",
        "controlIcon": "https://cdn.dev.afero.io/defaults/activity_feed_icon_disconnected.png",
        "createdTimestamp": 1447282829493,
        "deviceFriendlyName": "Modder",
        "deviceIcon": "https://cdn.dev.afero.io/b55b0e6e-18b1-4040-99bf-e173fc712ec0/14412295316571441228108597_afero_logo_letter_mark.png",
        "deviceId": "01233368d72ca571",
        "historyId": "20e9a040-446c-4640-888d-448d484e43e1",
        "historyType": "ACTION_DEVICE_UPDATE",
        "icon": "https://cdn.dev.afero.io/defaults/activity_feed_icon_disconnected.png",
        "message": "Device Updated"
    },
*/

@JsonIgnoreProperties(ignoreUnknown=true)
public class ActivityResponse {
    public String historyId;
    public String historyType;
    public long createdTimestamp;
    public String message;
    public String attributeValue;
    public String attributeLabel;
    public String icon;
    public String deviceIcon;
    public String controlIcon;
    public String deviceId;
    public String deviceFriendlyName;
}
