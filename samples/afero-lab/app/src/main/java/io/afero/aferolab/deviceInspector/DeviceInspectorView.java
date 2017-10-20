/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.deviceInspector;

import android.animation.LayoutTransition;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.afero.aferolab.R;
import io.afero.aferolab.attributeEditor.AttributeEditorView;
import io.afero.aferolab.widget.ProgressSpinnerView;
import io.afero.aferolab.wifiSetup.WifiSetupView;
import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.device.DeviceCollection;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceProfile;
import rx.Observable;
import rx.functions.Action1;
import rx.subjects.PublishSubject;


public class DeviceInspectorView extends FrameLayout {

    @BindView(R.id.device_name_text)
    TextView mDeviceNameText;

    @BindView(R.id.device_status_text)
    TextView mDeviceStatusText;

    @BindView(R.id.device_attribute_recycler_view)
    RecyclerView mAttributeListView;

    @BindView(R.id.device_info_card)
    ViewGroup mDeviceInfoCard;

    @BindView(R.id.attributes_card)
    ViewGroup mAttributesCard;

    @BindView(R.id.view_scrim)
    View mScrimView;

    @BindView(R.id.device_inspector_progress)
    ProgressSpinnerView mProgressView;

    @BindView(R.id.device_info_extra_open)
    ImageButton mDeviceInfoOpenButton;

    @BindView(R.id.device_info_extra_close)
    ImageButton mDeviceInfoCloseButton;

    @BindView(R.id.device_info_container)
    ViewGroup mDeviceInfoContainer;

    @BindView(R.id.device_info_extra_container)
    ViewGroup mDeviceInfoExtraContainer;

    @BindView(R.id.wifi_connect_button)
    Button mWifiConnectButton;


    private static final int TRANSITION_DURATION = 200;

    private DeviceInspectorController mController;
    private final AttributeAdapter mAttributeAdapter = new AttributeAdapter();
    private PublishSubject<DeviceInspectorView> mViewSubject;
    private WifiSetupView mWifiSetupView;

    public DeviceInspectorView(@NonNull Context context) {
        super(context);
    }

    public DeviceInspectorView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DeviceInspectorView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.bind(this);

        LayoutTransition lt = mDeviceInfoCard.getLayoutTransition();
        if (lt != null) {
            lt.enableTransitionType(LayoutTransition.CHANGING);
            lt.setStartDelay(LayoutTransition.CHANGING, 0);
            lt.setStartDelay(LayoutTransition.CHANGE_APPEARING, 0);
            lt.setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0);
        }

        lt = mAttributesCard.getLayoutTransition();
        if (lt != null) {
            lt.enableTransitionType(LayoutTransition.CHANGING);
            lt.setStartDelay(LayoutTransition.CHANGING, 0);
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mAttributeListView.setLayoutManager(layoutManager);
        mAttributeListView.setAdapter(mAttributeAdapter);
    }

    public void start(DeviceModel deviceModel, DeviceCollection deviceCollection, AferoClient aferoClient) {
        if (isStarted()) {
            return;
        }

        if (mController == null) {
            mController = new DeviceInspectorController(this, deviceCollection, aferoClient);
        }
        mController.start(deviceModel);

        mAttributeAdapter.start(deviceModel);
        mAttributeAdapter.getViewOnClick()
                .subscribe(new Action1<View>() {
                    @Override
                    public void call(View view) {
                        int pos = mAttributeListView.getChildAdapterPosition(view);
                        if (pos != RecyclerView.NO_POSITION) {
                            startAttributeEditor(mController.getDeviceModel(), mAttributeAdapter.getAttributeAt(pos));
                        }
                    }
                });

        startEnterTransition();
    }

    public void stop() {
        if (isStarted()) {
            mController.stop();
            mAttributeAdapter.stop();

            startExitTransition();
        }
    }

    public boolean isStarted() {
        return mController != null && mController.isStarted();
    }

    public Observable<DeviceInspectorView> getObservable() {
        if (mViewSubject == null) {
            mViewSubject = PublishSubject.create();
        }
        return mViewSubject;
    }

    public void setDeviceNameText(String name) {
        mDeviceNameText.setText(name);
    }

    public void setDeviceStatusText(@StringRes int statusResId) {
        mDeviceStatusText.setText(statusResId);
    }

    @OnClick(R.id.device_info_extra_open)
    void onClickDeviceInfoOpen() {
        mDeviceInfoExtraContainer.setVisibility(VISIBLE);
        mDeviceInfoOpenButton.setVisibility(GONE);
        mDeviceInfoCloseButton.setVisibility(VISIBLE);
    }

    @OnClick(R.id.device_info_extra_close)
    void onClickDeviceInfoClose() {
        mDeviceInfoExtraContainer.setVisibility(GONE);
        mDeviceInfoOpenButton.setVisibility(VISIBLE);
        mDeviceInfoCloseButton.setVisibility(GONE);
    }

    @OnClick(R.id.device_delete_button)
    void onClickDelete() {
        new AlertDialog.Builder(getContext())
                .setMessage(R.string.dialog_message_remove_device)
                .setCancelable(true)
                .setPositiveButton(R.string.button_title_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mController.deleteDevice();
                    }
                })
                .show();
    }

    @OnClick(R.id.wifi_connect_button)
    void onClickWifiConnect() {
        mController.onWifiConnect();
    }

    void onCompleted() {
        mViewSubject.onCompleted();
        mViewSubject = null;
    }

    void showProgress() {
        mProgressView.show();
    }

    void hideProgress() {
        mProgressView.hide();
    }

    private void startEnterTransition() {
        setVisibility(VISIBLE);
        mScrimView.setAlpha(0);
        mScrimView.animate().alpha(1).setDuration(TRANSITION_DURATION);
        mDeviceInfoCard.setTranslationX(getWidth());
        mDeviceInfoCard.animate().translationX(0).setDuration(TRANSITION_DURATION);
        mAttributesCard.setTranslationX(getWidth());
        mAttributesCard.animate().translationX(0).setDuration(TRANSITION_DURATION);
    }

    private void startExitTransition() {
        mScrimView.animate().alpha(0).setDuration(TRANSITION_DURATION);
        mDeviceInfoCard.animate().translationX(getWidth()).setDuration(TRANSITION_DURATION);
        mAttributesCard.animate().translationX(getWidth())
                .setDuration(TRANSITION_DURATION)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        mAttributeAdapter.clear();
                        setVisibility(INVISIBLE);
                    }
                });
    }

    private void startAttributeEditor(DeviceModel deviceModel, DeviceProfile.Attribute attribute) {
        AttributeEditorView view = ButterKnife.findById(getRootView(), R.id.attribute_editor);
        view.start(deviceModel, attribute);
    }

    public void showWifiSetup(boolean isVisible) {
        mWifiConnectButton.setVisibility(isVisible ? VISIBLE : GONE);
    }

    public void enableWifiSetup(boolean isEnabled) {
        mWifiConnectButton.setEnabled(isEnabled);
    }
}
