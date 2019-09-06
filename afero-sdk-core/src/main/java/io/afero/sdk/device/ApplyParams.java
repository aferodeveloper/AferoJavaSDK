/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;


import java.util.Map;
import java.util.TreeMap;

public class ApplyParams extends TreeMap<String,Object> {

    public ApplyParams() {
        super(String.CASE_INSENSITIVE_ORDER);
    }

    public static ApplyParams create(Map<String,Object> apply) {
        ApplyParams result = new ApplyParams();
        result.putAll(apply);
        return result;
    }
}
