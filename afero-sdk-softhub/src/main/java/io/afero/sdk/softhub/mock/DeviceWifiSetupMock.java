/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.softhub.mock;

import java.util.ArrayList;
import java.util.Arrays;

import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.softhub.DeviceWifiSetup;
import io.kiban.hubby.SetupWifiCallback;
import io.kiban.hubby.WifiSSIDEntry;
import rx.Observable;
import rx.Subscriber;

public class DeviceWifiSetupMock extends DeviceWifiSetup {

    public static final String HUB_ID = "hub-id";

    SetupWifiSubscriberCallback mCallback;
    boolean mInUnitTest = false;

    public DeviceWifiSetupMock(DeviceModel deviceModel, AferoClient aferoClient, boolean inUnitTest) {
        super(deviceModel, aferoClient);
        mInUnitTest = inUnitTest;
    }

    public DeviceWifiSetupMock(DeviceModel deviceModel, AferoClient aferoClient) {
        super(deviceModel, aferoClient);
    }

    private static WifiState mFinalWifiState = WifiState.PENDING;

    protected Observable.OnSubscribe<SetupWifiCallback.SetupWifiState> getSendWifiCredentialOnSubscribe(final String ssid, final String password) {
        return new Observable.OnSubscribe<SetupWifiCallback.SetupWifiState>() {
            @Override
            public void call(final Subscriber<? super SetupWifiCallback.SetupWifiState> subscriber) {
                mCallback = new SendWifiCredentialCallback(subscriber, mDeviceModel, mAferoClient);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.setupState(HUB_ID, SetupWifiCallback.SetupWifiState.START.getValue());
                        mSetupStateSubject.onNext(WifiState.PENDING);
                        mCallback.setupState(HUB_ID, SetupWifiCallback.SetupWifiState.AVAILABLE.getValue());
                        mCallback.setupState(HUB_ID, SetupWifiCallback.SetupWifiState.CONNECTED.getValue());
                        mCallback.setupState(HUB_ID, SetupWifiCallback.SetupWifiState.DONE.getValue());

                        WifiState newState = WifiState.CONNECTED;

                        if (!mInUnitTest) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            newState = WifiState.fromInt(mFinalWifiState.ordinal() + 1);
                            if (newState == null) {
                                newState = WifiState.ASSOCIATION_FAILED;
                            }
                            mFinalWifiState = newState;
                        }

                        mSetupStateSubject.onNext(newState);

                    }
                }).start();
            }
        };
    }

    protected Observable.OnSubscribe<WifiSSIDEntry> getGetWifiListOnSubscribe() {
        return new Observable.OnSubscribe<WifiSSIDEntry>() {
            @Override
            public void call(final Subscriber<? super WifiSSIDEntry> subscriber) {
                mCallback = new GetWifiListCallback(subscriber, mDeviceModel, mAferoClient);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.setupState(HUB_ID, SetupWifiCallback.SetupWifiState.START.getValue());
                        mCallback.setupState(HUB_ID, SetupWifiCallback.SetupWifiState.AVAILABLE.getValue());
                        mCallback.setupState(HUB_ID, SetupWifiCallback.SetupWifiState.CONNECTED.getValue());

                        WifiSSIDEntry[] entries = {
                                new WifiSSIDEntry("One", 55, true, false),
                                new WifiSSIDEntry("Two", 55, true, false),
                                new WifiSSIDEntry("Three", 55, true, false),
                                new WifiSSIDEntry("Four", 55, true, false),
                                new WifiSSIDEntry("Five", 55, true, false)
                        };
                        ArrayList<WifiSSIDEntry> list = new ArrayList<>(Arrays.asList(entries));

                        if (!mInUnitTest) {
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        mCallback.wifiListResult(HUB_ID, list);

                        mCallback.setupState(HUB_ID, SetupWifiCallback.SetupWifiState.DONE.getValue());

                    }
                }).start();
            }
        };
    }

}
