/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PostActionBody extends BaseResponse {
    public String type;
    public int attrId;
    public String value;
    public String data;

    public PostActionBody() {
    }

    public PostActionBody(int id, String v) {
        type = "attribute_write";
        attrId = id;
        value = v;
    }

    public PostActionBody(String t, int id, String v) {
        type = t;
        attrId = id;
        value = v;
    }
}
