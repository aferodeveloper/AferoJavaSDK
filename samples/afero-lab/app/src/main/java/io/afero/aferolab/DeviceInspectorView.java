/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceProfile;
import rx.functions.Action1;


public class DeviceInspectorView extends FrameLayout {

    @BindView(R.id.device_name_text)
    TextView mDeviceNameText;

    @BindView(R.id.device_status_text)
    TextView mDeviceStatusText;

    @BindView(R.id.device_attribute_recycler_view)
    RecyclerView mAttributeListView;

    @BindView(R.id.device_info_card)
    View mDeviceInfoCard;

    @BindView(R.id.attributes_card)
    View mAttributesCard;

    @BindView(R.id.view_scrim)
    View mScrimView;

    private static final int TRANSITION_DURATION = 200;

    private final DeviceInspectorController mController = new DeviceInspectorController(this);
    private final AttributeAdapter mAttributeAdapter = new AttributeAdapter();

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

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mAttributeListView.setLayoutManager(layoutManager);
        mAttributeListView.setAdapter(mAttributeAdapter);
    }

    public void start(DeviceModel deviceModel) {
        if (isStarted()) {
            return;
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
        return mController.isStarted();
    }

    public void setDeviceNameText(String name) {
        mDeviceNameText.setText(name);
    }

    public void setDeviceStatusText(@StringRes int statusResId) {
        mDeviceStatusText.setText(statusResId);
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
}
