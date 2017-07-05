/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.DeviceAssociateResponse;
import io.afero.sdk.client.afero.models.DeviceStatus;
import io.afero.sdk.conclave.ConclaveMessage;
import io.afero.sdk.conclave.DeviceEventSource;
import io.afero.sdk.conclave.models.DeviceError;
import io.afero.sdk.conclave.models.DeviceMute;
import io.afero.sdk.conclave.models.DeviceState;
import io.afero.sdk.conclave.models.DeviceSync;
import io.afero.sdk.conclave.models.InvalidateMessage;
import io.afero.sdk.conclave.models.OTAInfo;
import io.afero.sdk.log.AfLog;
import io.afero.sdk.utils.MetricUtil;
import io.afero.sdk.utils.RxUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.PublishSubject;

/**
 * The DeviceCollection class manages the collection of {@link DeviceModel}s associated with the
 * active {@link AferoClient} account.
 */
public class DeviceCollection {

    private final DeviceEventSource mDeviceEventSource;
    private final DeviceProfileCollection mDeviceProfileCollection;
    private final AferoClient mAferoClient;
    private final TreeMap<String,DeviceModel> mModelMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    private PublishSubject<DeviceModel> mModelCreateSubject = PublishSubject.create();
    private PublishSubject<DeviceModel> mModelUpdateSubject = PublishSubject.create();
    private PublishSubject<DeviceCollection> mModelSnapshotSubject = PublishSubject.create();
    private PublishSubject<DeviceModel> mModelDeleteSubject = PublishSubject.create();
    private PublishSubject<DeviceModel> mModelProfileChangeSubject = PublishSubject.create();

    private Subscription mInvalidateSubscription;
    private Subscription mMetricSubscription;
    private Subscription mDeviceErrorSubscription;
    private Subscription mOTASubscription;
    private Subscription mSnapshotSubscription;
    private Subscription mAttributeChangeSubscription;
    private Subscription mStatusChangeSubscription;
    private Subscription mMuteSubscription;

    /**
     * Constructs a {@code DeviceCollection}. The contents of the collection are managed dynamically
     * in response to messages received from the specified deviceEventSource.
     *
     * @param deviceEventSource Source for all device messages
     * @param deviceProfileCollection The {@link DeviceProfileCollection} used to store/retrieve the
     * {@link DeviceProfile}s association with the devices in the collection.
     * @param aferoClient The {@link AferoClient} used by the {@code DeviceCollection} to associate
     *                    and disassociate devices with the active account.
     */
    public DeviceCollection(DeviceEventSource deviceEventSource, DeviceProfileCollection deviceProfileCollection, AferoClient aferoClient) {
        mDeviceEventSource = deviceEventSource;
        mDeviceProfileCollection = deviceProfileCollection;
        mAferoClient = aferoClient;
    }

