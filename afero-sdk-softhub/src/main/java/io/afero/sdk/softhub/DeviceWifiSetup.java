/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.softhub;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.afero.hubby.Hubby;
import io.afero.hubby.SetupWifiCallback;
import io.afero.hubby.SetupWifiCallback.SetupWifiState;
import io.afero.hubby.WifiSSIDEntry;
import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.ActionResponse;
import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.client.afero.models.PostActionBody;
import io.afero.sdk.conclave.models.DeviceSync;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceProfile;
import io.afero.sdk.log.AfLog;
import io.afero.sdk.utils.RxUtils;
import rx.Emitter;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Cancellable;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

public class DeviceWifiSetup {

    private static final int TIMEOUT_DEFAULT = 60;

    protected final DeviceModel mDeviceModel;
    protected final AferoClient mAferoClient;
    private final WifiSetupImpl mWifiSetupImpl;

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

    public static final int ATTRIBUTE_WIFI_SETUP_STATE = Hubby.WIFI_SETUP_STATE_ATTRIBUTE;
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

    private PublishSubject<WifiState> mSetupStateSubject = PublishSubject.create();
    private PublishSubject<WifiState> mSteadyStateSubject = PublishSubject.create();

    private DeviceProfile.Attribute mSetupStateAttribute;
    private DeviceProfile.Attribute mSteadyStateAttribute;
    private Subscription mDeviceUpdateSubscription;

    public DeviceWifiSetup(DeviceModel deviceModel, AferoClient aferoClient) {
        this(new HubbyWifiSetupImpl(), deviceModel, aferoClient);
    }

    @SuppressWarnings("WeakerAccess") // used in unit tests
    DeviceWifiSetup(WifiSetupImpl impl, DeviceModel deviceModel, AferoClient aferoClient) {
        mDeviceModel = deviceModel;
        mAferoClient = aferoClient;
        mWifiSetupImpl = impl;

        mSetupStateAttribute = deviceModel.getAttributeById(ATTRIBUTE_WIFI_SETUP_STATE);
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
        mDeviceUpdateSubscription = mDeviceModel.getDeviceSyncPostUpdateObservable()
            .subscribe(new Action1<DeviceSync>() {
                @Override
                public void call(DeviceSync d) {
                    if (d.attribute != null) {
                        if (d.attribute.id == ATTRIBUTE_WIFI_SETUP_STATE) {
                            updateSetupState();
                        }
                        if (d.attribute.id == ATTRIBUTE_WIFI_STEADY_STATE) {
                            updateSteadyState();
                        }
                    }

                }
            });

        mWifiSetupImpl.localViewingStateChange(mDeviceModel.getId(), true);
    }

