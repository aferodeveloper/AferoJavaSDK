/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;

import io.afero.sdk.scheduler.OfflineScheduleEvent;
import io.afero.sdk.scheduler.OfflineScheduler;
import rx.Observable;
import rx.functions.Action2;
import rx.functions.Func0;

class DeviceDataMigrator {

    private final DeviceModel mDeviceModel;

    DeviceDataMigrator(DeviceModel deviceModel) {
        mDeviceModel = deviceModel;
    }

    Observable<DeviceDataMigrator> runMigrations() {
        Observable<OfflineScheduleEvent> o = OfflineScheduler.migrateToDeviceTimeZone(mDeviceModel);

        if (o != null) {
            return o.collect(new Func0<DeviceDataMigrator>() {
                @Override
                public DeviceDataMigrator call() {
                    return DeviceDataMigrator.this;
                }
            }, new Action2<DeviceDataMigrator, OfflineScheduleEvent>() {
                @Override
                public void call(DeviceDataMigrator ddm, OfflineScheduleEvent offlineScheduleEvent) {
                }
            });
        }

        return null;
    }
}
