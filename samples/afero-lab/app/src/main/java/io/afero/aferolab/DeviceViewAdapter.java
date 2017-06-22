/*
 * Copyright (c) 2017 Afero, Inc. All rights reserved.
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
import rx.functions.Func1;

public class DeviceViewAdapter extends RecyclerView.Adapter<DeviceViewAdapter.ViewHolder> {

    private Subscription mSnapshotSubscription;
    private Subscription mCreateSubscription;
    private Subscription mProfileChangeSubscription;
    private Subscription mRemoveSubscription;
    private final Vector<DeviceModel> mDevices = new Vector<>();

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
                } else {
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

    private static class DeviceIsPresentableFilter implements Func1<DeviceModel, Boolean> {
        @Override
        public Boolean call(DeviceModel deviceModel) {
            return deviceModel.getPresentation() != null;
        }
    };
    private final DeviceIsPresentableFilter mDeviceFilter = new DeviceIsPresentableFilter();

    public DeviceViewAdapter(DeviceCollection deviceCollection) {
        addDevices(deviceCollection.getDevices());

        mSnapshotSubscription = addDevices(deviceCollection.observeSnapshots()
            .flatMap(new Func1<Vector<DeviceModel>, Observable<DeviceModel>>() {
                @Override
                public Observable<DeviceModel> call(Vector<DeviceModel> deviceModels) {
                    return Observable.from(deviceModels);
                }
            }));

        mCreateSubscription = addDevices(deviceCollection.observeCreates());

        mProfileChangeSubscription = addDevices(deviceCollection.observeProfileChanges());

        mRemoveSubscription = removeDevices(deviceCollection.observeDeletes());
    }

    public void stop() {
        mSnapshotSubscription = RxUtils.safeUnSubscribe(mSnapshotSubscription);
        mCreateSubscription = RxUtils.safeUnSubscribe(mCreateSubscription);
        mRemoveSubscription = RxUtils.safeUnSubscribe(mRemoveSubscription);
        mProfileChangeSubscription = RxUtils.safeUnSubscribe(mProfileChangeSubscription);
    }

    public int indexOf(DeviceModel deviceModel) {
        return mDevices.indexOf(deviceModel);
    }

    private Subscription addDevices(Observable<DeviceModel> devices) {
        return devices
            .filter(mDeviceFilter)
            .onBackpressureBuffer()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(mAddDeviceAction);
    }

    private Subscription removeDevices(Observable<DeviceModel> devices) {
        return devices
            .observeOn(AndroidSchedulers.mainThread())
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

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_device, parent, false);
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

    static class ViewHolder extends RecyclerView.ViewHolder {

        ViewHolder(View view) {
            super(view);
        }

        public void update(DeviceModel deviceModel) {
            ((DeviceView)itemView).update(deviceModel);
        }
    }

}
