/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.rules;

import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.DeviceRules;
import rx.Observable;

public class DeviceRuleCollection extends RuleCollection {

    private final String mDeviceId;

    public DeviceRuleCollection(String deviceId, AferoClient aferoClient) {
        super(aferoClient);
        mDeviceId = deviceId;
    }

    @Override
    protected Observable<DeviceRules.Rule[]> getRules() {
        return mAferoClient.getDeviceRules(mDeviceId);
    }

    @Override
    protected Observable<DeviceRules.Rule> postRule(DeviceRules.Rule rule) {
        return mAferoClient.postRule(rule);
    }

    @Override
    protected Observable<DeviceRules.Rule> putRule(String ruleId, DeviceRules.Rule rule) {
        return mAferoClient.putRule(ruleId, rule);
    }

    @Override
    protected Observable<Void> deleteRule(String ruleId) {
        return mAferoClient.deleteRule(ruleId);
    }

}