    /**
     * Starts {@code DeviceCollection} operations. When {@code start} completes the
     * {@code DeviceCollection} will be subscribed to relevant {@link DeviceEventSource}
     * observables.
     */
    public void start() {

        mOTASubscription = mDeviceEventSource.observeOTA().subscribe(new Action1<OTAInfo>() {
            @Override
            public void call(OTAInfo otaInfo) {
                DeviceModel deviceModel = getModel(otaInfo.id);
                if (deviceModel != null) {
                    AfLog.d("mDeviceEventSource.observeOTA state="+otaInfo.state);
                    deviceModel.onOTA(otaInfo);
                }
            }
        });

        mSnapshotSubscription = mDeviceEventSource.observeSnapshot()
            .flatMap(new Func1<DeviceSync[], Observable<DeviceSync[]>>() {
                // Make sure we have a profile for the new device in our local registry
                // If not, fetch it before passing it on...
                @Override
                public Observable<DeviceSync[]> call(final DeviceSync[] deviceSyncs) {
                    AfLog.i("DeviceCollection.flatMap('snapshot'): deviceSync[].length=" + deviceSyncs.length);

                    // TODO: we're always fetching all profiles. optimize this. (use api filtering?)
                    return mDeviceProfileCollection
                        .fetchAccountProfiles()
                        .zipWith(Observable.just(deviceSyncs), new Func2<DeviceProfile[], DeviceSync[], DeviceSync[]>() {
                            @Override
                            public DeviceSync[] call(DeviceProfile[] deviceProfiles, DeviceSync[] ds) {
                                return ds;
                            }
                        });
                }
            })
            .doOnNext(new Action1<DeviceSync[]>() {
                @Override
                public void call(DeviceSync[] deviceSyncs) {

                    AfLog.i("DeviceCollection.onNext('snapshot'): deviceSync[].length=" + deviceSyncs.length);

                    TreeSet<String> deviceSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                    for (DeviceSync ds : deviceSyncs) {
                        deviceSet.add(ds.id);
                    }

                    for (DeviceSync ds : deviceSyncs) {
                        AfLog.i("DeviceCollection.onNext('snapshot'): deviceSync=" + ds.toString());
                        addOrUpdate(ds);
                    }

                    ArrayList<DeviceModel> removedDevices;
                    synchronized (mModelMap) {
                        removedDevices = new ArrayList<>(mModelMap.size());
                        Iterator<DeviceModel> iter = mModelMap.values().iterator();

                        while (iter.hasNext()) {
                            DeviceModel dm = iter.next();
                            if (!deviceSet.contains(dm.getId())) {
                                iter.remove();
                                removedDevices.add(dm);
                            }
                        }
                    }

                    // publish the deletes outside the synchronized block to avoid badness
                    for (DeviceModel dm : removedDevices) {
                        mModelDeleteSubject.onNext(dm);
                    }
                }
            })
            .subscribe(
                new Action1<DeviceSync[]>() {    // onNext
                    @Override
                    public void call(DeviceSync[] deviceSync) {

                        for (DeviceSync ds : deviceSync) {
                            DeviceModel deviceModel = getModel(ds.id);
                            DeviceProfile profile = mDeviceProfileCollection.getProfileFromID(ds.profileId);
                            if (profile != null) {
                                deviceModel.setProfile(profile);
                            }
                        }

                        mModelSnapshotSubject.onNext(DeviceCollection.this);
                    }
                },
                new Action1<Throwable>() {   // onError
                    @Override
                    public void call(Throwable t) {
                        AfLog.i("DeviceCollection.observeSnapshot.onError: e=" + t.toString());
                        AfLog.e(t);
                    }
                });

        mAttributeChangeSubscription = mDeviceEventSource.observeAttributeChange().onBackpressureBuffer()
            .subscribe(
                    new Action1<DeviceSync>() {    // onNext
                        @Override
                        public void call(DeviceSync deviceSync) {
                            AfLog.i("DeviceCollection.observeUpdate.onNext: deviceSync=" + deviceSync.toString());
                            DeviceModel deviceModel = getModel(deviceSync.id);
                            if (deviceModel != null) {
                                deviceModel.update(deviceSync);
                                mModelUpdateSubject.onNext(deviceModel);
                            }
                        }
                    },
                    new Action1<Throwable>() {   // onError
                        @Override
                        public void call(Throwable t) {
                            AfLog.i("DeviceCollection.observeUpdate.onError: e=" + t.toString());
                            AfLog.e(t);
                        }
                    });

        mStatusChangeSubscription = mDeviceEventSource.observeStatusChange().onBackpressureBuffer()
            .subscribe(
                    new Action1<DeviceState>() {    // onNext
                        @Override
                        public void call(DeviceState deviceState) {
//                            AfLog.i("DeviceCollection.observeState.onNext: deviceState=" + deviceState.toString());
                            DeviceModel deviceModel = getModel(deviceState.id);
                            if (deviceModel != null) {
                                deviceModel.update(deviceState.status);
                                mModelUpdateSubject.onNext(deviceModel);
                            }
                        }
                    },
                    new Action1<Throwable>() {   // onError
                        @Override
                        public void call(Throwable t) {
                            AfLog.i("DeviceCollection.observeState.onError: e=" + t.toString());
                            AfLog.e(t);
                        }
                    });

        mMuteSubscription = mDeviceEventSource.observeMute().onBackpressureBuffer()
            .subscribe(
                    new Action1<DeviceMute>() {    // onNext
                        @Override
                        public void call(DeviceMute deviceMute) {
//                            AfLog.i("DeviceCollection.observeMute.onNext: deviceMute=" + deviceMute.toString());
                            DeviceModel deviceModel = getModel(deviceMute.id);
                            if (deviceModel != null) {
                                deviceModel.onMute(deviceMute);
                            }
                        }
                    },
                    new Action1<Throwable>() {   // onError
                        @Override
                        public void call(Throwable t) {
                            AfLog.i("DeviceCollection.observeMute.onError: e=" + t.toString());
                            AfLog.e(t);
                        }
                    });

        mDeviceErrorSubscription = mDeviceEventSource.observeError().onBackpressureBuffer()
            .subscribe(
                    new Action1<DeviceError>() {    // onNext
                        @Override
                        public void call(DeviceError deviceError) {
//                            AfLog.i("DeviceCollection.observeState.onNext: deviceState=" + deviceState.toString());
                            DeviceModel deviceModel = getModel(deviceError.id);
                            if (deviceModel != null) {
                                deviceModel.onError(deviceError);
                            }
                        }
                    },
                    new Action1<Throwable>() {   // onError
                        @Override
                        public void call(Throwable t) {
                            AfLog.i("DeviceCollection.observeState.onError: e=" + t.toString());
                            AfLog.e(t);
                        }
                    });

        mInvalidateSubscription = mDeviceEventSource.observeInvalidate()
            .subscribe(new Action1<InvalidateMessage>() {
                @Override
                public void call(InvalidateMessage im) {
                    try {
                        String deviceId = im.json.get("deviceId").asText();
                        DeviceModel deviceModel = getModel(deviceId);
                        if (deviceModel == null) {
                            AfLog.e("Got invalidate on unknown deviceId: " + deviceId);
                            return;
                        }

                        switch (im.kind.toLowerCase()) {
                            case "profiles":
                                String profileId = im.json.get("profileId").asText();
                                updateDeviceProfile(deviceModel, profileId);
                                break;
                            case "location":
                                deviceModel.updateLocation();
                                break;
                        }
                    } catch (Exception e) {
                        AfLog.e("Unable to parse invalidate json: " + e);
                    }
                }
            });

        mMetricSubscription = MetricUtil.getInstance().getEventObservable().subscribe(new Observer<ConclaveMessage.Metric>() {
            @Override
            public void onCompleted() {}

            @Override
            public void onError(Throwable e) {}

            @Override
            public void onNext(ConclaveMessage.Metric metric) {
                mDeviceEventSource.sendMetrics(metric);
            }
        });
    }

