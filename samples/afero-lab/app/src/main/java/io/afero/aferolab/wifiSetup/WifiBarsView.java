/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.wifiSetup;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.afero.aferolab.R;


public class WifiBarsView extends FrameLayout {

    private ImageView mWifiBarsImageView;

    private static final int[] sBarsResIds = {
            R.drawable.ic_signal_wifi_0_bar_black_24dp,
            R.drawable.ic_signal_wifi_1_bar_black_24dp,
            R.drawable.ic_signal_wifi_2_bar_black_24dp,
            R.drawable.ic_signal_wifi_3_bar_black_24dp,
            R.drawable.ic_signal_wifi_4_bar_black_24dp,
    };

    private static final int[] sBarsLockResIds = {
            R.drawable.ic_signal_wifi_0_bar_black_24dp,
            R.drawable.ic_signal_wifi_1_bar_lock_black_24dp,
            R.drawable.ic_signal_wifi_2_bar_lock_black_24dp,
            R.drawable.ic_signal_wifi_3_bar_lock_black_24dp,
            R.drawable.ic_signal_wifi_4_bar_lock_black_24dp,
    };

    private WifiBarsPresenter mPresenter;

    public WifiBarsView(@NonNull Context context) {
        super(context);
    }

    public WifiBarsView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public WifiBarsView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mWifiBarsImageView = findViewById(R.id.wifi_bars);

        mPresenter = new WifiBarsPresenter(this);
    }

    public void setStatus(int rssi, boolean isSecure) {
        mPresenter.setStatus(rssi, isSecure);
    }

    public void setBars(int bars) {
    }

    void showStatus(int bars, boolean isSecure) {
        int[] resIds = isSecure ? sBarsLockResIds : sBarsResIds;
        int index = Math.min(resIds.length - 1, Math.max(0, bars));
        mWifiBarsImageView.setImageResource(resIds[index]);
    }
}
