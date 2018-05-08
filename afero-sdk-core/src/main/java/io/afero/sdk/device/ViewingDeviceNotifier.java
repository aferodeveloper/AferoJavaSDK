/*
 * Copyright (c) 2014-2018 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;


import java.util.concurrent.TimeUnit;

import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.ViewRequest;
import io.afero.sdk.client.afero.models.ViewResponse;
import io.afero.sdk.log.AfLog;
import io.afero.sdk.utils.RxUtils;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;


/**
 * This class manages the process of notifying a device that it is being viewed by a client.
 * The device will be notified of this viewing state and may choose to alter its operation
 * in some meaningful way such as sending attributes more frequently, or in the case of a
 * hub/gateway, it may preemptively connect to a peripheral in anticipation of incoming attribute
 * changes.
 */
public final class ViewingDeviceNotifier {

    private static final long DEFAULT_DURATION_SECONDS = 5 * 60;
    private static final long REFRESH_DELTA_SECONDS = 15;

    private final DeviceModel mDeviceModel;
    private final AferoClient mAferoClient;
    private Subscription mViewNotifierSubscription;

    private final Object mLock = new Object();
    private final long mDurationSeconds;
    private final long mRefreshDeltaSeconds;


    ViewingDeviceNotifier(DeviceModel deviceModel, AferoClient aferoClient, long durationSeconds, long refreshDeltaSeconds) {
        mDeviceModel = deviceModel;
        mAferoClient = aferoClient;
        mDurationSeconds = durationSeconds;
        mRefreshDeltaSeconds = refreshDeltaSeconds;
    }

    ViewingDeviceNotifier(DeviceModel deviceModel, AferoClient aferoClient) {
        mDeviceModel = deviceModel;
        mAferoClient = aferoClient;
        mDurationSeconds = DEFAULT_DURATION_SECONDS;
        mRefreshDeltaSeconds = REFRESH_DELTA_SECONDS;
    }

    /**
     * Notifies the Afero Cloud Service that the device is being viewed. Once {@code start()} is
     * called once, any subsequent {@code start()} calls are ignored until {@link #stop()} is called.
     *
     * @return this instance
     */
    public ViewingDeviceNotifier start() {
        synchronized (mLock) {
            if (isRunning()) {
                return this;
            }

            long intervalSeconds = mDurationSeconds - mRefreshDeltaSeconds;
            mViewNotifierSubscription = Observable.interval(0, intervalSeconds, TimeUnit.SECONDS)
                .flatMap(new Func1<Long, Observable<ViewResponse[]>>() {
                    @Override
                    public Observable<ViewResponse[]> call(Long x) {
                        AfLog.d("Observable.interval: x=" + x);
                        synchronized (mLock) {
                            if (isRunning()) {
                                return mAferoClient.postDeviceViewRequest(mDeviceModel, ViewRequest.start(mDurationSeconds));
                            }
                        }

                        return Observable.empty();
                    }
                })
                .subscribe(new RxUtils.IgnoreResponseObserver<ViewResponse[]>());
        }

        return this;
    }

    /**
     * Notifies the Afero Cloud Service that the device is no longer being viewed.
     * If {@link #start()} hasn't yet been called, {@code stop()} does nothing.
     */
    public void stop() {
        synchronized (mLock) {
            if (isRunning()) {
                mViewNotifierSubscription = RxUtils.safeUnSubscribe(mViewNotifierSubscription);
                mAferoClient.postDeviceViewRequest(mDeviceModel, ViewRequest.stop())
                    .subscribe(new RxUtils.IgnoreResponseObserver<ViewResponse[]>());
            }
        }
    }

    private boolean isRunning() {
        return mViewNotifierSubscription != null;
    }
}
