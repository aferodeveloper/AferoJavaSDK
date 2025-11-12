/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.deviceList;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.afero.aferolab.R;
import io.afero.aferolab.databinding.ViewDeviceListItemBinding;
import io.afero.sdk.device.DeviceModel;

public class DeviceListItemView extends FrameLayout {

    private ViewDeviceListItemBinding binding;

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
        binding = ViewDeviceListItemBinding.bind(this);
    }

    public void update(DeviceModel deviceModel) {
        binding.deviceName.setText(deviceModel.getName());

        int statusResId = R.string.device_status_offline;
        if (deviceModel.isAvailable()) {
            if (deviceModel.isRunning()) {
                statusResId = R.string.device_status_active;
            } else {
                statusResId = R.string.device_status_idle;
            }
        }

        binding.deviceStatus.setText(statusResId);
    }
}
