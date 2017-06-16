/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero;

import io.afero.sdk.client.afero.models.ActionResponse;
import io.afero.sdk.client.afero.models.ConclaveAccessDetails;
import io.afero.sdk.client.afero.models.DeviceRequest;
import io.afero.sdk.client.afero.models.Location;
import io.afero.sdk.client.afero.models.PostActionBody;
import io.afero.sdk.client.afero.models.RequestResponse;
import io.afero.sdk.device.DeviceProfile;
import rx.Observable;

public interface AferoClient {

    int getStatusCode(Throwable t);

    Observable<ActionResponse> postAction(String deviceId, PostActionBody body);

    Observable<DeviceProfile[]> getAccountDeviceProfiles(String locale, String imageSize);

    Observable<DeviceProfile> getDeviceProfile(String profileId, String locale, String imageSize);

    Observable<ConclaveAccessDetails> postConclaveAccess(String mobileDeviceId);

    Observable<RequestResponse[]> postDeviceRequest(String deviceId, DeviceRequest[] body);

    Observable<Location> getDeviceLocation(String deviceId);

    Observable<Void> deviceDisassociate(String deviceId);
}
