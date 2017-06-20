/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.hubby;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.mock.MockAferoClient;
import io.afero.sdk.client.mock.MockConclaveMessageSource;
import io.afero.sdk.conclave.ConclaveMessageSource;
import io.afero.sdk.device.DeviceCollection;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceProfileCollection;
import io.afero.sdk.hubby.mock.DeviceWifiSetupMock;
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

    protected final ObjectMapper mObjectMapper = new ObjectMapper();

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

    private static DeviceCollection makeDeviceCollection(ConclaveMessageSource source) {

        MockAferoClient aferoClient = new MockAferoClient();
        DeviceProfileCollection profileCollection = new DeviceProfileCollection(aferoClient, AferoClient.ImageSize.SIZE_3X, "mock-locale");

        return new DeviceCollection(source, profileCollection, aferoClient);
    }

    public static DeviceModel createDeviceModel() {
        MockConclaveMessageSource messageSource = new MockConclaveMessageSource();
        DeviceCollection deviceCollection = makeDeviceCollection(messageSource);
        final DeviceModel[] deviceModelResult = new DeviceModel[1];
        deviceCollection.addDevice("wifiSetupDevice", false)
                .toBlocking()
                .subscribe(new Action1<DeviceModel>() {
                    @Override
                    public void call(DeviceModel deviceModel) {
                        deviceModelResult[0] = deviceModel;
                    }
                });

        return deviceModelResult[0];
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

