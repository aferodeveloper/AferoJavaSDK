package io.afero.aferolab;

import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.GrantTypeValues;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.afero.aferolab.helper.PrefsHelper;
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
import rx.subjects.AsyncSubject;
import rx.subjects.BehaviorSubject;




public class LabClientHelper implements Interceptor, Authenticator {
    public static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
    private static final String HEADER_AUTHORIZATION = "Authorization";

    public OkHttpClient sClient;
    private AccessToken mAccessToken;


    private AuthorizationService mAuthService;
    public AuthorizationServiceConfiguration mServiceConfig;

    private final Object mTokenRefreshLock = new Object();
    private BehaviorSubject<AccessToken> mTokenSubject;


    LabClientHelper(HttpLoggingInterceptor.Level logLevel, int defaultTimeout, AuthorizationService authService) {
        mAuthService = authService;
        mServiceConfig = new AuthorizationServiceConfiguration(
                Uri.parse(BuildConfig.AFERO_OAUTH_AUTH_URL), // authorization endpoint
                Uri.parse(BuildConfig.AFERO_OAUTH_TOKEN_URL));
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
        // Check we have a refresh token
        if (mAccessToken == null || mAccessToken.refreshToken == null) {
            AfLog.d("authenticate: No refresh token, bailing" );
            return null;
        }


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
                AfLog.d("authenticate: Attempting to refresh token");

                // Get a new token
                try {
                    TokenRequest tokenRequest = new TokenRequest.Builder(
                            mServiceConfig,
                            BuildConfig.AFERO_CLIENT_ID
                    )
                            .setGrantType(GrantTypeValues.REFRESH_TOKEN)
                            .setRefreshToken(mAccessToken.refreshToken)
                            .build();

                    BehaviorSubject<AccessToken> pSubject = BehaviorSubject.create();

                    mAuthService.performTokenRequest(tokenRequest, new AuthorizationService.TokenResponseCallback() {
                        @Override
                        public void onTokenRequestCompleted(@Nullable TokenResponse response, @Nullable AuthorizationException ex) {
                            if (ex != null) {
                                AfLog.d("authenticate: onTokenRequestCompleted refresh token " + mAccessToken.refreshToken);

                                AfLog.d("authenticate: onTokenRequestCompleted exception " + ex);
                                pSubject.onError(ex);
                                return;
                            }

                            if (response != null) {

                                AccessToken accessToken = new AccessToken(response.accessToken, response.refreshToken);
                                accessToken.tokenType = response.tokenType;
                                accessToken.expiresIn = response.accessTokenExpirationTime;
                                accessToken.scope = response.scope;
                                pSubject.onNext(accessToken);

                                if (mTokenSubject != null) {
                                    mTokenSubject.onNext(accessToken);
                                }
                            } else {
                                AfLog.d("authenticate: oh o, no response");
                                mTokenSubject.onNext(null);

                            }
                        }
                    });
                    mAccessToken = pSubject.toBlocking().first();

                    // Add new header to rejected request and retry it
                    return response.request().newBuilder()
                            .header(HEADER_AUTHORIZATION, mAccessToken.tokenType + " " + mAccessToken.accessToken)
                            .build();

                } catch (Exception e) {
                    AfLog.d("authenticate: Failed to refresh token: " + e.toString());
                    mAccessToken = null;
                    notifyTokenException(e);
                }
            }
        }

        return null;
    }

    public void signOut() {
        mAccessToken = null;
        if (mTokenSubject == null) {
            mTokenSubject.onNext(null);
        }
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


    private static int responseCount(okhttp3.Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }
}
