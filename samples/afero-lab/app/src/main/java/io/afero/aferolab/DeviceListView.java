/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.afero.sdk.device.DeviceCollection;
import io.afero.sdk.device.DeviceModel;
import rx.Observable;
import rx.functions.Action1;
import rx.subjects.PublishSubject;


public class DeviceListView extends FrameLayout {

    @BindView(R.id.device_recycler_view)
    RecyclerView mDeviceCardsView;

    private DeviceViewAdapter mAdapter;
    private final PublishSubject<DeviceModel> mOnClickDeviceSubject = PublishSubject.create();

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

        mAdapter.getViewOnClick().subscribe(
                new Action1<View>() {
                    @Override
                    public void call(View view) {
                        int itemPosition = mDeviceCardsView.getChildLayoutPosition(view);
                        if (itemPosition != RecyclerView.NO_POSITION) {
                            mOnClickDeviceSubject.onNext(mAdapter.getDeviceModelAt(itemPosition));
                        }
                    }
                });
    }

    public void stop() {
        mAdapter.stop();
    }

    public Observable<DeviceModel> getDeviceOnClick() {
        return mOnClickDeviceSubject;
    }
}
