/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.retrofit2;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import io.afero.sdk.client.afero.AferoClient;
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
import io.afero.sdk.client.afero.models.ErrorBody;
import io.afero.sdk.client.afero.models.InvitationDetails;
import io.afero.sdk.client.afero.models.Location;
import io.afero.sdk.client.afero.models.PostActionBody;
import io.afero.sdk.client.afero.models.ResetPasswordBody;
import io.afero.sdk.client.afero.models.RuleExecuteBody;
import io.afero.sdk.client.afero.models.ViewRequest;
import io.afero.sdk.client.afero.models.ViewResponse;
import io.afero.sdk.client.afero.models.WriteRequest;
import io.afero.sdk.client.afero.models.WriteResponse;
import io.afero.sdk.client.retrofit2.api.AferoClientAPI;
import io.afero.sdk.client.retrofit2.models.AccessToken;
import io.afero.sdk.client.retrofit2.models.DeviceInfoBody;
import io.afero.sdk.client.retrofit2.models.DeviceTimeZoneResponse;
import io.afero.sdk.client.retrofit2.models.DeviceTimezone;
import io.afero.sdk.client.retrofit2.models.NameDeviceBody;
import io.afero.sdk.client.retrofit2.models.UserDetails;
import io.afero.sdk.conclave.DeviceEventSource;
import io.afero.sdk.conclave.models.DeviceSync;
import io.afero.sdk.device.DeviceCollection;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceProfile;
import io.afero.sdk.log.AfLog;
import io.afero.sdk.utils.JSONUtils;
import io.afero.sdk.utils.RxUtils;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.ByteString;
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

import static okhttp3.OkHttpClient.Builder;

