/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;

import java.util.ArrayList;

import io.afero.sdk.scheduler.OfflineScheduler;
import io.afero.sdk.utils.RxUtils;
import rx.Subscription;
import rx.functions.Action1;

class DeviceDataMigrator {

    private final DeviceCollection mDeviceCollection;

    private ArrayList<Action1<DeviceModel>> mMigrationActions = new ArrayList<>(1);

    private Subscription mDeviceCreateSubscription;


    private final Action1<DeviceModel> mOnNextDeviceAction = new Action1<DeviceModel>() {
        @Override
        public void call(DeviceModel deviceModel) {
            onNextDevice(deviceModel);
        }
    };

    DeviceDataMigrator(DeviceCollection deviceCollection) {
        mDeviceCollection = deviceCollection;

        mMigrationActions.add(new OfflineScheduleMigration());
    }

    void start() {
        mDeviceCreateSubscription = mDeviceCollection.observeCreates()
            .subscribe(mOnNextDeviceAction);

        mDeviceCollection.getDevices()
                .subscribe(mOnNextDeviceAction);
    }

    void stop() {
        mDeviceCreateSubscription = RxUtils.safeUnSubscribe(mDeviceCreateSubscription);
    }

    private void onNextDevice(DeviceModel deviceModel) {
        for (Action1<DeviceModel> migration : mMigrationActions) {
            migration.call(deviceModel);
        }
    }

    private class OfflineScheduleMigration implements Action1<DeviceModel> {
        @Override
        public void call(DeviceModel deviceModel) {
            OfflineScheduler.migrateAllToDeviceTimeZone(deviceModel);
        }
    }
}
