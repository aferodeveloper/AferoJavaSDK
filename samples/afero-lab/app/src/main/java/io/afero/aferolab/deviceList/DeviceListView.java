/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.deviceList;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.afero.aferolab.R;
import io.afero.aferolab.databinding.ViewDeviceListBinding;
import io.afero.sdk.device.DeviceCollection;
import io.afero.sdk.device.DeviceModel;
import rx.Observable;
import rx.functions.Action1;
import rx.subjects.PublishSubject;


public class DeviceListView extends FrameLayout {

    private ViewDeviceListBinding binding;

    private DeviceViewAdapter mAdapter;
    private final PublishSubject<DeviceModel> mOnClickDeviceSubject = PublishSubject.create();

    public DeviceListView(Context context) {
        super(context);
        init();
    }

    public DeviceListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DeviceListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        binding = ViewDeviceListBinding.inflate(LayoutInflater.from(getContext()), this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        binding.deviceRecyclerView.setLayoutManager(layoutManager);

        DividerItemDecoration dividerDecoration = new DividerItemDecoration(getContext(), layoutManager.getOrientation());
        dividerDecoration.setDrawable(ContextCompat.getDrawable(getContext(), R.drawable.device_list_divider));
        binding.deviceRecyclerView.addItemDecoration(dividerDecoration);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void start(DeviceCollection deviceCollection) {
        mAdapter = new DeviceViewAdapter(deviceCollection);
        binding.deviceRecyclerView.setAdapter(mAdapter);

        mAdapter.getViewOnClick().subscribe(
                new Action1<View>() {
                    @Override
                    public void call(View view) {
                        int itemPosition = binding.deviceRecyclerView.getChildLayoutPosition(view);
                        if (itemPosition != RecyclerView.NO_POSITION) {
                            mOnClickDeviceSubject.onNext(mAdapter.getDeviceModelAt(itemPosition));
                        }
                    }
                });
    }

    public void stop() {
        if (mAdapter != null) {
            mAdapter.stop();
        }
    }

    public Observable<DeviceModel> getDeviceOnClick() {
        return mOnClickDeviceSubject;
    }
}
