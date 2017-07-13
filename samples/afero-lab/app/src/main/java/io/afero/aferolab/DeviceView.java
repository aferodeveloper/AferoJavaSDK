/*
 * Copyright (c) 2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.afero.sdk.device.DeviceModel;

public class DeviceView extends FrameLayout {

    @BindView(R.id.device_name)
    TextView mNameText;

    @BindView(R.id.device_status)
    TextView mStatusText;

    public DeviceView(@NonNull Context context) {
        super(context);
    }

    public DeviceView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DeviceView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.bind(this);
    }

    public void update(DeviceModel deviceModel) {
        mNameText.setText(deviceModel.getName());

        int statusResId = R.string.device_status_offline;
        if (deviceModel.isAvailable()) {
            if (deviceModel.isRunning()) {
                statusResId = R.string.device_status_active;
            } else {
                statusResId = R.string.device_status_idle;
            }
        }

        mStatusText.setText(statusResId);
    }
}