/**
 * Concrete implementation of AferoClient that provides access to the Afero Cloud API.
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
public class AferoClientRetrofit2 implements AferoClient {

    public static final String GRANT_TYPE_PASSWORD = "password";
    public static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";

    private static final String AFERO_BASE_URL = "https://api.afero.io";

    private static final String HEADER_AUTHORIZATION = "Authorization";

    private Config mConfig = new Config();
    private final OkHttpClient mHttpClient;
    private final AferoClientAPI mAferoService;
    private final String mOAuthAuthorizationBase64;

    private String mActiveAccountId;
    private String mOwnerAccountId;

    private AccessToken mAccessToken;
    private final Object mTokenRefreshLock = new Object();
    private BehaviorSubject<AccessToken> mTokenSubject;
    private PublishSubject<String> mActiveAccountSubject = PublishSubject.create();

    /**
     * Config class used to initialize various parameters of the Retrofit2AferoClient instance.
     * Must be constructed using a ConfigBuilder.
     * @see ConfigBuilder
     */
    public static final class Config {

        private String baseUrl = AFERO_BASE_URL;
        private String oauthClientId;
        private String oauthClientSecret;
        private HttpLoggingInterceptor.Level httpLogLevel = HttpLoggingInterceptor.Level.NONE;
        private int defaultTimeout = 60;
        private ImageScale imageScale = ImageScale.SCALE_DEFAULT;

        private Config() {}

        private Config validate() {
            RuntimeException e = null;

            if (baseUrl == null || baseUrl.isEmpty()) {
                e = new IllegalArgumentException("baseUrl must be specified");
            }

            if (oauthClientId == null || oauthClientId.isEmpty()) {
                e = new IllegalArgumentException("oauthClientId must be specified");
            }

            if (httpLogLevel == null) {
                e = new IllegalArgumentException("httpLogLevel cannot be null");
            }

            if (defaultTimeout < 1) {
                e = new IllegalArgumentException("defaultTimeout is too short");
            }

            if (imageScale == null) {
                e = new IllegalArgumentException("imageScale cannot be null");
            }

            if (e != null) {
                throw e;
            }

            return this;
        }
    }

    /**
     * Used to create and set up a Config
     * @see Config
     */
    public static final class ConfigBuilder {
        private final Config config = new Config();

        public ConfigBuilder() {}

        /**
         * Sets the OAuth client ID.
         *
         * @param oauthClientId OAuth client id to be used during client authenticaton
         * @return this ConfigBuilder instance
         */
        public ConfigBuilder oauthClientId(String oauthClientId) {
            config.oauthClientId = oauthClientId;
            return this;
        }

        /**
         * Sets the OAuth client secret.
         *
         * @param oauthClientSecret OAuth client secret to be used for client authenticaton
         * @return this ConfigBuilder instance
         */
        public ConfigBuilder oauthClientSecret(String oauthClientSecret) {
            config.oauthClientSecret = oauthClientSecret;
            return this;
        }

        /**
         * Sets base host url for the Afero service. For Afero internal use only.
         *
         * @param url absolute URL specifying the Afero host to be passed to
         *            {@link Retrofit.Builder#baseUrl(String)}
         * @return this ConfigBuilder instance
         */
        public ConfigBuilder baseUrl(String url) {
            config.baseUrl = url;
            return this;
        }

        /**
         * Sets the level of logging output that will be generated by {@link OkHttpClient}.
         *
         * @param logLevel HttpLoggingInterceptor.Level which will be passed to
         *                {@link HttpLoggingInterceptor#setLevel}
         * @return this ConfigBuilder instance
         */
        public ConfigBuilder logLevel(HttpLoggingInterceptor.Level logLevel) {
            config.httpLogLevel = logLevel;
            return this;
        }

        /**
         * Sets the timeout for use by {@link Builder#connectTimeout(long, TimeUnit)},
         * {@link Builder#readTimeout(long, TimeUnit)}, and {@link Builder#writeTimeout(long, TimeUnit)}.
         *
         * @param timeoutSeconds number of seconds until timeout is triggered
         * @return this {@code ConfigBuilder} instance
         */
        public ConfigBuilder defaultTimeout(int timeoutSeconds) {
            config.defaultTimeout = timeoutSeconds;
            return this;
        }

        /**
         * Sets the image scale parameter used by the Afero Cloud for any image URLs that will be
         * included in a {@link io.afero.sdk.device.DeviceProfile.Presentation}.
         *
         * @param scale ImageScale value used for {@link AferoClientAPI#deviceProfiles}
         * @return this ConfigBuilder instance
         */
        public ConfigBuilder imageScale(ImageScale scale) {
            config.imageScale = scale;
            return this;
        }

        /**
         * Constructs and validates a {@link Config}.
         *
         * @return validated {@link Config} instance
         * @throws IllegalArgumentException if any parameter specified in this ConfigBuilder is invalid
         */
        public Config build() {
            return config.validate();
        }
    }

    /**
     * Sole constructor for AferoClientRetrofit2
     *
     * @param config Config
     */
    public AferoClientRetrofit2(Config config, OkHttpClient client) {
        mConfig = config;
        mHttpClient = client != null ? client : createHttpClient(config.httpLogLevel, config.defaultTimeout);
        mAferoService = createRetrofit().create(AferoClientAPI.class);
        mOAuthAuthorizationBase64 = config.oauthClientSecret != null ? Credentials.basic(config.oauthClientId, config.oauthClientSecret) : "";
    }

    // don't use me
    private AferoClientRetrofit2() {
        mConfig = null;
        mHttpClient = null;
        mAferoService = null;
        mOAuthAuthorizationBase64 = "";
    }

    /**
     * Sets both the owner and currently active account IDs of the currently authenticated account.
     * The active account ID is used in all subsequent Afero Cloud API calls that require an account ID.
     *
     * @param accountId ID of the authenticated Afero account returned from {@link #usersMe}
     */
    public void setOwnerAndActiveAccountId(String accountId) {
        mOwnerAccountId = accountId;
        mActiveAccountId = accountId;
    }

    /**
     * Gets the owner account ID of the currently authenticated Afero account. This ID will have been
     * typically the one returned from {@link #usersMe}
     *
     * @return ID of an authenticated Afero account
     */
    public String getOwnerAccountId() {
        return mOwnerAccountId;
    }

    /**
     * The account ID to be used in subsequent Afero Cloud API calls that require an account ID.
     * This can be either the owner account returned by {@link #usersMe} or a shared account
     * returned from the Afero Cloud API (accounts/{accountId}/accountUserSummary).
     *
     * @param accountId ID of an authenticated Afero account
     */
    public void setActiveAccountId(String accountId) {
        mActiveAccountId = accountId;
        mActiveAccountSubject.onNext(mActiveAccountId);
    }

    /**
     * @return ID previously set by {@link #setActiveAccountId} or {@link #setOwnerAndActiveAccountId}
     */
    @Override
    public String getActiveAccountId() {
        return mActiveAccountId;
    }

    /**
     * @return true if the owner account is also the currently active one
     */
    public boolean isOwnerAccountActive() {
        return mOwnerAccountId.equals(mActiveAccountId);
    }

    /**
     * Clears both the owner account and the active account. Typically done on user signout.
     */
    public void clearAccount() {
        mOwnerAccountId = null;
        mActiveAccountId = null;
    }

    /**
     * Observes active account changes triggered by calls to {@link #setActiveAccountId(String)}.
     *
     * <p>
     * Example:
     * <pre><code>
     *     aferoClient.observeActiveAccount().subscribe(new Action1&lt;String&gt;() {
     *         &#64;Override
     *         public void call(String accountId) {
     *              // new accountId has been set
     *         });
     * </code></pre>
     * </p>
     *
     * @return {@link Observable} that emits the new account ID in {@link rx.Observer#onNext}.
     */
    public Observable<String> observeActiveAccount() {
        return mActiveAccountSubject;
    }

    /**
     * Create a new Afero Cloud user account.
     *
     * @param body parameters for the new account including first/last name and password
     * @return {@link CreateAccountResponse} that includes the accountId
     */
    @Override
    public Observable<CreateAccountResponse> createAccount(CreateAccountBody body) {
        return mAferoService.createAccount(body);
    }

    /**
     * Create a new Afero Cloud user account.
     *
     * @param body parameters for the new account including first/last name and password
     * @return {@link CreateAccountResponse} that includes the accountId
     */
    @Override
    public Observable<CreateAccountResponse> createAccount(CreateAccountBody body, String appId, String platform) {
        String appIdAndPlatform = appId + ":" + platform.toUpperCase();
        byte[] bytes = appIdAndPlatform.getBytes(StandardCharsets.UTF_8);
        String appIdAndPlatformBase64Encoded = ByteString.of(bytes).base64();

        return mAferoService.createAccount(body, appIdAndPlatformBase64Encoded);
    }

    @Override
    public Observable<AccountDescriptionBody> putAccountDescription(String accountId, AccountDescriptionBody body) {
        return mAferoService.putAccountDescription(accountId, body);
    }

    /**
     * Initiates OAuth2 authentication with the specified user and password.
     *
     * <p>
     * Example:
     * <pre><code>
     *     aferoClient.getAccessToken(userId, password, AferoClientRetrofit2.GRANT_TYPE_PASSWORD)
     *         .subscribe(new Observer&lt;AccessToken&gt;() {
     *              &#64;Override
     *              public void onNext(AccessToken accessToken) {
     *                   // token received, user is now authenticated
     *                   aferoClient.setToken(accessToken);
     *                   // we should now call aferoClient.usersMe() to get the account ID
     *              }
     *
     *              &#64;Override
     *              public void onError(Throwable t) {
     *                   // call failed, log the error
     *                   AfLog.e(t);
     *              }
     *
     *              &#64;Override
     *              public void onCompleted() {
     *                  // call completed with no errors
     *              }
     *         });
     * </code></pre>
     * </p>
     *
     * @param user identifier for the user account - typically an email address
     * @param password user's password
     * @return {@link Observable} that emits an {@link AccessToken} in {@link rx.Observer#onNext}.
     */
    public Observable<AccessToken> getAccessToken(String user, String password) {
        return mAferoService.getAccessToken(AferoClientRetrofit2.GRANT_TYPE_PASSWORD, user, password, mOAuthAuthorizationBase64);
    }

    /**
     * Afero Cloud API call to trigger a password reset email to be sent to the
     * specified email address.
     *
     * @param email address to which the reset email will be sent
     * @return {@link Observable} that initiates the transaction when subscribed
     */
    public Observable<Void> resetPassword(String email) {
        return mAferoService.resetPassword(email, "", mOAuthAuthorizationBase64);
    }

    /**
     * Afero Cloud API call to reset the account password.
     *
     * @param resetCode code provided via password reset email sent via {@link #sendPasswordRecoveryEmail(String, String, String)}
     * @param newPassword new password to be used for account authentication
     * @return {@link Observable} that initiates the transaction when subscribed
     */
    @Override
    public Observable<Void> resetPasswordWithCode(String resetCode, String newPassword) {
        return mAferoService.resetPasswordWithCode(resetCode, new ResetPasswordBody(newPassword));
    }

    /**
     * Afero Cloud API call to send the user a password recovery email with reset code.
     *
     * @param appId unique platform identifier for the calling application which will used to select the appropriate email template
     * @return {@link Observable} that initiates the transaction when subscribed
     */
    @Override
    public Observable<Void> sendPasswordRecoveryEmail(String email, String appId, String platform) {
        String appIdAndPlatform = appId + ":" + platform.toUpperCase();
        byte[] bytes = appIdAndPlatform.getBytes(StandardCharsets.UTF_8);
        String appIdAndPlatformBase64Encoded = ByteString.of(bytes).base64();

        return mAferoService.sendPasswordRecoveryEmail(email, appIdAndPlatformBase64Encoded);
    }

    /**
     * Afero Cloud API call to resend an account verification email.
     *
     * @param email email address to which the account verification email will be sent
     * @param appId unique platform identifier for the calling application which will used to select the appropriate email template
     * @return {@link Observable} that initiates the transaction when subscribed
     */
    @Override
    public Observable<Void> resendVerificationEmail(String email, String appId) {
        byte[] bytes = appId.getBytes(StandardCharsets.UTF_8);
        String appIdBase64Encoded = ByteString.of(bytes).base64();

        return mAferoService.resendVerificationEmail(email, appIdBase64Encoded);
    }

    /**
     * Afero Cloud API call to fetch the {@link UserDetails} which includes the
     * account ID used in {@link #setOwnerAndActiveAccountId(String)}
     *
     * @return {@link Observable} that emits {@link UserDetails} in {@link rx.Observer#onNext}.
     */
    public Observable<UserDetails> usersMe() {
        return mAferoService.usersMe();
    }

    /**
     * <p><b>For internal use only. Use {@link DeviceCollection#addDevice} instead.</b></p>
     *
     * Afero Cloud API call that adds the specified device to the owner account,
     * and returns the {@link DeviceProfile} in the response.
     *
     * @param associationId ID used for association of a specific device instance, typically acquired via QR code scan
     * @return {@link Observable} that emits {@link DeviceAssociateResponse} in {@link rx.Observer#onNext}.
     */
    @Override
    public Observable<DeviceAssociateResponse> deviceAssociateGetProfile(String associationId, boolean isOwnershipVerified) {
        DeviceAssociateBody body = new DeviceAssociateBody(associationId);
        return isOwnershipVerified ? mAferoService.deviceAssociateVerified(mOwnerAccountId, body, getLocale(), mConfig.imageScale.toImageSizeSpecifier())
                : mAferoService.deviceAssociateGetProfile(mActiveAccountId, body, getLocale(), mConfig.imageScale.toImageSizeSpecifier());
    }

    /**
     * <p><b>For internal use only. Use {@link DeviceCollection#addDevice} instead.</b></p>
     *
     * Afero Cloud API call to add the specified device to the owner account.
     *
     * @param associationId ID used for association of a specific device instance, typically acquired via QR code scan
     * @return {@link Observable} that emits {@link DeviceAssociateResponse} in {@link rx.Observer#onNext}.
     */
    @Override
    public Observable<DeviceAssociateResponse> deviceAssociate(String associationId) {
        DeviceAssociateBody body = new DeviceAssociateBody(associationId);
        return mAferoService.deviceAssociate(mOwnerAccountId, body);
    }

    /**
     * <p><b>For internal use only. Use {@link DeviceCollection#removeDevice} instead.</b></p>
     *
     * Afero Cloud API call to remove the specifed device from the owner account.
     *
     * @param deviceModel {@link DeviceModel} to be disassociated
     * @return {@link Observable} that emits disassociated DeviceModel in {@link rx.Observer#onNext}.
     */
    @Override
    public Observable<DeviceModel> deviceDisassociate(DeviceModel deviceModel) {
        return mAferoService.deviceDisassociate(mOwnerAccountId, deviceModel.getId())
            .map(new RxUtils.Mapper<Void, DeviceModel>(deviceModel));
    }

    public Observable<NameDeviceBody> putFriendlyName(String deviceId, String name) {
        return mAferoService.putFriendlyName(getActiveAccountId(), deviceId, new NameDeviceBody(name));
    }

    public Observable<DeviceVersions> getDeviceVersions(String deviceId) {
        return mAferoService.getDeviceVersions(getActiveAccountId(), deviceId);
    }

    /**
     * Afero Cloud API call to set the timezone in which the specified device resides.
     *
     * @param deviceModel {@link DeviceModel} upon which the timezone will be set
     * @param tz {@link TimeZone}
     * @return {@link Observable} that initiates the call on subscribe
     */
    @Override
    public Observable<Void> putDeviceTimeZone(DeviceModel deviceModel, TimeZone tz) {
        return mAferoService.putDeviceTimezone(mActiveAccountId, deviceModel.getId(), new DeviceTimezone(tz.getID()));
    }

    /**
     * Afero Cloud API call to fetch the device's {@link TimeZone}, if any. If the device has no TimeZone set
     * the Observable will be empty and will simply complete with no error.
     *
     * @param deviceModel {@link DeviceModel}
     * @return {@link Observable} that emits the device TimeZone.
     * @see TimeZone#getTimeZone
     */
    @Override
    public Observable<TimeZone> getDeviceTimeZone(DeviceModel deviceModel) {
        return mAferoService.getDeviceTimezone(mActiveAccountId, deviceModel.getId())
                .flatMap(new Func1<DeviceTimeZoneResponse, Observable<TimeZone>>() {
                    @Override
                    public Observable<TimeZone> call(DeviceTimeZoneResponse timeZoneResponse) {
                        TimeZone tz = timeZoneResponse.timezone != null && !timeZoneResponse.timezone.isEmpty()
                                ? TimeZone.getTimeZone(timeZoneResponse.timezone)
                                : null;
                        return tz != null ? Observable.just(tz) : Observable.<TimeZone>empty();
                    }
                });
    }

    /**
     * Afero Cloud API call to get a snapshot of the devices (and their state) associated with the
     * active account.
     *
     * @return {@link Observable} that initiates the call on subscribe
     */
    @Override
    public Observable<DeviceSync[]> getDevicesWithState() {
        return mAferoService.getDevicesWithState(mActiveAccountId);
    }

    /**
     * <p><b>For internal use only. Use {@link DeviceModel#writeAttributes()} instead.</b></p>
     *
     * @param deviceModel {@link DeviceModel} upon which attribute will be written
     * @param body Array of {@link WriteRequest} containing the attribute values to be written
     * @param maxRetryCount maximum number of retry attempts
     * @param statusCode http status code that will trigger a retry
     * @return {@link Observable} that emits {@link ActionResponse} in {@link rx.Observer#onNext}.
     */
    @Override
    public Observable<ActionResponse> postAttributeWrite(DeviceModel deviceModel, PostActionBody body, int maxRetryCount, int statusCode) {
        Observable<ActionResponse> observable = mAferoService.postAction(mActiveAccountId, deviceModel.getId(), body);
        return maxRetryCount > 0 ? observable.retryWhen(new RetryOnError(maxRetryCount, statusCode)) : observable;
    }

    /**
     * <p><b>For internal use only. Use {@link DeviceModel#writeAttributes()} instead.</b></p>
     *
     * @param deviceModel {@link DeviceModel} upon which attributes will be written
     * @param body Array of {@link WriteRequest} containing the attribute values to be written
     * @param maxRetryCount maximum number of retry attempts
     * @param statusCode http status code that will trigger a retry
     * @return {@link Observable} that emits {@link WriteResponse} array in {@link rx.Observer#onNext}.
     */
    @Override
    public Observable<WriteResponse[]> postBatchAttributeWrite(DeviceModel deviceModel, WriteRequest[] body, int maxRetryCount, int statusCode) {
        Observable<WriteResponse[]> observable = mAferoService.postDeviceRequest(mActiveAccountId, deviceModel.getId(), body);
        return maxRetryCount > 0 ? observable.retryWhen(new RetryOnError(maxRetryCount, statusCode)) : observable;
    }

    /**
     * Request to notify the specified device that it is currently being "viewed" by the user.
     * Depending on the device this can potentially result in quicker attribute updates.
     *
     * @param deviceModel {@link DeviceModel} that is being viewed
     * @param body {@link ViewRequest} that specifies whether to start or stop viewing
     * @return empty {@link ViewResponse}
     */
    @Override
    public Observable<ViewResponse[]> postDeviceViewRequest(DeviceModel deviceModel, ViewRequest body) {
        ViewRequest[] requests = { body };
        return mAferoService.postDeviceViewRequest(mActiveAccountId, deviceModel.getId(), requests);
    }

    /**
     * Afero Cloud API to fetch all {@link DeviceProfile} object for the devices associated with
     * the active account.
     *
     * @return {@link Observable} that emits {@link DeviceProfile} array in {@link rx.Observer#onNext}.
     */
    @Override
    public Observable<DeviceProfile[]> getAccountDeviceProfiles() {
        return mAferoService.deviceProfiles(mActiveAccountId, getLocale(), mConfig.imageScale.toImageSizeSpecifier());
    }

    /**
     * @param associationId Device association ID returned from AferoSofthub in {@link AferoSofthub.SetupModeDeviceInfo}
     * @param version DeviceProfile version number returned from AferoSofthub in {@link AferoSofthub.SetupModeDeviceInfo}
     * @return {@link Observable} that emits a {@link DeviceProfile} in {@link rx.Observer#onNext}.
     */
    @Override
    public Observable<DeviceProfile> getDeviceProfilePreAssociation(String associationId, int version) {
        return mAferoService.deviceProfiles(associationId, version);
    }

    /**
     * <p><b>For internal use only. Use {@link DeviceCollection} instead to manage {@link DeviceModel} objects.</b></p>
     *
     * Afero Cloud API call to fetch the {@link DeviceProfile} attached to the specified device.
     *
     * @param profileId
     * @return {@link Observable} that emits {@link DeviceProfile} in {@link rx.Observer#onNext}.
     */
    @Override
    public Observable<DeviceProfile> getDeviceProfile(String profileId) {
        return mAferoService.deviceProfiles(mActiveAccountId, profileId, getLocale(), mConfig.imageScale.toImageSizeSpecifier());
    }

    /**
     * Afero Cloud API call to fetch the {@link Location} attached to the specified device
     *
     * @param deviceModel {@link DeviceModel} for which the Location will be retrieved
     * @return {@link Observable} that emits Location in {@link rx.Observer#onNext}.
     */
    @Override
    public Observable<Location> getDeviceLocation(DeviceModel deviceModel) {
        return mAferoService.getDeviceLocation(mActiveAccountId, deviceModel.getId());
    }

    /**
     * Afero Cloud API call to attach a {@link DeviceTag} to a device.
     *
     * @param deviceId String specifying the unique id of the device.
     * @param tagKey String containing the key for the tag.
     * @param tagValue String containing the value for the tag.
     * @return {@link Observable} that emits the DeviceTag if successful.
     */
    @Override
    public Observable<DeviceTag> postDeviceTag(String deviceId, String tagKey, String tagValue) {
        return mAferoService.postDeviceTag(mActiveAccountId, deviceId, new DeviceTag(tagKey, tagValue));
    }

    /**
     * Afero Cloud API call to update an existing {@link DeviceTag} on a device.
     *
     * @param deviceId String specifying the unique id of the device.
     * @param tag DeviceTag to to be updated.
     * @return {@link Observable} that emits the DeviceTag if successful.
     */
    @Override
    public Observable<DeviceTag> putDeviceTag(String deviceId, DeviceTag tag) {
        return mAferoService.putDeviceTag(mActiveAccountId, deviceId, tag);
    }

    /**
     * Afero Cloud API call to delete a {@link DeviceTag} from a device.
     *
     * @param deviceId String specifying the unique id of the device.
     * @param tagId String specifying the unique id of the tag.
     * @return {@link Observable} that emits nothing and completes on success.
     */
    @Override
    public Observable<Void> deleteDeviceTag(String deviceId, String tagId) {
        return mAferoService.deleteDeviceTag(mActiveAccountId, deviceId, tagId);
    }

    /**
     * <p><b>For internal use only. Use {@link DeviceCollection} and {@link DeviceEventSource} instead.</b></p>
     *
     * @return {@link Observable} that emits {@link ConclaveAccessDetails} in {@link rx.Observer#onNext}.
     */
    @Override
    public Observable<ConclaveAccessDetails> postConclaveAccess() {
        return mAferoService.postConclaveAccess(mActiveAccountId, new ConclaveAccessBody());
    }

    @Deprecated
    @Override
    public Observable<ConclaveAccessDetails> postConclaveAccess(String mobileClientId) {
        return mAferoService.postConclaveAccess(mActiveAccountId, new ConclaveAccessBody());
    }

    public Observable<DeviceInfoExtendedData> getDeviceInfo(String deviceId) {
        return mAferoService.getDeviceInfo(getActiveAccountId(), deviceId);
    }

    public Observable<ActionResponse[]> ruleExecuteActions(String ruleId, RuleExecuteBody body) {
        return mAferoService.ruleExecuteActions(getActiveAccountId(), ruleId, body);
    }
    public Observable<DeviceRules.Rule[]> getDeviceRules(String deviceId) {
        return mAferoService.getDeviceRules(getActiveAccountId(), deviceId);
    }

    public Observable<DeviceRules.Rule[]> getAccountRules() {
        return mAferoService.getAccountRules(getActiveAccountId());
    }

    public Observable<DeviceRules.Schedule> putSchedule(String scheduleId, DeviceRules.Schedule schedule) {
        return mAferoService.putSchedule(getActiveAccountId(), scheduleId, schedule);
    }

    public Observable<DeviceRules.Schedule> postSchedule(DeviceRules.Schedule schedule) {
        return mAferoService.postSchedule(getActiveAccountId(), schedule);
    }

    public Observable<DeviceRules.Rule> postRule(DeviceRules.Rule rule) {
        return mAferoService.postRule(getActiveAccountId(), rule);
    }

    public Observable<DeviceRules.Rule> putRule(String ruleId, DeviceRules.Rule rule) {
        return mAferoService.putRule(getActiveAccountId(), ruleId, rule);
    }

    public Observable<Void> deleteRule(String ruleId) {
        return mAferoService.deleteRule(getActiveAccountId(), ruleId);
    }

    public Observable<AccountUserSummary> getAccountUserSummary() {
        return mAferoService.getAccountUserSummary(getActiveAccountId());
    }

    public Observable<Void> postInvite(InvitationDetails invite) {
        return mAferoService.postInvite(getActiveAccountId(), invite);
    }

    public Observable<Void> deleteInvite(String invitationId) {
        return mAferoService.deleteInvite(getActiveAccountId(), invitationId);
    }

    public Observable<Void> deleteUser(String userId) {
        return mAferoService.deleteUser(getActiveAccountId(), userId);
    }

    /**
     * Attempts to extract to extract an http status code from the specified {@link Throwable}.
     * Handy for use when an Afero Cloud API {@link Observable} triggers {@link rx.Observer#onError}.
     *
     * @param t Throwable from which to extract the http status code
     * @return http status code
     */
    @Override
    public int getStatusCode(Throwable t) {
        return getStatusCodeInternal(t);
    }

    /**
     * Static version of {@link #getStatusCode}.
     *
     * @param t Throwable from which to extract the http status code
     * @return http status code
     */
    public static int getStatusCodeInternal(Throwable t) {

        if (t instanceof HttpException) {
            return ((HttpException)t).code();
        }

        return 0;
    }

    /**
     * Determines if the specified {@link Throwable} represents a transfer verification error,
     * which indicates the device is owned by another account but is eligible for transfer.
     *
     * @param t Throwable from which to extract the http status code
     * @return http status code
     */
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

    /**
     * Afero Cloud API call to attach the specified {@link Location} with the specified device.
     *
     * @param deviceId ID of the device to which the Location will be attached
     * @param location Location to attach to the device
     * @return {@link Observable} that emits the attached Location in {@link rx.Observer#onNext}.
     */
    @Override
    public Observable<Location> putDeviceLocation(String deviceId, Location location) {
        return mAferoService.putDeviceLocation(mActiveAccountId, deviceId, location)
                .map(new RxUtils.Mapper<Void, Location>(location));
    }

    /**
     * Afero Cloud API call that posts the specified {@link DeviceInfoBody} for the specified user.
     * The DeviceInfoBody most importantly includes a {@link DeviceInfoBody#pushId} for use with
     * push notification services, and a unique {@link DeviceInfoBody#mobileDeviceId} generated
     * by the app to identify the specific device on which the app is running.
     *
     * @param userId ID of the user returned via {@link #usersMe}
     * @param body {@link DeviceInfoBody} to be posted
     * @return {@link Observable} that emits an empty Response object in {@link rx.Observer#onNext}.
     */
    public Observable<Response<Void>> postDeviceInfo(String userId, DeviceInfoBody body) {
        return mAferoService.postDeviceInfo(userId, body);
    }

    /**
     * Afero Cloud API call to remove the {@link DeviceInfoBody} for the specified user.
     * Used internally by {@link #signOut}.
     *
     * @param userId ID of the user returned via {@link #usersMe}
     * @param mobileDeviceId unique device ID generated by the app
     * @return {@link Observable}
     */
    public Observable<Void> deleteDeviceInfo(String userId, String mobileDeviceId) {
        return mAferoService.deleteDeviceInfo(userId, mobileDeviceId);
    }

    /**
     * Signs out of the active account by calling {@link #deleteDeviceInfo}
     * clears the active {@link AccessToken}.
     *
     * @param userId ID of the user returned via {@link #usersMe}
     * @param mobileDeviceId unique device ID generated by the app
     */
    public void signOut(String userId, String mobileDeviceId) {
        rx.Observable<Void> observable;

        if (userId != null && (!userId.isEmpty()) && mobileDeviceId != null && (!mobileDeviceId.isEmpty())) {
            observable = deleteDeviceInfo(userId, mobileDeviceId);
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

    /**
     * Sets the active OAuth token
     *
     * @param token {@link AccessToken} to become active
     */
    public void setToken(AccessToken token) {
        mAccessToken = token;
    }

    /**
     * @return currently active {@link AccessToken} or null if not signed in
     */
    public AccessToken getToken() {
        return mAccessToken;
    }

    /**
     * Observes token refreshes that typically occur due to token expiration.
     *
     * @return {@link Observable} that emits the refreshed {@link AccessToken} in {@link rx.Observer#onNext}.
     */
    public Observable<AccessToken> tokenRefreshObservable() {
        if (mTokenSubject == null) {
            mTokenSubject = BehaviorSubject.create();
        }
        return mTokenSubject;
    }

    /**
     * Extracts more detailed error information from a Throwable returned by an Afero Cloud API call.
     *
     * @param t {@link Throwable} from which the ErrorBody will be derived
     * @return {@link ErrorBody} containing more details about the error, or null
     */
    public static ErrorBody getErrorBody(Throwable t) {
        try {
            if (t instanceof HttpException) {
                return JSONUtils.readValue(((HttpException)t).response().errorBody().bytes(), ErrorBody.class);
            }
        } catch (Exception ignore) {
            // ignore
        }
        return null;
    }

    /**
     * Getter for the base Afero host URL.
     *
     * @return the baseUrl specified in the {@link Config}
     */
    public String getBaseUrl() {
        return mConfig.baseUrl;
    }

    /**
     * Getter for the base Afero host URL.
     *
     * @return the baseUrl specified in the {@link Config}
     */
    protected String getLocale() {
        return Locale.getDefault().toString();
    }

    /**
     * Getter for the http client object.
     *
     * @return OkHttpClient used internally
     */
    protected OkHttpClient getHttpClient() {
        return mHttpClient;
    }

    protected Retrofit createRetrofit() {
        return new Retrofit.Builder()
            .client(getHttpClient())
            .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
            .addConverterFactory(JacksonConverterFactory.create(JSONUtils.getObjectMapper()))
            .baseUrl(getBaseUrl())
            .build();
    }

    private OkHttpClient createHttpClient(HttpLoggingInterceptor.Level logLevel, int defaultTimeout) {
        return AfHttpClient.create(logLevel, defaultTimeout).newBuilder()
            .addInterceptor(new AuthTokenInterceptor())
            .authenticator(new RefreshTokenAuthenticator())
            .build();
    }

    private class RefreshTokenAuthenticator implements Authenticator {
        @Override
        public Request authenticate(Route route, okhttp3.Response response) throws IOException {
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
                AccessToken currentToken = mAccessToken;

                if (currentToken != null && currentToken.refreshToken != null) {
                    AfLog.d("AferoClient: Attempting to refresh token");

                    // Get a new token
                    try {
                        mAccessToken = internalRefreshAccessToken(currentToken.refreshToken, GRANT_TYPE_REFRESH_TOKEN);
                        if (mTokenSubject != null) {
                            mTokenSubject.onNext(mAccessToken);
                        }

                        // Add new header to rejected request and retry it
                        return response.request().newBuilder()
                            .header(HEADER_AUTHORIZATION, mAccessToken.tokenType + " " + mAccessToken.accessToken)
                            .build();

                    } catch (Exception e) {
                        AfLog.d("AferoClient: Failed to refresh token");
                        mAccessToken = null;
                        notifyTokenException(e);
                    }
                }
            }

            return null;
        }
    }

    private void notifyTokenException(Exception e) {
        BehaviorSubject<AccessToken> tokenSubject = mTokenSubject;
        if (tokenSubject != null && !(tokenSubject.hasThrowable() || tokenSubject.hasCompleted())) {
            mTokenSubject = null;
            tokenSubject.onError(e);
        }
    }

    protected AccessToken internalRefreshAccessToken(String refreshToken, String grantType) throws IOException {
        Response<AccessToken> response = mAferoService
                .refreshAccessToken(grantType, refreshToken, mOAuthAuthorizationBase64)
                .execute();

        if (!response.isSuccessful()) {
            throw new HttpException(response);
        }

        if (response.body() == null) {
            throw new IOException("Null AccessToken in response");
        }

        return response.body();
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

    private static class RetryOnError implements Func1<Observable<? extends Throwable>, Observable<?>> {

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
