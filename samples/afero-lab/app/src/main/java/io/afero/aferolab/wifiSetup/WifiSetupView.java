package io.afero.aferolab.wifiSetup;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.afero.aferolab.R;
import io.afero.aferolab.widget.ProgressSpinnerView;
import io.afero.aferolab.widget.ScreenView;
import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.device.DeviceModel;
import rx.subjects.PublishSubject;

public class WifiSetupView extends ScreenView {

    @BindView(R.id.network_list_container)
    View mListContainer;

    @BindView(R.id.network_list_refresh)
    SwipeRefreshLayout mListRefresh;

    @BindView(R.id.network_list)
    ListView mListView;

    @BindView(R.id.network_list_empty_container)
    View mEmptyListContainer;

    @BindView(R.id.wifi_setup_title_label)
    TextView mTitleText;

    @BindView(R.id.wifi_setup_message_label)
    TextView mMessageText;

    @BindView(R.id.network_error)
    View mErrorContainer;

    @BindView(R.id.wifi_setup_progress)
    ProgressSpinnerView mProgressView;

//    @BindView(R.id.wifi_password_view)
//    WifiPasswordView mWifiPasswordView;

    private WifiSetupController mPresenter;
    private PublishSubject<WifiSetupView> mViewSubject = PublishSubject.create();


    public WifiSetupView(@NonNull Context context) {
        super(context);
    }

    public WifiSetupView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public WifiSetupView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public static WifiSetupView create(@NonNull View contextView) {
        return inflateView(R.layout.view_wifi_setup, contextView);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.bind(this);
    }

    public void start(DeviceModel deviceModel, AferoClient aferoClient) {
        pushOnBackStack();

        mPresenter = new WifiSetupController(this, deviceModel, aferoClient);
        mPresenter.start();

        mListRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mPresenter.onClickRefresh();
            }
        });
    }

    @Override
    public void stop() {
        mPresenter.stop();

//        mWifiPasswordSubscription = RxUtils.safeUnSubscribe(mWifiPasswordSubscription);

        super.stop();
    }

    public void setAdapter(WifiSSIDListAdapter adapter) {
        mListView.setAdapter(adapter);
    }

    public void askUserToTurnOnBluetooth(DeviceModel mDeviceModel) {
    }

    public void stopBluetoothNeeded() {
    }

    public void showLookingProgress() {
        mEmptyListContainer.setVisibility(View.GONE);
        mListContainer.setVisibility(View.GONE);
        mErrorContainer.setVisibility(GONE);
        mProgressView.show();
        mMessageText.setText(R.string.wifi_looking_for_device);
    }

    public void showConnectProgress() {
        mEmptyListContainer.setVisibility(View.GONE);
        mListContainer.setVisibility(View.GONE);
        mErrorContainer.setVisibility(GONE);
        mProgressView.show();
        mMessageText.setText(R.string.wifi_connecting_to_device);
    }

    public void hideProgress() {
        mListRefresh.setRefreshing(false);
        mProgressView.hide();
    }

    public void showEmptyView() {
        mEmptyListContainer.setVisibility(View.VISIBLE);
        mListContainer.setVisibility(View.GONE);
        mProgressView.hide();
    }

    public void showErrorView() {
        mEmptyListContainer.setVisibility(View.GONE);
        mListContainer.setVisibility(View.GONE);
        mErrorContainer.setVisibility(VISIBLE);
        mProgressView.hide();
        mMessageText.setText(getResources().getString(R.string.wifi_cant_connect_to_device));
    }

    public void showListView() {
        mEmptyListContainer.setVisibility(View.GONE);
        mListContainer.setVisibility(View.VISIBLE);
        mProgressView.hide();
        mErrorContainer.setVisibility(GONE);
    }

//    public Observable<WifiPasswordPresenter.WifiCredentials> startWifiPassword(WifiSSIDEntry ssidEntry) {
//        mWifiPasswordView.start(ssidEntry);
//        return mWifiPasswordView.getObservable()
//                .doOnCompleted(new Action0() {
//                    @Override
//                    public void call() {
//                        mWifiPasswordView.stop();
//                        mWifiPasswordSubscription = RxUtils.safeUnSubscribe(mWifiPasswordSubscription);
//                    }
//                });
//    }
//
//    public void stopWifiPassword() {
//        if (mWifiPasswordView.isActive()) {
//            mWifiPasswordView.stop();
//        }
//    }

//    public Observable<WifiConnectPresenter.Event> startWifiConnect(DeviceWifiSetup wifiSetup, String ssid, String password) {
//        mWifiConnectView = WifiConnectGenericView.newInstance(this);
//        mWifiConnectView.start(wifiSetup, ssid, password);
//        return mWifiConnectView.getObservable()
//                .observeOn(AndroidSchedulers.mainThread());
//    }

    public void stopWifiConnect() {
//        if (mWifiConnectView != null) {
//            mWifiConnectView.stop();
//            mWifiConnectView = null;
//        }
//
//        mWifiConnectSubscription = RxUtils.safeUnSubscribe(mWifiConnectSubscription);
    }

    @Override
    public boolean onBackPressed() {
//        if (mBluetoothNeededView != null) {
//            stopBluetoothNeeded();
//        }
//
//        if (mWifiPasswordView.isActive()) {
//            stopWifiPassword();
//            return true;
//        }
//
//        if (mWifiConnectView != null) {
//            stopWifiConnect();
//            return true;
//        }

        return false;
    }

    @OnClick({ R.id.refresh_button, R.id.empty_refresh_button, R.id.wifi_error_try_again_button })
    void onClickRefresh(View view) {
        mPresenter.onClickRefresh();
    }

    public void onCompleted() {
        mViewSubject.onCompleted();
    }

    public void showConnecting() {

    }

    public void showError() {

    }

    public void showError(@StringRes int resId) {

    }

    public void onSuccess() {

    }

    public void onTryAgain() {

    }

    public void onTryAgainPassword() {

    }

    public void showSuccess() {

    }

//    @OnClick(R.id.wifi_error_cancel_button)
//    void onClickCancel() {
//        mPresenter.onClickCancel();
//    }
//
//    @OnClick({ R.id.manual_wifi_button, R.id.empty_manual_wifi_button })
//    void onClickManualSSID() {
//        mPresenter.onClickManualSSID();
//    }

}
