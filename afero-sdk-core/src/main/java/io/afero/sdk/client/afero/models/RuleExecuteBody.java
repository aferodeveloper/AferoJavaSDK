/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

import io.afero.sdk.client.afero.models.BaseResponse;

public class RuleExecuteBody extends BaseResponse {
    public String type;

    public RuleExecuteBody() {
        type = "execute_actions";
    }
}
