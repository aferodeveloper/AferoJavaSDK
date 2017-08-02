package io.afero.sdk.device;

import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.client.afero.models.DeviceRequest;
import io.afero.sdk.client.afero.models.RequestResponse;
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

    private final DeviceModel mDeviceModel;
    private final TreeMap<Integer, DeviceRequest> mWriteRequests = new TreeMap<>();
    private final TreeMap<Integer, RequestResponse> mRequestResponses = new TreeMap<>();

    private Emitter<Result> mEmitter;
    private Subscription mDeviceSyncSubscription;

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
        mDeviceModel = deviceModel;
    }

    public WriteAttributeOperation put(int attrId, AttributeValue value) {
        mWriteRequests.put(attrId, new DeviceRequest(attrId, value.toString()));
        return this;
    }

    public Observable<Result> commit() {

        if (mWriteRequests.isEmpty()) {
            throw new IllegalArgumentException("Must put at least one attribute");
        }

        // see http://wiki.afero.io/display/CD/Batch+Attribute+Requests
        return mDeviceModel.postAttributeWriteRequests(mWriteRequests.values())
            .flatMap(new Func1<RequestResponse[], Observable<Result>>() {
                @Override
                public Observable<Result> call(RequestResponse[] requestResponses) {
                    // This Observable emits Results as they come in:
                    //  - Results for failed requests are emitted immediately
                    //  - Results for successful requests are emitted when the DeviceModel receive
                    //    an update with the corresponding requestId
                    return Observable.from(requestResponses)
                        .zipWith(Observable.from(mWriteRequests.values()),
                            new Func2<RequestResponse, DeviceRequest, DeviceRequestResponsePair>() {
                                @Override
                                public DeviceRequestResponsePair call(RequestResponse requestResponse, DeviceRequest deviceRequest) {
                                    return new DeviceRequestResponsePair(deviceRequest, requestResponse);
                                }
                            })
                        .filter(new Func1<DeviceRequestResponsePair, Boolean>() {
                            @Override
                            public Boolean call(DeviceRequestResponsePair drrp) {
                                if (drrp.requestResponse.isSuccess()) {
                                    mRequestResponses.put(drrp.requestResponse.requestId, drrp.requestResponse);
                                    return false;
                                }
                                return true;
                            }
                        })
                        .map(new Func1<DeviceRequestResponsePair, Result>() {
                            @Override
                            public Result call(DeviceRequestResponsePair drrp) {
                                return new Result(drrp.deviceRequest.attrId, drrp.requestResponse);
                            }
                        })
                        .mergeWith(Observable.create(
                            new Action1<Emitter<Result>>() {
                                @Override
                                public void call(Emitter<Result> emitter) {
                                    mEmitter = emitter;
                                }
                            }, Emitter.BackpressureMode.BUFFER)
                            .doOnNext(new Action1<Result>() {
                                @Override
                                public void call(Result writeResult) {

                                }
                            })
                            .timeout(TIMEOUT_ROUND_TRIP, TimeUnit.SECONDS));
                }
            })
            .doOnSubscribe(new Action0() {
                @Override
                public void call() {  // subscribe to device updates
                    mDeviceSyncSubscription = mDeviceModel.getDeviceSyncPostUpdateObservable()
                        .subscribe(new Action1<DeviceSync>() {
                            @Override
                            public void call(DeviceSync deviceSync) {
                                if (mEmitter != null && deviceSync.hasRequestId()) {
                                    // if this deviceSync matches one of our requestIds, emit it
                                    RequestResponse r = mRequestResponses.remove(deviceSync.requestId);
                                    if (r != null) {
                                        mEmitter.onNext(new Result(deviceSync.attribute.id, r));
                                    }

                                    // no more responses left means we're done
                                    if (mRequestResponses.isEmpty()) {
                                        mEmitter.onCompleted();
                                    }
                                }
                            }
                        });
                }
            })
            .doOnTerminate(new Action0() {
                @Override
                public void call() {  // unsubscribe from device updates
                    mDeviceSyncSubscription = RxUtils.safeUnSubscribe(mDeviceSyncSubscription);
                }
            })
            .doOnError(new Action1<Throwable>() {
                @Override
                public void call(Throwable t) {
                    mDeviceModel.onError(t);
                }
            });
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
