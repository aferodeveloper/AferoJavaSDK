/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.softhub;

import java.util.List;

import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.ActionResponse;
import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.client.afero.models.PostActionBody;
import io.afero.sdk.device.ControlModel;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceProfile;
import io.afero.sdk.log.AfLog;
import io.afero.sdk.utils.RxUtils;
import io.kiban.hubby.Hubby;
import io.kiban.hubby.SetupWifiCallback;
import io.kiban.hubby.SetupWifiCallback.SetupWifiState;
import io.kiban.hubby.WifiSSIDEntry;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.subjects.BehaviorSubject;

public class DeviceWifiSetup {

    protected final DeviceModel mDeviceModel;
    protected final AferoClient mAferoClient;

    /*
        Wi-Fi state:

        0 = Not Connected
        1 = Pending
        2 = Connected
        3 = Unknown Failure
        4 = Association Failed
        5 = Handshake Failed
        6 = Echo Failed
        7 = SSID Not Found

        See http://wiki.afero.io/display/FIR/Device+Attribute+Registry
    */

    public static final int ATTRIBUTE_WIFI_STEADY_STATE = 65006;
    public static final int ATTRIBUTE_WIFI_CONNECTED_SSID = 65004;
    public static final int ATTRIBUTE_WIFI_RSSI = 65005;

    public enum WifiState {
        NOT_CONNECTED,
        PENDING,
        CONNECTED,
        UNKNOWN_FAILURE,
        ASSOCIATION_FAILED,
        HANDSHAKE_FAILED,
        ECHO_FAILED,
        SSID_NOT_FOUND,
        ;

        public static WifiState fromInt(int i) {
            for (WifiState ws : values()) {
                if (ws.ordinal() == i) {
                    return ws;
                }
            }
            return null;
        }
    }

    private WifiState mSetupState = WifiState.NOT_CONNECTED;
    private WifiState mSteadyState = WifiState.NOT_CONNECTED;

    protected BehaviorSubject<WifiState> mSetupStateSubject = BehaviorSubject.create();
    protected BehaviorSubject<WifiState> mSteadyStateSubject = BehaviorSubject.create();

    private DeviceProfile.Attribute mSetupStateAttribute;
    private DeviceProfile.Attribute mSteadyStateAttribute;
    private Subscription mDeviceUpdateSubscription;

    public DeviceWifiSetup(DeviceModel deviceModel, AferoClient aferoClient) {
        mDeviceModel = deviceModel;
        mAferoClient = aferoClient;

        mSetupStateAttribute = deviceModel.getAttributeById(Hubby.WIFI_SETUP_STATE_ATTRIBUTE);
        mSteadyStateAttribute = deviceModel.getAttributeById(ATTRIBUTE_WIFI_STEADY_STATE);

        WifiState ws = getStateFromAttribute(mSetupStateAttribute);
        if (ws != null) {
            mSetupState = ws;
        }

        ws = getStateFromAttribute(mSteadyStateAttribute);
        if (ws != null) {
            mSteadyState = ws;
        }
    }

    public void start() {
        mDeviceUpdateSubscription = mDeviceModel.getUpdateObservable()
            .subscribe(new Action1<ControlModel>() {
                @Override
                public void call(ControlModel controlModel) {
                    updateSetupState();
                    updateSteadyState();
                }
            });

        Hubby.localViewingStateChange(mDeviceModel.getId(), true);
    }

    public void stop() {
        Hubby.localViewingStateChange(mDeviceModel.getId(), false);
        mDeviceUpdateSubscription = RxUtils.safeUnSubscribe(mDeviceUpdateSubscription);
    }

    public WifiState getSetupState() {
        return mSetupState;
    }

    public WifiState getSteadyState() {
        return mSteadyState;
    }

    public Observable<WifiState> observeSetupState() {
        return mSetupStateSubject;
    }

    public Observable<WifiState> observeSteadyState() {
        return mSteadyStateSubject;
    }

    public Observable<WifiSSIDEntry> getWifiSSIDList() {
        return Observable.create(getGetWifiListOnSubscribe());
    }

    public Observable<SetupWifiState> sendWifiCredential(String ssid, String password) {
        return Observable.create(getSendWifiCredentialOnSubscribe(ssid, password));
    }

