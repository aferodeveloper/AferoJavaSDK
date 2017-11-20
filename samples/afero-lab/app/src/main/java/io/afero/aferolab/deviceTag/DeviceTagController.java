package io.afero.aferolab.deviceTag;

import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceTagCollection;
import io.afero.sdk.utils.RxUtils;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
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
                        return mDeviceModel.addTag(result.key, result.value);
                    }
                })
                .subscribe(new RxUtils.IgnoreResponseObserver<DeviceTagCollection.Tag>());
    }

    void editTag(final DeviceTagCollection.Tag tag) {
        mView.openTagEditor(tag)
                .flatMap(new Func1<DeviceTagEditor.Result, Observable<DeviceTagCollection.Tag>>() {
                    @Override
                    public Observable<DeviceTagCollection.Tag> call(DeviceTagEditor.Result result) {
                        return result.event == DeviceTagEditor.Result.Event.DELETE
                                ? mDeviceModel.removeTag(result.tag)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .doOnNext(new Action1<DeviceTagCollection.Tag>() {
                                        @Override
                                        public void call(DeviceTagCollection.Tag tag) {
                                            mView.removeTag(tag);
                                        }
                                    })
                                : mDeviceModel.updateTag(result.tag.getId(), result.key, result.value)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .doOnNext(new Action1<DeviceTagCollection.Tag>() {
                                        @Override
                                        public void call(DeviceTagCollection.Tag tag) {
                                            mView.updateTag(tag);
                                        }
                                    });
                    }
                })
                .subscribe(new RxUtils.IgnoreResponseObserver<DeviceTagCollection.Tag>());
    }
}
