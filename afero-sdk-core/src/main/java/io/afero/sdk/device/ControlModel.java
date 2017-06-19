/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;

import java.math.BigDecimal;
import java.util.ArrayList;

import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.client.afero.models.DeviceRequest;
import io.afero.sdk.client.afero.models.RequestResponse;
import rx.Observable;

public interface ControlModel {

    void setAvailable(boolean available);

    boolean isAvailable();

    void writeModelValue(DeviceProfile.Attribute attribute, BigDecimal value);

    void writeModelValue(DeviceProfile.Attribute attribute, AttributeValue value);

    AttributeValue readPendingValue(DeviceProfile.Attribute attribute);

    AttributeValue readCurrentValue(DeviceProfile.Attribute attribute);

    rx.Observable<ControlModel> getUpdateObservable();

    boolean enableDisplayRules();

    boolean enableReadOnlyControls();

    Observable<RequestResponse> writeModelValues(ArrayList<DeviceRequest> req);
}
