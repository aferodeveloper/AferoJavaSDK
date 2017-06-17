/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.retrofit2;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;

import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.ActionResponse;
import io.afero.sdk.client.afero.models.ConclaveAccessBody;
import io.afero.sdk.client.afero.models.ConclaveAccessDetails;
import io.afero.sdk.client.afero.models.DeviceAssociateBody;
import io.afero.sdk.client.afero.models.DeviceAssociateResponse;
import io.afero.sdk.client.afero.models.DeviceRequest;
import io.afero.sdk.client.afero.models.ErrorBody;
import io.afero.sdk.client.afero.models.Location;
import io.afero.sdk.client.afero.models.PostActionBody;
import io.afero.sdk.client.afero.models.RequestResponse;
import io.afero.sdk.client.retrofit2.api.AferoClientAPI;
import io.afero.sdk.client.retrofit2.models.AccessToken;
import io.afero.sdk.client.retrofit2.models.DeviceInfoBody;
import io.afero.sdk.client.retrofit2.models.UserDetails;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceProfile;
import io.afero.sdk.log.AfLog;
import io.afero.sdk.utils.JSONUtils;
import io.afero.sdk.utils.RxUtils;
import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
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

public class AferoClientRetrofit2 implements AferoClient {

    public static final String GRANT_TYPE_PASSWORD = "password";
    public static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";

    private static final String HEADER_AUTHORIZATION = "Authorization";

    protected String mActiveAccountId;
    protected String mOwnerAccountId;
    protected final String mBaseUrl;

    private final AferoClientAPI mAferoService;

    private AccessToken mAccessToken;
    private final Object mTokenRefreshLock = new Object();
    private BehaviorSubject<AccessToken> mTokenSubject;
    private PublishSubject<String> mActiveAccountSubject = PublishSubject.create();


