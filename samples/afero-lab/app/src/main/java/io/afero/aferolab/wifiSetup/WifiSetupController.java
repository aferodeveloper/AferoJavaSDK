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
import rx.Observable;
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
                            public void onCompleted() {
                            }

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

        mView.showWifiScanError();
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

    void onClickWifiScanTryAgain() {
        onClickRefresh();
    }

    void onNetworkListItemClick(int position) {
        WifiSSIDEntry ssidEntry = mWifiNetworkListAdapter.getItem(position);
        mPickedSSIDEntry = ssidEntry;

        if (ssidEntry != null) {
            if (ssidEntry.isSecure()) {
                askUserForWifiPassword(ssidEntry);
            } else {
                sendWifiCredential(ssidEntry.getSSID(), "");
            }
        }
    }

    void onClickWifiConnectTryAgain() {
        if (mWifiState != null) {
            switch (mWifiState) {
                case SSID_NOT_FOUND:
                case UNKNOWN_FAILURE:
                case ASSOCIATION_FAILED:
                case ECHO_FAILED:
                    mView.showListView();
                    break;

                case HANDSHAKE_FAILED:
                    mView.askUserForWifiPassword();
                    break;

                default:
                    mView.onCompleted();
                    break;
            }
        } else {
            mView.showListView();
        }
    }

    private void askUserForWifiPassword(final WifiSSIDEntry ssidEntry) {
        mView.askUserForWifiPassword()
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(String password) {
                        sendWifiCredential(ssidEntry.getSSID(), password);
                    }
                });
    }

    private void sendWifiCredential(String ssid, String password) {
        mView.showWifiConnectProgress();
        mWifiSetup.sendWifiCredential(ssid, password)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<SetupWifiCallback.SetupWifiState>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(SetupWifiCallback.SetupWifiState state) {
                        onNextSendWifiCredentialState(state);
                    }
                });
    }

    void onClickCancel() {
        mView.onCompleted();
    }

    private void onNextSendWifiCredentialState(SetupWifiCallback.SetupWifiState state) {
        switch (state) {
            case START:
                mView.showWifiConnectProgress();
                startListeningToWifiSetupState();
                break;

            case DONE:
                break;

            case CANCELLED:
            case TIMED_OUT:
            case FAILED:
                mView.showSendWifiCredsError();
                break;
        }
    }

    private void startListeningToWifiSetupState() {
        mWifiSetupStateSubscription = mWifiSetup.observeSetupState()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<DeviceWifiSetup.WifiState>() {
                    @Override
                    public void onCompleted() {}

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(DeviceWifiSetup.WifiState wifiState) {
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
        mView.showSendWifiCredsError(failMessageResId);
    }
}
