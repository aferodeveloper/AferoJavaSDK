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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;


public class ViewingDeviceNotifierTest {
    @Test
    public void start() throws Exception {
        makeTester()
            .viewNotifierStart()
            .waitForViewRequest()
            .verifyViewNotifierStarted()
        ;
    }

    @Test
    public void stop() throws Exception {
        makeTester()
            .viewNotifierStart()
            .waitForViewRequest()
            .viewNotifierStop()
            .verifyViewNotifierStopped()
        ;
    }


    private ViewNotifierTester makeTester() {
        return new ViewNotifierTester();
    }

    class ViewNotifierTester {
        final MockAferoClient aferoClient = new MockAferoClient();
        final DeviceModel deviceModel = new DeviceModel("device-id", new DeviceProfile(), false, null);
        final ViewingDeviceNotifier viewingDeviceNotifier = new ViewingDeviceNotifier(deviceModel, aferoClient, 2, 1);
        final Object viewRequestObject = new Object();
        final Subscription viewRequestSubscription = aferoClient.observeViewRequests()
            .subscribe(new Action1<ViewRequest>() {
                @Override
                public void call(ViewRequest viewRequest) {
                    synchronized (viewRequestObject) {
                        viewRequestObject.notifyAll();
                    }
                }
            });

        ViewNotifierTester viewNotifierStart() {
            viewingDeviceNotifier.start();
            return this;
        }

        ViewNotifierTester viewNotifierStop() {
            viewingDeviceNotifier.stop();
            return this;
        }

        ViewNotifierTester waitForViewRequest() {
            try {
                synchronized (viewRequestObject) {
                    viewRequestObject.wait(5000);
                }
            } catch (InterruptedException e) {
                assertTrue(false);
            }
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
    }

}