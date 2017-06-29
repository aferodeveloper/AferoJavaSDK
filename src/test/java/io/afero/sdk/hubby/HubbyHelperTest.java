/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.hubby;

import android.app.Activity;
import android.content.Context;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;

import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.ActionResponse;
import io.afero.sdk.client.afero.models.ConclaveAccessDetails;
import io.afero.sdk.client.afero.models.DeviceAssociateResponse;
import io.afero.sdk.client.afero.models.DeviceRequest;
import io.afero.sdk.client.afero.models.Location;
import io.afero.sdk.client.afero.models.PostActionBody;
import io.afero.sdk.client.afero.models.RequestResponse;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceProfile;
import io.kiban.hubby.Hubby;
import io.kiban.hubby.NotificationCallback;
import io.kiban.hubby.OtaCallback;
import rx.Observable;
import rx.Observer;
import rx.functions.Action1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class HubbyHelperTest {

    private final static String DEVICE_ID = "device-id";

    @After
    public void hubbyHelperReleaseInstance() {
        HubbyHelper.releaseInstance();
    }

    @Test
    public void init() throws Exception {
        makeHubbyHelperTester()
                .verifyHubbyHelperCreated();
    }

    @Test
    public void start() throws Exception {
        makeHubbyHelperTester()

                .startHubby()
                .waitUntilStartedCalled()

                .verifyHubbyIsStarting()
                .verifyHubbyNotRunning()

                .hubbyCallbackInitializationCompleted()
                .waitUntilStartCompleted()

                .verifyHubbyNotStarting()
                .verifyHubbyIsRunning()
                .verifyHubbyStartCompleted()
        ;
    }

    @Test
    public void stop() throws Exception {
        makeHubbyHelperTester()

                .startHubbyCompletely()
                .verifyHubbyIsRunning()

                .stopHubby()
                .verifyHubbyNotRunning()
        ;
    }

    @Test
    public void onPause() throws Exception {
        makeHubbyHelperTester()

                .startHubbyCompletely()
                .verifyHubbyIsRunning()
                .verifyHubbyIsActive()

                .pauseHubby()

                .verifyHubbyNotActive()
                .verifyHubbyNotRunning()
        ;
    }

    @Test
    public void onResume() throws Exception {
        makeHubbyHelperTester()

                .startHubbyCompletely()
                .pauseHubby()

                .verifyHubbyNotActive()
                .verifyHubbyNotRunning()

                .resumeHubby()

                .waitUntilStartedCalled()
                .hubbyCallbackInitializationCompleted()
                .waitUntilStartCompleted()

                .verifyHubbyIsRunning()
                .verifyHubbyIsActive()
        ;
    }

    @Test
    public void isActive() throws Exception {
        makeHubbyHelperTester()

                .verifyHubbyIsActive()

                .pauseHubby()

                .verifyHubbyNotActive()

                .resumeHubby()

                .verifyHubbyIsActive()
        ;
    }

    @Test
    public void isRunning() throws Exception {
        makeHubbyHelperTester()

                .verifyHubbyNotRunning()

                .startHubbyCompletely()

                .verifyHubbyIsRunning()

                .stopHubby()

                .verifyHubbyNotRunning()
        ;
    }

    @Test
    public void isStarting() throws Exception {
        makeHubbyHelperTester()

                .verifyHubbyNotStarting()

                .startHubby()

                .verifyHubbyIsStarting()

                .waitUntilStartedCalled()

                .verifyHubbyIsStarting()

                .hubbyCallbackInitializationCompleted()
                .waitUntilStartCompleted()

                .verifyHubbyNotStarting()
        ;
    }

    @Test
    public void observeCompletionReason() throws Exception {
        makeHubbyHelperTester()
                .startHubbyCompletely()

                .stopHubby()

                .verifyCompleteReason(NotificationCallback.CompleteReason.STOP_CALLED)
            ;
    }

    @Test
    public void setService() throws Exception {
        makeHubbyHelperTester()

                .setServiceToDev()

                .startHubby()
                .waitUntilStartedCalled()

                .verifyServiceInConfigIsSetToDev()
            ;
    }

    @Test
    public void testOTAStartAndStop() throws Exception {
        makeHubbyHelperTester()
                .startHubbyCompletely()

                .hubbyCallbackOTAStatus(OtaCallback.OtaState.START, DEVICE_ID)

                .verifyOTAInProgress()

                .hubbyCallbackOTAStatus(OtaCallback.OtaState.STOP, DEVICE_ID)

                .verifyOTANotInProgress()
        ;
    }

    @Test
    public void testOTAWithHubbyPause() throws Exception {
        makeHubbyHelperTester()
                .startHubbyCompletely()

                .hubbyCallbackOTAStatus(OtaCallback.OtaState.START, DEVICE_ID)

                .verifyOTAInProgress()

                .pauseHubby()

                .verifyHubbyNotActive()
                .verifyHubbyIsRunning()

                .hubbyCallbackOTAStatus(OtaCallback.OtaState.STOP, DEVICE_ID)

                .verifyOTANotInProgress()
                .verifyHubbyNotActive()
                .verifyHubbyNotRunning()
        ;
    }

    // test support --------------------------------------------------------

    private HubbyHelperTester makeHubbyHelperTester() {
        return new HubbyHelperTester();
    }

    private class HubbyHelperTester {
        final Activity activity;
        final MockAferoClient aferoClient = new MockAferoClient();
        final MockHubbyImpl hubbyImpl = new MockHubbyImpl();
        final StartObserver startObserver = new StartObserver();
        final OnNextComplete onNextComplete = new OnNextComplete();

        HubbyHelper hubbyHelper;

        HubbyHelperTester() {
            activity = Robolectric.buildActivity(Activity.class).create().get();
            hubbyHelper = HubbyHelper.acquireInstance(activity, aferoClient);
            hubbyHelper.setHubbyImpl(hubbyImpl);
            hubbyHelper.observeCompletion().subscribe(onNextComplete);
        }

        HubbyHelperTester startHubby() {
            hubbyHelper.start().subscribe(startObserver);
            return this;
        }

        HubbyHelperTester startHubbyCompletely() {
            return startHubby()
                    .waitUntilStartedCalled()
                    .hubbyCallbackInitializationCompleted()
                    .waitUntilStartCompleted();
        }

        HubbyHelperTester waitUntilStartedCalled() {
            hubbyImpl.waitUntilStartedCalled();
            return this;
        }

        HubbyHelperTester waitUntilStartCompleted() {
            startObserver.waitUntilCompleted();
            return this;
        }

        HubbyHelperTester stopHubby() {
            hubbyHelper.stop();
            return this;
        }

        HubbyHelperTester pauseHubby() {
            hubbyHelper.onPause();
            return this;
        }

        HubbyHelperTester resumeHubby() {
            hubbyHelper.onResume();
            return this;
        }

        HubbyHelperTester hubbyCallbackInitializationCompleted() {
            hubbyImpl.callbackInitializationCompleted();
            return this;
        }

        HubbyHelperTester hubbyCallbackOTAStatus(OtaCallback.OtaState otaState, String deviceId) {
            hubbyImpl.callbackOTAStatus(otaState, deviceId);
            return this;
        }

        HubbyHelperTester setServiceToDev() {
            hubbyHelper.setService("dev");
            return this;
        }

        HubbyHelperTester verifyHubbyHelperCreated() {
            assertNotNull(hubbyHelper);
            return this;
        }

        HubbyHelperTester verifyHubbyStartCompleted() {
            assertTrue(startObserver.isCompleted);
            return this;
        }

        HubbyHelperTester verifyHubbyIsStarting() {
            assertTrue(hubbyHelper.isStarting());
            return this;
        }

        HubbyHelperTester verifyHubbyNotStarting() {
            assertFalse(hubbyHelper.isStarting());
            return this;
        }

        HubbyHelperTester verifyHubbyIsRunning() {
            assertTrue(hubbyHelper.isRunning());
            return this;
        }

        HubbyHelperTester verifyHubbyNotRunning() {
            assertFalse(hubbyHelper.isRunning());
            return this;
        }

        HubbyHelperTester verifyHubbyIsActive() {
            assertTrue(hubbyHelper.isActive());
            return this;
        }

        HubbyHelperTester verifyHubbyNotActive() {
            assertFalse(hubbyHelper.isActive());
            return this;
        }

        HubbyHelperTester verifyServiceInConfigIsSetToDev() {
            assertEquals("dev", hubbyImpl.mConfigs.get(Hubby.Config.SERVICE));
            return this;
        }

        HubbyHelperTester verifyCompleteReason(NotificationCallback.CompleteReason expectedReason) {
            assertEquals(expectedReason, onNextComplete.mReason);
            return this;
        }

        HubbyHelperTester verifyOTAInProgress() {
            assertTrue(hubbyHelper.isOTAInProgress());
            return this;
        }

        HubbyHelperTester verifyOTANotInProgress() {
            assertFalse(hubbyHelper.isOTAInProgress());
            return this;
        }
    }

    private class MockHubbyImpl implements HubbyHelper.HubbyImpl {

        private Context mContext;
        private HashMap<Hubby.Config, String> mConfigs;
        private NotificationCallback mCallback;
        private final Object mStartLock = new Object();

        @Override
        public void initialize(Context context) {
            mContext = context;
        }

        @Override
        public void start(HashMap<Hubby.Config, String> configs, final NotificationCallback callback) {
            mConfigs = configs;
            mCallback = callback;

            synchronized (mStartLock) {
                mStartLock.notifyAll();
            }
        }

        @Override
        public void stop() {
            mCallback.runComplete(NotificationCallback.CompleteReason.STOP_CALLED.getValue());
        }

        void callbackInitializationCompleted() {
            mCallback.initializationComplete();
        }

        void callbackOTAStatus(OtaCallback.OtaState otaState, String deviceId) {
            mCallback.otaStatus(otaState.getValue(), deviceId, 0, 0);
        }

        void waitUntilStartedCalled() {
            synchronized (mStartLock) {
                try {
                    mStartLock.wait(5000);
                } catch (InterruptedException e) {
                    assertTrue("waitUntilStartedCalled timed out", false);
                }
            }
        }
    }

    ;

    class MockAferoClient implements AferoClient {

        @Override
        public Observable<ActionResponse> postAttributeWrite(DeviceModel deviceModel, PostActionBody body, int maxRetryCount, int statusCode) {
            return null;
        }

        @Override
        public Observable<RequestResponse[]> postBatchAttributeWrite(DeviceModel deviceModel, DeviceRequest[] body, int maxRetryCount, int statusCode) {
            return null;
        }

        @Override
        public Observable<DeviceProfile> getDeviceProfile(String profileId, String locale, ImageSize imageSize) {
            return null;
        }

        @Override
        public Observable<DeviceProfile[]> getAccountDeviceProfiles(String locale, ImageSize imageSize) {
            return null;
        }

        @Override
        public Observable<ConclaveAccessDetails> postConclaveAccess(String mobileClientId) {
            return null;
        }

        @Override
        public Observable<Location> putDeviceLocation(String deviceId, Location location) {
            return null;
        }

        @Override
        public Observable<Location> getDeviceLocation(DeviceModel deviceModel) {
            return null;
        }

        @Override
        public Observable<DeviceAssociateResponse> deviceAssociate(String associationId, boolean isOwnershipVerified, String locale, ImageSize imageSize) {
            return null;
        }

        @Override
        public Observable<DeviceModel> deviceDisassociate(DeviceModel deviceModel) {
            return null;
        }

        @Override
        public String getActiveAccountId() {
            return "account-id";
        }

        @Override
        public int getStatusCode(Throwable t) {
            return 0;
        }

        @Override
        public boolean isTransferVerificationError(Throwable t) {
            return false;
        }
    }

    static class StartObserver implements Observer<HubbyHelper> {

        boolean isCompleted;
        final Object completedLock = new Object();

        @Override
        public void onCompleted() {
            synchronized (completedLock) {
                isCompleted = true;
                completedLock.notifyAll();
            }
        }

        @Override
        public void onError(Throwable e) {
            assertTrue(false);
        }

        @Override
        public void onNext(HubbyHelper hubbyHelper) {
        }

        public void waitUntilCompleted() {
            synchronized (completedLock) {
                try {
                    completedLock.wait(1000);
                } catch (InterruptedException e) {
                    assertTrue(false);
                }
            }
        }
    }

    private static class OnNextComplete implements Action1<NotificationCallback.CompleteReason> {

        NotificationCallback.CompleteReason mReason;

        OnNextComplete() {

        }

        @Override
        public void call(NotificationCallback.CompleteReason reason) {
            mReason = reason;
        }
    }

}