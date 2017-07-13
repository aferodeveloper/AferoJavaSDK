/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.conclave;

import io.afero.sdk.conclave.models.DeviceError;
import io.afero.sdk.conclave.models.DeviceMute;
import io.afero.sdk.conclave.models.DeviceState;
import io.afero.sdk.conclave.models.DeviceSync;
import io.afero.sdk.conclave.models.InvalidateMessage;
import io.afero.sdk.conclave.models.OTAInfo;
import rx.Observable;

public interface DeviceEventSource {

    Observable<DeviceSync[]> observeSnapshot();

    Observable<DeviceSync> observeAttributeChange();

    Observable<DeviceError> observeError();

    Observable<DeviceState> observeStatusChange();

    Observable<DeviceMute> observeMute();

    Observable<OTAInfo> observeOTA();

    Observable<InvalidateMessage> observeInvalidate();

    void sendMetrics(ConclaveMessage.Metric metric);
}