    /**
     * Stops all {@code DeviceCollection} operations. When {@code stop} completes the
     * {@code DeviceCollection} will be unsubscribed from relevant {@link DeviceEventSource}
     * observables.
     */
    public void stop() {
        mOTASubscription = RxUtils.safeUnSubscribe(mOTASubscription);
        mSnapshotSubscription = RxUtils.safeUnSubscribe(mSnapshotSubscription);
        mAttributeChangeSubscription = RxUtils.safeUnSubscribe(mAttributeChangeSubscription);
        mStatusChangeSubscription = RxUtils.safeUnSubscribe(mStatusChangeSubscription);
        mMuteSubscription = RxUtils.safeUnSubscribe(mMuteSubscription);
        mDeviceErrorSubscription = RxUtils.safeUnSubscribe(mDeviceErrorSubscription);
        mInvalidateSubscription = RxUtils.safeUnSubscribe(mInvalidateSubscription);
        mMetricSubscription = RxUtils.safeUnSubscribe(mMetricSubscription);
    }

    /**
     * Adds the device specified by the association id to the {@link AferoClient} active account.
     * Should initially be called with {@code isOwnershipVerified} set to {@code false}. If
     * {@code TransferVerificationRequired} error is returned, the user should be prompted whether
     * to transfer onwership of the device from its current account. If user answers affirmatively,
     * {@code addDevice} should be called again with {@code isOwnershipVerified} set to {@code true}
     * to complete the ownership transfer.
     *
     * @param associationId The association id representing the physical device, usually obtained
     *                      either via QR code scan or manual entry.
     * @param isOwnershipVerified Set to true to transfer eligible devices from their current owner
     *                            account.
     * @return {@link Observable} that returns the newly added {@link DeviceModel} or an error. If
     * TransferVerificationRequired error is returned, user should be prompted whether to take
     * ownership of device. If user answers affirmatively, addDevice should be called again with
     * {@param isOwnershipVerified} set to true.
     */
    public Observable<DeviceModel> addDevice(String associationId, boolean isOwnershipVerified) {
        return mAferoClient.deviceAssociateGetProfile(associationId, isOwnershipVerified, mDeviceProfileCollection.getLocale(), mDeviceProfileCollection.getImageSize())
            .onErrorResumeNext(new Func1<Throwable, Observable<? extends DeviceAssociateResponse>>() {
                @Override
                public Observable<? extends DeviceAssociateResponse> call(Throwable t) {
                    if (mAferoClient.isTransferVerificationError(t)) {
                        return Observable.error(new AferoClient.TransferVerificationRequired());
                    }

                    return Observable.error(t);
                }
            })
            .map(new MapDeviceAssociateResponseToDeviceModel());
    }

