package io.afero.aferolab.deviceTag;

import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceTagCollection;
import rx.Observable;
import rx.Observer;
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
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<DeviceTagCollection.Tag>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        mView.showAddTagError(mDeviceModel.getAferoClient().getStatusCode(e));
                    }

                    @Override
                    public void onNext(DeviceTagCollection.Tag tag) {
                        mView.addTag(tag);
                    }
                });
    }

    void editTag(final DeviceTagCollection.Tag tag) {
        mView.openTagEditor(tag)
                .flatMap(new Func1<DeviceTagEditor.Result, Observable<DeviceTagCollection.Tag>>() {
                    @Override
                    public Observable<DeviceTagCollection.Tag> call(DeviceTagEditor.Result result) {
                        if (result.event == DeviceTagEditor.Result.Event.DELETE) {
                            mView.showRemoveTagConfirmation(tag);
                            return Observable.just(tag);
                        }

                        return mDeviceModel.updateTag(result.tag)
                                .observeOn(AndroidSchedulers.mainThread())
                                .doOnNext(new Action1<DeviceTagCollection.Tag>() {
                                    @Override
                                    public void call(DeviceTagCollection.Tag tag) {
                                        mView.updateTag(tag);
                                    }
                                });
                    }
                })
                .subscribe(new Observer<DeviceTagCollection.Tag>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        mView.showEditTagError(mDeviceModel.getAferoClient().getStatusCode(e));
                    }

                    @Override
                    public void onNext(DeviceTagCollection.Tag tag) {
                    }
                });
    }

    void removeTag(DeviceTagCollection.Tag tag) {
        mDeviceModel.removeTag(tag)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<DeviceTagCollection.Tag>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        mView.showRemoveTagError(mDeviceModel.getAferoClient().getStatusCode(e));
                    }

                    @Override
                    public void onNext(DeviceTagCollection.Tag tag) {
                        mView.removeTag(tag);
                    }
                });
    }
}
