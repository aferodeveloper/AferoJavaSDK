/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.mock;

import io.afero.sdk.conclave.ConclaveMessage;
import io.afero.sdk.conclave.DeviceEventSource;
import io.afero.sdk.conclave.models.DeviceError;
import io.afero.sdk.conclave.models.DeviceMute;
import io.afero.sdk.conclave.models.DeviceState;
import io.afero.sdk.conclave.models.DeviceSync;
import io.afero.sdk.conclave.models.InvalidateMessage;
import io.afero.sdk.conclave.models.OTAInfo;
import rx.Observable;
import rx.subjects.PublishSubject;

public class MockDeviceEventSource implements DeviceEventSource {

    public final PublishSubject<DeviceSync[]> mSnapshotSubject = PublishSubject.create();
    public final PublishSubject<DeviceSync> mAttributeChangeSubject = PublishSubject.create();
    public final PublishSubject<DeviceError> mDeviceErrorSubject = PublishSubject.create();
    public final PublishSubject<DeviceState> mDeviceStateSubject = PublishSubject.create();
    public final PublishSubject<DeviceMute> mDeviceMuteSubject = PublishSubject.create();
    public final PublishSubject<OTAInfo> mOTAInfoSubject = PublishSubject.create();
    public final PublishSubject<InvalidateMessage> mInvalidateMessageSubject = PublishSubject.create();

    public void putSnapshot(DeviceSync[] deviceSyncs) {
        mSnapshotSubject.onNext(deviceSyncs);
    }

    public void putAttributeChanges(DeviceSync ds) {
        mAttributeChangeSubject.onNext(ds);
    }

    public void putDeviceError(DeviceError err) {
        mDeviceErrorSubject.onNext(err);
    }

    public void putDeviceState(DeviceState state) {
        mDeviceStateSubject.onNext(state);
    }

    public void putDeviceState(DeviceMute dm) {
        mDeviceMuteSubject.onNext(dm);
    }

    public void putOTAInfo(OTAInfo oi) {
        mOTAInfoSubject.onNext(oi);
    }

    public void putInvalidateMessage(InvalidateMessage im) {
        mInvalidateMessageSubject.onNext(im);
    }

    @Override
    public Observable<DeviceSync[]> observeSnapshot() {
        return mSnapshotSubject;
    }

    @Override
    public Observable<DeviceSync> observeAttributeChange() {
        return mAttributeChangeSubject;
    }

    @Override
    public Observable<DeviceError> observeError() {
        return mDeviceErrorSubject;
    }

    @Override
    public Observable<DeviceState> observeStatusChange() {
        return mDeviceStateSubject;
    }

    @Override
    public Observable<DeviceMute> observeMute() {
        return mDeviceMuteSubject;
    }

    @Override
    public Observable<OTAInfo> observeOTA() {
        return mOTAInfoSubject;
    }

    @Override
    public Observable<InvalidateMessage> observeInvalidate() {
        return mInvalidateMessageSubject;
    }

    @Override
    public void sendMetrics(ConclaveMessage.Metric metric) {

    }
}
