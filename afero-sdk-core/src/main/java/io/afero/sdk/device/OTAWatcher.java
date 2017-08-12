/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;

import java.util.concurrent.TimeUnit;

import io.afero.sdk.conclave.models.OTAInfo;
import io.afero.sdk.log.AfLog;
import io.afero.sdk.utils.RxUtils;
import rx.Observable;
import rx.Subscription;
import rx.subjects.BehaviorSubject;

class OTAWatcher {

    private final DeviceModel mDeviceModel;
    private final long mOTATimeoutSeconds;
    private OTAInfo.OtaState mOTAState;
    private int mOTAProgress;
    private Subscription mOTAWatchdogSubscription;
    private BehaviorSubject<Integer> mProgressSubject = BehaviorSubject.create();

    OTAWatcher(DeviceModel deviceModel, long otaTimeoutSeconds) {
        mDeviceModel = deviceModel;
        mOTATimeoutSeconds = otaTimeoutSeconds;
    }

    void onOTA(OTAInfo ota) {
        mOTAState = ota.getState();
        mOTAProgress = ota.getProgress();

        if (mOTAState == OTAInfo.OtaState.STOP) {
            onOTAStop();
        } else {
            resetOTAWatchdog();
        }
    }

    Observable<Integer> getProgressObservable() {
        return mProgressSubject;
    }

    private void onOTAStop() {
        cancelOTAWatchdog();

        mOTAState = OTAInfo.OtaState.STOP;
        mOTAProgress = 0;

        mProgressSubject.onCompleted();

        mDeviceModel.onOTAStop();
    }

    private void resetOTAWatchdog() {
        cancelOTAWatchdog();

        if (mOTATimeoutSeconds > 0) {
            mOTAWatchdogSubscription = Observable.just(mOTAProgress)
                    .delay(mOTATimeoutSeconds, TimeUnit.SECONDS)
                    .subscribe(new OTAWatchdogAction(this));
        }
    }

    private void cancelOTAWatchdog() {
        mOTAWatchdogSubscription = RxUtils.safeUnSubscribe(mOTAWatchdogSubscription);
    }

    private void onOTAWatchdogFired(int oldProgress) {
        AfLog.d("OTAWatcher.onOTAWatchdogFired");

        // if progress hasn't advanced then stop the OTA, it appears to be stuck
        if (oldProgress == mOTAProgress) {
            onOTAStop();
        }
    }

    private static class OTAWatchdogAction extends RxUtils.WeakAction1<Integer, OTAWatcher> {

        OTAWatchdogAction(OTAWatcher strongRef) {
            super(strongRef);
        }

        @Override
        public void call(OTAWatcher watcher, Integer progress) {
            watcher.onOTAWatchdogFired(progress);
        }
    }
}
