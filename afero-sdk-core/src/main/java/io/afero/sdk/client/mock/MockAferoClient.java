/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.mock;

import java.io.IOException;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Callable;

import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.ActionResponse;
import io.afero.sdk.client.afero.models.ConclaveAccessDetails;
import io.afero.sdk.client.afero.models.DeviceAssociateResponse;
import io.afero.sdk.client.afero.models.DeviceTag;
import io.afero.sdk.client.afero.models.WriteRequest;
import io.afero.sdk.client.afero.models.Location;
import io.afero.sdk.client.afero.models.PostActionBody;
import io.afero.sdk.client.afero.models.WriteResponse;
import io.afero.sdk.conclave.models.DeviceSync;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceProfile;
import rx.Observable;


public class MockAferoClient implements AferoClient {

    private final ResourceLoader mLoader;
    private DeviceAssociateResponse mDeviceAssociateResponse;
    private String mFileNameGetDevices = "getDevices.json";
    private Observable<WriteResponse[]> postBatchAttributeWriteResponse;
    private TimeZone mDeviceTimeZone;
    private int mRequestId;

    public MockAferoClient() {
        mLoader = new ResourceLoader();
    }

    public MockAferoClient(String pathPrefix) {
        mLoader = new ResourceLoader(pathPrefix);
    }

    @Override
    public Observable<ActionResponse> postAttributeWrite(DeviceModel deviceModel, PostActionBody body, int maxRetryCount, int statusCode) {
        ActionResponse response = new ActionResponse();
        return Observable.just(response);
    }

    @Override
    public Observable<WriteResponse[]> postBatchAttributeWrite(DeviceModel deviceModel, WriteRequest[] body, int maxRetryCount, int statusCode) {

        if (postBatchAttributeWriteResponse == null) {
            WriteResponse[] response = new WriteResponse[body.length];
            for (int i = 0; i < response.length; ++i) {
                WriteResponse rr = new WriteResponse();
                rr.requestId = ++mRequestId;
                rr.status = WriteResponse.STATUS_SUCCESS;
                rr.timestampMs = System.currentTimeMillis();
                response[i] = rr;
            }
            return Observable.just(response);
        }

        return postBatchAttributeWriteResponse;
    }

    @Override
    public Observable<DeviceProfile> getDeviceProfile(String profileId) {
        try {
            return Observable.just(mLoader.createObjectFromJSONResource(
                    "getDeviceProfile/" + profileId + ".json",
                    DeviceProfile.class));
        } catch (IOException e) {
            return Observable.error(e);
        }
    }

    @Override
    public Observable<DeviceProfile[]> getAccountDeviceProfiles() {
        return Observable.fromCallable(new Callable<DeviceProfile[]>() {
            @Override
            public DeviceProfile[] call() throws Exception {
                return mLoader.createObjectFromJSONResource("getAccountDeviceProfiles.json", DeviceProfile[].class);
            }
        });
    }

    @Override
    public Observable<ConclaveAccessDetails> postConclaveAccess() {
        return null;
    }

    @Override
    public Observable<ConclaveAccessDetails> postConclaveAccess(String mobileDeviceId) {
        return null;
    }

    @Override
    public Observable<Location> putDeviceLocation(String deviceId, Location location) {
        return Observable.just(location);
    }

    @Override
    public Observable<Location> getDeviceLocation(DeviceModel deviceModel) {
        return null;
    }

    @Override
    public Observable<DeviceTag> postDeviceTag(String deviceId, String tagValue) {
        DeviceTag tag = new DeviceTag(tagValue);
        tag.deviceTagId = UUID.randomUUID().toString();
        return Observable.just(tag);
    }

    @Override
    public Observable<Void> deleteDeviceTag(String deviceId, String tagId) {
        return Observable.just(null);
    }

    @Override
    public Observable<DeviceAssociateResponse> deviceAssociateGetProfile(String associationId, boolean isOwnershipVerified) {
        try {
            DeviceAssociateResponse dar = mDeviceAssociateResponse;
            if (dar == null) {
                dar = mLoader.createObjectFromJSONResource("deviceAssociate/" + associationId + ".json", DeviceAssociateResponse.class);
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
                dar = mLoader.createObjectFromJSONResource("deviceAssociate/" + associationId + ".json", DeviceAssociateResponse.class);
            }
            return Observable.just(dar);
        } catch (IOException e) {
            e.printStackTrace();
            return Observable.error(e);
        }
    }

    @Override
    public Observable<DeviceModel> deviceDisassociate(DeviceModel deviceModel) {
        return Observable.just(deviceModel);
    }

    @Override
    public Observable<Void> putDeviceTimeZone(DeviceModel deviceModel, TimeZone tz) {
        return Observable.just(null);
    }

    @Override
    public Observable<TimeZone> getDeviceTimeZone(DeviceModel deviceModel) {
        return mDeviceTimeZone != null ? Observable.just(mDeviceTimeZone) : Observable.<TimeZone>empty();
    }

    @Override
    public Observable<DeviceSync[]> getDevicesWithState() {
        return Observable.fromCallable(new Callable<DeviceSync[]>() {
            @Override
            public DeviceSync[] call() throws Exception {
                return mLoader.createObjectFromJSONResource(mFileNameGetDevices, DeviceSync[].class);
            }
        });
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

    public void setFileGetDevices(String file) {
        mFileNameGetDevices = file;
    }

    public void setPostBatchAttributeWriteResponse(Observable<WriteResponse[]> response) {
        postBatchAttributeWriteResponse = response;
    }

    public void setDeviceTimeZone(TimeZone tz) {
        mDeviceTimeZone = tz;
    }
}