    /**
     * Removes the device specified by the association id from the {@link AferoClient} active account.
     *
     * @param deviceModel The {@link DeviceModel} to be removed from the {@link AferoClient} active
     *                    account.
     * @return {@link Observable} that returns the removed {@link DeviceModel} or an error.
     */
    public Observable<DeviceModel> removeDevice(DeviceModel deviceModel) {
        return mAferoClient.deviceDisassociate(deviceModel)
            .doOnNext(new DeviceDisassociateAction(this));
    }

    /**
     * @return {@link Observable} containing a snapshot of all devices in the {@code DeviceCollection}
     */
    public Observable<DeviceModel> getDevices() {
        // Make a copy of the map since the original could change while the Observable is iterating.
        Map<String,DeviceModel> mapCopy = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        synchronized (mModelMap) {
            mapCopy.putAll(mModelMap);
            return Observable.from(mapCopy.values());
        }
    }

    /**
     * @param deviceId Identifier of a {@link DeviceModel}.
     * @return the {@link DeviceModel} with the specified {@code deviceId}, or {@null} if no such
     * {@link DeviceModel} exists.
     */
    public DeviceModel getModel(String deviceId) {
        synchronized (mModelMap) {
            return mModelMap.get(deviceId);
        }
    }

    /**
     * @return Observable that emits {@link DeviceModel}s as they are created and added to the
     * collection either as the result of events from {@link DeviceEventSource} or a call to
     * {@link DeviceCollection#addDevice(String, boolean)}.
     */
    public Observable<DeviceModel> observeCreates() {
        return mModelCreateSubject;
    }

    /**
     * @return Observable that emits {@link DeviceModel}s as they are removed from the collection
     * either as the result of events from {@link DeviceEventSource} or a call to
     * {@link DeviceCollection#removeDevice(DeviceModel)}.
     */
    public Observable<DeviceModel> observeDeletes() {
        return mModelDeleteSubject.onBackpressureBuffer();
    }

    /**
     * @return Observable that emits {@link DeviceModel}s that have received a new
     * {@link DeviceProfile}
     */
    public Observable<DeviceModel> observeProfileChanges() {
        return mModelProfileChangeSubject;
    }