    public static String getWifiSSID(DeviceModel deviceModel) {
        boolean isWifiCapable = isWifiSetupCapable(deviceModel);
        if (!isWifiCapable) {
            return null;
        }

        DeviceProfile.Attribute attribute = deviceModel.getAttributeById(DeviceWifiSetup.ATTRIBUTE_WIFI_CONNECTED_SSID);
        if (attribute == null) {
            return null;
        }

        AttributeValue attributeValue = deviceModel.getAttributeCurrentValue(attribute);
        if (attributeValue == null) {
            return null;
        }

        String wifiNetwork = attributeValue.toString().trim();
        if (wifiNetwork.isEmpty()) {
            return null;
        }

        return wifiNetwork;
    }

    public static boolean isWifiSetup(DeviceModel deviceModel) {
        return getWifiSSID(deviceModel) != null;
    }

    public static boolean isWifiSetupCapable(DeviceModel deviceModel) {
        return deviceModel.getAttributeById(Hubby.WIFI_SETUP_STATE_ATTRIBUTE) != null;
    }

    private WifiState getStateFromAttribute(DeviceProfile.Attribute attr) {
        AttributeValue av = mDeviceModel.getAttributeCurrentValue(mSetupStateAttribute);
        if (av != null) {
            int val = av.numericValue().intValue();

            return WifiState.fromInt(val);
        }

        return null;
    }

    private void updateSetupState() {
        WifiState ws = getStateFromAttribute(mSetupStateAttribute);
        if (ws != null && !ws.equals(mSetupState)) {
            mSetupState = ws;
            mSetupStateSubject.onNext(ws);
        }
    }

    private void updateSteadyState() {
        WifiState ws = getStateFromAttribute(mSteadyStateAttribute);
        if (ws != null && !ws.equals(mSteadyState)) {
            mSteadyState = ws;
            mSteadyStateSubject.onNext(ws);
        }
    }

    protected Observable.OnSubscribe<SetupWifiState> getSendWifiCredentialOnSubscribe(final String ssid, final String password) {
        return new Observable.OnSubscribe<SetupWifiState>() {
            @Override
            public void call(final Subscriber<? super SetupWifiState> subscriber) {
                SendWifiCredentialCallback cb = new SendWifiCredentialCallback(subscriber, mDeviceModel, mAferoClient);
                subscriber.add(getSendWifiCredentialSubscription(cb));
                Hubby.sendWifiCredentialToHub(mDeviceModel.getId(), ssid, password, cb);
            }
        };
    }

    protected Subscription getSendWifiCredentialSubscription(SetupWifiSubscriberCallback cb) {
        return new CancelSendWifiCredentialSubscription(cb, mDeviceModel.getId());
    }

    private static final class CancelSendWifiCredentialSubscription implements Subscription {

        private SetupWifiSubscriberCallback mCallback;
        private String mHubId;

        public CancelSendWifiCredentialSubscription(SetupWifiSubscriberCallback cb, String hubId) {
            mCallback = cb;
            mHubId = hubId;
        }

        @Override
        public void unsubscribe() {
            if (mCallback.isCancelable()) {
                Hubby.cancelSendWifiCredentialToHub(mHubId);
            }
        }

        @Override
        public boolean isUnsubscribed() {
            return false;
        }
    }

    protected Observable.OnSubscribe<WifiSSIDEntry> getGetWifiListOnSubscribe() {
        return new Observable.OnSubscribe<WifiSSIDEntry>() {
            @Override
            public void call(final Subscriber<? super WifiSSIDEntry> subscriber) {
                GetWifiListCallback cb = new GetWifiListCallback(subscriber, mDeviceModel, mAferoClient);
                subscriber.add(getWifiSSIDListSubscription(cb));
                Hubby.getWifiSSIDListFromHub(mDeviceModel.getId(), cb);
            }
        };
    }

    protected Subscription getWifiSSIDListSubscription(SetupWifiSubscriberCallback cb) {
        return new CancelGetWifiListSubscription(cb, mDeviceModel.getId());
    }

    private static final class CancelGetWifiListSubscription implements Subscription {

        private SetupWifiSubscriberCallback mCallback;
        private String mHubId;

        public CancelGetWifiListSubscription(SetupWifiSubscriberCallback cb, String hubId) {
            mCallback = cb;
            mHubId = hubId;
        }

        @Override
        public void unsubscribe() {
            if (mCallback.isCancelable()) {
                Hubby.cancelGetWifiSSIDListFromHub(mHubId);
            }
        }

        @Override
        public boolean isUnsubscribed() {
            return false;
        }
    }

    protected static final class GetWifiListCallback extends SetupWifiSubscriberCallback {

