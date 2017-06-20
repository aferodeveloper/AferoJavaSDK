/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.mock;

import java.io.IOException;

import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.DeviceAssociateResponse;
import io.afero.sdk.device.DeviceCollection;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceProfile;
import io.afero.sdk.device.DeviceProfileCollection;
import rx.functions.Action1;

public class DeviceModelMaker {

    private final io.afero.sdk.client.mock.ResourceLoader mLoader = new ResourceLoader();
    private final DeviceCollection mDeviceCollection;
    private final MockConclaveMessageSource mMessageSource;
    private final MockAferoClient mAferoClient;
    private final DeviceProfileCollection mProfileCollection;

    public DeviceModelMaker() {
        mMessageSource = new MockConclaveMessageSource();
        mAferoClient = new MockAferoClient();
        mProfileCollection = new DeviceProfileCollection(mAferoClient, AferoClient.ImageSize.SIZE_3X, "mock-locale");
        mDeviceCollection = new DeviceCollection(mMessageSource, mProfileCollection, mAferoClient);
    }

    public DeviceModel create(String associationId) {
        final DeviceModel[] deviceModelResult = new DeviceModel[1];
        mDeviceCollection.addDevice(associationId, false)
                .toBlocking()
                .subscribe(new Action1<DeviceModel>() {
                    @Override
                    public void call(DeviceModel deviceModel) {
                        deviceModelResult[0] = deviceModel;
                    }
                });

        return deviceModelResult[0];
    }

    public DeviceModel create(DeviceProfile profile) {
        try {
            DeviceAssociateResponse dar = mLoader.createObjectFromJSONResource("deviceAssociate/genericDevice.json", DeviceAssociateResponse.class);
            dar.profile = profile;

            mAferoClient.setDeviceAssociateResponse(dar);
            DeviceModel deviceModel = makeDeviceModel();
            mAferoClient.clearDeviceAssociateResponse();

            return deviceModel;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private DeviceModel makeDeviceModel() {
        final DeviceModel[] deviceModelResult = new DeviceModel[1];
        mDeviceCollection.addDevice("genericDevice", false)
                .toBlocking()
                .subscribe(new Action1<DeviceModel>() {
                    @Override
                    public void call(DeviceModel deviceModel) {
                        deviceModelResult[0] = deviceModel;
                    }
                });

        return deviceModelResult[0];
    }
}
