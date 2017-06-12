/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero;

import java.io.IOException;
import java.net.HttpURLConnection;

import io.afero.sdk.DeviceEventStreamInstance;
import io.afero.sdk.client.AccountServiceClient;
import io.afero.sdk.client.AfHttpClient;
import io.afero.sdk.client.afero.api.AferoClientAPI;
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
import io.afero.sdk.client.afero.models.ErrorBody;
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
import io.afero.sdk.utils.AfLog;
import io.afero.sdk.utils.JSONUtils;
import io.afero.sdk.utils.RxUtils;
import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.HttpException;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

public class AferoClient implements AccountServiceClient {

    public static final String GRANT_TYPE_PASSWORD = "password";
    public static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";

    private static final String HEADER_AUTHORIZATION = "Authorization";

    private String mActiveAccountId;
    private String mOwnerAccountId;
    private final String mBaseUrl;
    private final AferoClientAPI mAferoService;
    private AccessToken mAccessToken;
    private final Object mTokenRefreshLock = new Object();
    private BehaviorSubject<AccessToken> mTokenSubject;
    private PublishSubject<String> mActiveAccountSubject = PublishSubject.create();


    public AferoClient(String baseUrl, HttpLoggingInterceptor.Level logLevel, int defaultTimeout) {
        mBaseUrl = baseUrl;
        mAferoService = createAdapter(mBaseUrl, logLevel, defaultTimeout);
    }

    public String getBaseUrl() {
        return mBaseUrl;
    }

    public void setOwnerAccountId(String accountId) {
        mOwnerAccountId = accountId;
    }

    public String getOwnerAccountId() {
        return mOwnerAccountId;
    }

    public void setActiveAccountId(String accountId) {
        mActiveAccountId = accountId;
        mActiveAccountSubject.onNext(mActiveAccountId);
    }

    public String getActiveAccountId() {
        return mActiveAccountId;
    }

    public boolean isOwnerAccountActive() {
        return mOwnerAccountId.equals(mActiveAccountId);
    }

    public void clearAccount() {
        mOwnerAccountId = null;
        mActiveAccountId = null;
    }

    public Observable<String> observeActiveAccount() {
        return mActiveAccountSubject;
    }

    @Override
    public Observable<CreateAccountResult> createAccount(CreateAccountParams body) {

        CreateAccountBody cab = new CreateAccountBody();

        cab.user.firstName = body.firstName;
        cab.user.lastName = body.lastName;
        cab.credential.credentialId = body.email;
        cab.credential.password = body.password;
        cab.account.description = body.description;

        return createAccount(cab)
                .zipWith(Observable.just(cab), new Func2<CreateAccountResponse, CreateAccountBody, CreateAccountBody>() {
                    @Override
                    public CreateAccountBody call(CreateAccountResponse createAccountResponse, CreateAccountBody createAccountBody) {
                        return createAccountBody;
                    }
                })
                .concatMap(new Func1<CreateAccountBody, Observable<? extends CreateAccountResult>>() {
                    @Override
                    public Observable<? extends CreateAccountResult> call(CreateAccountBody cab) {
                        CreateAccountResult result = new CreateAccountResult();
                        result.email = cab.credential.credentialId;
                        result.password = cab.credential.password;

                        return Observable.just(result);
                    }
                });
    }

    @Override
    public Observable<ResponseBody> resetPassword(String email) {
        return mAferoService.resetPassword(email, "");
    }

    @Override
    public Observable<LoginResult> login(String email, String password) {
        return getAccessToken(email, password, AferoClient.GRANT_TYPE_PASSWORD)
                .flatMap(new Func1<AccessToken, Observable<LoginResult>>() {
                    @Override
                    public Observable<LoginResult> call(AccessToken token) {
                        LoginResult result = new LoginResult();
                        result.token = token;
                        return Observable.just(result);
                    }
                });
    }

    @Override
    public Observable<ResponseBody> resendVerificationEmail(String email) {
        return Observable.error(new Exception("Not supported"));
    }

    @Override
    public ResponseError getResponseError(Throwable e) {
        ErrorBody aferoError = AferoClient.getErrorBody(e);
        ResponseError errorResult = new ResponseError();

        errorResult.statusCode = AferoClient.getStatusCode(e);

        if (aferoError != null) {
            errorResult.errorCode = aferoError.error;
            errorResult.errorMessage = aferoError.error_description;
        }

        return errorResult;
    }

