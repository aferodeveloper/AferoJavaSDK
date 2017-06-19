/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.hubby;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.RequiresPermission;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.ConclaveAccessDetails;
import io.afero.sdk.client.afero.models.Location;
import io.afero.sdk.conclave.ConclaveAccessManager;
import io.afero.sdk.log.AfLog;
import io.afero.sdk.utils.JSONUtils;
import io.afero.sdk.utils.RxUtils;
import io.kiban.hubby.Account;
import io.kiban.hubby.Hubby;
import io.kiban.hubby.InitCallback;
import io.kiban.hubby.NewTokenCallback;
import io.kiban.hubby.OtaCallback;
import rx.Observer;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class HubbyHelper {

    private final AferoClient mAferoClient;
    private ConclaveAccessManager mConclaveAccessManager;
    private boolean mIsHubbyStarting;
    private boolean mIsHubbyRunning;
    private static boolean sIsHubbyInitialized;
    private String mHubbyAccountId;
    private String mClientId;

    private boolean mIsOTAInProgress;
    private PowerManager.WakeLock mOTAWakeLock;
    private long mOTAStopTime;
    private static final long OTA_END_DELAY = 10000;

    private Context mContext;
    private Handler mHandler;
    private boolean mIsActive;
    private boolean mHasHubLocation;

    private final boolean mIsNativeHubbyDisabled;

    private OTAEnded mOTAEndedRunnable = new OTAEnded(this);

    public HubbyHelper(ConclaveAccessManager cam, AferoClient aferoClient, boolean isSimulator) {
        mConclaveAccessManager = cam;
        mAferoClient = aferoClient;
        mIsNativeHubbyDisabled = isSimulator;

        if (mIsNativeHubbyDisabled) {
            return;
        }

        mConclaveAccessManager.getObservable().subscribe(new UpdateConclaveAccessObserver(this));
        mHandler = new Handler();
    }

    public void start(Context context, String clientId) {

        if (mIsNativeHubbyDisabled) {
            return;
        }

        if (isRunning()) {
            return;
        }

        mContext = context;

        mHubbyAccountId = mAferoClient.getActiveAccountId();
        mClientId = clientId;

        if (!sIsHubbyInitialized) {
            AfLog.i("HubbyManager: initializing hubby");
            Hubby.initialize(context);
            sIsHubbyInitialized = true;
        }

        HashMap<Hubby.Config,String> config = new HashMap<Hubby.Config, String>(1);
        config.put(Hubby.Config.OTA_WORKING_PATH, context.getFilesDir().getAbsolutePath());

        mConclaveAccessManager.getAccess(clientId)
            .subscribe(new StartConclaveAccessObserver(this, config));
    }

    public void stop() {

        if (mIsNativeHubbyDisabled) {
            return;
        }

        if (isRunning()) {
            AfLog.i("HubbyManager.stop");
            mHubbyAccountId = null;
            Hubby.stop();
            mIsHubbyRunning = false;
        }
    }

    public void onPause() {
        mIsActive = false;

        if (mIsNativeHubbyDisabled) {
            return;
        }

        if (mContext != null) {
            if (!(isOTAInProgress() || isWaitingToReleaseWakelock())) {
                stop();
            }
        }
    }

    public void onResume() {
        mIsActive = true;

    }

    public boolean isActive() {
        return mIsActive;
    }

    public boolean isRunning() {
        return mIsHubbyRunning;
    }
    public boolean isStarting() {
        return mIsHubbyStarting;
    }

    private InitCallback mInitCallback = new InitCallback() {
        @Override
        public void initFinished() {
            mIsHubbyStarting = false;
            mIsHubbyRunning = true;
            AfLog.i("HubbyManager: hubby started - " + mHubbyAccountId);

            // if we have not yet resolved a geolocation for this softhub, then
            // start a background task to do just that.
            if(!mHasHubLocation) {
                new HubbyLocationTask().execute(mConclaveAccessManager.getConclaveAccessDetails());
            }
        }
    };


    private OtaCallback mOTACallback = new OtaCallback() {
        /**
         * The function that's called when the local hub is performing an OTA to a given peripheral.
         * This is called on the one and only thread in Hubby so you shouldn't do anything intensive
         * on it as it'll slow down all processing within Hubby.
         *
         * @param state     - the state of the OTA
         * @param deviceId  - the device id of the peripheral being updated
         * @param offset    - how many bytes we've transferred to the device
         * @param total     - the total size of the OTA
         */
        @Override
        public void otaProgress(int state, String deviceId, int offset, int total) {
            AfLog.d("HubbyManager.otaProgress: " + offset + " of " + total);
            onOTAProgress(state);
        }
    };

    private void onGetFirstConclaveAccessDetails(ConclaveAccessDetails cad, HashMap<Hubby.Config,String> config) {
        if (!(isRunning() || isStarting())) {
            try {
                String conclaveAccessJson = JSONUtils.writeValueAsString(cad);
                Account hubbyAccount = new Account(mAferoClient.getActiveAccountId(), conclaveAccessJson, new NewConclaveTokenCallback(this));
                mHubbyAccountId = mAferoClient.getActiveAccountId();

                mIsHubbyStarting = true;
                AfLog.i("HubbyManager: starting hubby - " + mHubbyAccountId);
                Hubby.start(config, hubbyAccount, mInitCallback, mOTACallback);
            } catch (Exception e) {
                AfLog.e(e);
            }
        }
    }

    private void onUpdateConclaveAccessDetails(ConclaveAccessDetails cad) {
        if (isRunning()) {
            try {
                String conclaveAccessJson = JSONUtils.writeValueAsString(cad);
                Account hubbyAccount = new Account(mAferoClient.getActiveAccountId(), conclaveAccessJson, new NewConclaveTokenCallback(this));

                if (mHubbyAccountId != null && !mHubbyAccountId.equals(mAferoClient.getActiveAccountId())) {
                    AfLog.i("HubbyManager: removing account - " + mHubbyAccountId);
                    Hubby.removeAccount(mHubbyAccountId);
                }
                mHubbyAccountId = mAferoClient.getActiveAccountId();

                AfLog.i("HubbyManager: updating account - " + mHubbyAccountId);
                Hubby.updateAccount(hubbyAccount);
            } catch (Exception e) {
                AfLog.e(e);
            }
        }
    }

    private void onOTAProgress(int state) {
        if (state == OtaCallback.OtaState.START.getValue()) {
            mIsOTAInProgress = true;
            if (mOTAWakeLock == null) {
                AfLog.i("HubbyManager: OTA starting - grabbing wakelock");
                PowerManager powerManager = (PowerManager)mContext.getSystemService(Activity.POWER_SERVICE);
                mOTAWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "io.afero.OTAWakeLockTag");
                mOTAWakeLock.acquire();
            }
        }
        else if (state == OtaCallback.OtaState.STOP.getValue()) {
            mIsOTAInProgress = false;
            AfLog.i("HubbyManager: OTA stopping - waiting to release wakelock");
            mHandler.postDelayed(mOTAEndedRunnable, OTA_END_DELAY);
        }

        mOTAStopTime = System.currentTimeMillis() + OTA_END_DELAY;
    }

    private void onOTAEnd() {
        long now = System.currentTimeMillis();
        if (mOTAStopTime != 0 && now >= mOTAStopTime) {
            AfLog.i("HubbyManager: releasing wakelock");

            if (mOTAWakeLock != null) {
                mOTAWakeLock.release();
                mOTAWakeLock = null;
                mOTAStopTime = 0;
            }

            if (!isActive()) {
                stop();
            }
        } else if (!mIsOTAInProgress) {
            AfLog.i("HubbyManager: still waiting to release wakelock");
            mHandler.postDelayed(mOTAEndedRunnable, OTA_END_DELAY);
        }
    }

    private boolean isOTAInProgress() {
        return mIsOTAInProgress;
    }

    private boolean isWaitingToReleaseWakelock() {
        return mOTAWakeLock != null && mOTAWakeLock.isHeld();
    }

    private void onHubbyNeedsNewToken(String s) {
        mConclaveAccessManager.updateAccess(mClientId);
    }

    private static class NewConclaveTokenCallback implements NewTokenCallback {

        WeakReference<HubbyHelper> mRef;

        public NewConclaveTokenCallback(HubbyHelper hm) {
            mRef = new WeakReference<>(hm);
        }

        @Override
        public void needNewToken(String s) {
            HubbyHelper hm = mRef.get();
            if (hm != null) {
                hm.onHubbyNeedsNewToken(s);
            }
        }
    }

    private static class StartConclaveAccessObserver extends RxUtils.WeakObserver<ConclaveAccessDetails,HubbyHelper> {

        private HashMap<Hubby.Config,String> mConfig;

        public StartConclaveAccessObserver(HubbyHelper hm, HashMap<Hubby.Config,String> config) {
            super(hm);
            mConfig = config;
        }

        @Override
        public void onCompleted(HubbyHelper hm) {

        }

        @Override
        public void onError(HubbyHelper hm, Throwable e) {

        }

        @Override
        public void onNext(HubbyHelper hm, ConclaveAccessDetails cad) {
            hm.onGetFirstConclaveAccessDetails(cad, mConfig);
        }
    }

    private static class UpdateConclaveAccessObserver extends RxUtils.WeakObserver<ConclaveAccessDetails,HubbyHelper> {

        public UpdateConclaveAccessObserver(HubbyHelper hm) {
            super(hm);
        }

        @Override
        public void onCompleted(HubbyHelper hm) {

        }

        @Override
        public void onError(HubbyHelper hm, Throwable e) {

        }

        @Override
        public void onNext(HubbyHelper hm, ConclaveAccessDetails cad) {
            hm.onUpdateConclaveAccessDetails(cad);
        }
    }

    private static class OTAEnded implements Runnable {

        WeakReference<HubbyHelper> mRef;

        OTAEnded(HubbyHelper hm) {
            mRef = new WeakReference<HubbyHelper>(hm);
        }

        @Override
        public void run() {
            HubbyHelper a = mRef.get();
            if (a != null) {
                a.onOTAEnd();
            }
        }
    }


    private class HubbyLocationTask extends AsyncTask<Object, Void, Void> {

        @Override
        @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
        protected Void doInBackground(Object... objects) {
            AfLog.i("HubbyManager: Resolving location for software hub...");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (mContext.checkSelfPermission(ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        mContext.checkSelfPermission(ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    AfLog.i("Could not resolve a location for the softhub, location is disabled");
                return null;
            }

            try {
                android.location.Location location = null;
                LocationManager locationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);

                // if we have access to the LocationManager, then try to get a reading. Use GPS first, then
                // fallback to network provider if no direct GPS reading is available.

                if (locationManager != null) {

                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    // try to get a GPS location first
                    if (location == null) {

                        // if a GPS location is not available, try to get a Network location
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                }

                // OK, if we've got a location, then tease out the deviceId from the
                // softhub and update the location in the cloud.

                if (location != null) {

                    // peel out the conclaveAccessDetails
                    ConclaveAccessDetails conclaveAccessDetails = (ConclaveAccessDetails) objects[0];

                    // little method here for parsing out the deviceId from the conclaveAccessDetails
                    final String deviceId = resolveDeviceIdFromConclaveAcccess(conclaveAccessDetails);

                    if (deviceId != null) {
                        AfLog.i("HubbyManager: Resolved Location for Softhub (" + deviceId + "), latitude=" + location.getLatitude() + ",longitude=" + location.getLongitude());

                        // create an Afero location object from the android location object
                        Location aferoLocation = new Location();
                        aferoLocation.latitude = String.valueOf(location.getLatitude());
                        aferoLocation.longitude = String.valueOf(location.getLongitude());
                        aferoLocation.locationSourceType = "HUB_LOCATION_GPS";

// FIXME: Remove this?
//                        // send it to the service
//                        mAferoClient.putDeviceLocation(deviceId, aferoLocation).subscribe(new Observer<Location>() {
//                            @Override
//                            public void onCompleted() {
//                                AfLog.i("HubbyManager: Successfully stored new location for softhub " + deviceId);
//                            }
//                            @Override
//                            public void onError(Throwable e) {
//                                AfLog.i("HubbyManager: Problem storing location for softhub " + deviceId);
//                                e.printStackTrace();
//                            }
//
//                            @Override
//                            public void onNext(Location loc) {}
//                        });

                    } else {
                        AfLog.i("HubbyManager: Could not resolve softhub deviceId after location resolution");
                    }
                }else{
                    AfLog.i("Could not resolve a location for the softhub");
                }

            } catch (Exception ex) {
                AfLog.e(ex);
            }


            return null;
        }

        private String resolveDeviceIdFromConclaveAcccess(ConclaveAccessDetails conclaveAccessDetails) {
            String deviceId = null;

            for(int x=0;x<conclaveAccessDetails.tokens.length;x++){
                if(conclaveAccessDetails.tokens[x].client != null){
                    if(conclaveAccessDetails.tokens[x].client.get("type") != null){
                        if(conclaveAccessDetails.tokens[x].client.get("type").toLowerCase().equals("softhub")){
                            deviceId = conclaveAccessDetails.tokens[x].client.get("deviceId");
                        }
                    }
                }
            }
            return deviceId;
        }

    }

}
