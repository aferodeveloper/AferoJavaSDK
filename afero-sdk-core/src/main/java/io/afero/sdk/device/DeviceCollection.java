/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;

import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.DeviceAssociateResponse;
import io.afero.sdk.client.afero.models.DeviceStatus;
import io.afero.sdk.conclave.ConclaveMessage;
import io.afero.sdk.conclave.ConclaveMessageSource;
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
 * This class manages a collection of Afero peripheral devices. It manages the collection dynamically
 * by creating and deleting {@link DeviceModel} objects in response to messages received via the
 * the {@link ConclaveMessageSource}.
 */
public class DeviceCollection {

    private final ConclaveMessageSource mConclaveMessageSource;
    private final DeviceProfileCollection mDeviceProfileCollection;
    private final AferoClient mAferoClient;
    private final Vector<DeviceModel> mModels = new Vector<>();
    private final TreeMap<String,DeviceModel> mModelMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    private PublishSubject<DeviceModel> mModelCreateSubject = PublishSubject.create();
    private PublishSubject<DeviceModel> mModelUpdateSubject = PublishSubject.create();
    private PublishSubject<Vector<DeviceModel>> mModelSnapshotSubject = PublishSubject.create();
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

    public DeviceCollection(ConclaveMessageSource messageSource, DeviceProfileCollection deviceProfileCollection, AferoClient aferoClient) {
        mConclaveMessageSource = messageSource;
        mDeviceProfileCollection = deviceProfileCollection;
        mAferoClient = aferoClient;
    }

    public void start() {

        mOTASubscription = mConclaveMessageSource.observeOTA().subscribe(new Action1<OTAInfo>() {
            @Override
            public void call(OTAInfo otaInfo) {
                DeviceModel deviceModel = mModelMap.get(otaInfo.id);
                if (deviceModel != null) {
                    AfLog.d("mConclaveMessageSource.observeOTA state="+otaInfo.state);
                    deviceModel.onOTA(otaInfo);
                }
            }
        });

        mSnapshotSubscription = mConclaveMessageSource.observeSnapshot()
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

                    synchronized (mModels) {
                        for (int i = mModels.size(); --i >= 0; ) {
                            DeviceModel dm = mModels.elementAt(i);
                            if (!deviceSet.contains(dm.getId())) {
                                onDeleteDevice(dm);
                            }
                        }
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

                        mModelSnapshotSubject.onNext(mModels);
                    }
                },
                new Action1<Throwable>() {   // onError
                    @Override
                    public void call(Throwable t) {
                        AfLog.i("DeviceCollection.observeSnapshot.onError: e=" + t.toString());
                        AfLog.e(t);
                    }
                });

        mAttributeChangeSubscription = mConclaveMessageSource.observeAttributeChange().onBackpressureBuffer()
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

        mStatusChangeSubscription = mConclaveMessageSource.observeStatusChange().onBackpressureBuffer()
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

        mMuteSubscription = mConclaveMessageSource.observeMute().onBackpressureBuffer()
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

        mDeviceErrorSubscription = mConclaveMessageSource.observeError().onBackpressureBuffer()
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

        mInvalidateSubscription = mConclaveMessageSource.observeInvalidate()
            .subscribe(new Action1<InvalidateMessage>() {
                @Override
                public void call(InvalidateMessage im) {
                    try {
                        String deviceId = im.json.get("deviceId").asText();
                        DeviceModel deviceModel = mModelMap.get(deviceId);
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
                mConclaveMessageSource.sendMetrics(metric);
            }
        });
    }

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

    public Observable<DeviceModel> addDevice(String associationId, boolean isOwnershipVerified) {
        return mAferoClient.deviceAssociate(associationId, isOwnershipVerified, mDeviceProfileCollection.getLocale(), mDeviceProfileCollection.getImageSize())
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

    public Observable<DeviceModel> removeDevice(DeviceModel deviceModel) {
        return mAferoClient.deviceDisassociate(deviceModel)
            .doOnNext(new DeviceDisassociateAction(this));
    }

    public void reset() {
        mModelMap.clear();

        for (DeviceModel device : mModels) {
            mModelDeleteSubject.onNext(device);
        }

        mModels.clear();
    }

    public Observable<DeviceModel> getDevices() {
        return Observable.from(mModels);
    }

    public Observable<DeviceModel> observeCreates() {
        return mModelCreateSubject;
    }

    public Observable<DeviceModel> observeProfileChanges() {
        return mModelProfileChangeSubject;
    }

    public Observable<Vector<DeviceModel>> observeSnapshots() {
        return mModelSnapshotSubject;
    }

    public Observable<DeviceModel> observeDeletes() {
        return mModelDeleteSubject.onBackpressureBuffer();
    }

    public boolean hasUnAvailableDevices() {

        for (DeviceModel dm : mModels) {
            if (dm.getPresentation() != null && !dm.isAvailable()) {
                return true;
            }
        }

        return false;
    }

    public boolean hasAnyUserDevices() {

        for (DeviceModel dm : mModels) {
            if (dm.getPresentation() != null) {
                return true;
            }
        }

        return false;
    }

    public boolean contains(DeviceModel model) {
        return mModels.contains(model);
    }

    public int getCount() {
        return mModels.size();
    }

    public DeviceModel getModelAt(int i) {
        return mModels.elementAt(i);
    }

    public DeviceModel getModel(String id) {
        return mModelMap.get(id);
    }

    private DeviceModel addOrUpdate(String deviceId, DeviceStatus ds, DeviceProfile deviceProfile) {
        synchronized (mModels) {
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
        synchronized (mModels) {
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
        synchronized (mModels) {
            mModels.add(deviceModel);
            mModelMap.put(deviceModel.getId(), deviceModel);
        }

        mModelCreateSubject.onNext(deviceModel);

        return deviceModel;
    }

    private void onDeleteDevice(DeviceModel deviceModel) {
        synchronized (mModels) {
            String id = deviceModel.getId();

            mModelMap.remove(id);
            mModels.remove(deviceModel);
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
