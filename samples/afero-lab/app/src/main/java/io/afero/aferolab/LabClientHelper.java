package io.afero.aferolab;

import android.net.Uri;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.openid.appauth.AuthorizationServiceConfiguration;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.afero.sdk.client.retrofit2.models.AccessToken;
import io.afero.sdk.log.AfLog;
import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import rx.Observable;
import rx.subjects.BehaviorSubject;

public class LabClientHelper implements Interceptor, Authenticator {
    public static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
    private static final String HEADER_AUTHORIZATION = "Authorization";

    public OkHttpClient sClient;
    private AccessToken mAccessToken;

    private  final Object mTokenRefreshLock = new Object();
    private BehaviorSubject<AccessToken> mTokenSubject;


    LabClientHelper(HttpLoggingInterceptor.Level logLevel, int defaultTimeout) {
        sClient = new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                    @Override
                    public void log(String message) {
                        AfLog.d(message);
                    }
                }).setLevel(logLevel))
                .addInterceptor(this)
                .authenticator(this)
                .connectTimeout(defaultTimeout, TimeUnit.SECONDS)
                .readTimeout(defaultTimeout, TimeUnit.SECONDS)
                .writeTimeout(defaultTimeout, TimeUnit.SECONDS)
                .build();
    }

    OkHttpClient getClient() {
        return sClient;
    }


    void setToken(AccessToken token) {
        mAccessToken = token;
    }
    AccessToken getToken() {
        return mAccessToken;
    }

    static AuthorizationServiceConfiguration mServiceConfig =
            new AuthorizationServiceConfiguration(
            Uri.parse(BuildConfig.AFERO_OAUTH_AUTH_URL), // authorization endpoint
                        Uri.parse(BuildConfig.AFERO_OAUTH_TOKEN_URL));



    @Override
    public okhttp3.Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        AfLog.d("LAB Client : setting access token");

        Request.Builder builder = request.newBuilder();
        if (request.header(HEADER_AUTHORIZATION) == null && mAccessToken != null) {
            String type = mAccessToken.tokenType != null ? mAccessToken.tokenType : "Bearer";
            String auth = type + " " + mAccessToken.accessToken;
            builder.header(HEADER_AUTHORIZATION, auth);
        }

        return chain.proceed(builder.build());
    }

    // implements Authenticator
    @Override
    public Request authenticate(Route route, okhttp3.Response response) throws IOException {
        AfLog.d("#### AferoClient: Attempting to refresh token  " + (mAccessToken == null));

        if (responseCount(response) >= 2) {
            synchronized (mTokenRefreshLock) {
                mAccessToken = null;
                notifyTokenException(new Exception("RefreshTokenAuthenticator: Too many retries"));
            }

            // If both the original call and the call with refreshed token failed,
            // it will probably keep failing, so don't try again.
            return null;
        }

        synchronized (mTokenRefreshLock) { // prevent concurrent token refreshes

            if (mAccessToken != null && mAccessToken.refreshToken != null) {
                AfLog.d("AferoClient: Attempting to refresh token");

                // Get a new token
                try {
                    mAccessToken = internalRefreshAccessToken(mAccessToken.refreshToken, GRANT_TYPE_REFRESH_TOKEN);
                    if (mTokenSubject != null) {
                        mTokenSubject.onNext(mAccessToken);
                    }

                    // Add new header to rejected request and retry it
                    return response.request().newBuilder()
                            .header(HEADER_AUTHORIZATION, mAccessToken.tokenType + " " + mAccessToken.accessToken)
                            .build();

                } catch (Exception e) {
                    AfLog.d("AferoClient: Failed to refresh token: " + e.toString());
                    mAccessToken = null;
                    notifyTokenException(e);
                }
            }
        }

        return null;
    }

    public Observable<AccessToken> tokenRefreshObservable() {
        if (mTokenSubject == null) {
            mTokenSubject = BehaviorSubject.create();
        }
        return mTokenSubject;
    }

    private void notifyTokenException(Exception e) {
        BehaviorSubject<AccessToken> tokenSubject = mTokenSubject;
        if (tokenSubject != null && !(tokenSubject.hasThrowable() || tokenSubject.hasCompleted())) {
            mTokenSubject = null;
            tokenSubject.onError(e);
        }
    }

    protected AccessToken internalRefreshAccessToken(String refreshToken, String grantType) throws IOException {
        AfLog.d( "Refresh with access token" );
        Request request = new Request.Builder().url(mServiceConfig.tokenEndpoint.toString()).post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"),
                "grant_type=refresh_token&refresh_token=" + refreshToken + "&client_id=" + BuildConfig.AFERO_CLIENT_ID
        )).build();
        okhttp3.Response response = sClient.newCall(request).execute();
        String result = response.body().string();
        ObjectMapper objectMapper = new ObjectMapper();
        AccessToken accessToken = objectMapper.readValue(result, AccessToken.class);

        return accessToken;
    }

    private static int responseCount(okhttp3.Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }


}
