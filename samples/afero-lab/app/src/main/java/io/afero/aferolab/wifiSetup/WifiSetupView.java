package io.afero.aferolab.wifiSetup;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import io.afero.aferolab.R;
import io.afero.aferolab.databinding.ViewWifiSetupBinding;
import io.afero.aferolab.widget.PasswordDialog;
import io.afero.aferolab.widget.ScreenView;
import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.device.DeviceModel;
import rx.Observable;
import rx.subjects.PublishSubject;

public class WifiSetupView extends ScreenView {

    private ViewWifiSetupBinding binding;

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
        binding = ViewWifiSetupBinding.bind(this);

        binding.refreshButton.setOnClickListener(this::onClickRefresh);
        binding.emptyRefreshButton.setOnClickListener(this::onClickRefresh);
        binding.networkList.setOnItemClickListener(this::onNetworkListItemClick);
        binding.wifiErrorCancelButton.setOnClickListener(v -> onClickCancel());
        binding.wifiScanTryAgainButton.setOnClickListener(v -> onClickWifiScanTryAgain());
        binding.wifiSendCredsTryAgainButton.setOnClickListener(v -> onClickSendCredsTryAgain());
        binding.wifiSetupDoneButton.setOnClickListener(v -> onClickDone());
    }

    public WifiSetupView start(DeviceModel deviceModel, AferoClient aferoClient) {
        pushOnBackStack();

        mController = new WifiSetupController(this, deviceModel, aferoClient);
        mController.start();

        binding.networkListRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
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
        binding.networkList.setAdapter(adapter);
    }

    void askUserToTurnOnBluetooth(DeviceModel deviceModel) {
    }

    void stopBluetoothNeeded() {
    }

    void showLookingProgress() {
        binding.wifiSetupMessageLabel.setText(R.string.wifi_looking_for_device);

        binding.networkListEmptyContainer.setVisibility(GONE);
        binding.networkListContainer.setVisibility(GONE);
        binding.networkError.setVisibility(GONE);
        binding.wifiSetupProgress.getRoot().setVisibility(View.VISIBLE);
    }

    void showConnectProgress() {
        binding.wifiSetupMessageLabel.setText(R.string.wifi_connecting_to_device);

        binding.networkListEmptyContainer.setVisibility(GONE);
        binding.networkListContainer.setVisibility(GONE);
        binding.networkError.setVisibility(GONE);
        binding.wifiSetupProgress.getRoot().setVisibility(View.VISIBLE);
    }

    void hideProgress() {
        binding.networkListRefresh.setRefreshing(false);
        binding.wifiSetupProgress.getRoot().setVisibility(View.GONE);
    }

    void showEmptyView() {
        binding.networkListEmptyContainer.setVisibility(VISIBLE);
        binding.networkListContainer.setVisibility(GONE);
        binding.wifiSetupProgress.getRoot().setVisibility(View.GONE);
    }

    void showListView() {
        binding.networkListEmptyContainer.setVisibility(GONE);
        binding.networkListContainer.setVisibility(VISIBLE);
        binding.networkError.setVisibility(GONE);
        binding.wifiSetupProgress.getRoot().setVisibility(View.GONE);
    }

    void showWifiConnectProgress() {
        binding.wifiSetupMessageLabel.setText(R.string.wifi_please_wait);

        binding.networkListEmptyContainer.setVisibility(GONE);
        binding.networkListContainer.setVisibility(GONE);
        binding.networkError.setVisibility(GONE);
        binding.wifiSetupProgress.getRoot().setVisibility(View.VISIBLE);
    }

    void showWifiScanError() {
        binding.wifiSetupMessageLabel.setText(R.string.wifi_cant_connect_to_device);
        binding.wifiSendCredsTryAgainButton.setVisibility(GONE);
        binding.wifiScanTryAgainButton.setVisibility(VISIBLE);
        showErrorContainer();
    }

    void showSendWifiCredsError() {
        showSendWifiCredsError(R.string.wifi_cant_connect_to_device);
    }

    void showSendWifiCredsError(@StringRes int resId) {
        binding.wifiSetupMessageLabel.setText(resId);
        binding.wifiSendCredsTryAgainButton.setVisibility(VISIBLE);
        binding.wifiScanTryAgainButton.setVisibility(GONE);
        showErrorContainer();
    }

    private void showErrorContainer() {
        binding.networkListEmptyContainer.setVisibility(GONE);
        binding.networkListContainer.setVisibility(GONE);
        binding.networkError.setVisibility(VISIBLE);
        binding.wifiSetupProgress.getRoot().setVisibility(View.GONE);
    }

    void showSuccess() {
        binding.wifiSetupMessageLabel.setText(R.string.wifi_your_device_is_now_connected);

        binding.wifiSetupSuccess.setVisibility(VISIBLE);
        binding.networkListContainer.setVisibility(GONE);
        binding.networkError.setVisibility(GONE);
        binding.wifiSetupProgress.getRoot().setVisibility(View.GONE);
    }

    void onCompleted() {
        mViewSubject.onNext(this);
        mViewSubject.onCompleted();
    }

    void onClickRefresh(View view) {
        mController.onClickRefresh();
    }

    void onNetworkListItemClick(AdapterView<?> parent, View view, int position, long id) {
        mController.onNetworkListItemClick(position);
    }

    void onClickCancel() {
        mController.onClickCancel();
    }

    void onClickWifiScanTryAgain() {
        mController.onClickWifiScanTryAgain();
    }

    void onClickSendCredsTryAgain() {
        mController.onClickWifiConnectTryAgain();
    }

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
