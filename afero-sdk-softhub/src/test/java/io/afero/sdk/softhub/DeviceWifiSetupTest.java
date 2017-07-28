/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.softhub;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;

import io.afero.sdk.client.mock.MockAferoClient;
import io.afero.sdk.client.mock.MockDeviceEventSource;
import io.afero.sdk.device.DeviceCollection;
import io.afero.sdk.device.DeviceModel;
import io.kiban.hubby.SetupWifiCallback;
import io.kiban.hubby.WifiSSIDEntry;
import rx.Observer;
import rx.functions.Action1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class DeviceWifiSetupTest {

    @Test
    public void testSendWifiCredential() throws Exception {
        createTester()
                .sendWifiCredential("ssid", "password")
                .verifySetupWifiState(SetupWifiCallback.SetupWifiState.DONE)
                .verifySendWifiCredsNoError()
                .verifySendWifiCredsComplete()
                ;
    }

    @Test
    public void testGetWifiSSIDList() throws Exception {
        createTester()
                .getWifiSSIDList()
                .verifyGetWifiSSIDListSize(5)
                .verifyGetWifiSSIDListNoError()
                .verifyGetWifiSSIDListComplete()
                ;
    }

    private static DeviceWifiSetupTester createTester() {
        return new DeviceWifiSetupTester();
    }

    private static class DeviceWifiSetupTester {

        final DeviceWifiSetup wifiSetup;
        SendWifiCredsObserver sendWifiCredsObserver = new SendWifiCredsObserver();
        GetWifiSSIDListObserver wifiSSIDListObserver = new GetWifiSSIDListObserver();

        DeviceWifiSetupTester() {
            wifiSetup = new DeviceWifiSetup(new MockWifiSetup(), createDeviceModel(), new MockAferoClient());
        }

        DeviceWifiSetupTester sendWifiCredential(String ssid, String password) {

            wifiSetup.sendWifiCredential("ssid", "password")
                    .toBlocking()
                    .subscribe(sendWifiCredsObserver);

            return this;
        }

        DeviceWifiSetupTester getWifiSSIDList() {

            wifiSetup.getWifiSSIDList()
                    .toBlocking()
                    .subscribe(wifiSSIDListObserver);

            return this;
        }

        DeviceWifiSetupTester verifySetupWifiState(SetupWifiCallback.SetupWifiState state) {
            assertEquals(state, sendWifiCredsObserver.wifiState);
            return this;
        }

        DeviceWifiSetupTester verifySendWifiCredsNoError() {
            assertNull(sendWifiCredsObserver.error);
            return this;
        }

        DeviceWifiSetupTester verifySendWifiCredsComplete() {
            assertTrue(sendWifiCredsObserver.onCompleteCalled);
            return this;
        }

        DeviceWifiSetupTester verifyGetWifiSSIDListSize(int size) {
            assertEquals(size, wifiSSIDListObserver.ssidList.size());
            return this;
        }

        DeviceWifiSetupTester verifyGetWifiSSIDListNoError() {
            assertNull(wifiSSIDListObserver.error);
            return this;
        }

        DeviceWifiSetupTester verifyGetWifiSSIDListComplete() {
            assertTrue(wifiSSIDListObserver.onCompleteCalled);
            return this;
        }
    }

    private static class MockWifiSetup implements DeviceWifiSetup.WifiSetupImpl {
        @Override
        public void localViewingStateChange(String deviceId, boolean state) {

        }

        @Override
        public void getWifiSSIDListFromHub(final String deviceId, final DeviceWifiSetup.GetWifiListCallback cb) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    cb.setupState(deviceId, SetupWifiCallback.SetupWifiState.START.getValue());
                    cb.setupState(deviceId, SetupWifiCallback.SetupWifiState.AVAILABLE.getValue());
                    cb.setupState(deviceId, SetupWifiCallback.SetupWifiState.CONNECTED.getValue());

                    WifiSSIDEntry[] entries = {
                            new WifiSSIDEntry("One", 55, true, false),
                            new WifiSSIDEntry("Two", 55, true, false),
                            new WifiSSIDEntry("Three", 55, true, false),
                            new WifiSSIDEntry("Four", 55, true, false),
                            new WifiSSIDEntry("Five", 55, true, false)
                    };
                    ArrayList<WifiSSIDEntry> list = new ArrayList<>(Arrays.asList(entries));

                    cb.wifiListResult(deviceId, list);

                    cb.setupState(deviceId, SetupWifiCallback.SetupWifiState.DONE.getValue());

                }
            }).start();

        }

        @Override
        public void cancelGetWifiSSIDListFromHub(String deviceId) {

        }

        @Override
        public void sendWifiCredentialToHub(final String deviceId, String ssid, String password, final SetupWifiCallback cb) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    cb.setupState(deviceId, SetupWifiCallback.SetupWifiState.START.getValue());
                    cb.setupState(deviceId, SetupWifiCallback.SetupWifiState.AVAILABLE.getValue());
                    cb.setupState(deviceId, SetupWifiCallback.SetupWifiState.CONNECTED.getValue());
                    cb.setupState(deviceId, SetupWifiCallback.SetupWifiState.DONE.getValue());
                }
            }).start();
        }

        @Override
        public void cancelSendWifiCredentialToHub(String deviceId) {

        }
    }

    private static class SendWifiCredsObserver implements Observer<SetupWifiCallback.SetupWifiState> {

        SetupWifiCallback.SetupWifiState wifiState;
        boolean onCompleteCalled;
        Throwable error;

        @Override
        public void onCompleted() {
            onCompleteCalled = true;
        }

        @Override
        public void onError(Throwable e) {
            error = e;
        }

        @Override
        public void onNext(SetupWifiCallback.SetupWifiState setupWifiState) {
            wifiState = setupWifiState;
        }
    }

    private static class GetWifiSSIDListObserver implements Observer<WifiSSIDEntry> {

        final ArrayList<WifiSSIDEntry> ssidList = new ArrayList<>();
        boolean onCompleteCalled;
        Throwable error;

        @Override
        public void onCompleted() {
            onCompleteCalled = true;
        }

        @Override
        public void onError(Throwable e) {
            error = e;
        }

        @Override
        public void onNext(WifiSSIDEntry we) {
            ssidList.add(we);
        }
    }

    private static DeviceCollection makeDeviceCollection() {
        return new DeviceCollection(new MockDeviceEventSource(), new MockAferoClient());
    }

    public static DeviceModel createDeviceModel() {
        DeviceCollection deviceCollection = makeDeviceCollection();
        deviceCollection.start().toBlocking().subscribe();
        final DeviceModel[] deviceModelResult = new DeviceModel[1];
        deviceCollection.addDevice("wifiSetupDevice", false)
                .toBlocking()
                .subscribe(new Action1<DeviceModel>() {
                    @Override
                    public void call(DeviceModel deviceModel) {
                        deviceModelResult[0] = deviceModel;
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });

        return deviceModelResult[0];
    }

}

