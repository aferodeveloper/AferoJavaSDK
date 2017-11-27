/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.softhub;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.afero.hubby.Hubby;
import io.afero.hubby.NotificationCallback;
import io.afero.hubby.OtaCallback;
import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.DeviceAssociateResponse;
import io.afero.sdk.log.AfLog;
import io.afero.sdk.utils.RxUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class AferoSofthub {

    private static AferoSofthub sInstance;
    private static boolean sIsHubbyInitialized;

    private static final String WAKE_LOCK_TAG = "io.afero.sdk.hubby.AferoSofthub.OTAWakeLockTag";
    private static final long OTA_END_DELAY = 30000;
    private static final long WAKE_LOCK_WATCHDOG_INTERVAL = 5000;
    private static final int HARDWARE_INFO_MAX_LENGTH = 500;

    private HubbyImpl mHubbyImpl = new NativeHubbyImpl();

    private final WeakReference<Context> mContextRef;
    private final AferoClient mAferoClient;
    private final String mSetupPath;
    private final String mOTAPath;
    private final String mAppInfo;
    private final NotificationCallback mNotificationCallback;

    private boolean mIsHubbyRunning;

    private final HashMap<String, OTAEntry> mActiveOTAs = new HashMap<>(2);
    private PowerManager.WakeLock mOTAWakeLock;
    private long mOTAStopTime;
    private Subscription mOTAEndSubscription;

    private boolean mIsActive = true;
    private String mService;

    private PublishSubject<AferoSofthub> mStartSubject;
    private final PublishSubject<NotificationCallback.CompleteReason> mCompleteSubject = PublishSubject.create();
    private final PublishSubject<String> mAssociateSubject = PublishSubject.create();

    private final Action0 mStartOnSubscribe = new Action0() {
        @Override
        public void call() {
            mStartSubject = PublishSubject.create();
        }
    };

    private final Func1<AferoSofthub, Observable<AferoSofthub>> mMapToStartSubject = new Func1<AferoSofthub, Observable<AferoSofthub>>() {
        @Override
        public Observable<AferoSofthub> call(AferoSofthub aferoSofthub) {
            return mStartSubject;
        }
    };

    private AferoSofthub(@NonNull Context context, @NonNull AferoClient aferoClient, @Nullable String appInfo) {
        mContextRef = new WeakReference<>(context);
        mAferoClient = aferoClient;
        mAppInfo = appInfo;

        mNotificationCallback = new HubbyNotificationCallback(this);

        mOTAPath = context.getCacheDir().getAbsolutePath();
        mSetupPath = context.getFilesDir().getAbsolutePath();
    }

    public static AferoSofthub acquireInstance(@NonNull Context context, @NonNull AferoClient aferoClient, @Nullable String appInfo) {
        if (sInstance == null) {
            sInstance = new AferoSofthub(context, aferoClient, appInfo);
        }

        return sInstance;
    }

    public static void releaseInstance() {
        sInstance = null;
    }

    public synchronized @NonNull Observable<AferoSofthub> start() {
        AfLog.i("AferoSofthub.start");

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
        AfLog.i("AferoSofthub.stop");

        if (isRunning()) {
            mIsHubbyRunning = false;
            mHubbyImpl.stop();
        }
    }

    Observable<NotificationCallback.CompleteReason> observeCompletion() {
        return mCompleteSubject;
    }

    Observable<String> observeAssociation() {
        return mAssociateSubject;
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

//        if (!(isStarting() || isRunning())) {
//            start().subscribe(new RxUtils.IgnoreResponseObserver<AferoSofthub>());
//        }
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
        AfLog.i("AferoSofthub.onCallStart");

        if (!sIsHubbyInitialized) {
            AfLog.i("AferoSofthub: initializing hubby");

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
        AfLog.i("AferoSofthub: starting hubby");

        String hwInfo = "os:android,manufacturer:" + Build.MANUFACTURER +
                ",model:" + Build.MODEL +
                ",version:" + Build.VERSION.RELEASE
                ;

        if (mAppInfo != null && !mAppInfo.isEmpty()) {
            hwInfo += "," + mAppInfo.trim();
            if (hwInfo.length() > HARDWARE_INFO_MAX_LENGTH) {
                hwInfo = hwInfo.substring(0, HARDWARE_INFO_MAX_LENGTH);
            }
        }

        final String setupDirName = "shs" + (Build.MANUFACTURER + Build.MODEL + mAferoClient.getActiveAccountId()).hashCode();

        HashMap<Hubby.Config,String> config = new HashMap<>(1);
        config.put(Hubby.Config.SOFT_HUB_SETUP_PATH, mSetupPath + "/" + setupDirName);
        config.put(Hubby.Config.OTA_WORKING_PATH, mOTAPath);
        config.put(Hubby.Config.HARDWARE_INFO, hwInfo);

        if (mService != null) {
            config.put(Hubby.Config.SERVICE, mService);
        }

        mHubbyImpl.start(config, mNotificationCallback);
    }

    private void onInitializationComplete() {
        AfLog.i("AferoSofthub.onInitializationComplete");
        mIsHubbyRunning = true;

        mStartSubject.onCompleted();
        mStartSubject = null;
    }

    private void onRunComplete(NotificationCallback.CompleteReason completeReason) {
        AfLog.i("AferoSofthub.onRunComplete");
        mCompleteSubject.onNext(completeReason);
    }

    private void onSecureHubAssociationNeeded(String assId) {
        AfLog.i("AferoSofthub.onSecureHubAssociationNeeded");

        mAferoClient.deviceAssociate(assId)
            .subscribe(new Observer<DeviceAssociateResponse>() {
                @Override
                public void onCompleted() {}

                @Override
                public void onError(Throwable e) {
                    AfLog.e("AferoSofthub startup error - deviceAssociate failed");
                    AfLog.e(e);
                    mStartSubject.onError(e);
                    mStartSubject = null;
                }

                @Override
                public void onNext(DeviceAssociateResponse response) {
                    mAssociateSubject.onNext(response.deviceId);
                }
            });
    }

    private void onOtaStateChange(String deviceId, OtaCallback.OtaState otaState, int offset, int total) {
        AfLog.i("AferoSofthub.onOtaStateChange");

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
        AfLog.i("AferoSofthub.onOTAStart");

        synchronized (mActiveOTAs) {
            if (mOTAWakeLock == null) {
                AfLog.i("AferoSofthub.onOTAStart: Grabbing wakelock");

                Context context = getContext();
                if (context != null) {
                    PowerManager powerManager = (PowerManager)context.getSystemService(Activity.POWER_SERVICE);
                    mOTAWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
                    mOTAWakeLock.acquire();
                }

                startWakeLockWatchdog();

                OTAEntry ota = new OTAEntry();
                mActiveOTAs.put(deviceId, ota);
            }
        }
    }

    private void onOTAStop(String deviceId) {
        AfLog.i("AferoSofthub: OTA stopping - waiting to release wakelock");

        synchronized (mActiveOTAs) {
            mActiveOTAs.remove(deviceId);
        }

        if (!isOTAInProgress()) {
            mOTAStopTime = 0;
            onOTAEnd();
        }
    }

    private void onOTAOngoing(String deviceId) {
        updateWakeLockWatchdog();
    }

    private void onOTAEnd() {
        long now = System.currentTimeMillis();
        if (mOTAStopTime == 0 || now >= mOTAStopTime) {
            AfLog.i("AferoSofthub: releasing wakelock");

            synchronized (mActiveOTAs) {
                mActiveOTAs.clear();
            }

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

    private void startWakeLockWatchdog() {
        mOTAStopTime = System.currentTimeMillis() + OTA_END_DELAY;
        mOTAEndSubscription = Observable.interval(WAKE_LOCK_WATCHDOG_INTERVAL, TimeUnit.MILLISECONDS, Schedulers.computation())
            .subscribe(new Action1<Long>() {
                @Override
                public void call(Long x) {
                    onOTAEnd();
                }
            });
    }

    private void updateWakeLockWatchdog() {
        mOTAStopTime = System.currentTimeMillis() + OTA_END_DELAY;
    }

    boolean isOTAInProgress() {
        return !mActiveOTAs.isEmpty();
    }

    boolean isWakeLockHeld() {
        return mOTAWakeLock != null && mOTAWakeLock.isHeld();
    }

    private static class HubbyNotificationCallback implements NotificationCallback {

        private WeakReference<AferoSofthub> mRef;

        private final OtaCallback.OtaState[] mOtaStateValues = OtaCallback.OtaState.values();
        private final CompleteReason[] mCompleteReasonValues = CompleteReason.values();

        HubbyNotificationCallback(AferoSofthub hub) {
            mRef = new WeakReference<>(hub);
        }

        @Override
        public void initializationComplete() {
            AfLog.d("HubbyNotificationCallback.initializationComplete");
            AferoSofthub hub = mRef.get();
            if (hub != null) {
                hub.onInitializationComplete();
            }
        }

        @Override
        public void runComplete(int completeReasonRaw) {
            AfLog.d("HubbyNotificationCallback.runComplete: completeReason=" + completeReasonRaw);

            AferoSofthub hub = mRef.get();
            if (hub != null) {
                CompleteReason completeReason = completeReasonfromInt(completeReasonRaw);
                if (completeReason != null) {
                    hub.onRunComplete(completeReason);
                }
            }
        }

        @Override
        public void otaStatus(int stateRaw, String deviceId, int offset, int total) {
            AfLog.d("HubbyNotificationCallback.otaStatus: state=" + stateRaw + " deviceId=" + deviceId + " offset=" + offset + " total=" + total);

            AferoSofthub hub = mRef.get();
            if (hub != null) {
                OtaCallback.OtaState otaState = otaStatefromInt(stateRaw);
                if (otaState != null) {
                    hub.onOtaStateChange(deviceId, otaState, offset, total);
                }
            }
        }

        @Override
        public void secureHubAssociationNeeded(String assId) {
            AfLog.d("HubbyNotificationCallback.secureHubAssociationNeeded: assId=" + assId);
            AferoSofthub hub = mRef.get();
            if (hub != null) {
                hub.onSecureHubAssociationNeeded(assId);
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

    private static class HubbyStarter implements Callable<AferoSofthub> {

        private WeakReference<AferoSofthub> mRef;

        HubbyStarter(AferoSofthub hub) {
            mRef = new WeakReference<>(hub);
        }

        @Override
        public AferoSofthub call() throws Exception {
            AferoSofthub hub = mRef.get();
            hub.onCallStart();
            return hub;
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
