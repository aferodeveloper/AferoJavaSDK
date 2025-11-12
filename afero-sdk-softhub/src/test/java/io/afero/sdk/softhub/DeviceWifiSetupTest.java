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

import io.afero.hubby.Hubby;
import io.afero.hubby.SetupWifiCallback;
import io.afero.hubby.WifiSSIDEntry;
import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.client.mock.MockAferoClient;
import io.afero.sdk.client.mock.MockDeviceEventSource;
import io.afero.sdk.conclave.models.DeviceSync;
import io.afero.sdk.device.DeviceCollection;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceProfile;
import rx.Observer;
import rx.functions.Action1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class DeviceWifiSetupTest {
    @Test
    public void todoDeviceWifiSetupTest() throws Exception {

    }
/*
// Not compatible to run on Apple Silicon at the moment
    @Test
    public void testSendWifiCredential() throws Exception {
        createTester()
                .deviceWifiSetupState(DeviceWifiSetup.WifiState.NOT_CONNECTED)

                .startWifiSetup()
                .sendWifiCredential("ssid", "password")

                .deviceWifiSetupState(DeviceWifiSetup.WifiState.CONNECTED)

                .verifySetupWifiState(SetupWifiCallback.SetupWifiState.DONE)
                .verifySendWifiCredsNoError()
                .verifySendWifiCredsComplete()
                ;
    }

    @Test
    public void testGetWifiSSIDList() throws Exception {
        createTester()
                .startWifiSetup()
                .getWifiSSIDList()
                .getWifiListCallbackSendList()

                .verifyGetWifiSSIDListSize(5)
                .verifyGetWifiSSIDListNoError()
                .verifyGetWifiSSIDListComplete()
                ;
    }

    //  See https://kibanlabs.atlassian.net/browse/ANDROID-1176
    @Test
    public void testSendWifiCredential_ANDROID_1176() throws Exception {
        createTester()
                .deviceWifiSetupState(DeviceWifiSetup.WifiState.CONNECTED)

                .startWifiSetup()
                .sendWifiCredential("ssid", "password")

                .deviceWifiSetupState(DeviceWifiSetup.WifiState.CONNECTED)

                .verifySetupWifiState(SetupWifiCallback.SetupWifiState.DONE)
                .verifyWifiSetupState(DeviceWifiSetup.WifiState.CONNECTED)
                .verifySendWifiCredsComplete()
        ;
    }

*/


    private static DeviceWifiSetupTester createTester() {
        return new DeviceWifiSetupTester();
    }

    private static class DeviceWifiSetupTester {

        final MockWifiSetupImpl wifiSetupImpl = new MockWifiSetupImpl();
        final MockAferoClient aferoClient = new MockAferoClient();
        final MockDeviceEventSource deviceEventSource = new MockDeviceEventSource();
        final DeviceCollection deviceCollection = new DeviceCollection(deviceEventSource, aferoClient);
        final DeviceWifiSetup wifiSetup;

        SendWifiCredsObserver sendWifiCredsObserver = new SendWifiCredsObserver();
        GetWifiSSIDListObserver wifiSSIDListObserver = new GetWifiSSIDListObserver();

        DeviceWifiSetupTester() {
            deviceCollection.start().toBlocking().subscribe();
            wifiSetup = new DeviceWifiSetup(wifiSetupImpl, createDeviceModel(), aferoClient);
        }

        DeviceWifiSetupTester startWifiSetup() {
            wifiSetup.start();
            return this;
        }

        DeviceWifiSetupTester sendWifiCredential(String ssid, String password) {
            wifiSetup.sendWifiCredential("ssid", "password")
                    .subscribe(sendWifiCredsObserver);

            wifiSetupCallback(SetupWifiCallback.SetupWifiState.START);
            wifiSetupCallback(SetupWifiCallback.SetupWifiState.AVAILABLE);
            wifiSetupCallback(SetupWifiCallback.SetupWifiState.CONNECTED);
            wifiSetupCallback(SetupWifiCallback.SetupWifiState.DONE);

            return this;
        }

        DeviceWifiSetupTester getWifiSSIDList() {
            wifiSetup.getWifiSSIDList()
                    .subscribe(wifiSSIDListObserver);

            return this;
        }

        DeviceWifiSetupTester wifiSetupCallback(SetupWifiCallback.SetupWifiState state) {
            wifiSetupImpl.hubbySetupCallback(state);
            return this;
        }

        DeviceWifiSetupTester getWifiListCallbackSendList() {
            wifiSetupImpl.hubbyWifiListCallbackSendList();
            return this;
        }

        DeviceWifiSetupTester deviceWifiSetupState(DeviceWifiSetup.WifiState wifiState) {
            DeviceModel deviceModel = wifiSetup.getDeviceModel();

            DeviceSync ds = new DeviceSync();
            ds.setDeviceId(deviceModel.getId());
            ds.attribute = new DeviceSync.AttributeEntry(Hubby.WIFI_SETUP_STATE_ATTRIBUTE, Integer.toString(wifiState.ordinal()));

            deviceEventSource.putAttributeChanges(ds);
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

        DeviceWifiSetupTester verifyWifiSetupState(DeviceWifiSetup.WifiState wifiState) {
            DeviceProfile.Attribute wifiSetupStateAttribute = wifiSetup.getDeviceModel().getAttributeById(Hubby.WIFI_SETUP_STATE_ATTRIBUTE);
            AttributeValue value = wifiSetup.getDeviceModel().getAttributeCurrentValue(wifiSetupStateAttribute);
            assertEquals(Integer.toString(wifiState.ordinal()), value.toString());
            return this;
        }

        private DeviceModel createDeviceModel() {
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

    private static class MockWifiSetupImpl implements DeviceWifiSetup.WifiSetupImpl {
        private String deviceId;
        private SetupWifiCallback setupWifiCallback;
        private DeviceWifiSetup.GetWifiListCallback getWifiListCallback;

        @Override
        public void localViewingStateChange(String deviceId, boolean state) {

        }

        @Override
        public void getWifiSSIDListFromHub(final String deviceId, final DeviceWifiSetup.GetWifiListCallback cb) {
            this.deviceId = deviceId;
            getWifiListCallback = cb;
        }

        @Override
        public void cancelGetWifiSSIDListFromHub(String deviceId) {

        }

        void hubbySetupCallback(SetupWifiCallback.SetupWifiState state) {
            setupWifiCallback.setupState(deviceId, state.getValue());
        }

        void hubbyWifiListCallbackSendList() {
            WifiSSIDEntry[] entries = {
                    new WifiSSIDEntry("One", 55, true, false),
                    new WifiSSIDEntry("Two", 55, true, false),
                    new WifiSSIDEntry("Three", 55, true, false),
                    new WifiSSIDEntry("Four", 55, true, false),
                    new WifiSSIDEntry("Five", 55, true, false)
            };

            getWifiListCallback.wifiListResult(deviceId, new ArrayList<>(Arrays.asList(entries)));
        }

        @Override
        public void sendWifiCredentialToHub(String deviceId, String ssid, String password, SetupWifiCallback cb) {
            this.deviceId = deviceId;
            setupWifiCallback = cb;
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

}

