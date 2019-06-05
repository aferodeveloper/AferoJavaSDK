/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.retrofit2.api;

import io.afero.sdk.client.afero.models.AccountDescriptionBody;
import io.afero.sdk.client.afero.models.AccountUserSummary;
import io.afero.sdk.client.afero.models.ActionResponse;
import io.afero.sdk.client.afero.models.ConclaveAccessBody;
import io.afero.sdk.client.afero.models.ConclaveAccessDetails;
import io.afero.sdk.client.afero.models.CreateAccountBody;
import io.afero.sdk.client.afero.models.CreateAccountResponse;
import io.afero.sdk.client.afero.models.DeviceAssociateBody;
import io.afero.sdk.client.afero.models.DeviceAssociateResponse;
import io.afero.sdk.client.afero.models.DeviceInfoExtendedData;
import io.afero.sdk.client.afero.models.DeviceRules;
import io.afero.sdk.client.afero.models.DeviceTag;
import io.afero.sdk.client.afero.models.DeviceVersions;
import io.afero.sdk.client.afero.models.InvitationDetails;
import io.afero.sdk.client.afero.models.Location;
import io.afero.sdk.client.afero.models.PostActionBody;
import io.afero.sdk.client.afero.models.RuleExecuteBody;
import io.afero.sdk.client.afero.models.ViewRequest;
import io.afero.sdk.client.afero.models.ViewResponse;
import io.afero.sdk.client.afero.models.WriteRequest;
import io.afero.sdk.client.afero.models.WriteResponse;
import io.afero.sdk.client.retrofit2.models.AccessToken;
import io.afero.sdk.client.retrofit2.models.DeviceInfoBody;
import io.afero.sdk.client.retrofit2.models.DeviceTimeZoneResponse;
import io.afero.sdk.client.retrofit2.models.DeviceTimezone;
import io.afero.sdk.client.retrofit2.models.NameDeviceBody;
import io.afero.sdk.client.retrofit2.models.UserDetails;
import io.afero.sdk.conclave.models.DeviceSync;
import io.afero.sdk.device.DeviceProfile;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

public interface AferoClientAPI {
    static final String V1 = "/v1/";

    @FormUrlEncoded
    @POST("/oauth/token")
    Observable<AccessToken> getAccessToken(
            @Field("grant_type") String grantType,
            @Field("username") String user,
            @Field("password") String password,
            @Header("Authorization") String authorization
    );

    @FormUrlEncoded
    @POST("/oauth/token")
    Call<AccessToken> refreshAccessToken(
            @Field("grant_type") String grantType,
            @Field("refresh_token") String refreshToken,
            @Header("Authorization") String authorization
    );

    @POST(V1 + "accounts")
    Observable<CreateAccountResponse> createAccount(
        @Body CreateAccountBody body
    );

    @POST(V1 + "credentials/{email}/passwordReset")
    Observable<Void> resetPassword(
            @Path("email") String email,
            @Body String string,
            @Header("Authorization") String authorization
    );

    @POST(V1 + "shortvalues/{resetCode}/passwordReset")
    Observable<Void> resetPasswordWithCode(
        @Path("resetCode") String resetCode,
        @Body String newPassword
    );

    @POST(V1 + "credentials/{email}/passwordReset")
    Observable<Void> sendPasswordRecoveryEmail(
        @Path("email") String email,
        @Header("x-afero-app") String appIdAndPlatformBase64Encoded
    );

    @GET(V1 + "users/me")
    Observable<UserDetails> usersMe();

    @POST(V1 + "accounts/{accountId}/conclaveAccess")
    Observable<ConclaveAccessDetails> postConclaveAccess(
            @Path("accountId") String accountId,
            @Body ConclaveAccessBody body
    );

    @Deprecated
    @POST(V1 + "accounts/{accountId}/mobileDevices/{mobileDeviceId}/conclaveAccess")
    Observable<ConclaveAccessDetails> postConclaveAccess(
            @Path("accountId") String accountId,
            @Path("mobileDeviceId") String mobileClientId,
            @Body ConclaveAccessBody body
    );

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

    @POST(V1 + "accounts/{accountId}/devices?expansions=state%2Cprofile%2Cattributes%2Ctimezone%2Ctags")
    Observable<DeviceAssociateResponse> deviceAssociateGetProfile(
            @Path("accountId") String accountId,
            @Body DeviceAssociateBody body,
            @Query("locale") String locale,
            @Query("imageSize") String imageSize
    );

    @POST(V1 + "accounts/{accountId}/devices?expansions=state%2Cprofile%2Cattributes%2Ctimezone%2Ctags&verified=true")
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

    @PUT(V1 + "accounts/{accountId}/devices/{deviceId}/timezone")
    Observable<Void> putDeviceTimezone(
            @Path("accountId") String accountId,
            @Path("deviceId") String deviceId,
            @Body DeviceTimezone body
    );

