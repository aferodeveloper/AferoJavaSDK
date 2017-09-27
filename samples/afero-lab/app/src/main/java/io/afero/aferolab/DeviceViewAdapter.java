/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import io.afero.sdk.device.DeviceCollection;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.utils.RxUtils;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subjects.PublishSubject;

public class DeviceViewAdapter extends RecyclerView.Adapter<DeviceViewAdapter.ViewHolder> {

    private Subscription mUpdateSubscription;
    private Subscription mRemoveSubscription;
    private final Vector<DeviceModel> mDevices = new Vector<>();
    private final PublishSubject<View> mOnClickViewSubject = PublishSubject.create();

    private Comparator<DeviceModel> mSortComparator = new Comparator<DeviceModel>() {
        @Override
        public int compare(DeviceModel a, DeviceModel b) {
            return a.getName().compareToIgnoreCase(b.getName());
        }
    };

    private final Action1<DeviceModel> mAddDeviceAction = new Action1<DeviceModel>() {
        @Override
        public void call(DeviceModel deviceModel) {
            synchronized (mDevices) {
                int i = mDevices.indexOf(deviceModel);
                if (i != -1) {
                    mDevices.remove(i);

                    if (!isDevicePresentable(deviceModel)) {
                        notifyItemRemoved(i);
                        return;
                    }

                    int newIndex = Collections.binarySearch(mDevices, deviceModel, mSortComparator);
                    if (newIndex < 0) {
                        newIndex = -newIndex - 1;
                    }

                    mDevices.add(newIndex, deviceModel);

                    if (newIndex != i) {
                        notifyItemMoved(i, newIndex);
                    } else {
                        notifyItemChanged(i);
                    }
                } else if (isDevicePresentable(deviceModel)) {
                    i = Collections.binarySearch(mDevices, deviceModel, mSortComparator);
                    if (i < 0) {
                        i = -i - 1;
                    }

                    mDevices.add(i, deviceModel);
                    notifyItemInserted(i);
                }
            }
        }
    };

    public DeviceViewAdapter(DeviceCollection deviceCollection) {
        mUpdateSubscription = updateDevices(deviceCollection.getDevices()
                .concatWith(deviceCollection.observeCreates()
                        .mergeWith(deviceCollection.observeUpdates())));
        mRemoveSubscription = removeDevices(deviceCollection.observeDeletes());
    }

    public void stop() {
        mUpdateSubscription = RxUtils.safeUnSubscribe(mUpdateSubscription);
        mRemoveSubscription = RxUtils.safeUnSubscribe(mRemoveSubscription);
    }

    Observable<View> getViewOnClick() {
        return mOnClickViewSubject;
    }

    public int indexOf(DeviceModel deviceModel) {
        return mDevices.indexOf(deviceModel);
    }

    private Subscription updateDevices(Observable<DeviceModel> devices) {
        return devices.onBackpressureBuffer()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(mAddDeviceAction);
    }

    private Subscription removeDevices(Observable<DeviceModel> devices) {
        return devices.observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<DeviceModel>() {
                @Override
                public void call(DeviceModel deviceModel) {
                    synchronized (mDevices) {
                        int i = mDevices.indexOf(deviceModel);
                        if (i != -1) {
                            mDevices.remove(i);
                            notifyItemRemoved(i);
                        }
                    }
                }
            });
    }

    private boolean isDevicePresentable(DeviceModel deviceModel) {
        return deviceModel.getPresentation() != null;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_device_list_item, parent, false);
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
        DeviceModel deviceModel = mDevices.get(position);
        holder.update(deviceModel);
    }

    @Override
    public int getItemCount() {
        return mDevices.size();
    }

    public DeviceModel getDeviceModelAt(int itemPosition) {
        return mDevices.get(itemPosition);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ViewHolder(View view) {
            super(view);
        }

        public void update(DeviceModel deviceModel) {
            ((DeviceListItemView)itemView).update(deviceModel);
        }
    }

}
