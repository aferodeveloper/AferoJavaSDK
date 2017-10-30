package io.afero.aferolab.wifiSetup;

import android.bluetooth.BluetoothAdapter;
import android.support.annotation.StringRes;

import java.util.concurrent.TimeUnit;

import io.afero.aferolab.R;
import io.afero.hubby.SetupWifiCallback;
import io.afero.hubby.WifiSSIDEntry;
import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.log.AfLog;
import io.afero.sdk.softhub.DeviceWifiSetup;
import io.afero.sdk.utils.RxUtils;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

class WifiSetupController {

    private static final int DEVICE_AVAILABILITY_TIMEOUT = 60;

    private final WifiSetupView mView;
    private final DeviceModel mDeviceModel;
    private final AferoClient mAferoClient;

    private DeviceWifiSetup mWifiSetup;
    private WifiSSIDListAdapter mWifiNetworkListAdapter;
    private Subscription mWifiScanSubscription;
    private WifiSSIDEntry mPickedSSIDEntry;

    private Subscription mDeviceUpdateSubscription;
    private DeviceWifiSetup.WifiState mWifiState;
    private Subscription mWifiSetupStateSubscription;

    WifiSetupController(WifiSetupView wifiSetupView, DeviceModel deviceModel, AferoClient aferoClient) {
        mView = wifiSetupView;
        mDeviceModel = deviceModel;
        mAferoClient = aferoClient;
    }

    void start() {
        mWifiSetup = new DeviceWifiSetup(mDeviceModel, mAferoClient);
        mWifiSetup.start();

        mWifiNetworkListAdapter = new WifiSSIDListAdapter(mView.getContext(), mWifiSetup);
        mView.setAdapter(mWifiNetworkListAdapter);

        startScan();
    }

    void stop() {
        mDeviceUpdateSubscription = RxUtils.safeUnSubscribe(mDeviceUpdateSubscription);
        mWifiSetup.stop();
        mWifiNetworkListAdapter.stop();
//        mView.stopWifiPassword();
        mView.stopWifiConnect();
        mView.stopBluetoothNeeded();
    }

    private void startScan() {

        mDeviceUpdateSubscription = RxUtils.safeUnSubscribe(mDeviceUpdateSubscription);

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null || !btAdapter.isEnabled()) {
            mView.askUserToTurnOnBluetooth(mDeviceModel);
            return;
        }

        if (!mDeviceModel.isAvailable()) {
            mView.showLookingProgress();
            if (mDeviceUpdateSubscription == null) {
                mDeviceUpdateSubscription = mDeviceModel.getUpdateObservable()
                        .timeout(DEVICE_AVAILABILITY_TIMEOUT, TimeUnit.SECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<DeviceModel>() {
                            @Override
                            public void onCompleted() {}

                            @Override
                            public void onError(Throwable e) {
                                mDeviceUpdateSubscription = null;
                                onWifiScanError(e);
                            }

                            @Override
                            public void onNext(DeviceModel deviceModel) {
                                AfLog.d("################# mDeviceModel.isAvailable()=" + mDeviceModel.isAvailable());
                                if (mDeviceModel.isAvailable()) {
                                    startScan();
                                }
                            }
                        });
            }
            return;
        }

        mView.showConnectProgress();

        if (mWifiScanSubscription != null) {
            return;
        }

        AfLog.d("################# mWifiNetworkListAdapter.start=" + mDeviceModel.isAvailable());