        private Subscriber<? super WifiSSIDEntry> mSubscriber;

        public GetWifiListCallback(Subscriber<? super WifiSSIDEntry> subscriber, DeviceModel deviceModel, AferoClient aferoClient) {
            super(subscriber, deviceModel, aferoClient);
            mSubscriber = subscriber;
        }

            @Override
        public void wifiListResult(String s, List<WifiSSIDEntry> list) {
            for (WifiSSIDEntry we : list) {
                mSubscriber.onNext(we);
            }
            mSubscriber.onCompleted();
        }
    }

    protected static final class SendWifiCredentialCallback extends SetupWifiSubscriberCallback {

        private Subscriber<? super SetupWifiState> mSubscriber;

        public SendWifiCredentialCallback(Subscriber<? super SetupWifiState> subscriber, DeviceModel deviceModel, AferoClient aferoClient) {
            super(subscriber, deviceModel, aferoClient);
            mSubscriber = subscriber;
        }

        public void onNext(SetupWifiState state) {
            switch (state) {
                case START:
                case AVAILABLE:
                case CONNECTED:
                    mSubscriber.onNext(state);
                    break;

                case DONE:
                    mSubscriber.onNext(state);
                    break;
            }

            super.onNext(state);
        }
    }

    protected static class SetupWifiSubscriberCallback implements SetupWifiCallback {

        private final AferoClient mAferoClient;
        private final DeviceModel mDeviceModel;
        private Subscriber<?> mSubscriber;
        private SetupWifiState mCurrentState;

        public SetupWifiSubscriberCallback(Subscriber<?> subscriber, DeviceModel deviceModel, AferoClient aferoClient) {
            mSubscriber = subscriber;
            mDeviceModel = deviceModel;
            mAferoClient = aferoClient;
        }

        public void onNext(SetupWifiState state) {
            mCurrentState = state;

            switch (state) {
                case START:
                case AVAILABLE:
                case CONNECTED:
                    break;

                case DONE:
                    mSubscriber.onCompleted();
                    break;

                case CANCELLED:
                case TIMED_OUT:
                case TIMED_OUT_NOT_AVAILABLE:
                case TIMED_OUT_CONNECT:
                case TIMED_OUT_COMMUNICATING:
                case FAILED:
                    mSubscriber.onError(new Error("setupWifiState=" + state));
                    break;
            }
        }

        public boolean isCancelable() {
            if (mCurrentState != null) {
                switch (mCurrentState) {
                    case START:
                    case AVAILABLE:
                    case CONNECTED:
                        return true;

                    case DONE:
                    case CANCELLED:
                    case TIMED_OUT:
                    case TIMED_OUT_NOT_AVAILABLE:
                    case TIMED_OUT_CONNECT:
                    case TIMED_OUT_COMMUNICATING:
                    case FAILED:
                        return false;
                }
            }

            return false;
        }

        @Override
        public void setupState(String hubId, int stateValue) {

            if (mSubscriber.isUnsubscribed()) {
                return;
            }

            SetupWifiState state = fromSetupWifiStateValue(stateValue);
            if (state != null) {
                onNext(state);
            } else {
                mSubscriber.onError(new Error("setupWifiState is NULL"));
            }
        }

        @Override
        public boolean writeAttribute(final String deviceId, int attributeId, final String type, final String hexData) {

            if (mSubscriber.isUnsubscribed()) {
                return false;
            }

            PostActionBody body = new PostActionBody();
            body.type = type;
            body.attrId = attributeId;
            body.data = hexData;

            mAferoClient.postAttributeWrite(mDeviceModel, body, 0, 0)
                .subscribe(new Observer<ActionResponse>() {
                    @Override
                    public void onCompleted() {}

                    @Override
                    public void onError(Throwable e) {
                        AfLog.e(e);
                        if (!mSubscriber.isUnsubscribed()) {
                            mSubscriber.onError(new Throwable(e));
                        }
                    }

                    @Override
                    public void onNext(ActionResponse actionResponse) {}
                });

            return true;
        }

        @Override
        public void wifiListResult(String s, List<WifiSSIDEntry> list) {
        }
    }

    public static SetupWifiState fromSetupWifiStateValue(int value) {
        for (SetupWifiState s : SetupWifiState.values()) {
            if (s.getValue() == value) {
                return s;
            }
        }
        return null;
    }

    public DeviceModel getDeviceModel() {
        return mDeviceModel;
    }
}
