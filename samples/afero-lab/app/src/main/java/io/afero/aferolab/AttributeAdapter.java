/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Vector;

import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceProfile;
import io.afero.sdk.utils.RxUtils;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subjects.PublishSubject;

public class AttributeAdapter extends RecyclerView.Adapter<AttributeAdapter.ViewHolder> {

    private final Vector<DeviceProfile.Attribute> mAttributes = new Vector<>();
    private final PublishSubject<View> mOnClickViewSubject = PublishSubject.create();
    private DeviceModel mDeviceModel;
    private Subscription mDeviceUpdateSubscription;

    public AttributeAdapter() {
    }

    public void start(DeviceModel deviceModel) {
        mDeviceModel = deviceModel;
        mDeviceUpdateSubscription = deviceModel.getUpdateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<DeviceModel>() {
                    @Override
                    public void call(DeviceModel deviceModel) {
                        notifyDataSetChanged();
                    }
                });

        DeviceProfile deviceProfile = deviceModel.getProfile();

        mAttributes.clear();
        for (DeviceProfile.Service service : deviceProfile.getServices()) {
            for (DeviceProfile.Attribute attribute : service.getAttributes()) {
                mAttributes.add(attribute);
            }
        }

        notifyDataSetChanged();
    }

    public void stop() {
        mDeviceModel = null;
        mDeviceUpdateSubscription = RxUtils.safeUnSubscribe(mDeviceUpdateSubscription);
    }

    public void clear() {
        mAttributes.clear();
        notifyDataSetChanged();
    }

    Observable<View> getViewOnClick() {
        return mOnClickViewSubject;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final AttributeListItemView view = (AttributeListItemView)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_attribute_list_item, parent, false);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnClickViewSubject.onNext(view);
            }
        });

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.update(mDeviceModel, mAttributes.get(position));
    }

    @Override
    public int getItemCount() {
        return mAttributes.size();
    }

    public DeviceProfile.Attribute getAttributeAt(int itemPosition) {
        return mAttributes.get(itemPosition);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        AttributeListItemView mAttributeView;

        ViewHolder(AttributeListItemView view) {
            super(view);
            mAttributeView = view;
        }

        void update(DeviceModel deviceModel, DeviceProfile.Attribute attribute) {
            mAttributeView.update(deviceModel, attribute);
        }

    }
}
