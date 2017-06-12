/*
 * Copyright (c) 2014-2016 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.hubby;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.ArrayList;

import io.afero.sdk.AferoTest;
import io.afero.sdk.BuildConfig;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceModelTest;
import io.afero.sdk.hubby.mock.DeviceWifiSetupMock;
import io.kiban.hubby.SetupWifiCallback;
import io.kiban.hubby.WifiSSIDEntry;
import rx.Observer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class DeviceWifiSetupTest extends AferoTest {

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

    DeviceModel createDeviceModel() throws IOException {
        return DeviceModelTest.createDeviceModel(loadDeviceProfile("deviceModelTestProfile.json"), null);
    }

    @Before
    public void beforeTests() {
    }

    @Test
    public void testSendWifiCredential() throws Exception {
        DeviceModel deviceModel = createDeviceModel();
        DeviceWifiSetupMock wifiSetup = new DeviceWifiSetupMock(deviceModel, null, true);
        SendWifiCredsObserver observer = new SendWifiCredsObserver();

        wifiSetup.sendWifiCredential("ssid", "password")
            .toBlocking()
            .subscribe(observer);

        assertEquals(SetupWifiCallback.SetupWifiState.DONE, observer.wifiState);
        assertNull(observer.error);
        assertTrue(observer.onCompleteCalled);
    }

    @Test
    public void testGetWifiSSIDList() throws Exception {
        DeviceModel deviceModel = createDeviceModel();
        DeviceWifiSetupMock wifiSetup = new DeviceWifiSetupMock(deviceModel, null, true);
        GetWifiSSIDListObserver observer = new GetWifiSSIDListObserver();

        wifiSetup.getWifiSSIDList()
            .toBlocking()
            .subscribe(observer);

        assertEquals(5, observer.ssidList.size());
        assertNull(observer.error);
        assertTrue(observer.onCompleteCalled);
    }
}