    public AferoClientRetrofit2(String baseUrl, HttpLoggingInterceptor.Level logLevel, int defaultTimeout) {
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

    public Observable<AccessToken> getAccessToken(String user, String password, String grantType) {
        return mAferoService.getAccessToken(user, password, grantType);
    }

    public AccessToken refreshAccessToken(String refreshToken, String grantType) throws IOException {
        Response<AccessToken> response = mAferoService
                .refreshAccessToken(refreshToken, grantType)
                .execute();
        return response.isSuccessful() ? response.body() : null;
    }

    public Observable<Void> resetPassword(String email) {
        return mAferoService.resetPassword(email, "");
    }

    public Observable<UserDetails> usersMe() {
        return mAferoService.usersMe();
    }

    @Override
    public String getActiveAccountId() {
        return mActiveAccountId;
    }

    @Override
    public Observable<DeviceAssociateResponse> deviceAssociate(String associationId, boolean isOwnershipVerified, String locale, ImageSize imageSize) {
        DeviceAssociateBody body = new DeviceAssociateBody(associationId);
        return isOwnershipVerified ? mAferoService.deviceAssociateVerified(mActiveAccountId, body, locale, imageSize.toImageSizeSpecifier())
                : mAferoService.deviceAssociate(mActiveAccountId, body, locale, imageSize.toImageSizeSpecifier());
    }

    @Override
    public Observable<DeviceModel> deviceDisassociate(DeviceModel deviceModel) {
        return mAferoService.deviceDisassociate(mActiveAccountId, deviceModel.getId())
            .map(new RxUtils.Mapper<Void, DeviceModel>(deviceModel));
    }

    @Override
    public Observable<ActionResponse> postAttributeWrite(DeviceModel deviceModel, PostActionBody body, int retryCount, int statusCode) {
        Observable<ActionResponse> observable = mAferoService.postAction(mActiveAccountId, deviceModel.getId(), body);
        return retryCount > 0 ? observable.retryWhen(new RetryOnError(retryCount, statusCode)) : observable;
    }

    @Override
    public Observable<RequestResponse[]> postBatchAttributeWrite(DeviceModel deviceModel, DeviceRequest[] body, int retryCount, int statusCode) {
        Observable<RequestResponse[]> observable = mAferoService.postDeviceRequest(mActiveAccountId, deviceModel.getId(), body);
        return retryCount > 0 ? observable.retryWhen(new RetryOnError(retryCount, statusCode)) : observable;
    }

    @Override
    public Observable<DeviceProfile[]> getAccountDeviceProfiles(String locale, ImageSize imageSize) {
        return mAferoService.deviceProfiles(mActiveAccountId, locale, imageSize.toImageSizeSpecifier());
    }

    @Override
    public Observable<DeviceProfile> getDeviceProfile(String profileId, String locale, ImageSize imageSize) {
        return mAferoService.deviceProfiles(mActiveAccountId, profileId, locale, imageSize.toImageSizeSpecifier());
    }

    @Override
    public Observable<Location> putDeviceLocation(String deviceId, Location location) {
        return mAferoService.putDeviceLocation(mActiveAccountId, deviceId, location)
            .map(new RxUtils.Mapper<Void, Location>(location));
    }

    @Override
    public Observable<Location> getDeviceLocation(DeviceModel deviceModel) {
        return mAferoService.getDeviceLocation(mActiveAccountId, deviceModel.getId());
    }

    @Override
    public Observable<ConclaveAccessDetails> postConclaveAccess(String mobileClientId) {
        return mAferoService.postConclaveAccess(mActiveAccountId, mobileClientId, new ConclaveAccessBody());
    }

    @Override
    public int getStatusCode(Throwable t) {
        return getStatusCodeInternal(t);
    }

    public static int getStatusCodeInternal(Throwable t) {

        if (t instanceof HttpException) {
            return ((HttpException)t).code();
        }

        return 0;
    }

    @Override
    public boolean isTransferVerificationError(Throwable t) {
        try {
            if (t instanceof HttpException) {
                HttpException e = (HttpException)t;
                return (e.code() == HttpURLConnection.HTTP_FORBIDDEN &&
                        "true".equalsIgnoreCase(e.response().headers().get("transfer-verification-enabled")));
            }
        } catch (Throwable ignore) {} // belt & suspenders

        return false;
    }

    public Observable<Response<Void>> postDeviceInfo(String userId, DeviceInfoBody body) {
        return mAferoService.postDeviceInfo(userId, body);
    }

    public Observable<Void> deleteDeviceInfo(String userId, String mobileDeviceId) {
        return mAferoService.deleteDeviceInfo(userId, mobileDeviceId);
    }

    public void signOut(String userId, String mobileClientId) {
        rx.Observable<Void> observable;

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
        }).subscribe(new RxUtils.IgnoreResponseObserver<Void>());
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
                    try {
                        AccessToken newToken = refreshAccessToken(currentToken.refreshToken, GRANT_TYPE_REFRESH_TOKEN);
                        if (newToken != null) {
                            mAccessToken = newToken;
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

    public static class RetryOnError implements Func1<Observable<? extends Throwable>, Observable<?>> {

        private int mMaxRetryCount;
        private int mRetryOnStatus;

        public RetryOnError(int maxRetryCount, int retryOnStatus) {
            mMaxRetryCount = maxRetryCount;
            mRetryOnStatus = retryOnStatus;
        }

        public RetryOnError(int maxRetryCount) {
            this(maxRetryCount, 0);
        }

        @Override
        public Observable<?> call(Observable<? extends Throwable> observable) {
            return observable.zipWith(Observable.range(1, mMaxRetryCount), new Func2<Throwable, Integer, Retry>() {
                @Override
                public Retry call(Throwable throwable, Integer retry) {
                    return new Retry(retry, throwable);
                }
            })
            .flatMap(new Func1<Retry, Observable<?>>() {
                @Override
                public Observable<?> call(Retry retry) {
                    if (retry.throwable instanceof HttpException) {
                        int status = ((HttpException) retry.throwable).code();

                        AfLog.e("RetryOnError: retry=" + retry.retryCount + " '" + retry.throwable.getMessage() + "' status=" + status);

                        if (mRetryOnStatus == 0 || mRetryOnStatus == status) {
                            return Observable.timer(retry.retryCount, TimeUnit.SECONDS);
                        }
                    }

                    return Observable.error(retry.throwable);
                }
            });
        }
    }

    private static class Retry {
        int retryCount;
        Throwable throwable;

        Retry(int r, Throwable t) {
            retryCount = r;
            throwable = t;
        }
    }
}
