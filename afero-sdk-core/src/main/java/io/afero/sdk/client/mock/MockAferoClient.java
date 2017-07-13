/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.mock;

import java.io.IOException;
import java.util.TimeZone;

import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.ActionResponse;
import io.afero.sdk.client.afero.models.ConclaveAccessDetails;
import io.afero.sdk.client.afero.models.DeviceAssociateResponse;
import io.afero.sdk.client.afero.models.DeviceRequest;
import io.afero.sdk.client.afero.models.Location;
import io.afero.sdk.client.afero.models.PostActionBody;
import io.afero.sdk.client.afero.models.RequestResponse;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceProfile;
import rx.Observable;

public class MockAferoClient implements AferoClient {

    private final ResourceLoader mLoader = new ResourceLoader();
    private final String pathPrefix;
    private DeviceAssociateResponse mDeviceAssociateResponse;

    public MockAferoClient() {
        pathPrefix = "";
    }

    public MockAferoClient(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    @Override
    public Observable<ActionResponse> postAttributeWrite(DeviceModel deviceModel, PostActionBody body, int maxRetryCount, int statusCode) {
        ActionResponse response = new ActionResponse();
        return Observable.just(response);
    }

    @Override
    public Observable<RequestResponse[]> postBatchAttributeWrite(DeviceModel deviceModel, DeviceRequest[] body, int maxRetryCount, int statusCode) {
        RequestResponse[] response = new RequestResponse[body.length];
        for (int i = 0; i < response.length; ++i) {
            response[i] = new RequestResponse();
        }
        return Observable.just(response);
    }

    @Override
    public Observable<DeviceProfile> getDeviceProfile(String profileId) {
        return null;
    }

    @Override
    public Observable<DeviceProfile[]> getAccountDeviceProfiles() {
        return null;
    }

    @Override
    public Observable<ConclaveAccessDetails> postConclaveAccess(String mobileClientId) {
        return null;
    }

    @Override
    public Observable<Location> getDeviceLocation(DeviceModel deviceModel) {
        return null;
    }

    @Override
    public Observable<DeviceAssociateResponse> deviceAssociateGetProfile(String associationId, boolean isOwnershipVerified) {
        try {
            DeviceAssociateResponse dar = mDeviceAssociateResponse;
            if (dar == null) {
                dar = mLoader.createObjectFromJSONResource(pathPrefix + "deviceAssociate/" + associationId + ".json", DeviceAssociateResponse.class);
            }
            return Observable.just(dar);
        } catch (IOException e) {
            return Observable.error(e);
        }
    }

    @Override
    public Observable<DeviceAssociateResponse> deviceAssociate(String associationId) {
        try {
            DeviceAssociateResponse dar = mDeviceAssociateResponse;
            if (dar == null) {
                dar = mLoader.createObjectFromJSONResource(pathPrefix + "deviceAssociate/" + associationId + ".json", DeviceAssociateResponse.class);
            }
            return Observable.just(dar);
        } catch (IOException e) {
            e.printStackTrace();
            return Observable.error(e);
        }
    }

    @Override
    public Observable<DeviceModel> deviceDisassociate(DeviceModel deviceModel) {
        return null;
    }

    @Override
    public Observable<Void> putDeviceTimezone(DeviceModel deviceModel, TimeZone tz) {
        return Observable.just(null);
    }

    @Override
    public String getActiveAccountId() {
        return null;
    }

    @Override
    public int getStatusCode(Throwable t) {
        return 0;
    }

    @Override
    public boolean isTransferVerificationError(Throwable t) {
        return false;
    }

    public void setDeviceAssociateResponse(DeviceAssociateResponse dar) {
        mDeviceAssociateResponse = dar;
    }

    public void clearDeviceAssociateResponse() {
        mDeviceAssociateResponse = null;
    }
}
