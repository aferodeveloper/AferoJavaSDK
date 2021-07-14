package io.afero.aferolab.wifiSetup;

import android.content.Context;

import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import io.afero.aferolab.R;
import io.afero.aferolab.widget.PasswordDialog;
import io.afero.aferolab.widget.ProgressSpinnerView;
import io.afero.aferolab.widget.ScreenView;
import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.device.DeviceModel;
import rx.Observable;
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

    @BindView(R.id.wifi_setup_success)
    View mWifiSetupSuccessView;

    @BindView(R.id.wifi_send_creds_try_again_button)
    Button mTryAgainSendWifiCredsButton;

    @BindView(R.id.wifi_scan_try_again_button)
    Button mTryAgainWifiScanButton;

    private WifiSetupController mController;
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

    public WifiSetupView start(DeviceModel deviceModel, AferoClient aferoClient) {
        pushOnBackStack();

        mController = new WifiSetupController(this, deviceModel, aferoClient);
        mController.start();

        mListRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mController.onClickRefresh();
            }
        });

        return this;
    }

    public Observable<WifiSetupView> getObservable() {
        return mViewSubject;
    }

    @Override
    public void stop() {
        mController.stop();

        super.stop();
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    void setAdapter(WifiSSIDListAdapter adapter) {
        mListView.setAdapter(adapter);
    }

    void askUserToTurnOnBluetooth(DeviceModel deviceModel) {
    }

    void stopBluetoothNeeded() {
    }

    void showLookingProgress() {
        mMessageText.setText(R.string.wifi_looking_for_device);

        mEmptyListContainer.setVisibility(GONE);
        mListContainer.setVisibility(GONE);
        mErrorContainer.setVisibility(GONE);
        mProgressView.show();
    }

    void showConnectProgress() {
        mMessageText.setText(R.string.wifi_connecting_to_device);

        mEmptyListContainer.setVisibility(GONE);
        mListContainer.setVisibility(GONE);
        mErrorContainer.setVisibility(GONE);
        mProgressView.show();
    }

    void hideProgress() {
        mListRefresh.setRefreshing(false);
        mProgressView.hide();
    }

    void showEmptyView() {
        mEmptyListContainer.setVisibility(VISIBLE);
        mListContainer.setVisibility(GONE);
        mProgressView.hide();
    }

    void showListView() {
        mEmptyListContainer.setVisibility(GONE);
        mListContainer.setVisibility(VISIBLE);
        mErrorContainer.setVisibility(GONE);
        mProgressView.hide();
    }

    void showWifiConnectProgress() {
        mMessageText.setText(R.string.wifi_please_wait);

        mEmptyListContainer.setVisibility(GONE);
        mListContainer.setVisibility(GONE);
        mErrorContainer.setVisibility(GONE);
        mProgressView.show();
    }

    void showWifiScanError() {
        mMessageText.setText(R.string.wifi_cant_connect_to_device);
        mTryAgainSendWifiCredsButton.setVisibility(GONE);
        mTryAgainWifiScanButton.setVisibility(VISIBLE);
        showErrorContainer();
    }

    void showSendWifiCredsError() {
        showSendWifiCredsError(R.string.wifi_cant_connect_to_device);
    }

    void showSendWifiCredsError(@StringRes int resId) {
        mMessageText.setText(resId);
        mTryAgainSendWifiCredsButton.setVisibility(VISIBLE);
        mTryAgainWifiScanButton.setVisibility(GONE);
        showErrorContainer();
    }

    private void showErrorContainer() {
        mEmptyListContainer.setVisibility(GONE);
        mListContainer.setVisibility(GONE);
        mErrorContainer.setVisibility(VISIBLE);
        mProgressView.hide();
    }

    void showSuccess() {
        mMessageText.setText(R.string.wifi_your_device_is_now_connected);

        mWifiSetupSuccessView.setVisibility(VISIBLE);
        mListContainer.setVisibility(GONE);
        mErrorContainer.setVisibility(GONE);
        mProgressView.hide();
    }

    void onCompleted() {
        mViewSubject.onNext(this);
        mViewSubject.onCompleted();
    }

    @OnClick({ R.id.refresh_button, R.id.empty_refresh_button })
    void onClickRefresh(View view) {
        mController.onClickRefresh();
    }

    @OnItemClick(R.id.network_list)
    void onNetworkListItemClick(AdapterView<?> parent, View view, int position, long id) {
        mController.onNetworkListItemClick(position);
    }

    @OnClick(R.id.wifi_error_cancel_button)
    void onClickCancel() {
        mController.onClickCancel();
    }

    @OnClick(R.id.wifi_scan_try_again_button)
    void onClickWifiScanTryAgain() {
        mController.onClickWifiScanTryAgain();
    }

    @OnClick(R.id.wifi_send_creds_try_again_button)
    void onClickSendCredsTryAgain() {
        mController.onClickWifiConnectTryAgain();
    }

    @OnClick(R.id.wifi_setup_done_button)
    void onClickDone() {
        onCompleted();
    }

    Observable<String> askUserForWifiPassword() {
        return new PasswordDialog(this, R.string.wifi_password_dialog_title).start();
    }

//    @OnClick({ R.id.manual_wifi_button, R.id.empty_manual_wifi_button })
//    void onClickManualSSID() {
//        mController.onClickManualSSID();
//    }

}
