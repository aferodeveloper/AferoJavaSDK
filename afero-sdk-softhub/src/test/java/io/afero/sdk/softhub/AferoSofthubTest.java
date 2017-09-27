/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.softhub;

import android.app.Activity;
import android.content.Context;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;

import io.afero.hubby.Hubby;
import io.afero.hubby.NotificationCallback;
import io.afero.hubby.OtaCallback;
import io.afero.sdk.client.mock.MockAferoClient;
import rx.Observer;
import rx.functions.Action1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class AferoSofthubTest {

    private final static String DEVICE_ID = "device-id";

    @After
    public void hubbyHelperReleaseInstance() {
        AferoSofthub.releaseInstance();
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
    public void observeAssociation() throws Exception {
        makeHubbyHelperTester()
                .startHubbyCompletely()

                .hubbyCallbackAssociationNeeded("aferoSofthubTest")

                .verifyAssociatedDeviceId("device-id")
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
                .verifyWakeLockHeld()

                .hubbyCallbackOTAStatus(OtaCallback.OtaState.STOP, DEVICE_ID)
                .waitUntilHubbyStop()

                .verifyOTANotInProgress()
                .verifyHubbyNotActive()
                .verifyHubbyNotRunning()
                .verifyWakeLockReleased()
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
        final OnNextAssociation onNextAssociation = new OnNextAssociation();

        AferoSofthub aferoSofthub;

        HubbyHelperTester() {
            activity = Robolectric.buildActivity(Activity.class).create().get();
            aferoSofthub = AferoSofthub.acquireInstance(activity, aferoClient, "clientId: 17824C90-4FBC-4C22-96C6-F6755495280D");
            aferoSofthub.setHubbyImpl(hubbyImpl);
            aferoSofthub.observeCompletion().subscribe(onNextComplete);
            aferoSofthub.observeAssociation().subscribe(onNextAssociation);
        }

        HubbyHelperTester startHubby() {
            aferoSofthub.start().subscribe(startObserver);
            return this;
        }

        HubbyHelperTester startHubbyCompletely() {
            return startHubby()
                    .waitUntilStartedCalled()
                    .hubbyCallbackInitializationCompleted()
                    .waitUntilStartCompleted();
        }

        HubbyHelperTester waitUntilStartedCalled() {
            hubbyImpl.waitUntilStarted();
            return this;
        }

        HubbyHelperTester waitUntilStartCompleted() {
            startObserver.waitUntilCompleted();
            return this;
        }

        HubbyHelperTester waitUntilHubbyStop() {
            hubbyImpl.waitUntilStopped();
            return this;
        }

        HubbyHelperTester stopHubby() {
            aferoSofthub.stop();
            return this;
        }

        HubbyHelperTester pauseHubby() {
            aferoSofthub.onPause();
            return this;
        }

        HubbyHelperTester resumeHubby() {
            aferoSofthub.onResume();
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

        HubbyHelperTester hubbyCallbackAssociationNeeded(String assId) {
            hubbyImpl.callbackAssociationNeeded(assId);
            return this;
        }

        HubbyHelperTester setServiceToDev() {
            aferoSofthub.setService("dev");
            return this;
        }

        HubbyHelperTester verifyHubbyHelperCreated() {
            assertNotNull(aferoSofthub);
            return this;
        }

        HubbyHelperTester verifyHubbyStartCompleted() {
            assertTrue(startObserver.isCompleted);
            return this;
        }

        HubbyHelperTester verifyHubbyIsStarting() {
            assertTrue(aferoSofthub.isStarting());
            return this;
        }

        HubbyHelperTester verifyHubbyNotStarting() {
            assertFalse(aferoSofthub.isStarting());
            return this;
        }

        HubbyHelperTester verifyHubbyIsRunning() {
            assertTrue(aferoSofthub.isRunning());
            return this;
        }

        HubbyHelperTester verifyHubbyNotRunning() {
            assertFalse(aferoSofthub.isRunning());
            return this;
        }

        HubbyHelperTester verifyHubbyIsActive() {
            assertTrue(aferoSofthub.isActive());
            return this;
        }

        HubbyHelperTester verifyHubbyNotActive() {
            assertFalse(aferoSofthub.isActive());
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
            assertTrue(aferoSofthub.isOTAInProgress());
            return this;
        }

        HubbyHelperTester verifyOTANotInProgress() {
            assertFalse(aferoSofthub.isOTAInProgress());
            return this;
        }

        HubbyHelperTester verifyWakeLockHeld() {
            assertTrue(aferoSofthub.isWakeLockHeld());
            return this;
        }

        HubbyHelperTester verifyWakeLockReleased() {
            assertFalse(aferoSofthub.isWakeLockHeld());
            return this;
        }

        HubbyHelperTester verifyAssociatedDeviceId(String deviceId) {
            assertEquals(deviceId, onNextAssociation.mDeviceId);
            return this;
        }
    }

    private class MockHubbyImpl implements AferoSofthub.HubbyImpl {

        private Context mContext;
        private HashMap<Hubby.Config, String> mConfigs;
        private NotificationCallback mCallback;
        private final Object mStartLock = new Object();
        private final Object mStopLock = new Object();

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

            synchronized (mStopLock) {
                mStopLock.notifyAll();
            }
        }

        void callbackInitializationCompleted() {
            mCallback.initializationComplete();
        }

        void callbackOTAStatus(OtaCallback.OtaState otaState, String deviceId) {
            mCallback.otaStatus(otaState.getValue(), deviceId, 0, 0);
        }

        void callbackAssociationNeeded(String assId) {
            mCallback.secureHubAssociationNeeded(assId);
        }

        void waitUntilStarted() {
            synchronized (mStartLock) {
                try {
                    mStartLock.wait(5000);
                } catch (InterruptedException e) {
                    assertTrue("waitUntilStarted timed out", false);
                }
            }
        }

        void waitUntilStopped() {
            synchronized (mStopLock) {
                try {
                    mStopLock.wait(5000);
                } catch (InterruptedException e) {
                    assertTrue("waitUntilStopped timed out", false);
                }
            }
        }
    }


    static class StartObserver implements Observer<AferoSofthub> {

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
        public void onNext(AferoSofthub aferoSofthub) {
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

    private static class OnNextAssociation implements Action1<String> {

        String mDeviceId;

        @Override
        public void call(String deviceId) {
            mDeviceId = deviceId;
        }
    }
}