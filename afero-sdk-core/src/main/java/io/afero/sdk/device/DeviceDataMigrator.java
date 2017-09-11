/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;

import java.util.ArrayList;

import io.afero.sdk.scheduler.OfflineScheduler;
import rx.functions.Action1;

class DeviceDataMigrator {

    private final DeviceModel mDeviceModel;
    private ArrayList<Action1<DeviceModel>> mMigrationActions = new ArrayList<>(1);


    DeviceDataMigrator(DeviceModel deviceModel) {
        mDeviceModel = deviceModel;

        mMigrationActions.add(new OfflineScheduleMigration());
    }

    void runMigrations() {
        for (Action1<DeviceModel> migration : mMigrationActions) {
            migration.call(mDeviceModel);
        }
    }

    private class OfflineScheduleMigration implements Action1<DeviceModel> {
        @Override
        public void call(DeviceModel deviceModel) {
            OfflineScheduler.migrateToDeviceTimeZone(deviceModel);
        }
    }
}
