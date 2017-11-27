/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.deviceList;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.afero.aferolab.R;
import io.afero.sdk.device.DeviceModel;

public class DeviceListItemView extends FrameLayout {

    @BindView(R.id.device_name)
    TextView mNameText;

    @BindView(R.id.device_status)
    TextView mStatusText;

    public DeviceListItemView(@NonNull Context context) {
        super(context);
    }

    public DeviceListItemView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DeviceListItemView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
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
