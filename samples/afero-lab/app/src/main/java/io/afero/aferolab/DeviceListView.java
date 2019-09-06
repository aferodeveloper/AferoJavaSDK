/*
 * Copyright (c) 2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.afero.sdk.device.DeviceCollection;

public class DeviceListView extends FrameLayout {

    @BindView(R.id.device_recycler_view)
    RecyclerView mDeviceCardsView;

    private DeviceViewAdapter mAdapter;

    public DeviceListView(Context context) {
        super(context);
    }

    public DeviceListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DeviceListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.bind(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mDeviceCardsView.setLayoutManager(layoutManager);

        DividerItemDecoration dividerDecoration = new DividerItemDecoration(getContext(), layoutManager.getOrientation());
        dividerDecoration.setDrawable(ContextCompat.getDrawable(getContext(), R.drawable.device_list_divider));
        mDeviceCardsView.addItemDecoration(dividerDecoration);
    }

    public void start(DeviceCollection deviceCollection) {
        mAdapter = new DeviceViewAdapter(deviceCollection);
        mDeviceCardsView.setAdapter(mAdapter);
    }

    public void stop() {
        mAdapter.stop();
    }

    public void setAdapter(RecyclerView.Adapter adapter) {
        mDeviceCardsView.setAdapter(adapter);
    }
}
