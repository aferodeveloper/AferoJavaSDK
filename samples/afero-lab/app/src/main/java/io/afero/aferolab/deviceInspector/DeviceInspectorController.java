/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.deviceInspector;

import android.view.View;

import io.afero.aferolab.R;
import io.afero.aferolab.wifiSetup.WifiSetupView;
import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.device.DeviceCollection;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.softhub.DeviceWifiSetup;
import io.afero.sdk.utils.RxUtils;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

class DeviceInspectorController {

    private final DeviceInspectorView mView;
    private final DeviceCollection mDeviceCollection;
    private final AferoClient mAferoClient;
    private Subscription mDeviceUpdateSubscription;
    private DeviceModel mDeviceModel;

    DeviceInspectorController(DeviceInspectorView view, DeviceCollection deviceCollection, AferoClient aferoClient) {
        mView = view;
        mDeviceCollection = deviceCollection;
        mAferoClient = aferoClient;
    }

    void start(DeviceModel deviceModel) {

        mDeviceModel = deviceModel;
        mDeviceUpdateSubscription = deviceModel.getUpdateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<DeviceModel>() {
                    @Override
                    public void call(DeviceModel deviceModel) {
                        onDeviceUpdate(deviceModel);
                    }
                });

        onDeviceUpdate(deviceModel);

        mView.showWifiSetup(DeviceWifiSetup.isWifiSetupCapable(deviceModel));

        mView.setVisibility(View.VISIBLE);
    }

    void stop() {
        mDeviceUpdateSubscription = RxUtils.safeUnSubscribe(mDeviceUpdateSubscription);
    }

    boolean isStarted() {
        return mDeviceUpdateSubscription != null;
    }

    DeviceModel getDeviceModel() {
        return mDeviceModel;
    }

    void deleteDevice() {
        mView.showProgress();

        mDeviceCollection.removeDevice(mDeviceModel)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<DeviceModel>() {
                    @Override
                    public void onCompleted() {
                        mView.hideProgress();
                        mView.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        mView.hideProgress();
                    }

                    @Override
                    public void onNext(DeviceModel deviceModel) {
                    }
                });
    }

    /**
     * Updates the DeviceInspectorView when DeviceModel state changes
     *
     * @param deviceModel
     */
    private void onDeviceUpdate(DeviceModel deviceModel) {
        final boolean isAvailable = deviceModel.isAvailable();

        mView.setDeviceNameText(deviceModel.getName());

        int statusResId = R.string.device_status_offline;
        if (isAvailable) {
            statusResId = deviceModel.isRunning() ? R.string.device_status_active : R.string.device_status_idle;
        }
        mView.setDeviceStatusText(statusResId);

        mView.enableWifiSetup(isAvailable);
    }

    void onWifiConnect() {
        WifiSetupView.create(mView).start(mDeviceModel, mAferoClient);
    }
}
