package io.afero.aferolab.deviceTag;

import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceTagCollection;
import io.afero.sdk.utils.RxUtils;
import rx.Observable;
import rx.functions.Func1;

class DeviceTagController {


    private final DeviceTagsView mView;
    private final DeviceModel mDeviceModel;

    DeviceTagController(DeviceTagsView view, DeviceModel deviceModel) {
        mView = view;
        mDeviceModel = deviceModel;
    }

    void addTag() {
        mView.openTagEditor(null)
                .flatMap(new Func1<DeviceTagEditor.Result, Observable<DeviceTagCollection.Tag>>() {
                    @Override
                    public Observable<DeviceTagCollection.Tag> call(DeviceTagEditor.Result result) {
                        return mDeviceModel.putTag(result.key, result.value);
                    }
                })
                .subscribe(new RxUtils.IgnoreResponseObserver<DeviceTagCollection.Tag>());
    }

    void editTag(DeviceTagCollection.Tag tag) {
        mView.openTagEditor(tag)
                .flatMap(new Func1<DeviceTagEditor.Result, Observable<DeviceTagCollection.Tag>>() {
                    @Override
                    public Observable<DeviceTagCollection.Tag> call(DeviceTagEditor.Result result) {
                        return mDeviceModel.putTag(result.key, result.value);
                    }
                })
                .subscribe(new RxUtils.IgnoreResponseObserver<DeviceTagCollection.Tag>());
    }
}