    /**
     * @return Observable that emits the DeviceCollection whenever a new "snapshot" of devices has
     * been processed.
     */
    public Observable<DeviceCollection> observeSnapshots() {
        return mModelSnapshotSubject;
    }

    /**
     * @return The current count of {@link DeviceModel}s in the collection.
     */
    public int getCount() {
        synchronized (mModelMap) {
            return mModelMap.size();
        }
    }

    /**
     * Removes all {@link DeviceModel}s from the local cache. Typically done when signing out
     * or switching active accounts.
     */
    public void reset() {

        Observable<DeviceModel> devices = getDevices();

        synchronized (mModelMap) {
            mModelMap.clear();
        }

        devices.subscribe(new Action1<DeviceModel>() {
            @Override
            public void call(DeviceModel deviceModel) {
                mModelDeleteSubject.onNext(deviceModel);
            }
        });
    }

    private DeviceModel addOrUpdate(String deviceId, DeviceStatus ds, DeviceProfile deviceProfile) {
        synchronized (mModelMap) {
            DeviceModel deviceModel = getModel(deviceId);
            if (deviceModel != null) {
                deviceModel.update(ds);
            } else {
                deviceModel = add(deviceId, ds, deviceProfile);
            }
            return deviceModel;
        }
    }

    private DeviceModel addOrUpdate(DeviceSync ds) {
        synchronized (mModelMap) {
            DeviceModel deviceModel = getModel(ds.id);
            if (deviceModel != null) {
                deviceModel.update(ds);
            } else {
                add(ds);
            }
            return deviceModel;
        }
    }

    private void updateDeviceProfile(final DeviceModel deviceModel, String profileId) {
        mDeviceProfileCollection
            .fetchDeviceProfile(profileId)
            .subscribe(new Observer<DeviceProfile>() {
                @Override
                public void onCompleted() {
                }

                @Override
                public void onError(Throwable e) {
                    AfLog.e(e);
                }

                @Override
                public void onNext(DeviceProfile profile) {
                    deviceModel.setProfile(profile);
                    mModelProfileChangeSubject.onNext(deviceModel);
                }
            });
    }

    private DeviceModel add(DeviceSync ds) {
        DeviceProfile profile = mDeviceProfileCollection.getProfileFromID(ds.profileId);

        if (profile != null) {
            DeviceModel deviceModel = new DeviceModel(ds.id, profile, false, mAferoClient);
            deviceModel.update(ds);

            return add(deviceModel);
        }

        return null;
    }

    private DeviceModel add(String deviceId, DeviceStatus ds, DeviceProfile profile) {

        if (profile != null) {
            DeviceModel deviceModel = new DeviceModel(deviceId, profile, false, mAferoClient);
            deviceModel.update(ds);

            return add(deviceModel);
        }

        return null;
    }

    private DeviceModel add(DeviceModel deviceModel) {
        synchronized (mModelMap) {
            mModelMap.put(deviceModel.getId(), deviceModel);
        }

        mModelCreateSubject.onNext(deviceModel);

        return deviceModel;
    }

    private void onDeleteDevice(DeviceModel deviceModel) {
        synchronized (mModelMap) {
            mModelMap.remove(deviceModel.getId());
        }

        mModelDeleteSubject.onNext(deviceModel);
    }

    private static class DeviceDisassociateAction extends RxUtils.WeakAction1<DeviceModel, DeviceCollection> {

        DeviceDisassociateAction(DeviceCollection dc) {
            super(dc);
        }

        @Override
        public void call(DeviceCollection deviceCollection, DeviceModel deviceModel) {
            deviceCollection.onDeleteDevice(deviceModel);
        }
    }

    private class MapDeviceAssociateResponseToDeviceModel implements Func1<DeviceAssociateResponse, DeviceModel> {
        @Override
        public DeviceModel call(DeviceAssociateResponse response) {
            return addOrUpdate(response.deviceId, response.deviceState, response.profile);
        }
    }
}