        mWifiScanSubscription = mWifiNetworkListAdapter.start()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<WifiSSIDListAdapter>() {
                    @Override
                    public void onCompleted() {
                        onWifiScanComplete();
                    }

                    @Override
                    public void onError(Throwable e) {
                        onWifiScanError(e);
                    }

                    @Override
                    public void onNext(WifiSSIDListAdapter wifiNetworkListAdapter) {
                    }
                });
    }

    private void onWifiScanError(Throwable e) {
        AfLog.d("WifiNetworkListPresenter.onWifiScanError");
        e.printStackTrace();
        mWifiScanSubscription = null;
        mView.hideProgress();

        mView.showErrorView();
    }

    private void onWifiScanComplete() {
        AfLog.d("WifiNetworkListPresenter.onWifiScanComplete");
        mWifiScanSubscription = null;
        showNetworkList();
        mView.hideProgress();
    }

    private void showNetworkList() {
        if (mWifiNetworkListAdapter.isEmpty()) {
            mView.showEmptyView();
        } else {
            mView.showListView();
        }
    }

    void onClickRefresh() {
        mWifiNetworkListAdapter.clear();
        startScan();
    }

    public void onClickManualSSID() {
        startWifiPassword(null);
    }

    public void onNetworkListItemClick(int position) {
        WifiSSIDEntry ssidEntry = mWifiNetworkListAdapter.getItem(position);
        mPickedSSIDEntry = ssidEntry;

        if (ssidEntry != null) {
            if (ssidEntry.isSecure()) {
                startWifiPassword(ssidEntry);
            } else {
                attemptWifiConnection(ssidEntry.getSSID(), "");
            }
        }
    }

    private void startWifiPassword(WifiSSIDEntry ssidEntry) {
//        mView.startWifiPassword(ssidEntry)
//                .subscribe(new Observer<WifiPasswordPresenter.WifiCredentials>() {
//                    @Override
//                    public void onCompleted() {
//                        mView.stopWifiPassword();
//                    }
//
//                    @Override
//                    public void onError(Throwable e) {
//
//                    }
//
//                    @Override
//                    public void onNext(WifiPasswordPresenter.WifiCredentials wc) {
//                        attemptWifiConnection(wc.ssid, wc.password);
//                    }
//                });
    }

    private void attemptWifiConnection(String ssid, String password) {
        mWifiSetup.sendWifiCredential(ssid, password);
//        mView.startWifiConnect(mWifiSetup, ssid, password)
//                .subscribe(new Observer<WifiConnectPresenter.Event>() {
//                    @Override
//                    public void onCompleted() {
//                        mView.stopWifiConnect();
//                    }
//
//                    @Override
//                    public void onError(Throwable e) {
//
//                    }
//
//                    @Override
//                    public void onNext(WifiConnectPresenter.Event event) {
//
//                        switch (event) {
//                            case SUCCESS:
//                                mView.stopWifiConnect();
//                                mEventSubject.onCompleted();
//                                break;
//
//                            case TRY_AGAIN:
//                                mView.stopWifiConnect();
//                                break;
//
//                            case TRY_AGAIN_PASSWORD:
//                                mView.stopWifiConnect();
//                                if (mPickedSSIDEntry != null) {
//                                    startWifiPassword(mPickedSSIDEntry);
//                                }
//                                break;
//                        }
//                    }
//                });
    }

    void onClickCancel() {
        mView.onCompleted();
    }

    private void onNextSetupWifiState(SetupWifiCallback.SetupWifiState setupWifiState) {
        switch (setupWifiState) {
            case START:
                mView.showConnecting();
                startListeningToWifiSetupState();
                break;

            case DONE:
                break;

            case CANCELLED:
            case TIMED_OUT:
            case FAILED:
                mView.showError();
                break;
        }
    }

    public void onClickNext() {
        mView.onSuccess();
    }

    public void onClickWifiConnectTryAgain() {
        if (mWifiState != null) {
            switch (mWifiState) {
                case SSID_NOT_FOUND:
                case UNKNOWN_FAILURE:
                case ASSOCIATION_FAILED:
                case ECHO_FAILED:
                    mView.onWifiConnectTryAgain();
                    break;

                case HANDSHAKE_FAILED:
                    mView.onWifiConnectTryAgainPassword();
                    break;

                default:
                    mView.onSuccess();
                    break;
            }
        } else {
//            startConnection(mWifiStateObservable);
        }
    }

    private void startListeningToWifiSetupState() {
        mWifiSetupStateSubscription = mWifiSetup.observeSetupState()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<DeviceWifiSetup.WifiState>() {
                    @Override
                    public void call(DeviceWifiSetup.WifiState wifiState) {
                        onWifiSetupStateChange(wifiState);
                    }
                });
    }

    private void stopListeningToWifiSetupState() {
        mWifiSetupStateSubscription = RxUtils.safeUnSubscribe(mWifiSetupStateSubscription);
    }

    private void onWifiSetupStateChange(DeviceWifiSetup.WifiState wifiState) {
        AfLog.d("onWifiSetupStateChange: wifiSetupState=" + wifiState.toString());

        switch (wifiState) {
            case NOT_CONNECTED:
                break;

            case PENDING:
                break;

            case UNKNOWN_FAILURE:
            case SSID_NOT_FOUND:
            case ASSOCIATION_FAILED:
                mWifiState = wifiState;
                onWifiSetupFailure(R.string.wifi_unable_to_associate);
                break;

            case HANDSHAKE_FAILED:
                mWifiState = wifiState;
                onWifiSetupFailure(R.string.wifi_unable_to_authenticate);
                break;

            case ECHO_FAILED:
                mWifiState = wifiState;
                onWifiSetupFailure(R.string.wifi_unable_to_reach_afero);
                break;

            case CONNECTED:
                stopListeningToWifiSetupState();
                mView.showSuccess();
                break;
        }
    }

    private void onWifiSetupFailure(@StringRes int failMessageResId) {
        stopListeningToWifiSetupState();
        mView.showError(failMessageResId);
    }
}
