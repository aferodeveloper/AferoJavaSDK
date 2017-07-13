/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;

import java.util.HashMap;

import io.afero.sdk.client.afero.AferoClient;
import rx.Observable;
import rx.functions.Action1;
import rx.subjects.PublishSubject;

class DeviceProfileCollection {

    private final AferoClient mAferoClient;
    private HashMap<String, DeviceProfile> mProfiles = new HashMap<>();
    private PublishSubject<DeviceProfile> mProfileSubject = PublishSubject.create();

    DeviceProfileCollection(AferoClient aferoClient) {
        mAferoClient = aferoClient;
    }

    private DeviceProfile addProfile(DeviceProfile profile) {
        mProfiles.put(profile.getId(), profile);
        mProfileSubject.onNext(profile);
        return profile;
    }

    DeviceProfile getProfileFromID(String profileId) {
        return mProfiles.get(profileId);
    }

    Observable<DeviceProfile[]> fetchAccountProfiles() {
        return mAferoClient.getAccountDeviceProfiles()
            .doOnNext(new Action1<DeviceProfile[]>() {
                @Override
                public void call(DeviceProfile[] deviceProfiles) {
                    for (DeviceProfile dp : deviceProfiles) {
                        addProfile(dp);
                    }
                }
            });
    }

    Observable<DeviceProfile> fetchDeviceProfile(String profileId) {
        return mAferoClient.getDeviceProfile(profileId)
            .doOnNext(new Action1<DeviceProfile>() {
                @Override
                public void call(DeviceProfile dp) {
                    addProfile(dp);
                }
            });
    }

    Observable<DeviceProfile> getObservable() {
        return mProfileSubject;
    }
}
