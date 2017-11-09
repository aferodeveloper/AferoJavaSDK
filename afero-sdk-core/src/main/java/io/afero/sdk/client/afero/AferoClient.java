/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero;

import java.util.TimeZone;

import io.afero.sdk.client.afero.models.ActionResponse;
import io.afero.sdk.client.afero.models.ConclaveAccessDetails;
import io.afero.sdk.client.afero.models.DeviceAssociateResponse;
import io.afero.sdk.client.afero.models.DeviceTag;
import io.afero.sdk.client.afero.models.Location;
import io.afero.sdk.client.afero.models.PostActionBody;
import io.afero.sdk.client.afero.models.WriteRequest;
import io.afero.sdk.client.afero.models.WriteResponse;
import io.afero.sdk.conclave.models.DeviceSync;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceProfile;
import rx.Observable;

public interface AferoClient {

    Observable<ActionResponse> postAttributeWrite(DeviceModel deviceModel, PostActionBody body, int maxRetryCount, int statusCode);

    Observable<WriteResponse[]> postBatchAttributeWrite(DeviceModel deviceModel, WriteRequest[] body, int maxRetryCount, int statusCode);

    Observable<DeviceProfile> getDeviceProfile(String profileId);

    Observable<DeviceProfile[]> getAccountDeviceProfiles();

    @Deprecated
    Observable<ConclaveAccessDetails> postConclaveAccess(String mobileClientId);

    Observable<ConclaveAccessDetails> postConclaveAccess();

    Observable<Location> putDeviceLocation(String deviceId, Location location);

    Observable<Location> getDeviceLocation(DeviceModel deviceModel);

    Observable<DeviceTag> postDeviceTag(String deviceId, String tagValue);

    Observable<DeviceTag> putDeviceTag(String deviceId, String tagId, String tagValue);

    Observable<Void> deleteDeviceTag(String deviceId, String tagId);

    Observable<DeviceAssociateResponse> deviceAssociateGetProfile(String associationId, boolean isOwnershipVerified);

    Observable<DeviceAssociateResponse> deviceAssociate(String associationId);

    Observable<DeviceModel> deviceDisassociate(DeviceModel deviceModel);

    Observable<Void> putDeviceTimeZone(DeviceModel deviceModel, TimeZone tz);

    Observable<TimeZone> getDeviceTimeZone(DeviceModel deviceModel);

    Observable<DeviceSync[]> getDevicesWithState();

    String getActiveAccountId();

    int getStatusCode(Throwable t);

    boolean isTransferVerificationError(Throwable t);

    class TransferVerificationRequired extends Throwable {}

    enum ImageScale {

        SCALE_1X,
        SCALE_2X,
        SCALE_3X,
        SCALE_DEFAULT;

        public String toImageSizeSpecifier() {
            switch (this) {
                case SCALE_1X:
                    return "1x";

                case SCALE_2X:
                    return "2x";

                case SCALE_3X:
                default:
                    return "3x";
            }
        }

        public static ImageScale fromDisplayDensity(float displayDensity) {
            switch (Math.round(displayDensity)) {
                case 0:
                case 1:
                    return SCALE_1X;

                case 2:
                    return SCALE_2X;

                case 3:
                    return SCALE_3X;
            }

            return SCALE_DEFAULT;
        }
    }
}
