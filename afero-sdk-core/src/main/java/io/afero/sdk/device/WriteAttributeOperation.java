package io.afero.sdk.device;

import java.lang.ref.WeakReference;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import io.afero.sdk.client.afero.models.AferoError;
import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.client.afero.models.DeviceRequest;
import io.afero.sdk.client.afero.models.RequestResponse;
import io.afero.sdk.conclave.models.DeviceError;
import io.afero.sdk.conclave.models.DeviceSync;
import io.afero.sdk.utils.RxUtils;
import rx.Emitter;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

public final class WriteAttributeOperation {

    private static final long TIMEOUT_ROUND_TRIP = 30;

    private final WeakReference<DeviceModel> mDeviceModelRef;
    private final TreeMap<Integer, DeviceRequest> mWriteRequests = new TreeMap<>();
    private final TreeMap<Integer, DeviceRequestResponsePair> mPendingResponses = new TreeMap<>();

    private Emitter<Result> mEmitter;
    private Subscription mDeviceSyncSubscription;
    private Subscription mDeviceErrorSubscription;

    public static final class Result {

        public enum Status {
            SUCCESS,
            FAILURE
        }

        public final int attributeId;
        public final Status status;
        private final long timestampMs;

        private Result(int attrId, RequestResponse r) {
            attributeId = attrId;
            timestampMs = r.timestampMs;
            status = r.isSuccess() ? Status.SUCCESS : Status.FAILURE;
        }
    }

    WriteAttributeOperation(DeviceModel deviceModel) {
        mDeviceModelRef = new WeakReference<>(deviceModel);
    }

    private WriteAttributeOperation() {
        mDeviceModelRef = null;
    }

    public WriteAttributeOperation put(int attrId, AttributeValue value) {
        mWriteRequests.put(attrId, new DeviceRequest(attrId, value.toString()));
        return this;
    }

    public Observable<Result> commit() {

        if (mWriteRequests.isEmpty()) {
            throw new IllegalArgumentException("Must put at least one attribute");
        }

        DeviceModel deviceModel = getDevice();
        if (deviceModel == null) {
            throw new IllegalArgumentException("DeviceModel is null");
        }

        // see http://wiki.afero.io/display/CD/Batch+Attribute+Requests
        return deviceModel.postAttributeWriteRequests(mWriteRequests.values())
            .flatMap(new Func1<RequestResponse[], Observable<Result>>() {
                @Override
                public Observable<Result> call(RequestResponse[] requestResponses) {
                    // This Observable emits Results as they come in:
                    //  - Results for failed requests (rare) are emitted immediately
                    //  - Results for successful requests are emitted when the DeviceModel receives
                    //    an update with the corresponding requestId
                    return Observable.from(requestResponses)
                        .zipWith(Observable.from(mWriteRequests.values()),
                                // requests and responses match up in order in their respective lists
                                // so zip them together into one object for easier processing...
                            new Func2<RequestResponse, DeviceRequest, DeviceRequestResponsePair>() {
                                @Override
                                public DeviceRequestResponsePair call(RequestResponse requestResponse, DeviceRequest deviceRequest) {
                                    DeviceRequestResponsePair drrp = new DeviceRequestResponsePair(deviceRequest, requestResponse);
                                    if (drrp.requestResponse.isSuccess()) {
                                        mPendingResponses.put(drrp.requestResponse.requestId, drrp);
                                    }
                                    return drrp;
                                }
                            })
                        .filter(new Func1<DeviceRequestResponsePair, Boolean>() {
                            @Override
                            public Boolean call(DeviceRequestResponsePair drrp) {
                                return !drrp.requestResponse.isSuccess();
                            }
                        })
                        .map(toWriteResult())
                        .mergeWith(emitResultsAsDeviceUpdatesArrive())
                        .timeout(TIMEOUT_ROUND_TRIP, TimeUnit.SECONDS);
                }
            })
            .doOnSubscribe(subscribeToDeviceUpdates())
            .doOnTerminate(unsubscribeFromDeviceUpdates())
            .doOnError(notifyDeviceOfError());
    }

