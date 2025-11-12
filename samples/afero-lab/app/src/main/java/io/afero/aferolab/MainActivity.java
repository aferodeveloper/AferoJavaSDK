/*
 * Copyright (c) 2014-2019 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ClientAuthentication;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;

import io.afero.aferolab.addDevice.AddDeviceView;
import io.afero.aferolab.addDevice.AddSetupModeDeviceView;
import io.afero.aferolab.databinding.ActivityMainBinding;
import io.afero.aferolab.deviceInspector.DeviceInspectorView;
import io.afero.aferolab.helper.BackStack;
import io.afero.aferolab.helper.PermissionsHelper;
import io.afero.aferolab.helper.PrefsHelper;
import io.afero.aferolab.resetPassword.RequestCodeView;
import io.afero.aferolab.widget.AferoEditText;
import io.afero.aferolab.widget.ScreenView;
import io.afero.sdk.android.clock.AndroidClock;
import io.afero.sdk.android.log.AndroidLog;
import io.afero.sdk.client.retrofit2.AferoClientRetrofit2;
import io.afero.sdk.client.retrofit2.models.AccessToken;
import io.afero.sdk.client.retrofit2.models.DeviceInfoBody;
import io.afero.sdk.client.retrofit2.models.UserDetails;
import io.afero.sdk.conclave.ConclaveClient;
import io.afero.sdk.device.ConclaveDeviceEventSource;
import io.afero.sdk.device.DeviceCollection;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.log.AfLog;
import io.afero.sdk.softhub.AferoSofthub;
import io.afero.sdk.utils.RxUtils;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

public class MainActivity extends AppCompatActivity {

    private Subscription mTokenRefreshSubscription;
    private Subscription mConclaveStatusSubscription;
    private Subscription mDeviceEventStreamSubscription;

    private DeviceEventSourceConnectObserver mDeviceEventSourceConnectObserver;

    private AferoClientRetrofit2 mAferoClient;
    private DeviceCollection mDeviceCollection;
    private ConclaveDeviceEventSource mDeviceEventSource;
    private AferoSofthub mAferoSofthub;
    private ConnectivityReceiver mConnectivityReceiver;

    private ActivityResultLauncher<Intent> launcher;
    private AuthorizationService mAuthService;
    private AuthState mAuthState;

    private String mUserId;

    private final Observer<AferoSofthub> mAferoSofthubStartObserver = new RxUtils.IgnoreResponseObserver<>();

    private ActivityMainBinding binding;

    private BackStack<ScreenView> mBackStack = new BackStack<>();
    private LabClientHelper clientHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ScreenView.setBackStack(mBackStack);

        setSupportActionBar(binding.appToolbar);

        AndroidClock.init();
        AfLog.init(new AndroidLog("AfLab"));

        final String accountId = PrefsHelper.getAccountId(this);
        AccessToken token = PrefsHelper.getAccessToken(this);


        AferoClientRetrofit2.ConfigBuilder configBuilder = new AferoClientRetrofit2.ConfigBuilder()
                .oauthClientId(BuildConfig.AFERO_CLIENT_ID)
                .baseUrl("https://" + BuildConfig.AFERO_SERVICE_HOSTNAME)
                .logLevel(BuildConfig.HTTP_LOG_LEVEL);

        mAuthService = new AuthorizationService(this);


        if (BuildConfig.AFERO_CLIENT_SECRET != null) {
            configBuilder.oauthClientSecret(BuildConfig.AFERO_CLIENT_SECRET);
            mAferoClient = new AferoClientRetrofit2(configBuilder.build(), null);

        } else if (BuildConfig.AFERO_OAUTH_AUTH_URL != null && BuildConfig.AFERO_OAUTH_TOKEN_URL != null) {
            clientHelper = new LabClientHelper(HttpLoggingInterceptor.Level.BASIC, 60, mAuthService);
            observeTokenRefresh();
            mAuthState = new AuthState(clientHelper.mServiceConfig);
            launcher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            // There are no request codes
                            Intent data = result.getData();
                            AuthorizationResponse response = AuthorizationResponse.fromIntent(data);
                            AuthorizationException ex = AuthorizationException.fromIntent(data);

                            exchangeAuthorizationCode(response);
                        } else {
                            binding.buttonSignIn.setEnabled(true);
                        }
                    });
            mAferoClient = new AferoClientRetrofit2(configBuilder.build(), clientHelper.getClient());
        } else {
            throw new IllegalArgumentException("Either configure client secret or auth/token/redirect url");
        }

        mAferoClient.setOwnerAndActiveAccountId(accountId);

        mDeviceCollection = new DeviceCollection(mAferoClient);

        if (token != null) {
            setToken(token);
            mDeviceCollection.start()
                    .subscribe(new DeviceCollectionStartObserver(this));
        }

        mDeviceEventSource = (ConclaveDeviceEventSource) mDeviceCollection.getDeviceEventSource();
        mDeviceEventSourceConnectObserver = new DeviceEventSourceConnectObserver(this);

        mConclaveStatusSubscription = mDeviceEventSource.observeConclaveStatus()
                .onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ConclaveClient.Status>() {
                    @Override
                    public void call(ConclaveClient.Status status) {
                        onConclaveStatusChange(status);
                    }
                });

        mAferoSofthub = AferoSofthub.acquireInstance(this, mAferoClient, "appId: " + BuildConfig.APPLICATION_ID);
        if (BuildConfig.AFERO_SOFTHUB_SERVICE != null) {
            mAferoSofthub.setService(BuildConfig.AFERO_SOFTHUB_SERVICE);
        }

        if (BuildConfig.AFERO_SERVICE_HOSTNAME != null) {
            mAferoSofthub.setHost(BuildConfig.AFERO_SERVICE_HOSTNAME);
        }

        if (BuildConfig.AFERO_SOFTHUB_AUTHENTICATOR_CERT != null) {
            mAferoSofthub.setAuthCert(BuildConfig.AFERO_SOFTHUB_AUTHENTICATOR_CERT);
        }

        mAferoSofthub.observeSetupModeDevices()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<AferoSofthub.SetupModeDeviceInfo>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(AferoSofthub.SetupModeDeviceInfo setupModeDeviceInfo) {
                        onSetupModeDeviceDetected(setupModeDeviceInfo);
                    }
                });

        binding.deviceListView.start(mDeviceCollection);
        binding.deviceListView.getDeviceOnClick()
                .subscribe(new Action1<DeviceModel>() {
                    @Override
                    public void call(DeviceModel deviceModel) {
                        final DeviceInspectorView deviceInspectorView = DeviceInspectorView.create(binding.getRoot());
                        deviceInspectorView.start(deviceModel, mDeviceCollection, mAferoClient);

                        deviceInspectorView.getObservable().subscribe(new Observer<DeviceInspectorView>() {
                            @Override
                            public void onCompleted() {
                                if (deviceInspectorView.isStarted()) {
                                    deviceInspectorView.stop();
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                            }

                            @Override
                            public void onNext(DeviceInspectorView deviceInspectorView) {
                            }
                        });
                    }
                });

        //
        if (getToken() != null) {
            // listen for token refresh failures
            observeTokenRefresh();

            startDeviceStream();
        }

        setupViews();

        showConclaveStatus(ConclaveClient.Status.DISCONNECTED);

        PermissionsHelper.checkRequiredPermissions(this);

        binding.buttonSignIn.setOnClickListener(v -> onClickSignIn());
        binding.buttonForgotPassword.setOnClickListener(v -> onClickForgotPassword());
        binding.editTextPassword.setOnEditorActionListener((textView, actionId, event) -> onEditorActionSignIn(textView, actionId, event));
    }

    @MainThread
    private void exchangeAuthorizationCode(AuthorizationResponse authorizationResponse) {
        performTokenRequest(
                authorizationResponse.createTokenExchangeRequest(),
                this::handleCodeExchangeResponse);
    }

    @MainThread
    private void performTokenRequest(
            TokenRequest request,
            AuthorizationService.TokenResponseCallback callback) {
        ClientAuthentication clientAuthentication;
        try {
            clientAuthentication = mAuthState.getClientAuthentication();
        } catch (ClientAuthentication.UnsupportedAuthenticationMethod ex) {
            binding.buttonSignIn.setEnabled(false);

            Log.d("main", "Token request cannot be made, client authentication for the token "
                    + "endpoint could not be constructed (%s)", ex);
            return;
        }
        mAuthService.performTokenRequest(
                request,
                clientAuthentication,
                callback);
    }


    @WorkerThread
    private void handleCodeExchangeResponse(
            @Nullable TokenResponse tokenResponse,
            @Nullable AuthorizationException authException) {


        if (authException == null) {
            AccessToken accessToken = new AccessToken(tokenResponse.accessToken, tokenResponse.refreshToken);
            accessToken.expiresIn = tokenResponse.accessTokenExpirationTime;
            accessToken.tokenType = tokenResponse.tokenType;
            accessToken.scope = tokenResponse.scope;

            PrefsHelper.saveAccessToken(this, accessToken);

            setToken(accessToken);

            mAferoClient.usersMe()
                    .subscribe(new SignInObserver(this));
        }
        binding.buttonSignIn.setEnabled(true);
        setupViews();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mDeviceEventStreamSubscription = RxUtils.safeUnSubscribe(mDeviceEventStreamSubscription);

        try {
            if (mConnectivityReceiver != null) {
                unregisterReceiver(mConnectivityReceiver);
            }
        } catch (Exception e) {
            AfLog.e(e);
        }

        mDeviceEventSource.stop();

        mAferoSofthub.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mConnectivityReceiver == null) {
            mConnectivityReceiver = new ConnectivityReceiver(this);
        }
        registerReceiver(mConnectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        if (isActiveNetworkConnectedOrConnecting() && isSignedIn()) {
            observeTokenRefresh();

            startDeviceStream();
        }

        mAferoSofthub.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_add_device) {
            onActionAddDevice();
            return true;
        } else if (itemId == R.id.action_sign_out) {
            onActionSignOut();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void onActionAddDevice() {
        final AddDeviceView addDeviceView = AddDeviceView.create(binding.getRoot());
        addDeviceView.start(mDeviceCollection, mAferoClient);
        addDeviceView.getObservable().subscribe(new Observer<AddDeviceView>() {
            @Override
            public void onCompleted() {
                addDeviceView.stop();
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(AddDeviceView addDeviceView) {
            }
        });
    }

    /**
     * This will cause to {@link AferoClientRetrofit2#tokenRefreshObservable()} to emit onCompleted,
     * which will call {@link #onSignOut()}
     */
    void onActionSignOut() {
        if (clientHelper != null) {
            clientHelper.signOut();
        } else {
            mAferoClient.signOut(null, null);
            setToken(null);
        }


        setupViews();
    }

    @Override
    public void onBackPressed() {
        ScreenView view = mBackStack.onBackPressed();
        if (view != null) {
            view.stop();
        } else {
            super.onBackPressed();
        }
    }

    private void setupViews() {
        if (BuildConfig.AFERO_CLIENT_SECRET == null) {
            binding.editTextEmail.setVisibility(View.GONE);
            binding.editTextPassword.setVisibility(View.GONE);
            binding.buttonForgotPassword.setVisibility(View.GONE);
        }

        if (isSignedIn()) {
            binding.groupSignIn.setVisibility(View.GONE);
            binding.groupStatus.setVisibility(View.VISIBLE);

            binding.textAccountName.setText(PrefsHelper.getAccountName(this));
        } else {
            binding.groupSignIn.setVisibility(View.VISIBLE);
            binding.groupStatus.setVisibility(View.GONE);
            binding.editTextEmail.showKeyboard();
        }
    }

    private void showConclaveStatus(ConclaveClient.Status status) {
        binding.textNetworkStatus.setText(status.toString());
    }

    private void showNoNetworkView() {
        binding.textNetworkStatus.setText(R.string.no_network);
    }

    private void hideNoNetworkView() {
    }

    private void startSofthub() {
        Log.d("main", "#### Start softhub ");
        if (!mAferoSofthub.isRunning()) {
            mAferoSofthub.start()
                    .subscribe(mAferoSofthubStartObserver);
        }
    }

    private void onSetupModeDeviceDetected(AferoSofthub.SetupModeDeviceInfo setupModeDeviceInfo) {
        final AddSetupModeDeviceView view = AddSetupModeDeviceView.create(binding.getRoot());
        view.start(mDeviceCollection, mAferoClient, setupModeDeviceInfo);
        view.getObservable().subscribe(new Observer<AddSetupModeDeviceView>() {
            @Override
            public void onCompleted() {
                view.stop();
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(AddSetupModeDeviceView addDeviceView) {
            }
        });
    }

    public boolean onEditorActionSignIn(TextView textView, int actionId, KeyEvent event) {
        if (AferoEditText.isDone(actionId, event)) {
            if (textView.getId() == R.id.edit_text_password) {
                onClickSignIn();
            }
        }

        return true;
    }

    public void onClickSignIn() {
        if (BuildConfig.AFERO_OAUTH_AUTH_URL != null
                && BuildConfig.AFERO_OAUTH_REDIRECT_SCHEME != null &&
                BuildConfig.AFERO_OAUTH_REDIRECT_HOST != null) {

            binding.buttonSignIn.setEnabled(false);
            showConclaveStatus(ConclaveClient.Status.CONNECTING);

            AuthorizationRequest.Builder authRequestBuilder =
                    new AuthorizationRequest.Builder(
                            clientHelper.mServiceConfig, // the authorization service configuration
                            BuildConfig.AFERO_CLIENT_ID,
                            // the client ID, typically pre-registered and static
                            ResponseTypeValues.CODE, // the response_type value: we want a code
                            Uri.parse(BuildConfig.AFERO_OAUTH_REDIRECT_SCHEME + "://" + BuildConfig.AFERO_OAUTH_REDIRECT_HOST)
                    );
            authRequestBuilder.setScopes("openid", "offline_access");
            Intent authIntent = mAuthService.getAuthorizationRequestIntent(authRequestBuilder.build());

            launcher.launch(authIntent);
        } else {

            binding.editTextPassword.hideKeyboard();
            startSignIn(binding.editTextEmail.getText().toString(), binding.editTextPassword.getText().toString());
        }
    }

    public void onClickForgotPassword() {
        binding.editTextPassword.hideKeyboard();
        RequestCodeView.create(binding.getRoot()).start(mAferoClient);
    }

    private void startSignIn(String email, String password) {
        binding.groupSignIn.setVisibility(View.GONE);
        binding.groupStatus.setVisibility(View.VISIBLE);

        showConclaveStatus(ConclaveClient.Status.CONNECTING);

        mAferoClient.getAccessToken(email, password)
                .concatMap(new MapAccessTokenToUserDetails())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SignInObserver(this));
    }

    @UiThread
    private void onSignIn(UserDetails userDetails) {
        AfLog.d("#### onSignIn");

        binding.editTextPassword.setText("");

        // listen for token refresh failures
        observeTokenRefresh();

        mUserId = userDetails.userId;
        String accountId = null;
        String accountName = null;

        for (UserDetails.AuthUserAccountAccess access : userDetails.accountAccess) {
            if (access.privileges.owner) {
                accountId = access.account.accountId;
                accountName = access.account.description;
                break;
            }
        }

        PrefsHelper.saveAccessToken(this, getToken());
        PrefsHelper.saveUserId(this, mUserId);
        PrefsHelper.saveAccountId(this, accountId);
        PrefsHelper.saveAccountName(this, accountName);

        setName(accountName);

        mAferoClient.setOwnerAndActiveAccountId(accountId);

        mDeviceCollection.start()
                .subscribe(new DeviceCollectionStartObserver(this));

        startDeviceStream();
    }

    private void setName(final String name) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                binding.textAccountName.setText(name);

            }
        });
    }

    private void onSignInError(Throwable e) {
        binding.textNetworkStatus.setText(e.getMessage());
        binding.editTextPassword.setText("");
        onSignOut();
    }

    private void onSignOut() {

        for (ScreenView view = mBackStack.pop(); view != null; view = mBackStack.pop()) {
            view.stop();
        }

        mTokenRefreshSubscription = RxUtils.safeUnSubscribe(mTokenRefreshSubscription);

        mUserId = null;
        PrefsHelper.clearAccountPrefs(this);

        mAferoSofthub.stop();

        mDeviceEventSource.stop();

        setToken(null);
        mAferoClient.clearAccount();

        if (mDeviceCollection.isStarted()) {
            mDeviceCollection.stop();
            mDeviceCollection.reset();
        }

        setupViews();
    }

    private void setToken(AccessToken token) {
        if (clientHelper != null) {
            clientHelper.setToken(token);
        } else {
            mAferoClient.setToken(token);
        }

    }

    private AccessToken getToken() {
        return clientHelper != null ? clientHelper.getToken() : mAferoClient.getToken();
    }

