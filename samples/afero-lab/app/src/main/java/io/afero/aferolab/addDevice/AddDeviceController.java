/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.addDevice;

import io.afero.aferolab.R;
import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.device.DeviceCollection;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.utils.RxUtils;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;

class AddDeviceController {

    private final AddDeviceView mView;
    private final DeviceCollection mDeviceCollection;
    private final AferoClient mAferoClient;

    private String mDeviceAssociationId;
    private Subscription mDeviceAssociationSubscription;

    AddDeviceController(AddDeviceView view, DeviceCollection deviceCollection, AferoClient aferoClient) {
        mView = view;
        mDeviceCollection = deviceCollection;
        mAferoClient = aferoClient;
    }

    void start() {

    }

    void stop() {
        mDeviceAssociationSubscription = RxUtils.safeUnSubscribe(mDeviceAssociationSubscription);
    }

    void addDevice(String associationId) {
        if (associationId == null || associationId.isEmpty()) {
            return;
        }

        mView.showProgress();

        mDeviceAssociationId = associationId;
        mDeviceAssociationSubscription = mDeviceCollection
                .addDevice(associationId, false)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new AddDeviceObserver(this));
    }

    void onTransferVerified() {
        mDeviceAssociationSubscription = mDeviceCollection
                .addDevice(mDeviceAssociationId, true)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new AddDeviceObserver(this));
    }

    private void onDeviceAssociateResponse(DeviceModel deviceModel) {
        mDeviceAssociationSubscription = null;
        mView.hideProgress();
        mView.onCompleted();
    }

    private void onDeviceAssociateError(Throwable e) {
        mDeviceAssociationSubscription = null;
        mView.hideProgress();

        int messageStringId = 0;
        switch (mAferoClient.getStatusCode(e)) {
            case HTTP_FORBIDDEN:
                messageStringId = R.string.error_add_device_forbidden;
                break;

            case HTTP_CONFLICT:
                messageStringId = R.string.error_add_device_conflict;
                break;

            case HTTP_BAD_REQUEST:
                messageStringId = R.string.error_add_device_generic;
                break;

            default:
                messageStringId = R.string.error_generic;
                break;
        }

        mView.showErrorAlert(messageStringId);
    }

    private void onTransferVerificationRequired() {
        mDeviceAssociationSubscription = null;
        mView.hideProgress();
        mView.askUserForTransferVerification();
    }

    private static class AddDeviceObserver extends RxUtils.WeakObserver<DeviceModel, AddDeviceController> {
        AddDeviceObserver(AddDeviceController controller) {
            super(controller);
        }

        @Override
        public void onCompleted(AddDeviceController controller) {
        }

        @Override
        public void onError(final AddDeviceController controller, Throwable e) {
            if (e instanceof AferoClient.TransferVerificationRequired) {
                controller.onTransferVerificationRequired();
            } else {
                controller.onDeviceAssociateError(e);
            }
        }

        @Override
        public void onNext(final AddDeviceController controller, final DeviceModel deviceModel) {
            controller.onDeviceAssociateResponse(deviceModel);
        }
    }
}