    private Observable<Result> emitResultsAsDeviceUpdatesArrive() {
        return Observable.create(
                new Action1<Emitter<Result>>() {
                    @Override
                    public void call(Emitter<Result> emitter) {
                        mEmitter = emitter;
                    }
                }, Emitter.BackpressureMode.BUFFER)
                .doOnNext(notifyDeviceOfResult());
    }

    // tell device about this Result so it can update its Device.State
    private Action1<Result> notifyDeviceOfResult() {
        return new Action1<Result>() {
            @Override
            public void call(Result writeResult) {
                DeviceModel deviceModel = getDevice();
                if (deviceModel != null) {
                    deviceModel.onWriteResult(writeResult);
                }
            }
        };
    }

    // tell device about this error so it can update its Device.State
    private Action1<Throwable> notifyDeviceOfError() {
        return new Action1<Throwable>() {
            @Override
            public void call(Throwable t) {
                DeviceModel deviceModel = getDevice();
                if (deviceModel != null) {
                    deviceModel.onError(t);
                }
            }
        };
    }

    private Func1<DeviceRequestResponsePair, Result> toWriteResult() {
        return new Func1<DeviceRequestResponsePair, Result>() {
            @Override
            public Result call(DeviceRequestResponsePair drrp) {
                return new Result(drrp.deviceRequest.attrId, drrp.requestResponse);
            }
        };
    }

    private Action0 subscribeToDeviceUpdates() {
        return new Action0() {
            @Override
            public void call() {
                DeviceModel deviceModel = getDevice();
                if (deviceModel == null) {
                    return;
                }

                mDeviceSyncSubscription = deviceModel.getDeviceSyncPostUpdateObservable()
                    .subscribe(new Action1<DeviceSync>() {
                        @Override
                        public void call(DeviceSync deviceSync) {
                            if (deviceSync.hasRequestId()) {
                                emitResult(deviceSync.requestId);
                            }
                        }
                    });

                mDeviceErrorSubscription = deviceModel.getErrorObservable()
                    .subscribe(new Action1<AferoError>() {
                        @Override
                        public void call(AferoError error) {
                            DeviceRequestResponsePair drrp = getResponseFromError(error);
                            if (drrp != null) {
                                emitResult(drrp.requestResponse.requestId);
                            }
                        }
                    });
            }
        };
    }

    private Action0 unsubscribeFromDeviceUpdates() {
        return new Action0() {
            @Override
            public void call() {
                mDeviceSyncSubscription = RxUtils.safeUnSubscribe(mDeviceSyncSubscription);
                mDeviceErrorSubscription = RxUtils.safeUnSubscribe(mDeviceErrorSubscription);
            }
        };
    }

    private void emitResult(int requestId) {
        if (mEmitter == null) {
            return;
        }

        // if this deviceSync matches one of our requestIds, emit it
        DeviceRequestResponsePair drrp = mPendingResponses.remove(requestId);
        if (drrp != null) {
            mEmitter.onNext(new Result(drrp.deviceRequest.attrId, drrp.requestResponse));
        }

        // no more responses left means we're done
        if (mPendingResponses.isEmpty()) {
            mEmitter.onCompleted();
        }
    }

    private DeviceRequestResponsePair getResponseFromError(AferoError error) {
        if (error instanceof DeviceError) {
            int reqId = ((DeviceError) error).requestId;
            return mPendingResponses.get(reqId);
        }

        return null;
    }

    private DeviceModel getDevice() {
        return mDeviceModelRef.get();
    }

    private class DeviceRequestResponsePair {
        final DeviceRequest deviceRequest;
        final RequestResponse requestResponse;

        private DeviceRequestResponsePair(DeviceRequest dr, RequestResponse rr) {
            deviceRequest = dr;
            requestResponse = rr;
        }
    }
}
