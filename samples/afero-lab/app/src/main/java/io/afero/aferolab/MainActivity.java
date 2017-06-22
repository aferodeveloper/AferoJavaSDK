/*
 * Copyright (c) 2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.retrofit2.AferoClientRetrofit2;
import io.afero.sdk.client.retrofit2.models.AccessToken;
import io.afero.sdk.client.retrofit2.models.DeviceInfoBody;
import io.afero.sdk.client.retrofit2.models.UserDetails;
import io.afero.sdk.conclave.ConclaveAccessManager;
import io.afero.sdk.conclave.ConclaveClient;
import io.afero.sdk.device.DeviceCollection;
import io.afero.sdk.device.DeviceEventStream;
import io.afero.sdk.device.DeviceProfileCollection;
import io.afero.sdk.hubby.HubbyHelper;
import io.afero.sdk.log.AfLog;
import io.afero.sdk.utils.RxUtils;
import retrofit2.Response;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity {

    public static final String BASE_URL_AFERO = "https://api.dev.afero.io";
//    public static final String BASE_URL_AFERO = "https://api.afero.io";

    private static final int DEFAULT_SERVICE_TIMEOUT = 60;

    private Subscription mTokenRefreshSubscription;
    private Subscription mConclaveStatusSubscription;
    private Subscription mSignInSubscription;
    private Subscription mDeviceEventStreamSubscription;

    private DeviceEventStreamConnectObserver mDeviceEventStreamConnectObserver;

    private AferoClientRetrofit2 mAferoClient;
    private DeviceProfileCollection mDeviceProfileCollection;
    private DeviceCollection mDeviceCollection;
    private DeviceEventStream mDeviceEventStream;
    private ConclaveAccessManager mConclaveAccessManager;
    private HubbyHelper mHubbyHelper;
    private ConnectivityReceiver mConnectivityReceiver;

    private String mUserId;

    @BindView(R.id.device_list_view)
    DeviceListView mDeviceListView;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        final String accountId = Prefs.getAccountId(this);
        final String accessToken = Prefs.getAccessToken(this);
        final String refreshToken = Prefs.getRefreshToken(this);

        mAferoClient = new AferoClientRetrofit2(BASE_URL_AFERO, BuildConfig.HTTP_LOG_LEVEL, DEFAULT_SERVICE_TIMEOUT);
        if (accessToken.length() > 0 && refreshToken.length() > 0) {
            mAferoClient.setToken(new AccessToken(accessToken, refreshToken));
        }

        mAferoClient.setActiveAccountId(accountId);
        mAferoClient.setOwnerAccountId(accountId);

        mConclaveAccessManager = new ConclaveAccessManager(mAferoClient);

        mDeviceEventStream = new DeviceEventStream(mConclaveAccessManager);

        mHubbyHelper = new HubbyHelper(mConclaveAccessManager, mAferoClient, isSimulator());

        final AferoClient.ImageSize imageSize = AferoClient.ImageSize
                .fromDisplayDensity(getResources().getDisplayMetrics().density);
        mDeviceProfileCollection = new DeviceProfileCollection(mAferoClient, imageSize, Locale.getDefault().toString());
        mDeviceCollection = new DeviceCollection(mDeviceEventStream, mDeviceProfileCollection, mAferoClient);

        mDeviceEventStreamConnectObserver = new DeviceEventStreamConnectObserver(this);

        setupViews();

        showConclaveStatus(ConclaveClient.Status.DISCONNECTED);

        PermissionsHelper.checkRequiredPermissions(this);

        if (!accessToken.isEmpty()) {
            startDeviceStream();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mTokenRefreshSubscription = RxUtils.safeUnSubscribe(mTokenRefreshSubscription);
        mConclaveStatusSubscription = RxUtils.safeUnSubscribe(mConclaveStatusSubscription);
        mDeviceEventStreamSubscription = RxUtils.safeUnSubscribe(mDeviceEventStreamSubscription);

        try {
            if (mConnectivityReceiver != null) {
                unregisterReceiver(mConnectivityReceiver);
            }
        } catch (Exception e) {
            AfLog.e(e);
        }

        mDeviceEventStream.stop();

        mHubbyHelper.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // listen for token refresh failures
        mTokenRefreshSubscription = mAferoClient.tokenRefreshObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new TokenObserver(this));

        mConclaveStatusSubscription = mDeviceEventStream.observeConclaveStatus()
                .onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ConclaveClient.Status>() {
                    @Override
                    public void call(ConclaveClient.Status status) {
                        onConclaveStatusChange(status);
                    }
                });

        if (mConnectivityReceiver == null) {
            mConnectivityReceiver = new ConnectivityReceiver(this);
        }
        registerReceiver(mConnectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        mHubbyHelper.onResume();

        if (isActiveNetworkConnectedOrConnecting() && isSignedIn()) {
            if ((!mDeviceEventStream.isConnected()) && mDeviceEventStreamSubscription == null) {
                startDeviceStream();
            }
        }
    }

    @OnClick(R.id.button_sign_out)
    void onClickSignOut() {
        onSignOut();
    }

    private void setupViews() {
        mDeviceListView.start(mDeviceCollection);

        if (isSignedIn()) {
            mSignInGroup.setVisibility(GONE);
            mStatusGroup.setVisibility(VISIBLE);

            mAccountNameText.setText(Prefs.getAccountName(this));
        } else {
            mSignInGroup.setVisibility(VISIBLE);
            mStatusGroup.setVisibility(GONE);
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

    private void startHubby() {
        if (!mHubbyHelper.isRunning()) {
            mHubbyHelper.start(this, ClientID.get(this));
        }
    }

    @OnEditorAction(R.id.edit_text_password)
    public boolean onEditorActionSignIn(TextView textView, int actionId, KeyEvent event) {
        if (AferoEditText.isDone(actionId, event)) {
            if (textView.getId() == R.id.edit_text_password) {
                startSignIn(mEmailEditText.getText().toString(), mPasswordEditText.getText().toString());
                mPasswordEditText.hideKeyboard();
            }
        }

        return true;
    }

    private void startSignIn(String email, String password) {
        mSignInGroup.setVisibility(GONE);
        mStatusGroup.setVisibility(VISIBLE);

        showConclaveStatus(ConclaveClient.Status.CONNECTING);

        mSignInSubscription = mAferoClient.getAccessToken(email, password, AferoClientRetrofit2.GRANT_TYPE_PASSWORD)
                .concatMap(new MapAccessTokenToUserDetails())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SignInObserver(this));
    }

    private void onSignIn(UserDetails userDetails) {
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

        Prefs.saveAccessToken(this, mAferoClient.getToken().accessToken);
        Prefs.saveRefreshToken(this, mAferoClient.getToken().refreshToken);
        Prefs.saveUserId(this, mUserId);
        Prefs.saveAccountId(this, accountId);
        Prefs.saveAccountName(this, accountName);

        mAccountNameText.setText(accountName);

        mAferoClient.setOwnerAccountId(accountId);
        mAferoClient.setActiveAccountId(accountId);

        startDeviceStream();
    }

    private void onSignInError(Throwable e) {
        mNetworkStatus.setText(e.getMessage());
        onSignOut();
    }

    private void onSignOut() {
        mTokenRefreshSubscription = RxUtils.safeUnSubscribe(mTokenRefreshSubscription);

        Prefs.clearAccountPrefs(this);

        mHubbyHelper.stop();

        mDeviceEventStream.stop();

        mAferoClient.setToken(null);
        mAferoClient.clearAccount();

        mDeviceCollection.reset();

        setupViews();
    }

    private boolean isSignedIn() {
        return mAferoClient.getToken() != null;
    }

    private void startDeviceStream() {
        mDeviceEventStreamSubscription = registerClient()
                .concatMap(new StartDeviceEventStreamFunc(this))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mDeviceEventStreamConnectObserver);
    }

    private void onConclaveStatusChange(ConclaveClient.Status status) {
        showConclaveStatus(status);
    }

    private boolean isSimulator() {
        return Build.MANUFACTURER.equals("unknown");
    }

    private Observable<Response<Void>> registerClient() {
        rx.Observable<String> observable = null;

        // register for push notifications
//        if (!mHasRegisteredGCM) {
//            mHasRegisteredGCM = true;
//            if (GCMClient.checkPlayServices(this)) {
//                observable = GCMClient.getRegistrationId(getApplicationContext())
//                        .onErrorResumeNext(new JustEmptyString());
//            }
//        }

        if (observable == null) {
            observable = Observable.just("");
        }

        if (!ClientID.getIDWasRegistered()) {
            return observable.flatMap(new MapPushIdToDeviceInfo(this))
                .subscribeOn(Schedulers.io());
        }

        return Observable.just(Response.success((Void) null));
    }

    private void onDeviceEventStreamConnectComplete() {
        mDeviceEventStreamSubscription = null;
        startHubby();
    }

    private void onDeviceEventStreamConnectError(Throwable e) {
        mDeviceEventStreamSubscription = null;
        if (mAferoClient.getStatusCode(e) != HttpURLConnection.HTTP_UNAUTHORIZED) {
            showNoNetworkView();
        }
    }

    private boolean isActiveNetworkConnectedOrConnecting() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void onConnectivityChange() {
        AfLog.i("onConnectivityChange");

        if (isActiveNetworkConnectedOrConnecting()) {
            hideNoNetworkView();

            if (isSignedIn()) {
                if (mDeviceEventStream != null && (!mDeviceEventStream.isConnected()) && mDeviceEventStreamSubscription == null) {
                    startDeviceStream();
                }
            }
        } else {
            showNoNetworkView();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionsHelper.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    private static class StartDeviceEventStreamFunc extends RxUtils.WeakFunc1<Response, Observable<Object>, MainActivity> {

        StartDeviceEventStreamFunc(MainActivity strongRef) {
            super(strongRef);
        }

        @Override
        public Observable<Object> call(MainActivity activity, Response baseResponse) {
            return activity.callStartDeviceEventStream();
        }
    }

    private Observable<Object> callStartDeviceEventStream() {
        if (!mDeviceEventStream.hasStarted()) {
            final String accountId = mAferoClient.getActiveAccountId();
            final String userId = mUserId;
            final String clientId = ClientID.get(this);
            return mDeviceEventStream.start(accountId, userId, "android", clientId);
        } else {
            return mDeviceEventStream.reconnect();
        }
    }

    private static class MapPushIdToDeviceInfo extends RxUtils.WeakFunc1<String, Observable<Response<Void>>, MainActivity> {

        MapPushIdToDeviceInfo(MainActivity activity) {
            super(activity);
        }

        @Override
        public Observable<Response<Void>> call(MainActivity mainActivity, String pushId) {
            return mainActivity.callMapPushIdToDeviceInfo(pushId);
        }
    }

    private Observable<Response<Void>> callMapPushIdToDeviceInfo(String pushId) {
        DeviceInfoBody deviceInfo = new DeviceInfoBody("ANDROID", pushId, ClientID.get(this));

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
            Prefs.saveAccessToken(activity, token.accessToken);
            Prefs.saveRefreshToken(activity, token.refreshToken);
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

    private static class DeviceEventStreamConnectObserver extends RxUtils.WeakObserver<Object, MainActivity> {

        public DeviceEventStreamConnectObserver(MainActivity strongRef) {
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
        public void onNext(MainActivity activity, Object o) {
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
}