private void observeTokenRefresh() {
    if (clientHelper != null) {
        if (mTokenRefreshSubscription == null) {
            AfLog.d("#### Start listening to new tokens");
            mTokenRefreshSubscription = clientHelper.tokenRefreshObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new TokenObserver(this));
        } else {
            AfLog.d("#### already listening to new tokens");
        }
    } else {
        if (mTokenRefreshSubscription == null) {
            mTokenRefreshSubscription = mAferoClient.tokenRefreshObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new TokenObserver(this));
        }
    }
}


    private boolean isSignedIn() {
        return getToken() != null;
    }

    private void startDeviceStream() {
        if (mDeviceEventSource != null && (!mDeviceEventSource.isConnected()) && mDeviceEventStreamSubscription == null) {
            mDeviceEventStreamSubscription = startDeviceEventStream()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(mDeviceEventSourceConnectObserver);
        }
    }

    private void onConclaveStatusChange(ConclaveClient.Status status) {
        showConclaveStatus(status);
    }

    private void onDeviceEventStreamConnectComplete() {
        mDeviceEventStreamSubscription = null;
        startSofthub();
    }

    private void onDeviceEventStreamConnectError(Throwable e) {
        mDeviceEventStreamSubscription = null;
        if (mAferoClient.getStatusCode(e) != HttpURLConnection.HTTP_UNAUTHORIZED) {
            showNoNetworkView();
        }
    }

    private boolean isActiveNetworkConnectedOrConnecting() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm != null ? cm.getActiveNetworkInfo() : null;
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void onConnectivityChange() {
        if (isActiveNetworkConnectedOrConnecting()) {
            hideNoNetworkView();

            if (isSignedIn()) {
                startDeviceStream();
            }
        } else {
            showNoNetworkView();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionsHelper.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    private Observable<ConclaveDeviceEventSource> startDeviceEventStream() {
        Observable<ConclaveDeviceEventSource> startStreamObservable;

        if (!mDeviceEventSource.hasStarted()) {
            final String accountId = mAferoClient.getActiveAccountId();
            final String userId = mUserId;

            startStreamObservable = mDeviceEventSource.start(accountId, userId, ClientID.get(this), "android");
        } else {
            startStreamObservable = mDeviceEventSource.reconnect();
        }

        return registerClientID()
                .flatMap(
                        new RxUtils.FlatMapper<Response<Void>, ConclaveDeviceEventSource>(startStreamObservable)
                );
    }

    private Observable<Response<Void>> registerClientID() {
        if (!ClientID.getIDWasRegistered()) {
            DeviceInfoBody deviceInfo = new DeviceInfoBody(DeviceInfoBody.PLATFORM_ANDROID, "", ClientID.get(this), BuildConfig.APPLICATION_ID);

            deviceInfo.extendedData.app_version = BuildConfig.VERSION_NAME;
            deviceInfo.extendedData.app_build_number = BuildConfig.VERSION_CODE;
            deviceInfo.extendedData.app_identifier = BuildConfig.APPLICATION_ID;
            deviceInfo.extendedData.app_build_type = BuildConfig.BUILD_TYPE;

            final String userId = mUserId;
            if (userId != null && !userId.isEmpty()) {
                return mAferoClient.postDeviceInfo(userId, deviceInfo)
                        .doOnNext(new Action1<Response<Void>>() {
                            @Override
                            public void call(Response<Void> response) {
                                ClientID.setIDWasRegistered(true);
                            }
                        });
            }
        }

        return Observable.just(Response.success((Void) null));
    }

    private static class TokenObserver extends RxUtils.WeakObserver<AccessToken, MainActivity> {

        TokenObserver(MainActivity activity) {
            super(activity);
        }

        @Override
        public void onCompleted(MainActivity activity) {
            activity.onSignOut();
        }

        @Override
        public void onError(MainActivity activity, Throwable e) {
            activity.onSignOut();
        }

        @Override
        public void onNext(MainActivity activity, AccessToken token) {
            AfLog.d("#### Save new Access Token");

            PrefsHelper.saveAccessToken(activity, token);
        }
    }

    private static class SignInObserver extends RxUtils.WeakObserver<UserDetails, MainActivity> {

        SignInObserver(MainActivity activity) {
            super(activity);
        }

        @Override
        public void onCompleted(MainActivity activity) {
        }

        @Override
        public void onError(MainActivity activity, Throwable e) {
            activity.onSignInError(e);
        }

        @Override
        public void onNext(MainActivity activity, UserDetails userDetails) {
            activity.onSignIn(userDetails);
        }
    }

    private class MapAccessTokenToUserDetails implements Func1<AccessToken, Observable<UserDetails>> {
        @Override
        public Observable<UserDetails> call(AccessToken accessToken) {
            setToken(accessToken);
            return mAferoClient.usersMe();
        }
    }

    private static class DeviceEventSourceConnectObserver extends RxUtils.WeakObserver<ConclaveDeviceEventSource, MainActivity> {

        DeviceEventSourceConnectObserver(MainActivity strongRef) {
            super(strongRef);
        }

        @Override
        public void onCompleted(MainActivity activity) {
            activity.onDeviceEventStreamConnectComplete();
        }

        @Override
        public void onError(MainActivity activity, Throwable e) {
            activity.onDeviceEventStreamConnectError(e);
        }

        @Override
        public void onNext(MainActivity activity, ConclaveDeviceEventSource o) {
        }
    }

    private static class ConnectivityReceiver extends BroadcastReceiver {

        WeakReference<MainActivity> mRef;

        ConnectivityReceiver(MainActivity activity) {
            mRef = new WeakReference<>(activity);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            MainActivity activity = mRef.get();
            if (activity != null) {
                activity.onConnectivityChange();
            }
        }
    }

    private class DeviceCollectionStartObserver extends RxUtils.WeakObserver<DeviceCollection, MainActivity> {

        DeviceCollectionStartObserver(MainActivity activity) {
            super(activity);
        }

        @Override
        public void onCompleted(final MainActivity activity) {
        }

        @Override
        public void onError(MainActivity activity, Throwable t) {
        }

        @Override
        public void onNext(MainActivity activity, DeviceCollection deviceCollection) {
        }
    }
}