    public void stop() {
        mWifiSetupImpl.localViewingStateChange(mDeviceModel.getId(), false);
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

    @SuppressWarnings("WeakerAccess")
    public Observable<WifiSSIDEntry> getWifiSSIDList() {
        return Observable.create(new Action1<Emitter<List<WifiSSIDEntry>>>() {
                @Override
                public void call(Emitter<List<WifiSSIDEntry>> wifiSSIDEntryEmitter) {
                    final GetWifiListCallback wifiListCallback = new GetWifiListCallback(wifiSSIDEntryEmitter, mDeviceModel, mAferoClient);
                    mWifiSetupImpl.getWifiSSIDListFromHub(mDeviceModel.getId(), wifiListCallback);

                }
            }, Emitter.BackpressureMode.BUFFER)
            .flatMap(new Func1<List<WifiSSIDEntry>, Observable<WifiSSIDEntry>>() {
                @Override
                public Observable<WifiSSIDEntry> call(List<WifiSSIDEntry> wifiSSIDEntries) {
                    return Observable.from(wifiSSIDEntries);
                }
            })
            .timeout(TIMEOUT_DEFAULT, TimeUnit.SECONDS);
    }

    @SuppressWarnings("WeakerAccess")
    public Observable<SetupWifiState> sendWifiCredential(final String ssid, final String password) {
        return Observable.create(new Action1<Emitter<SetupWifiState>>() {
                @Override
                public void call(Emitter<SetupWifiState> wifiStateEmitter) {
                    SendWifiCredentialCallback cb = new SendWifiCredentialCallback(wifiStateEmitter, mDeviceModel, mAferoClient);
                    mWifiSetupImpl.sendWifiCredentialToHub(mDeviceModel.getId(), ssid, password, cb);
                }
            }, Emitter.BackpressureMode.BUFFER)
            .timeout(TIMEOUT_DEFAULT, TimeUnit.SECONDS);
    }

    @SuppressWarnings("WeakerAccess")
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

    @SuppressWarnings("WeakerAccess")
    public static boolean isWifiSetup(DeviceModel deviceModel) {
        return getWifiSSID(deviceModel) != null;
    }

    @SuppressWarnings("WeakerAccess")
    public static boolean isWifiSetupCapable(DeviceModel deviceModel) {
        return deviceModel.getAttributeById(ATTRIBUTE_WIFI_SETUP_STATE) != null;
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
        if (ws != null) {
            AfLog.d("DeviceWifiSetup: wifiSetupState=" + ws.toString());
            mSetupState = ws;
            mSetupStateSubject.onNext(ws);
        }
    }

    private void updateSteadyState() {
        WifiState ws = getStateFromAttribute(mSteadyStateAttribute);
        if (ws != null) {
            mSteadyState = ws;
            mSteadyStateSubject.onNext(ws);
        }
    }

    protected final class GetWifiListCallback extends SetupWifiSubscriberCallback<List<WifiSSIDEntry>> {

        private boolean mIsCancelable = true;

        public GetWifiListCallback(Emitter<List<WifiSSIDEntry>> wifiSSIDEntryEmitter, DeviceModel deviceModel, AferoClient aferoClient) {
            super(wifiSSIDEntryEmitter, deviceModel, aferoClient);

            wifiSSIDEntryEmitter.setCancellation(new Cancellable() {
                @Override
                public void cancel() throws Exception {
                    AfLog.d("GetWifiListCallback.cancel");
                    if (isCancelable()) {
                        AfLog.d("GetWifiListCallback.cancel calling cancelGetWifiSSIDListFromHub");
                        mWifiSetupImpl.cancelGetWifiSSIDListFromHub(getDeviceModel().getId());
                    }
                }
            });
        }

        @Override
        public void wifiListResult(String s, List<WifiSSIDEntry> list) {
            AfLog.d("GetWifiListCallback.wifiListResult: mIsCancelable=false");
            mIsCancelable = false;
            emit(list);
            completed();
        }

        @Override
        boolean isCancelable() {
            AfLog.d("GetWifiListCallback.isCancelable: mIsCancelable=" + mIsCancelable);
            return mIsCancelable && super.isCancelable();
        }
    }

    protected final class SendWifiCredentialCallback extends SetupWifiSubscriberCallback<SetupWifiState> {

        public SendWifiCredentialCallback(Emitter<SetupWifiState> wifiStateEmitter, DeviceModel deviceModel, AferoClient aferoClient) {
            super(wifiStateEmitter, deviceModel, aferoClient);

            wifiStateEmitter.setCancellation(new Cancellable() {
                @Override
                public void cancel() throws Exception {
                    if (isCancelable()) {
                        mWifiSetupImpl.cancelSendWifiCredentialToHub(getDeviceModel().getId());
                    }
                }
            });
        }

        public void onNext(SetupWifiState state) {
            switch (state) {
                case START:
                case AVAILABLE:
                case CONNECTED:
                case DONE:
                    emit(state);
                    break;
            }

            super.onNext(state);
        }
    }

    protected static class SetupWifiSubscriberCallback<T> implements SetupWifiCallback {

        private final AferoClient mAferoClient;
        private final Emitter<T> mEmitter;
        private final DeviceModel mDeviceModel;
        private SetupWifiState mCurrentState;

        SetupWifiSubscriberCallback(Emitter<T> emitter, DeviceModel deviceModel, AferoClient aferoClient) {
            mEmitter = emitter;
            mDeviceModel = deviceModel;
            mAferoClient = aferoClient;
        }

        public void onNext(SetupWifiState state) {
            mCurrentState = state;

            AfLog.d("SetupWifiSubscriberCallback: state=" + state.toString());

            switch (state) {
                case START:
                case AVAILABLE:
                case CONNECTED:
                    break;

                case DONE:
                    completed();
                    break;

                case CANCELLED:
                case TIMED_OUT:
                case TIMED_OUT_NOT_AVAILABLE:
                case TIMED_OUT_CONNECT:
                case TIMED_OUT_COMMUNICATING:
                case FAILED:
                    error(new Error("setupWifiState=" + state));
                    break;
            }
        }

        boolean isCancelable() {

            if (mCurrentState != null) {
                AfLog.d("SetupWifiSubscriberCallback.isCancelable: " + mCurrentState.toString());

                switch (mCurrentState) {
                    case START:
                    case AVAILABLE:
                    case CONNECTED:
                        AfLog.d("SetupWifiSubscriberCallback.isCancelable: true");
                        return true;

                    case DONE:
                    case CANCELLED:
                    case TIMED_OUT:
                    case TIMED_OUT_NOT_AVAILABLE:
                    case TIMED_OUT_CONNECT:
                    case TIMED_OUT_COMMUNICATING:
                    case FAILED:
                        AfLog.d("SetupWifiSubscriberCallback.isCancelable: false");
                        return false;
                }
            }

            AfLog.d("SetupWifiSubscriberCallback.isCancelable: false");

            return false;
        }

        @Override
        public void setupState(String hubId, int stateValue) {

            SetupWifiState state = fromSetupWifiStateValue(stateValue);
            if (state != null) {
                onNext(state);
            } else {
                error(new Error("setupWifiState is NULL"));
            }
        }

        @Override
        public boolean writeAttribute(final String deviceId, int attributeId, final String type, final String hexData) {

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
                        mEmitter.onError(new Throwable(e));
                    }

                    @Override
                    public void onNext(ActionResponse actionResponse) {}
                });

