/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.api;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

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
import retrofit2.mock.BehaviorDelegate;
import rx.Observable;

import static junit.framework.Assert.assertNotNull;

public class MockAferoClientAPI implements AferoClientAPI {

    private final BehaviorDelegate<AferoClientAPI> mDelegate;
    private final ObjectMapper mObjectMapper = new ObjectMapper();

    private static final String API_ROOT = "mockClientAPIData/";

    public MockAferoClientAPI(BehaviorDelegate<AferoClientAPI> delegate) {
        mDelegate = delegate;
    }

    private <T> T loadObject(String path, Class<T> valueType) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(API_ROOT + path);
        assertNotNull(is);
        return mObjectMapper.readValue(is, valueType);
    }

    @Override
    public Observable<AccessToken> getAccessToken(String user, String password, String grantType) {
        return null;
    }

    @Override
    public Call<AccessToken> refreshAccessToken(String refreshToken, String grantType) {
        return null;
    }

    @Override
    public Observable<UserDetails> usersMe() {
        return null;
    }

    @Override
    public Observable<Void> putUserTermsOfService(String userId, TermsOfServiceBody body) {
        return null;
    }

    @Override
    public Observable<Response<Void>> postDeviceInfo(String userId, DeviceInfoBody body) {
        return null;
    }

    @Override
    public Observable<Void> deleteDeviceInfo(String userId, String mobileDeviceId) {
        return null;
    }

    @Override
    public Observable<Void> resetPassword(String email, String string) {
        return null;
    }

    @Override
    public Observable<CreateAccountResponse> createAccount(CreateAccountBody body) {
        return null;
    }

    @Override
    public Observable<AccountDescriptionBody> putAccountDescription(String accountId, AccountDescriptionBody body) {
        return null;
    }

    @Override
    public Observable<DeviceAssociateResponse> deviceAssociate(String accountId, DeviceAssociateBody body, String locale, String imageSize) {
        return null;
    }

    @Override
    public Observable<DeviceAssociateResponse> deviceAssociateVerified(String accountId, DeviceAssociateBody body, String locale, String imageSize) {
        return null;
    }

    @Override
    public Observable<Void> deviceDisassociate(String accountId, String deviceId) {
        return null;
    }

    @Override
    public Observable<Void> putDeviceLocation(String accountId, String deviceId, Location body) {
        return null;
    }

    @Override
    public Observable<Location> getDeviceLocation(String accountId, String deviceId) {
        return null;
    }

    @Override
    public Observable<ActionResponse> postAction(String accountId, String deviceId, PostActionBody body) {
        return null;
    }

    @Override
    public Observable<DeviceInfoExtendedData> getDeviceInfo(String accountId, String deviceId) {
        return null;
    }

    @Override
    public Observable<DeviceProfile[]> deviceProfiles(String accountId, String locale, String imageSize) {

        try {
            DeviceProfile[] deviceProfiles = loadObject("accounts/" + accountId + "/deviceProfiles.json", DeviceProfile[].class);
            return mDelegate.returningResponse(deviceProfiles)
                    .deviceProfiles(accountId, locale, imageSize);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mDelegate.returningResponse(null).deviceProfiles(accountId, locale, imageSize);
    }

    @Override
    public Observable<DeviceProfile> deviceProfiles(String accountId, String profileId, String locale, String imageSize) {
        try {
            DeviceProfile[] deviceProfiles = loadObject("accounts/" + accountId + "/deviceProfiles.json", DeviceProfile[].class);
            DeviceProfile result = null;
            for (DeviceProfile dp : deviceProfiles) {
                if (profileId.equals(dp.getId())) {
                    result = dp;
                    break;
                }
            }
            return mDelegate.returningResponse(result)
                    .deviceProfiles(accountId, profileId, locale, imageSize);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mDelegate.returningResponse(null).deviceProfiles(accountId, profileId, locale, imageSize);
    }

    @Override
    public Observable<DeviceRules.Rule[]> getDeviceRules(String accountId, String deviceId) {
        return null;
    }

    @Override
    public Observable<DeviceRules.Rule[]> getAccountRules(String accountId) {
        return null;
    }

    @Override
    public Observable<DeviceRules.Schedule> putSchedule(String accountId, String scheduleId, DeviceRules.Schedule schedule) {
        return null;
    }

    @Override
    public Observable<DeviceRules.Schedule> postSchedule(String accountId, DeviceRules.Schedule schedule) {
        return null;
    }

    @Override
    public Observable<DeviceRules.Rule> postRule(String accountId, DeviceRules.Rule rule) {
        return null;
    }

    @Override
    public Observable<DeviceRules.Rule> putRule(String accountId, String ruleId, DeviceRules.Rule rule) {
        return null;
    }

    @Override
    public Observable<Void> deleteRule(String accountId, String ruleId) {
        return null;
    }

    @Override
    public Observable<ActionResponse[]> ruleExecuteActions(String accountId, String ruleId, RuleExecuteBody body) {
        return null;
    }

    @Override
    public Observable<ActivityResponse[]> getActivity(String accountId, Long endTimestamp, Integer limit, String filter) {
        return null;
    }

    @Override
    public Observable<ActivityResponse[]> getDeviceActivity(String accountId, String deviceId, Long endTimestamp, Integer limit, String filter) {
        return null;
    }

    @Override
    public Observable<NameDeviceBody> putFriendlyName(String accountId, String deviceId, NameDeviceBody body) {
        return null;
    }

    @Override
    public Observable<AccountUserSummary> getAccountUserSummary(String accountId) {
        return null;
    }

    @Override
    public Observable<Void> postInvite(String accountId, InvitationDetails details) {
        return null;
    }

    @Override
    public Observable<Void> deleteInvite(String accountId, String invitationId) {
        return null;
    }

    @Override
    public Observable<Void> deleteUser(String accountId, String userId) {
        return null;
    }

    @Override
    public Observable<ConclaveAccessDetails> postConclaveAccess(String accountId, String mobileDeviceId, ConclaveAccessBody body) {
        return null;
    }

    @Override
    public Observable<Void> putSetupState(String accountId, String deviceId, SetupStateBody body) {
        return null;
    }

    @Override
    public Observable<RequestResponse[]> postDeviceRequest(String accountId, String deviceId, DeviceRequest[] body) {
        return null;
    }

    @Override
    public Observable<DeviceVersions> getDeviceVersions(String accountId, String deviceId) {
        return null;
    }
}
