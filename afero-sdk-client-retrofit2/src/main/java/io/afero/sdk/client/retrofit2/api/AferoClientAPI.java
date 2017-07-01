/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.retrofit2.api;

import io.afero.sdk.client.afero.models.ActionResponse;
import io.afero.sdk.client.afero.models.ConclaveAccessBody;
import io.afero.sdk.client.afero.models.ConclaveAccessDetails;
import io.afero.sdk.client.afero.models.DeviceAssociateBody;
import io.afero.sdk.client.afero.models.DeviceAssociateResponse;
import io.afero.sdk.client.afero.models.DeviceRequest;
import io.afero.sdk.client.afero.models.Location;
import io.afero.sdk.client.afero.models.PostActionBody;
import io.afero.sdk.client.afero.models.RequestResponse;
import io.afero.sdk.client.retrofit2.models.AccessToken;
import io.afero.sdk.client.retrofit2.models.DeviceInfoBody;
import io.afero.sdk.client.retrofit2.models.UserDetails;
import io.afero.sdk.device.DeviceProfile;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

public interface AferoClientAPI {
    static final String V1 = "/v1/";
    public static final String BASIC_AUTH_HEADER = "Authorization: Basic bW9iaWxlQ2xpZW50czptb2JpbGUxMjM=";// + Base64.encodeToString("mobileClients:mobile123".getBytes(), Base64.NO_WRAP);

    @FormUrlEncoded
    @Headers(BASIC_AUTH_HEADER)
    @POST("/oauth/token")
    Observable<AccessToken> getAccessToken(
            @Field("username") String user,
            @Field("password") String password,
            @Field("grant_type") String grantType
    );

    @FormUrlEncoded
    @Headers(BASIC_AUTH_HEADER)
    @POST("/oauth/token")
    Call<AccessToken> refreshAccessToken(
            @Field("refresh_token") String refreshToken,
            @Field("grant_type") String grantType
    );

    @Headers(BASIC_AUTH_HEADER)
    @POST(V1 + "credentials/{email}/passwordReset")
    Observable<Void> resetPassword(
            @Path("email") String email,
            @Body String string
    );

    @GET(V1 + "users/me")
    Observable<UserDetails> usersMe();

    @POST(V1 + "users/{userId}/mobileDevices")
    Observable<Response<Void>> postDeviceInfo(
            @Path("userId") String userId,
            @Body DeviceInfoBody body
    );

    @DELETE(V1 + "users/{userId}/mobileDevices/{mobileDeviceId}")
    Observable<Void> deleteDeviceInfo(
            @Path("userId") String userId,
            @Path("mobileDeviceId") String mobileDeviceId
    );

    @POST(V1 + "accounts/{accountId}/devices")
    Observable<DeviceAssociateResponse> deviceAssociate(
            @Path("accountId") String accountId,
            @Body DeviceAssociateBody body
    );

    @POST(V1 + "accounts/{accountId}/devices?expansions=state%2Cprofile%2Cattributes")
    Observable<DeviceAssociateResponse> deviceAssociateGetProfile(
            @Path("accountId") String accountId,
            @Body DeviceAssociateBody body,
            @Query("locale") String locale,
            @Query("imageSize") String imageSize
    );

    @POST(V1 + "accounts/{accountId}/devices?expansions=state%2Cprofile%2Cattributes&verified=true")
    Observable<DeviceAssociateResponse> deviceAssociateVerified(
            @Path("accountId") String accountId,
            @Body DeviceAssociateBody body,
            @Query("locale") String locale,
            @Query("imageSize") String imageSize
    );

    @DELETE(V1 + "accounts/{accountId}/devices/{deviceId}")
    Observable<Void> deviceDisassociate(
            @Path("accountId") String accountId,
            @Path("deviceId") String deviceId
    );

    @PUT(V1 + "accounts/{accountId}/devices/{deviceId}/location")
    Observable<Void> putDeviceLocation(
            @Path("accountId") String accountId,
            @Path("deviceId") String deviceId,
            @Body Location body
    );

    @GET(V1 + "accounts/{accountId}/devices/{deviceId}/location")
    Observable<Location> getDeviceLocation(
            @Path("accountId") String accountId,
            @Path("deviceId") String deviceId
    );

    @POST(V1 + "accounts/{accountId}/devices/{deviceId}/actions")
    Observable<ActionResponse> postAction(
            @Path("accountId") String accountId,
            @Path("deviceId") String deviceId,
            @Body PostActionBody body
    );

    @GET(V1 + "accounts/{accountId}/deviceProfiles")
    Observable<DeviceProfile[]> deviceProfiles(
            @Path("accountId") String accountId,
            @Query("locale") String locale,
            @Query("imageSize") String imageSize
    );

    @GET(V1 + "accounts/{accountId}/deviceProfiles/{profileId}")
    Observable<DeviceProfile> deviceProfiles(
            @Path("accountId") String accountId,
            @Path("profileId") String profileId,
            @Query("locale") String locale,
            @Query("imageSize") String imageSize
    );

    @POST(V1 + "accounts/{accountId}/mobileDevices/{mobileDeviceId}/conclaveAccess")
    Observable<ConclaveAccessDetails> postConclaveAccess(
            @Path("accountId") String accountId,
            @Path("mobileDeviceId") String mobileClientId,
            @Body ConclaveAccessBody body
    );

    @POST(V1 + "/accounts/{accountId}/devices/{deviceId}/requests")
    Observable<RequestResponse[]> postDeviceRequest(
            @Path("accountId") String accountId,
            @Path("deviceId") String deviceId,
            @Body DeviceRequest[] body
    );

}