            return true;
        }

        @Override
        public void wifiListResult(String s, List<WifiSSIDEntry> list) {
        }

        protected DeviceModel getDeviceModel() {
            return mDeviceModel;
        }

        protected void emit(T t) {
            mEmitter.onNext(t);
        }

        protected void error(Throwable t) {
            mEmitter.onError(t);
        }

        protected void completed() {
            mEmitter.onCompleted();
        }
    }

    private static SetupWifiState fromSetupWifiStateValue(int value) {
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

    interface WifiSetupImpl {
        void localViewingStateChange(String deviceId, boolean state);

        void getWifiSSIDListFromHub(String deviceId, GetWifiListCallback cb);
        void cancelGetWifiSSIDListFromHub(String deviceId);

        void sendWifiCredentialToHub(String deviceId, String ssid, String password, SetupWifiCallback cb);
        void cancelSendWifiCredentialToHub(String deviceId);
    }

    private static class HubbyWifiSetupImpl implements WifiSetupImpl {
        @Override
        public void localViewingStateChange(String deviceId, boolean state) {
            Hubby.localViewingStateChange(deviceId, true);
        }

        @Override
        public void getWifiSSIDListFromHub(String deviceId, GetWifiListCallback cb) {
            Hubby.getWifiSSIDListFromHub(deviceId, cb);
        }

        @Override
        public void cancelGetWifiSSIDListFromHub(String deviceId) {
            Hubby.cancelGetWifiSSIDListFromHub(deviceId);
        }

        @Override
        public void sendWifiCredentialToHub(String deviceId, String ssid, String password, SetupWifiCallback cb) {
            Hubby.sendWifiCredentialToHub(deviceId, ssid, password, cb);
        }

        @Override
        public void cancelSendWifiCredentialToHub(String deviceId) {
            Hubby.cancelSendWifiCredentialToHub(deviceId);
        }
    }
}
