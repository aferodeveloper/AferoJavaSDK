/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

public class SceneExecuteBody extends BaseResponse {
    public String type;

    public SceneExecuteBody() {
        type = "scene_execute";
    }
}
