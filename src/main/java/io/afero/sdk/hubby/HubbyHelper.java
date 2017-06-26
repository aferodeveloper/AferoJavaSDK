/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.hubby;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.log.AfLog;
import io.afero.sdk.utils.RxUtils;
import io.kiban.hubby.Hubby;
import io.kiban.hubby.NotificationCallback;
import io.kiban.hubby.OtaCallback;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class HubbyHelper {

    private static HubbyHelper sInstance;
    private static boolean sIsHubbyInitialized;

    private static final long OTA_END_DELAY = 10000;

    private HubbyImpl mHubbyImpl = new NativeHubbyImpl();

    private final WeakReference<Context> mContextRef;
    private final AferoClient mAferoClient;
    private final String mSetupPath;
    private final String mOTAPath;
    private final NotificationCallback mNotificationCallback;

    private boolean mIsHubbyRunning;

    private final HashMap<String, OTAEntry> mActiveOTAs = new HashMap<>(2);
    private PowerManager.WakeLock mOTAWakeLock;
    private long mOTAStopTime;
    private Subscription mOTAEndSubscription;

    private boolean mIsActive = true;
    private String mService;

    private PublishSubject<HubbyHelper> mStartSubject;
    private final PublishSubject<NotificationCallback.CompleteReason> mCompleteSubject = PublishSubject.create();

    // for associate call
    private final AferoClient.ImageSize mImageSize;

    private final Action0 mStartOnSubscribe = new Action0() {
        @Override
        public void call() {
            mStartSubject = PublishSubject.create();
        }
    };

    private final Func1<HubbyHelper, Observable<HubbyHelper>> mMapToStartSubject = new Func1<HubbyHelper, Observable<HubbyHelper>>() {
        @Override
        public Observable<HubbyHelper> call(HubbyHelper hubbyHelper) {
            return mStartSubject;
        }
    };

    private HubbyHelper(@NonNull Context context, @NonNull AferoClient aferoClient) {
        mContextRef = new WeakReference<>(context);
        mAferoClient = aferoClient;

        mNotificationCallback = new HubbyNotificationCallback(this);

        mOTAPath = context.getCacheDir().getAbsolutePath();
        mSetupPath = context.getFilesDir().getAbsolutePath();

        mImageSize = AferoClient.ImageSize
            .fromDisplayDensity(context.getResources().getDisplayMetrics().density);
    }

    public static HubbyHelper acquireInstance(@NonNull Context context, @NonNull AferoClient aferoClient) {
        if (sInstance == null) {
            sInstance = new HubbyHelper(context, aferoClient);
        }

        return sInstance;
    }

    public static void releaseInstance() {
        sInstance = null;
    }

    public synchronized @NonNull Observable<HubbyHelper> start() {
        AfLog.i("HubbyHelper.start");

        if (isRunning()) {
            return Observable.error(new IllegalStateException("already running"));
        }

        if (isStarting()) {
            return Observable.error(new IllegalStateException("already starting"));
        }

        return Observable.fromCallable(new HubbyStarter(this))
            .subscribeOn(Schedulers.computation())
            .flatMap(mMapToStartSubject)
            .doOnSubscribe(mStartOnSubscribe);
    }

    public synchronized void stop() {
        AfLog.i("HubbyHelper.stop");

        if (isRunning()) {
            mHubbyImpl.stop();
            mIsHubbyRunning = false;
        }
    }

    public Observable<NotificationCallback.CompleteReason> observeCompletion() {
        return mCompleteSubject;
    }

    public void onPause() {
        mIsActive = false;

        // The app is going into the background.
        // If the context (Activity) has been destroyed or
        // there's no OTA in progress, then shut down Hubby
        if (getContext() == null || !isOTAInProgress()) {
            stop();
        }
    }

    public void onResume() {
        mIsActive = true;

        if (!(isStarting() || isRunning())) {
            start().subscribe(new RxUtils.IgnoreResponseObserver<HubbyHelper>());
        }
    }

    public boolean isActive() {
        return mIsActive;
    }

    public boolean isRunning() {
        return mIsHubbyRunning;
    }

    public boolean isStarting() {
        return mStartSubject != null;
    }

    public void setService(@NonNull String service) {
        mService = service;
    }

    // For unit testing
    void setHubbyImpl(HubbyImpl hi) {
        mHubbyImpl = hi;
    }

    private @Nullable Context getContext() {
        return mContextRef.get();
    }

    private synchronized void onCallStart() {
        AfLog.i("HubbyHelper.onCallStart");

        if (!sIsHubbyInitialized) {
            AfLog.i("HubbyHelper: initializing hubby");

            Context context = getContext();
            if (context != null) {
                mHubbyImpl.initialize(context);
                sIsHubbyInitialized = true;
            } else {
                mStartSubject = null;
                throw new IllegalStateException("Context is null, unable to call Hubby.initialize");
            }
        }

        if (!isRunning()) {
            startHubby();
        } else {
            mStartSubject = null;
            throw new IllegalStateException("Already running or starting");
        }
    }

    private void startHubby() {
        AfLog.i("HubbyHelper: starting hubby");

        HashMap<Hubby.Config,String> config = new HashMap<>(1);
        config.put(Hubby.Config.SOFT_HUB_SETUP_PATH, mSetupPath + "/" + mAferoClient.getActiveAccountId());
        config.put(Hubby.Config.OTA_WORKING_PATH, mOTAPath);

        String hwInfo = "os:android,manufacturer:" + Build.MANUFACTURER + ",model:" + Build.MODEL + ",version:" + Build.VERSION.RELEASE;
        config.put(Hubby.Config.HARDWARE_INFO, hwInfo);

        if (mService != null) {
            config.put(Hubby.Config.SERVICE, mService);
        }

        mHubbyImpl.start(config, mNotificationCallback);
    }

    private void onInitializationComplete() {
        AfLog.i("HubbyHelper.onInitializationComplete");
        mIsHubbyRunning = true;

        mStartSubject.onCompleted();
        mStartSubject = null;
    }

    private void onRunComplete(NotificationCallback.CompleteReason completeReason) {
        AfLog.i("HubbyHelper.onRunComplete");
        mCompleteSubject.onNext(completeReason);
    }

    private void onSecureHubAssociationNeeded(String assId) {
        AfLog.i("HubbyHelper.onSecureHubAssociationNeeded");

        mAferoClient.deviceAssociate(assId, false, Locale.getDefault().toString(), mImageSize)
            .subscribe();
    }

    private void onOtaStateChange(String deviceId, OtaCallback.OtaState otaState, int offset, int total) {
        AfLog.i("HubbyHelper.onOtaStateChange");

        switch (otaState) {
            case START:
                onOTAStart(deviceId);
                break;

            case ONGOING:
                onOTAOngoing(deviceId);
                break;

            case STOP:
                onOTAStop(deviceId);
                break;
        }
    }

    private void onOTAStart(String deviceId) {
        AfLog.i("HubbyHelper.onOTAStart");

        synchronized (mActiveOTAs) {
            OTAEntry ota = new OTAEntry();
            mActiveOTAs.put(deviceId, ota);

            if (mOTAWakeLock == null) {
                AfLog.i("HubbyHelper.onOTAStart: Grabbing wakelock");

                Context context = getContext();
                if (context != null) {
                    PowerManager powerManager = (PowerManager)context.getSystemService(Activity.POWER_SERVICE);
                    mOTAWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, BuildConfig.APPLICATION_ID + ".OTAWakeLockTag");
                    mOTAWakeLock.acquire();
                }
            }
        }
    }

    private void onOTAStop(String deviceId) {
        AfLog.i("HubbyHelper: OTA stopping - waiting to release wakelock");

        synchronized (mActiveOTAs) {
            mActiveOTAs.remove(deviceId);

            if (mActiveOTAs.isEmpty()) {

            }
        }
        mOTAEndSubscription = Observable.interval(OTA_END_DELAY, TimeUnit.MILLISECONDS, Schedulers.computation())
            .subscribe(new Action1<Long>() {
                @Override
                public void call(Long x) {
                    onOTAEnd();
                }
            });
    }

    private void onOTAOngoing(String deviceId) {
        mOTAStopTime = System.currentTimeMillis() + OTA_END_DELAY;
    }

    private void onOTAEnd() {
        long now = System.currentTimeMillis();
        if (mOTAStopTime != 0 && now >= mOTAStopTime) {
            AfLog.i("HubbyHelper: releasing wakelock");

            mOTAEndSubscription = RxUtils.safeUnSubscribe(mOTAEndSubscription);

            if (mOTAWakeLock != null) {
                mOTAWakeLock.release();
                mOTAWakeLock = null;
                mOTAStopTime = 0;
            }

            if (!isActive()) {
                stop();
            }
        }
    }

    private boolean isOTAInProgress() {
        return !mActiveOTAs.isEmpty();
    }

    private boolean isWaitingToReleaseWakelock() {
        return mOTAWakeLock != null && mOTAWakeLock.isHeld();
    }

    private static class HubbyNotificationCallback implements NotificationCallback {

        private WeakReference<HubbyHelper> mRef;

        private final OtaCallback.OtaState[] mOtaStateValues = OtaCallback.OtaState.values();
        private final CompleteReason[] mCompleteReasonValues = CompleteReason.values();

        HubbyNotificationCallback(HubbyHelper hh) {
            mRef = new WeakReference<>(hh);
        }

        @Override
        public void initializationComplete() {
            AfLog.d("HubbyNotificationCallback.initializationComplete");
            HubbyHelper hh = mRef.get();
            if (hh != null) {
                hh.onInitializationComplete();
            }
        }

        @Override
        public void runComplete(int completeReasonRaw) {
            AfLog.d("HubbyNotificationCallback.runComplete: completeReason=" + completeReasonRaw);

            HubbyHelper hh = mRef.get();
            if (hh != null) {
                CompleteReason completeReason = completeReasonfromInt(completeReasonRaw);
                if (completeReason != null) {
                    hh.onRunComplete(completeReason);
                }
            }
        }

        @Override
        public void otaStatus(int stateRaw, String deviceId, int offset, int total) {
            AfLog.d("HubbyNotificationCallback.otaStatus: state=" + stateRaw + " deviceId=" + deviceId + " offset=" + offset + " total=" + total);

            HubbyHelper hh = mRef.get();
            if (hh != null) {
                OtaCallback.OtaState otaState = otaStatefromInt(stateRaw);
                if (otaState != null) {
                    hh.onOtaStateChange(deviceId, otaState, offset, total);
                }
            }
        }

        @Override
        public void secureHubAssociationNeeded(String assId) {
            AfLog.d("HubbyNotificationCallback.secureHubAssociationNeeded: assId=" + assId);
            HubbyHelper hh = mRef.get();
            if (hh != null) {
                hh.onSecureHubAssociationNeeded(assId);
            }
        }

        private CompleteReason completeReasonfromInt(int i) {
            for (CompleteReason cr : mCompleteReasonValues) {
                if (cr.ordinal() == i) {
                    return cr;
                }
            }

            return null;
        }

        private OtaCallback.OtaState otaStatefromInt(int i) {
            for (OtaCallback.OtaState os : mOtaStateValues) {
                if (os.ordinal() == i) {
                    return os;
                }
            }

            return null;
        }

    }

    private static class HubbyStarter implements Callable<HubbyHelper> {

        private WeakReference<HubbyHelper> mRef;

        HubbyStarter(HubbyHelper hh) {
            mRef = new WeakReference<>(hh);
        }

        @Override
        public HubbyHelper call() throws Exception {
            HubbyHelper hh = mRef.get();
            hh.onCallStart();
            return hh;
        }
    }

    private class OTAEntry {
    }

    interface HubbyImpl {
        void initialize(Context context);

        void start(final HashMap<Hubby.Config, String> configs, final NotificationCallback callback);

        void stop();
    }

    private class NativeHubbyImpl implements HubbyImpl {

        @Override
        public void initialize(Context context) {
            Hubby.initialize(context);
        }

        @Override
        public void start(HashMap<Hubby.Config, String> configs, NotificationCallback callback) {
            Hubby.start(configs, callback);
        }

        @Override
        public void stop() {
            Hubby.stop();
        }
    }
}
