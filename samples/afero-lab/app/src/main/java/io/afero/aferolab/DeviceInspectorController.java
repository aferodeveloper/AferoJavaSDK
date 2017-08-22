package io.afero.aferolab;

import android.view.View;

import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.utils.RxUtils;
import rx.Subscription;
import rx.functions.Action1;

class DeviceInspectorController {

    private final DeviceInspectorView mView;
    private Subscription mDeviceUpdateSubscription;

    DeviceInspectorController(DeviceInspectorView view) {
        mView = view;
    }

    void start(DeviceModel deviceModel) {
        mDeviceUpdateSubscription = deviceModel.getUpdateObservable()
                .subscribe(new Action1<DeviceModel>() {
                    @Override
                    public void call(DeviceModel deviceModel) {
                        onDeviceUpdate(deviceModel);
                    }
                });

        onDeviceUpdate(deviceModel);

        mView.setVisibility(View.VISIBLE);
    }

    void stop() {
        mDeviceUpdateSubscription = RxUtils.safeUnSubscribe(mDeviceUpdateSubscription);
        mView.setVisibility(View.INVISIBLE);
    }

    private void onDeviceUpdate(DeviceModel deviceModel) {
        mView.setDeviceNameText(deviceModel.getName());

        int statusResId = R.string.device_status_offline;
        if (deviceModel.isAvailable()) {
            if (deviceModel.isRunning()) {
                statusResId = R.string.device_status_active;
            } else {
                statusResId = R.string.device_status_idle;
            }
        }
        mView.setDeviceStatusText(statusResId);
    }
}