    @Override
    public boolean isError(ResponseError err, ErrorType errorType) {

        if (err == null) {
            return false;
        }

        switch (errorType) {
            case ACCOUNT_ALREADY_EXISTS:
                return ErrorBody.ERROR_ALREADY_EXISTS.equalsIgnoreCase(err.errorCode)
                        || err.statusCode == HttpURLConnection.HTTP_CONFLICT;
        }

        return false;
    }

    @Override
    public Observable<Profile> getProfile() {
        return Observable.just(new Profile());
    }

    @Override
    public Observable<ResponseBody> updateProfile(Profile profile) {
        return Observable.just(null);
    }

    @Override
    public boolean isUserInfoRequired() {
        return false;
    }

    @Override
    public boolean isUserInfoComplete(Profile userProfile) {
        return true;
    }

    public void signOut(String userId, String mobileClientId) {
        rx.Observable<ResponseBody> observable;

        DeviceEventStreamInstance.get().stop();
        DeviceEventStreamInstance.get().setAccountId(null);

        if (userId != null && (!userId.isEmpty()) && mobileClientId != null && (!mobileClientId.isEmpty())) {
            observable = deleteDeviceInfo(userId, mobileClientId);
        } else {
            observable = rx.Observable.empty();
        }

        observable.doOnTerminate(new Action0() {
            @Override
            public void call() {
                synchronized (mTokenRefreshLock) {
                    mAccessToken = null;

                    BehaviorSubject<AccessToken> tokenSubject = mTokenSubject;
                    if (tokenSubject != null) {
                        mTokenSubject = null;
                        tokenSubject.onCompleted();
                    }
                }
            }
        }).subscribe(new RxUtils.IgnoreResponseObserver<ResponseBody>());
    }

    public void setToken(AccessToken token) {
        mAccessToken = token;
    }

    public AccessToken getToken() {
        return mAccessToken;
    }

    public Observable<AccessToken> tokenRefreshObservable() {
        if (mTokenSubject == null) {
            mTokenSubject = BehaviorSubject.create();
        }
        return mTokenSubject;
    }

    public static int getStatusCode(Throwable t) {

        if (t instanceof HttpException) {
            return ((HttpException)t).code();
        }

        return 0;
    }

    public static ErrorBody getErrorBody(Throwable e) {
        try {
            if (e instanceof HttpException) {
                return JSONUtils.readValue(((HttpException)e).response().errorBody().bytes(), ErrorBody.class);
            }
        } catch (Exception ignore) {
            // ignore
        }
        return null;
    }

    public Observable<AccessToken> getAccessToken(String user, String password, String grantType) {
        return mAferoService.getAccessToken(user, password, grantType);
    }

    public Call<AccessToken> refreshAccessToken(String refreshToken, String grantType) {
        return mAferoService.refreshAccessToken(refreshToken, grantType);
    }

    public Observable<UserDetails> usersMe() {
        return mAferoService.usersMe();
    }

    public Observable<ResponseBody> putUserTermsOfService(String userId, TermsOfServiceBody body) {
        return mAferoService.putUserTermsOfService(userId, body);
    }

    public Observable<Response<Void>> postDeviceInfo(String userId, DeviceInfoBody body) {
        return mAferoService.postDeviceInfo(userId, body);
    }

    public Observable<ResponseBody> deleteDeviceInfo(String userId, String mobileDeviceId) {
        return mAferoService.deleteDeviceInfo(userId, mobileDeviceId);
    }

    public Observable<CreateAccountResponse> createAccount(CreateAccountBody body) {
        return mAferoService.createAccount(body);
    }

    public Observable<AccountDescriptionBody> putAccountDescription(String accountId, AccountDescriptionBody body) {
        return mAferoService.putAccountDescription(accountId, body);
    }

    public Observable<DeviceAssociateResponse> deviceAssociate(DeviceAssociateBody body, String locale, String imageSize) {
        return mAferoService.deviceAssociate(mActiveAccountId, body, locale, imageSize);
    }

    public Observable<DeviceAssociateResponse> deviceAssociateVerified(DeviceAssociateBody body, String locale, String imageSize) {
        return mAferoService.deviceAssociateVerified(mActiveAccountId, body, locale, imageSize);
    }

