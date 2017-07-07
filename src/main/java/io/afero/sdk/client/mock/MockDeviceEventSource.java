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
import rx.functions.Action0;
import rx.subjects.PublishSubject;

public class MockDeviceEventSource implements DeviceEventSource {

    public final PublishSubject<DeviceSync[]> mSnapshotSubject = PublishSubject.create();
    public final PublishSubject<DeviceSync> mAttributeChangeSubject = PublishSubject.create();
    public final PublishSubject<DeviceError> mDeviceErrorSubject = PublishSubject.create();
    public final PublishSubject<DeviceState> mDeviceStateSubject = PublishSubject.create();
    public final PublishSubject<DeviceMute> mDeviceMuteSubject = PublishSubject.create();
    public final PublishSubject<OTAInfo> mOTAInfoSubject = PublishSubject.create();
    public final PublishSubject<InvalidateMessage> mInvalidateMessageSubject = PublishSubject.create();

    public int mSnapshotSubscriptionCount;
    public int mInvalidateMessageSubscriptionCount;
    public int mAttributeChangeSubscriptionCount;
    public int mDeviceErrorSubscriptionCount;
    public int mDeviceStateSubscriptionCount;
    public int mDeviceMuteSubscriptionCount;
    public int mOTAInfoSubscriptionCount;

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
        return mSnapshotSubject
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        mSnapshotSubscriptionCount++;
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        mSnapshotSubscriptionCount--;
                    }
                })
                ;
    }

    @Override
    public Observable<DeviceSync> observeAttributeChange() {
        return mAttributeChangeSubject
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        mAttributeChangeSubscriptionCount++;
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        mAttributeChangeSubscriptionCount--;
                    }
                });
    }

    @Override
    public Observable<DeviceError> observeError() {
        return mDeviceErrorSubject
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        mDeviceErrorSubscriptionCount++;
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        mDeviceErrorSubscriptionCount--;
                    }
                });
    }

    @Override
    public Observable<DeviceState> observeStatusChange() {
        return mDeviceStateSubject
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        mDeviceStateSubscriptionCount++;
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        mDeviceStateSubscriptionCount--;
                    }
                });
    }

    @Override
    public Observable<DeviceMute> observeMute() {
        return mDeviceMuteSubject
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        mDeviceMuteSubscriptionCount++;
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        mDeviceMuteSubscriptionCount--;
                    }
                });
    }

    @Override
    public Observable<OTAInfo> observeOTA() {
        return mOTAInfoSubject
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        mOTAInfoSubscriptionCount++;
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        mOTAInfoSubscriptionCount--;
                    }
                });
    }

    @Override
    public Observable<InvalidateMessage> observeInvalidate() {
        return mInvalidateMessageSubject
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        mInvalidateMessageSubscriptionCount++;
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        mInvalidateMessageSubscriptionCount--;
                    }
                });
    }

    @Override
    public void sendMetrics(ConclaveMessage.Metric metric) {

    }
}
