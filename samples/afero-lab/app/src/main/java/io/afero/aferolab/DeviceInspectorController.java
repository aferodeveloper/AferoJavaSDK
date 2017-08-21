package io.afero.aferolab;

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
    }

    void stop() {
        mDeviceUpdateSubscription = RxUtils.safeUnSubscribe(mDeviceUpdateSubscription);
    }

    private void onDeviceUpdate(DeviceModel deviceModel) {

    }
}