    public Observable<DeviceAssociateResponse> deviceAssociateWithTransferVerificationCheck(DeviceAssociateBody body, String locale, AferoClientAPI.ImageSize imageSize) {
        return deviceAssociate(body, locale, imageSize.toImageSizeSpecifier())
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends DeviceAssociateResponse>>() {
                    @Override
                    public Observable<? extends DeviceAssociateResponse> call(Throwable t) {
                        try {
                            if (t instanceof HttpException) {
                                HttpException e = (HttpException)t;
                                if (e.code() == HttpURLConnection.HTTP_FORBIDDEN &&
                                        "true".equalsIgnoreCase(e.response().headers().get("transfer-verification-enabled"))) {
                                    return Observable.error(new TransferVerificationRequired());
                                }
                            }
                        } catch (Throwable ignore) {} // belt & suspenders

                        return Observable.error(t);
                    }
                });
    }

    public static class TransferVerificationRequired extends Throwable {}

    public Observable<ResponseBody> deviceDisassociate(String deviceId) {
        return mAferoService.deviceDisassociate(mActiveAccountId, deviceId);
    }

    public Observable<ResponseBody> putDeviceLocation(String deviceId, Location body) {
        return mAferoService.putDeviceLocation(mActiveAccountId, deviceId, body);
    }

    public Observable<Location> getDeviceLocation(String deviceId) {
        return mAferoService.getDeviceLocation(mActiveAccountId, deviceId);
    }

    public Observable<ActionResponse> postAction(String deviceId, PostActionBody body) {
        return mAferoService.postAction(mActiveAccountId, deviceId, body);
    }

    public Observable<DeviceInfoExtendedData> getDeviceInfo(String deviceId) {
        return mAferoService.getDeviceInfo(mActiveAccountId, deviceId);
    }

    public Observable<DeviceProfile[]> getAccountDeviceProfiles(String locale, String imageSize) {
        return mAferoService.deviceProfiles(mActiveAccountId, locale, imageSize);
    }

    public Observable<DeviceProfile> getDeviceProfile(String profileId, String locale, String imageSize) {
        return mAferoService.deviceProfiles(mActiveAccountId, profileId, locale, imageSize);
    }

    public Observable<DeviceRules.Rule[]> getDeviceRules(String deviceId) {
        return mAferoService.getDeviceRules(mActiveAccountId, deviceId);
    }

    public Observable<DeviceRules.Rule[]> getAccountRules() {
        return mAferoService.getAccountRules(mActiveAccountId);
    }

    public Observable<DeviceRules.Schedule> putSchedule(String scheduleId, DeviceRules.Schedule schedule) {
        return mAferoService.putSchedule(mActiveAccountId, scheduleId, schedule);
    }

    public Observable<DeviceRules.Schedule> postSchedule(DeviceRules.Schedule schedule) {
        return mAferoService.postSchedule(mActiveAccountId, schedule);
    }

    public Observable<DeviceRules.Rule> postRule(DeviceRules.Rule rule) {
        return mAferoService.postRule(mActiveAccountId, rule);
    }

    public Observable<DeviceRules.Rule> putRule(String ruleId, DeviceRules.Rule rule) {
        return mAferoService.putRule(mActiveAccountId, ruleId, rule);
    }

    public Observable<ResponseBody> deleteRule(String ruleId) {
        return mAferoService.deleteRule(mActiveAccountId, ruleId);
    }

    public Observable<ActionResponse[]> ruleExecuteActions(String ruleId, RuleExecuteBody body) {
        return mAferoService.ruleExecuteActions(mActiveAccountId, ruleId, body);
    }

    public Observable<ActivityResponse[]> getActivity(Long endTimestamp, Integer limit, String filter) {
        return mAferoService.getActivity(mActiveAccountId, endTimestamp, limit, filter);
    }

    public Observable<ActivityResponse[]> getDeviceActivity(String deviceId, Long endTimestamp, Integer limit, String filter) {
        return mAferoService.getDeviceActivity(mActiveAccountId, deviceId, endTimestamp, limit, filter);
    }

    public Observable<NameDeviceBody> putFriendlyName(String deviceId, String name) {
        return mAferoService.putFriendlyName(mActiveAccountId, deviceId, new NameDeviceBody(name));
    }

    public Observable<AccountUserSummary> getAccountUserSummary() {
        return mAferoService.getAccountUserSummary(mActiveAccountId);
    }

    public Observable<ResponseBody> postInvite(InvitationDetails details) {
        return mAferoService.postInvite(mActiveAccountId, details);
    }

    public Observable<ResponseBody> deleteInvite(String invitationId) {
        return mAferoService.deleteInvite(mActiveAccountId, invitationId);
    }

    public Observable<ResponseBody> deleteUser(String userId) {
        return mAferoService.deleteUser(mActiveAccountId, userId);
    }

    public Observable<ConclaveAccessDetails> postConclaveAccess(String mobileDeviceId) {
        return mAferoService.postConclaveAccess(mActiveAccountId, mobileDeviceId, new ConclaveAccessBody());
    }

    public Observable<ResponseBody> putSetupState(String deviceId, SetupStateBody body) {
        return mAferoService.putSetupState(mActiveAccountId, deviceId, body);
    }

    public Observable<RequestResponse[]> postDeviceRequest(String deviceId, DeviceRequest[] body) {
        return mAferoService.postDeviceRequest(mActiveAccountId, deviceId, body);
    }

    public Observable<DeviceVersions> getDeviceVersions(String deviceId) {
        return mAferoService.getDeviceVersions(mActiveAccountId, deviceId);
    }

    protected AferoClientAPI createAdapter(String baseUrl, HttpLoggingInterceptor.Level logLevel, int defaultTimeout) {

//        if (BuildConfig.DEBUG) {
//            okClient.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("172.16.0.159", 8888)));
//        }

        OkHttpClient httpClient = AfHttpClient.create(logLevel, defaultTimeout).newBuilder()
                .addInterceptor(new AuthTokenInterceptor())
                .authenticator(new RefreshTokenAuthenticator())
                .build();

        Retrofit.Builder builder = new Retrofit.Builder()
                .client(httpClient)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(JacksonConverterFactory.create(JSONUtils.getObjectMapper()))
                .baseUrl(baseUrl);

        return builder.build().create(AferoClientAPI.class);
    }

    private class RefreshTokenAuthenticator implements Authenticator {
        @Override
        public Request authenticate(Route route, okhttp3.Response response) throws IOException {
            if(responseCount(response) >= 2) {
                mAccessToken = null;

                BehaviorSubject<AccessToken> tokenSubject = mTokenSubject;
                if (tokenSubject != null) {
                    mTokenSubject = null;
                    tokenSubject.onError(new Exception());
                }

                // If both the original call and the call with refreshed token failed,
                // it will probably keep failing, so don't try again.
                return null;
            }

            synchronized (mTokenRefreshLock) { // prevent concurrent token refreshes
                AccessToken currentToken = mAccessToken;

                if (currentToken != null && currentToken.refreshToken != null) {

                    AfLog.d("AferoClient: Attempting to refresh token");

                    // Get a new token
                    Call<AccessToken> call = refreshAccessToken(currentToken.refreshToken, GRANT_TYPE_REFRESH_TOKEN);

                    try {
                        Response<AccessToken> refreshResponse = call.execute();
                        if (refreshResponse.isSuccessful()) {
                            mAccessToken = refreshResponse.body();
                            if (mTokenSubject != null) {
                                mTokenSubject.onNext(mAccessToken);
                            }
                        }

                        // Add new header to rejected request and retry it
                        return response.request().newBuilder()
                                .header(HEADER_AUTHORIZATION, mAccessToken.tokenType + " " + mAccessToken.accessToken)
                                .build();
                    } catch (Exception e) {
                        AfLog.d("AferoClient: Failed to refresh token");
                        mAccessToken = null;

                        BehaviorSubject<AccessToken> tokenSubject = mTokenSubject;
                        if (tokenSubject != null) {
                            mTokenSubject = null;
                            tokenSubject.onError(e);
                        }
                    }
                }
            }

            return null;
        }
    }

    private static int responseCount(okhttp3.Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }

    private class AuthTokenInterceptor implements Interceptor {

        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Request request = chain.request();

            Request.Builder builder = request.newBuilder();
            AccessToken requestToken = mAccessToken;
            if (request.header(HEADER_AUTHORIZATION) == null && requestToken != null) {
                String type = requestToken.tokenType != null ? requestToken.tokenType : "Bearer";
                String auth = type + " " + requestToken.accessToken;
                builder.header(HEADER_AUTHORIZATION, auth);
            }

            return chain.proceed(builder.build());
        }
    }

}
