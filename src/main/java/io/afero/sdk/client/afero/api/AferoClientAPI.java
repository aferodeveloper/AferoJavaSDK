/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.api;

import io.afero.sdk.client.afero.models.AccessToken;
import io.afero.sdk.client.afero.models.AccountDescriptionBody;
import io.afero.sdk.client.afero.models.AccountUserSummary;
import io.afero.sdk.client.afero.models.ActionResponse;
import io.afero.sdk.client.afero.models.ActivityResponse;
import io.afero.sdk.client.afero.models.ConclaveAccessBody;
import io.afero.sdk.client.afero.models.ConclaveAccessDetails;
import io.afero.sdk.client.afero.models.CreateAccountBody;
import io.afero.sdk.client.afero.models.CreateAccountResponse;
import io.afero.sdk.client.afero.models.DeviceAssociateBody;
import io.afero.sdk.client.afero.models.DeviceAssociateResponse;
import io.afero.sdk.client.afero.models.DeviceInfoBody;
import io.afero.sdk.client.afero.models.DeviceInfoExtendedData;
import io.afero.sdk.client.afero.models.DeviceRequest;
import io.afero.sdk.client.afero.models.DeviceRules;
import io.afero.sdk.client.afero.models.DeviceVersions;
import io.afero.sdk.client.afero.models.InvitationDetails;
import io.afero.sdk.client.afero.models.Location;
import io.afero.sdk.client.afero.models.NameDeviceBody;
import io.afero.sdk.client.afero.models.PostActionBody;
import io.afero.sdk.client.afero.models.RequestResponse;
import io.afero.sdk.client.afero.models.RuleExecuteBody;
import io.afero.sdk.client.afero.models.SetupStateBody;
import io.afero.sdk.client.afero.models.TermsOfServiceBody;
import io.afero.sdk.client.afero.models.UserDetails;
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
    static final String VERSION = "/v1/";
    public static final String DEFAULT_ACTIVITY_FILTER = "historyType in ['ATTRIBUTE', 'AVAILABLE', 'UNAVAILABLE', 'ASSOCIATE', 'DISASSOCIATE'] and attributeLabel NEQ 'NONE'";
    static final String BASIC_AUTH_HEADER = "Authorization: Basic bW9iaWxlQ2xpZW50czptb2JpbGUxMjM=";// + Base64.encodeToString("mobileClients:mobile123".getBytes(), Base64.NO_WRAP);

    static final int HTTP_LOCKED = 423; // https://tools.ietf.org/html/rfc4918#section-11.3

    enum ImageSize {

        SIZE_1X,
        SIZE_2X,
        SIZE_3X;

        public String toImageSizeSpecifier() {
            switch (ordinal()) {
                case 0:
                    return "1x";

                case 1:
                    return "2x";

                default:
                case 2:
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

            return SIZE_3X;
        }
    }

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

    @GET(VERSION + "users/me")
    Observable<UserDetails> usersMe();

    @PUT(VERSION + "users/{userId}/tos")
    Observable<Void> putUserTermsOfService(
            @Path("userId") String userId,
            @Body TermsOfServiceBody body
    );

    @POST(VERSION + "users/{userId}/mobileDevices")
    Observable<Response<Void>> postDeviceInfo(
            @Path("userId") String userId,
            @Body DeviceInfoBody body
    );

    @DELETE(VERSION + "users/{userId}/mobileDevices/{mobileDeviceId}")
    Observable<Void> deleteDeviceInfo(
            @Path("userId") String userId,
            @Path("mobileDeviceId") String mobileDeviceId
    );

    @Headers(BASIC_AUTH_HEADER)
    @POST(VERSION + "credentials/{email}/passwordReset")
    Observable<Void> resetPassword(
            @Path("email") String email,
            @Body String string
    );

    @POST(VERSION + "accounts")
    Observable<CreateAccountResponse> createAccount(
            @Body CreateAccountBody body
    );

    @PUT(VERSION + "accounts/{accountId}/description")
    Observable<AccountDescriptionBody> putAccountDescription(
            @Path("accountId") String accountId,
            @Body AccountDescriptionBody body
    );

    @POST(VERSION + "accounts/{accountId}/devices?expansions=state%2Cprofile%2Cattributes")
    Observable<DeviceAssociateResponse> deviceAssociate(
            @Path("accountId") String accountId,
            @Body DeviceAssociateBody body,
            @Query("locale") String locale,
            @Query("imageSize") String imageSize
    );

    @POST(VERSION + "accounts/{accountId}/devices?expansions=state%2Cprofile%2Cattributes&verified=true")
    Observable<DeviceAssociateResponse> deviceAssociateVerified(
            @Path("accountId") String accountId,
            @Body DeviceAssociateBody body,
            @Query("locale") String locale,
            @Query("imageSize") String imageSize
    );

    @DELETE(VERSION + "accounts/{accountId}/devices/{deviceId}")
    Observable<Void> deviceDisassociate(
            @Path("accountId") String accountId,
            @Path("deviceId") String deviceId
    );

    @PUT(VERSION + "accounts/{accountId}/devices/{deviceId}/location")
    Observable<Void> putDeviceLocation(
            @Path("accountId") String accountId,
            @Path("deviceId") String deviceId,
            @Body Location body
    );

    @GET(VERSION + "accounts/{accountId}/devices/{deviceId}/location")
    Observable<Location> getDeviceLocation(
            @Path("accountId") String accountId,
            @Path("deviceId") String deviceId
    );

    @POST(VERSION + "accounts/{accountId}/devices/{deviceId}/actions")
    Observable<ActionResponse> postAction(
            @Path("accountId") String accountId,
            @Path("deviceId") String deviceId,
            @Body PostActionBody body
    );

    @GET(VERSION + "accounts/{accountId}/devices/{deviceId}?expansions=extendedData")
    Observable<DeviceInfoExtendedData> getDeviceInfo(
            @Path("accountId") String accountId,
            @Path("deviceId") String deviceId
    );


    @GET(VERSION + "accounts/{accountId}/deviceProfiles")
    Observable<DeviceProfile[]> deviceProfiles(
            @Path("accountId") String accountId,
            @Query("locale") String locale,
            @Query("imageSize") String imageSize
    );

    @GET(VERSION + "accounts/{accountId}/deviceProfiles/{profileId}")
    Observable<DeviceProfile> deviceProfiles(
            @Path("accountId") String accountId,
            @Path("profileId") String profileId,
            @Query("locale") String locale,
            @Query("imageSize") String imageSize
    );

    @GET(VERSION + "accounts/{accountId}/devices/{deviceId}/rules?expansions=schedule")
    Observable<DeviceRules.Rule[]> getDeviceRules(
            @Path("accountId") String accountId,
            @Path("deviceId") String deviceId
    );

    @GET(VERSION + "accounts/{accountId}/rules?expansions=schedule")
    Observable<DeviceRules.Rule[]> getAccountRules(
            @Path("accountId") String accountId
    );

    @PUT(VERSION + "accounts/{accountId}/schedules/{scheduleId}")
    Observable<DeviceRules.Schedule> putSchedule(
            @Path("accountId") String accountId,
            @Path("scheduleId") String scheduleId,
            @Body DeviceRules.Schedule schedule
    );

    @POST(VERSION + "accounts/{accountId}/schedules")
    Observable<DeviceRules.Schedule> postSchedule(
            @Path("accountId") String accountId,
            @Body DeviceRules.Schedule schedule
    );

    @POST(VERSION + "accounts/{accountId}/rules")
    Observable<DeviceRules.Rule> postRule(
            @Path("accountId") String accountId,
            @Body DeviceRules.Rule rule
    );

    @PUT(VERSION + "accounts/{accountId}/rules/{ruleId}")
    Observable<DeviceRules.Rule> putRule(
            @Path("accountId") String accountId,
            @Path("ruleId") String ruleId,
            @Body DeviceRules.Rule rule
    );

    @DELETE(VERSION + "accounts/{accountId}/rules/{ruleId}")
    Observable<Void> deleteRule(
            @Path("accountId") String accountId,
            @Path("ruleId") String ruleId
    );

    @POST(VERSION + "accounts/{accountId}/rules/{ruleId}/actions")
    Observable<ActionResponse[]> ruleExecuteActions(
            @Path("accountId") String accountId,
            @Path("ruleId") String ruleId,
            @Body RuleExecuteBody body
    );

    @GET(VERSION + "accounts/{accountId}/activity")
    Observable<ActivityResponse[]> getActivity(
            @Path("accountId") String accountId,
            @Query("endTimestamp") Long endTimestamp,
            @Query("limit") Integer limit,
            @Query("filter") String filter
    );

    @GET(VERSION + "accounts/{accountId}/devices/{deviceId}/activity")
    Observable<ActivityResponse[]> getDeviceActivity(
            @Path("accountId") String accountId,
            @Path("deviceId") String deviceId,
            @Query("endTimestamp") Long endTimestamp,
            @Query("limit") Integer limit,
            @Query("filter") String filter
    );

    @PUT(VERSION + "accounts/{accountId}/devices/{deviceId}/friendlyName")
    Observable<NameDeviceBody> putFriendlyName(
            @Path("accountId") String accountId,
            @Path("deviceId") String deviceId,
            @Body NameDeviceBody body
    );

    @GET(VERSION + "accounts/{accountId}/accountUserSummary")
    Observable<AccountUserSummary> getAccountUserSummary(
            @Path("accountId") String accountId
    );

    @POST(VERSION + "accounts/{accountId}/invitations")
    Observable<Void> postInvite(
            @Path("accountId") String accountId,
            @Body InvitationDetails details
    );

    @DELETE(VERSION + "accounts/{accountId}/invitations/{invitationId}")
    Observable<Void> deleteInvite(
            @Path("accountId") String accountId,
            @Path("invitationId") String invitationId
    );

    @DELETE(VERSION + "accounts/{accountId}/userAccountAccess/{userId}")
    Observable<Void> deleteUser(
            @Path("accountId") String accountId,
            @Path("userId") String userId
    );

    @POST(VERSION + "accounts/{accountId}/mobileDevices/{mobileDeviceId}/conclaveAccess")
    Observable<ConclaveAccessDetails> postConclaveAccess(
            @Path("accountId") String accountId,
            @Path("mobileDeviceId") String mobileDeviceId,
            @Body ConclaveAccessBody body
    );

    @PUT(VERSION + "/accounts/{accountId}/devices/{deviceId}/state/setupState")
    Observable<Void> putSetupState(
            @Path("accountId") String accountId,
            @Path("deviceId") String deviceId,
            @Body SetupStateBody body
    );

    @POST(VERSION + "/accounts/{accountId}/devices/{deviceId}/requests")
    Observable<RequestResponse[]> postDeviceRequest(
            @Path("accountId") String accountId,
            @Path("deviceId") String deviceId,
            @Body DeviceRequest[] body
    );

    @GET(VERSION + "accounts/{accountId}/devices/{deviceId}/versions")
    Observable<DeviceVersions> getDeviceVersions(
            @Path("accountId") String accountId,
            @Path("deviceId") String deviceId
    );

}
