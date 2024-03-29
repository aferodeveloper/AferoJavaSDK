/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.afero.sdk.conclave.models.OTAInfo;
import rx.Observer;
import rx.schedulers.Schedulers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OTAWatcherTest {

    @Test
    public void testNormalOTA() throws Exception {
        makeTester()
                .verifyOTAInProgress(false)

                .otaStart()
                .otaObserve()

                .verifyOTAInProgress(true)
                .verifyOTACompleted(false)
                .verifyOTAProgress(0)

                .otaOngoing(10)
                .verifyOTAProgress(10)

                .otaOngoing(25)
                .verifyOTAProgress(25)

                .otaOngoing(50)
                .verifyOTAProgress(50)

                .otaOngoing(75)
                .verifyOTAProgress(75)

                .otaOngoing(99)
                .verifyOTAProgress(100)

                .otaOngoing(100)
                .verifyOTAProgress(0)

                .otaOngoing(125)
                .verifyOTAProgress(25)

                .otaOngoing(150)
                .verifyOTAProgress(50)

                .otaOngoing(175)
                .verifyOTAProgress(75)

                .otaOngoing(199)
                .verifyOTAProgress(100)

                .otaOngoing(200)
                .verifyOTAProgress(100)

                .otaStop()
                .verifyOTACompleted(true)
        ;

    }

    @Test
    public void testOTATimeout() throws Exception {
        makeTester()
                .otaStart()
                .waitForOTATimeout()
                .verifyOTATimeout()
        ;
    }


    private Tester makeTester() throws IOException {
        return new Tester();
    }

    private class Tester {
        static final int OTA_TOTAL = 200;

        private static final long OTA_TIMEOUT_SECONDS = 2;

        private final OTAWatcher otaWatcher = new OTAWatcher(OTA_TIMEOUT_SECONDS);
        private final Object otaTimeoutLock = new Object();

        private boolean otaCompleted;
        private Integer otaProgress;
        private Throwable otaError;


        Tester() throws IOException {
        }

        Tester otaStart() {
            OTAInfo ota = new OTAInfo(OTAInfo.OtaState.START, "device-id", 0, OTA_TOTAL);
            otaWatcher.onOTA(ota);
            return this;
        }

        Tester otaObserve() {
            otaWatcher.getProgressObservable()
                    .subscribe(new Observer<Integer>() {
                        @Override
                        public void onCompleted() {
                            otaCompleted = true;
                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onNext(Integer progress) {
                            otaProgress = progress;
                        }
                    });

            return this;
        }

        Tester otaOngoing(int p) {
            OTAInfo ota = new OTAInfo(OTAInfo.OtaState.ONGOING, "device-id", p, OTA_TOTAL);
            otaWatcher.onOTA(ota);
            return this;
        }

        Tester otaStop() {
            OTAInfo ota = new OTAInfo(OTAInfo.OtaState.STOP, "device-id", OTA_TOTAL, OTA_TOTAL);
            otaWatcher.onOTA(ota);
            return this;
        }

        Tester waitForOTATimeout() throws InterruptedException {

            otaWatcher.getProgressObservable()
                .subscribeOn(Schedulers.computation())
                .timeout(OTA_TIMEOUT_SECONDS + 1, TimeUnit.SECONDS)
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onCompleted() {
                        otaCompleted = true;
                        synchronized (otaTimeoutLock) {
                            otaTimeoutLock.notifyAll();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        otaError = e;
                        synchronized (otaTimeoutLock) {
                            otaTimeoutLock.notifyAll();
                        }
                    }

                    @Override
                    public void onNext(Integer integer) {

                    }
                });

            synchronized (otaTimeoutLock) {
                otaTimeoutLock.wait(TimeUnit.SECONDS.toMillis(OTA_TIMEOUT_SECONDS + 2));
            }

            return this;
        }

        Tester verifyOTAInProgress(boolean expected) {
            assertEquals(expected, otaProgress != null);
            return this;
        }

        Tester verifyOTAProgress(int expected) {
            assertEquals(expected, (int)otaProgress);
            return this;
        }

        Tester verifyOTACompleted(boolean expected) {
            assertEquals(expected, otaCompleted);
            return this;
        }

        Tester verifyOTATimeout() {
            assertTrue(otaCompleted);
            assertNull(otaError);
            return this;
        }
    }
}