    @GET(V1 + "accounts/{accountId}/devices/{deviceId}/timezone")
    Observable<DeviceTimeZoneResponse> getDeviceTimezone(
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

    @GET(V1 + "devices/{associationId}/deviceProfiles/versions/{versionNumber}")
    Observable<DeviceProfile> deviceProfiles(
        @Path("associationId") String associationId,
        @Path("versionNumber") int versionNumber
    );

    @POST(V1 + "accounts/{accountId}/devices/{deviceId}/requests")
    Observable<WriteResponse[]> postDeviceRequest(
            @Path("accountId") String accountId,
            @Path("deviceId") String deviceId,
            @Body WriteRequest[] body
    );

    @GET(V1 + "accounts/{accountId}/devices?expansions=state%2Cattributes%2Ctimezone%2Ctags")
    Observable<DeviceSync[]> getDevicesWithState(
            @Path("accountId") String accountId
    );

    @POST(V1 + "accounts/{accountId}/devices/{deviceId}/deviceTag")
    Observable<DeviceTag> postDeviceTag(
            @Path("accountId") String accountId,
            @Path("deviceId") String deviceId,
            @Body DeviceTag tag
    );

    @PUT(V1 + "accounts/{accountId}/devices/{deviceId}/deviceTag")
    Observable<DeviceTag> putDeviceTag(
            @Path("accountId") String accountId,
            @Path("deviceId") String deviceId,
            @Body DeviceTag tag
    );

    @DELETE(V1 + "accounts/{accountId}/devices/{deviceId}/deviceTag/{deviceTag}")
    Observable<Void> deleteDeviceTag(
            @Path("accountId") String accountId,
            @Path("deviceId") String deviceId,
            @Path("deviceTag") String deviceTagId
    );

    @POST(V1 + "accounts/{accountId}/devices/{deviceId}/requests")
    Observable<ViewResponse[]> postDeviceViewRequest(
            @Path("accountId") String accountId,
            @Path("deviceId") String deviceId,
            @Body ViewRequest[] body
    );

    @PUT(V1 + "accounts/{accountId}/description")
    Observable<AccountDescriptionBody> putAccountDescription(
        @Path("accountId") String accountId,
        @Body AccountDescriptionBody body
    );

    @GET(V1 + "accounts/{accountId}/devices/{deviceId}?expansions=extendedData")
    Observable<DeviceInfoExtendedData> getDeviceInfo(
        @Path("accountId") String accountId,
        @Path("deviceId") String deviceId
    );

    @GET(V1 + "accounts/{accountId}/devices/{deviceId}/rules?expansions=schedule")
    Observable<DeviceRules.Rule[]> getDeviceRules(
        @Path("accountId") String accountId,
        @Path("deviceId") String deviceId
    );

    @GET(V1 + "accounts/{accountId}/rules?expansions=schedule")
    Observable<DeviceRules.Rule[]> getAccountRules(
        @Path("accountId") String accountId
    );

    @PUT(V1 + "accounts/{accountId}/schedules/{scheduleId}")
    Observable<DeviceRules.Schedule> putSchedule(
        @Path("accountId") String accountId,
        @Path("scheduleId") String scheduleId,
        @Body DeviceRules.Schedule schedule
    );

    @POST(V1 + "accounts/{accountId}/schedules")
    Observable<DeviceRules.Schedule> postSchedule(
        @Path("accountId") String accountId,
        @Body DeviceRules.Schedule schedule
    );

    @POST(V1 + "accounts/{accountId}/rules")
    Observable<DeviceRules.Rule> postRule(
        @Path("accountId") String accountId,
        @Body DeviceRules.Rule rule
    );

    @PUT(V1 + "accounts/{accountId}/rules/{ruleId}")
    Observable<DeviceRules.Rule> putRule(
        @Path("accountId") String accountId,
        @Path("ruleId") String ruleId,
        @Body DeviceRules.Rule rule
    );

    @DELETE(V1 + "accounts/{accountId}/rules/{ruleId}")
    Observable<Void> deleteRule(
        @Path("accountId") String accountId,
        @Path("ruleId") String ruleId
    );

    @POST(V1 + "accounts/{accountId}/rules/{ruleId}/actions")
    Observable<ActionResponse[]> ruleExecuteActions(
        @Path("accountId") String accountId,
        @Path("ruleId") String ruleId,
        @Body RuleExecuteBody body
    );

    @PUT(V1 + "accounts/{accountId}/devices/{deviceId}/friendlyName")
    Observable<NameDeviceBody> putFriendlyName(
        @Path("accountId") String accountId,
        @Path("deviceId") String deviceId,
        @Body NameDeviceBody body
    );

    @GET(V1 + "accounts/{accountId}/accountUserSummary")
    Observable<AccountUserSummary> getAccountUserSummary(
        @Path("accountId") String accountId
    );

    @POST(V1 + "accounts/{accountId}/invitations")
    Observable<Void> postInvite(
        @Path("accountId") String accountId,
        @Body InvitationDetails details
    );

    @DELETE(V1 + "accounts/{accountId}/invitations/{invitationId}")
    Observable<Void> deleteInvite(
        @Path("accountId") String accountId,
        @Path("invitationId") String invitationId
    );

    @DELETE(V1 + "accounts/{accountId}/userAccountAccess/{userId}")
    Observable<Void> deleteUser(
        @Path("accountId") String accountId,
        @Path("userId") String userId
    );

    @GET(V1 + "accounts/{accountId}/devices/{deviceId}/versions")
    Observable<DeviceVersions> getDeviceVersions(
        @Path("accountId") String accountId,
        @Path("deviceId") String deviceId
    );
}
