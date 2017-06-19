/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero;

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

public interface AferoClient {

    Observable<ActionResponse> postAttributeWrite(DeviceModel deviceModel, PostActionBody body, int maxRetryCount, int statusCode);

    Observable<RequestResponse[]> postBatchAttributeWrite(DeviceModel deviceModel, DeviceRequest[] body, int maxRetryCount, int statusCode);

    Observable<DeviceProfile> getDeviceProfile(String profileId, String locale, ImageSize imageSize);

    Observable<DeviceProfile[]> getAccountDeviceProfiles(String locale, ImageSize imageSize);

    Observable<ConclaveAccessDetails> postConclaveAccess(String mobileClientId);

    Observable<Location> putDeviceLocation(String deviceId, Location location);

    Observable<Location> getDeviceLocation(DeviceModel deviceModel);

    Observable<DeviceAssociateResponse> deviceAssociate(String associationId, boolean isOwnershipVerified, String locale, ImageSize imageSize);

    Observable<DeviceModel> deviceDisassociate(DeviceModel deviceModel);

    String getActiveAccountId();

    int getStatusCode(Throwable t);

    boolean isTransferVerificationError(Throwable t);

    class TransferVerificationRequired extends Throwable {}

    enum ImageSize {

        SIZE_1X,
        SIZE_2X,
        SIZE_3X,
        SIZE_DEFAULT;

        public String toImageSizeSpecifier() {
            switch (ordinal()) {
                case 0:
                    return "1x";

                case 1:
                    return "2x";

                case 2:
                default:
                    return "3x";
            }
        }

        public static ImageSize fromDisplayDensity(float displayDensity) {
            switch (Math.round(displayDensity)) {
                case 0:
                case 1:
                    return SIZE_1X;

                case 2:
                    return SIZE_2X;

                case 3:
                    return SIZE_3X;
            }

            return SIZE_DEFAULT;
        }
    }
}
