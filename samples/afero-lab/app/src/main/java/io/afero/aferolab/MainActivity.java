/*
 * Copyright (c) 2014-2019 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import io.afero.aferolab.addDevice.AddDeviceView;
import io.afero.aferolab.addDevice.AddSetupModeDeviceView;
import io.afero.aferolab.attributeEditor.AttributeEditorView;
import io.afero.aferolab.deviceInspector.DeviceInspectorView;
import io.afero.aferolab.deviceList.DeviceListView;
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

    private String mUserId;

    private final Observer<AferoSofthub> mAferoSofthubStartObserver = new RxUtils.IgnoreResponseObserver<>();

    @BindView(R.id.root_view)
    ViewGroup mRootView;

    @BindView(R.id.app_toolbar)
    Toolbar mAppToolbar;

    @BindView(R.id.device_list_view)
    DeviceListView mDeviceListView;

    @BindView(R.id.device_inspector)
    DeviceInspectorView mDeviceInspectorView;

    @BindView(R.id.attribute_editor)
    AttributeEditorView mAttributeEditorView;

    @BindView(R.id.edit_text_email)
    AferoEditText mEmailEditText;

    @BindView(R.id.edit_text_password)
    AferoEditText mPasswordEditText;

    @BindView(R.id.group_sign_in)
    ViewGroup mSignInGroup;

    @BindView(R.id.group_status)
    ViewGroup mStatusGroup;

    @BindView(R.id.text_account_name)
    TextView mAccountNameText;

    @BindView(R.id.text_network_status)
    TextView mNetworkStatus;

    private BackStack<ScreenView> mBackStack = new BackStack<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        ScreenView.setBackStack(mBackStack);

        setSupportActionBar(mAppToolbar);

        AndroidClock.init();
        AfLog.init(new AndroidLog("AfLab"));

        final String accountId = PrefsHelper.getAccountId(this);
        final String accessToken = PrefsHelper.getAccessToken(this);
        final String refreshToken = PrefsHelper.getRefreshToken(this);

        AccessToken token = !(accessToken.isEmpty() || refreshToken.isEmpty())
            ? new AccessToken(accessToken, refreshToken)
            : null;

        AferoClientRetrofit2.Config aferoClientConfig = new AferoClientRetrofit2.ConfigBuilder()
                .oauthClientId(BuildConfig.AFERO_CLIENT_ID)
                .oauthClientSecret(BuildConfig.AFERO_CLIENT_SECRET)
                .baseUrl(BuildConfig.AFERO_SERVICE_URL)
                .logLevel(BuildConfig.HTTP_LOG_LEVEL)
                .build();

        mAferoClient = new AferoClientRetrofit2(aferoClientConfig);
        mAferoClient.setOwnerAndActiveAccountId(accountId);

        mDeviceCollection = new DeviceCollection(mAferoClient);

        if (token != null) {
            mAferoClient.setToken(new AccessToken(accessToken, refreshToken));
            mDeviceCollection.start()
                    .subscribe(new DeviceCollectionStartObserver(this));
        }

        mDeviceEventSource = (ConclaveDeviceEventSource)mDeviceCollection.getDeviceEventSource();
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
        mAferoSofthub.setService(BuildConfig.AFERO_SOFTHUB_SERVICE);
        mAferoSofthub.observeSetupModeDevices()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<AferoSofthub.SetupModeDeviceInfo>() {
                @Override
                public void onCompleted() {}

                @Override
                public void onError(Throwable e) {}

                @Override
                public void onNext(AferoSofthub.SetupModeDeviceInfo setupModeDeviceInfo) {
                    onSetupModeDeviceDetected(setupModeDeviceInfo);
                }
            });

        mDeviceListView.start(mDeviceCollection);
        mDeviceListView.getDeviceOnClick()
                .subscribe(new Action1<DeviceModel>() {
                    @Override
                    public void call(DeviceModel deviceModel) {
                        mDeviceInspectorView.start(deviceModel, mDeviceCollection, mAferoClient);
                        mDeviceInspectorView.getObservable().subscribe(new Observer<DeviceInspectorView>() {
                            @Override
                            public void onCompleted() {
                                stopDeviceInspector();
                            }

                            @Override
                            public void onError(Throwable e) {}

                            @Override
                            public void onNext(DeviceInspectorView deviceInspectorView) {}
                        });
                    }
                });

        if (mAferoClient.getToken() != null) {
            // listen for token refresh failures
            mTokenRefreshSubscription = mAferoClient.tokenRefreshObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new TokenObserver(this));

            startDeviceStream();
        }

        setupViews();

        showConclaveStatus(ConclaveClient.Status.DISCONNECTED);

        PermissionsHelper.checkRequiredPermissions(this);
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
        switch (item.getItemId()) {
            case R.id.action_add_device:
                onActionAddDevice();
                return true;

            case R.id.action_sign_out:
                onActionSignOut();
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    void onActionAddDevice() {
        final AddDeviceView addDeviceView = AddDeviceView.create(mRootView);
        addDeviceView.start(mDeviceCollection, mAferoClient);
        addDeviceView.getObservable().subscribe(new Observer<AddDeviceView>() {
            @Override
            public void onCompleted() {
                addDeviceView.stop();
            }

            @Override
            public void onError(Throwable e) {}

            @Override
            public void onNext(AddDeviceView addDeviceView) {}
        });
    }

    /**
     * This will cause to {@link AferoClientRetrofit2#tokenRefreshObservable()} to emit onCompleted,
     * which will call {@link #onSignOut()}
     */
    void onActionSignOut() {
        mAferoClient.signOut(null, null);
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

    private void stopDeviceInspector() {
        if (mDeviceInspectorView.isStarted()) {
            mDeviceInspectorView.stop();
        }
    }

    private void setupViews() {
        if (isSignedIn()) {
            mSignInGroup.setVisibility(View.GONE);
            mStatusGroup.setVisibility(View.VISIBLE);

            mAccountNameText.setText(PrefsHelper.getAccountName(this));
        } else {
            mSignInGroup.setVisibility(View.VISIBLE);
            mStatusGroup.setVisibility(View.GONE);
            mEmailEditText.showKeyboard();
        }
    }

    private void showConclaveStatus(ConclaveClient.Status status) {
        mNetworkStatus.setText(status.toString());
    }

    private void showNoNetworkView() {
        mNetworkStatus.setText(R.string.no_network);
    }

    private void hideNoNetworkView() {
    }

    private void startSofthub() {
        if (!mAferoSofthub.isRunning()) {
            mAferoSofthub.start()
                .subscribe(mAferoSofthubStartObserver);
        }
    }

    private void onSetupModeDeviceDetected(AferoSofthub.SetupModeDeviceInfo setupModeDeviceInfo) {
        final AddSetupModeDeviceView view = AddSetupModeDeviceView.create(mRootView);
        view.start(mDeviceCollection, mAferoClient, setupModeDeviceInfo);
        view.getObservable().subscribe(new Observer<AddSetupModeDeviceView>() {
            @Override
            public void onCompleted() {
                view.stop();
            }

            @Override
            public void onError(Throwable e) {}

            @Override
            public void onNext(AddSetupModeDeviceView addDeviceView) {}
        });
    }

    @OnEditorAction(R.id.edit_text_password)
    public boolean onEditorActionSignIn(TextView textView, int actionId, KeyEvent event) {
        if (AferoEditText.isDone(actionId, event)) {
            if (textView.getId() == R.id.edit_text_password) {
                onClickSignIn();
            }
        }

        return true;
    }

    @OnClick(R.id.button_sign_in)
    public void onClickSignIn() {
        mPasswordEditText.hideKeyboard();
        startSignIn(mEmailEditText.getText().toString(), mPasswordEditText.getText().toString());
    }

    @OnClick(R.id.button_forgot_password)
    public void onClickForgotPassword() {
        mPasswordEditText.hideKeyboard();
        RequestCodeView.create(mRootView).start(mAferoClient);
    }

    private void startSignIn(String email, String password) {
        mSignInGroup.setVisibility(View.GONE);
        mStatusGroup.setVisibility(View.VISIBLE);

        showConclaveStatus(ConclaveClient.Status.CONNECTING);

        mAferoClient.getAccessToken(email, password)
                .concatMap(new MapAccessTokenToUserDetails())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SignInObserver(this));
    }

    private void onSignIn(UserDetails userDetails) {

        mPasswordEditText.setText("");

        // listen for token refresh failures
        mTokenRefreshSubscription = mAferoClient.tokenRefreshObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new TokenObserver(this));

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

        PrefsHelper.saveAccessToken(this, mAferoClient.getToken().accessToken);
        PrefsHelper.saveRefreshToken(this, mAferoClient.getToken().refreshToken);
        PrefsHelper.saveUserId(this, mUserId);
        PrefsHelper.saveAccountId(this, accountId);
        PrefsHelper.saveAccountName(this, accountName);

        mAccountNameText.setText(accountName);

        mAferoClient.setOwnerAndActiveAccountId(accountId);

        mDeviceCollection.start()
                .subscribe(new DeviceCollectionStartObserver(this));

        startDeviceStream();
    }

    private void onSignInError(Throwable e) {
        mNetworkStatus.setText(e.getMessage());
        mPasswordEditText.setText("");
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

        mAferoClient.setToken(null);
        mAferoClient.clearAccount();

        if (mDeviceCollection.isStarted()) {
            mDeviceCollection.stop();
            mDeviceCollection.reset();
        }

        setupViews();
    }

    private boolean isSignedIn() {
        return mAferoClient.getToken() != null;
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
            PrefsHelper.saveAccessToken(activity, token.accessToken);
            PrefsHelper.saveRefreshToken(activity, token.refreshToken);
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
            mAferoClient.setToken(accessToken);
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
