/*
 * Copyright (c) 2014-2018 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;

import org.junit.Test;

import io.afero.sdk.client.afero.models.ViewRequest;
import io.afero.sdk.client.mock.MockAferoClient;
import rx.Subscription;
import rx.functions.Action1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;


public class ViewingDeviceNotifierTest {
    @Test
    public void start() throws Exception {
        makeTester()
            .viewNotifierStart()
            .waitForViewRequest()
            .verifyViewRequestDidNotTimeout()
            .verifyViewNotifierStarted()
        ;
    }

    @Test
    public void stop() throws Exception {
        makeTester()
            .viewNotifierStart()

            .waitForViewRequestCount(3, 5000)
            .verifyViewRequestDidNotTimeout()
            .verifyViewRequestCountAtLeast(3)

            .viewNotifierStop()
            .verifyViewNotifierStopped()

            .waitForViewRequest(2000)
            .verifyViewRequestDidTimeout()
        ;
    }


    private ViewNotifierTester makeTester() {
        return new ViewNotifierTester();
    }

    private class ViewNotifierTester {
        private final MockAferoClient aferoClient = new MockAferoClient();
        private final DeviceModel deviceModel = new DeviceModel("device-id", new DeviceProfile(), false, null);
        private final ViewingDeviceNotifier viewingDeviceNotifier = new ViewingDeviceNotifier(deviceModel, aferoClient, 2, 1);
        private final Object viewRequestObject = new Object();
        private final Subscription viewRequestSubscription = aferoClient.observeViewRequests()
            .subscribe(new Action1<ViewRequest>() {
                @Override
                public void call(ViewRequest viewRequest) {
                    synchronized (viewRequestObject) {
                        viewRequestCount++;
                        System.out.println("viewRequestCount=" + viewRequestCount);
                        viewRequestTimedOut = false;
                        viewRequestObject.notifyAll();
                    }
                }
            });
        private int viewRequestCount;
        private boolean viewRequestTimedOut;

        ViewNotifierTester viewNotifierStart() {
            System.out.println("viewNotifierStart");
            viewingDeviceNotifier.start();
            return this;
        }

        ViewNotifierTester viewNotifierStop() {
            System.out.println("viewNotifierStop");
            viewingDeviceNotifier.stop();
            return this;
        }

        ViewNotifierTester waitForViewRequest() {
            return waitForViewRequest(5000);
        }

        ViewNotifierTester waitForViewRequest(long timeout) {
            try {
                synchronized (viewRequestObject) {
                    System.out.println("waiting for viewRequest...");
                    viewRequestTimedOut = true;
                    viewRequestObject.wait(timeout);
                }
            } catch (InterruptedException e) {
                // ignore
            }

            System.out.println("viewRequestTimedOut = " + viewRequestTimedOut);
            return this;
        }

        ViewNotifierTester waitForViewRequestCount(int expectedCount, long timeout) {
            try {
                while (viewRequestCount < expectedCount) {
                    synchronized (viewRequestObject) {
                        System.out.println("waiting for viewRequest...");
                        viewRequestObject.wait(timeout);
                    }
                    viewRequestTimedOut = false;
                    System.out.println("viewRequestTimedOut = false");
                }
            } catch (InterruptedException e) {
                System.out.println("viewRequestTimedOut = true");
                viewRequestTimedOut = true;
            }

            return this;
        }

        ViewNotifierTester verifyViewRequestDidTimeout() {
            assertTrue(viewRequestTimedOut);
            return this;
        }

        ViewNotifierTester verifyViewRequestDidNotTimeout() {
            assertFalse(viewRequestTimedOut);
            return this;
        }

        ViewNotifierTester verifyViewNotifierStarted() {
            assertEquals(deviceModel.getId(), aferoClient.getViewingDeviceId());
            assertNotEquals(0, aferoClient.getViewingDeviceSeconds());
            return this;
        }

        ViewNotifierTester verifyViewNotifierStopped() {
            assertEquals(deviceModel.getId(), aferoClient.getViewingDeviceId());
            assertEquals(0, aferoClient.getViewingDeviceSeconds());
            return this;
        }

        ViewNotifierTester verifyViewRequestCountAtLeast(int expectedCount) {
            assertTrue(viewRequestCount >= expectedCount);
            return this;
        }
    }

}