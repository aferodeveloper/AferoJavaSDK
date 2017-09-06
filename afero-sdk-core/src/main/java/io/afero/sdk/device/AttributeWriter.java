/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.afero.sdk.client.afero.models.AferoError;
import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.client.afero.models.WriteRequest;
import io.afero.sdk.client.afero.models.WriteResponse;
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

/**
 * The AttributeWriter class provides an interface for writing {@link DeviceModel} attributes.
 *
 * @see DeviceModel#writeAttributes
 */
public final class AttributeWriter {

    private static final int WRITE_ATTRIBUTE_RETRY_COUNT = 4;
    private static final long DEFAULT_TIMEOUT = 30;
    private static final int HTTP_LOCKED = 423; // https://tools.ietf.org/html/rfc4918#section-11.3
    private static final int REQUEST_BATCH_SIZE = 5;

    private final WeakReference<DeviceModel> mDeviceModelRef;
    private final TreeMap<Integer, WriteRequest> mWriteRequests = new TreeMap<>();
    private final TreeMap<Integer, WriteRequestResponsePair> mPendingResponses = new TreeMap<>();
    private final HashMap<Integer, Result> mResultsNotAttempted = new HashMap<>();
    private final long mTimeoutSeconds;

    private final Observable<Result> mDeviceResultObservable = deviceResultObservable();
    private Emitter<Result> mResultEmitter;
    private Subscription mDeviceSyncSubscription;
    private Subscription mDeviceErrorSubscription;

    /**
     * Class that represents the final status of a write to a particular device attribute.
     */
    public static final class Result {

        public enum Status {
            /**
             * The attribute write succeeded and was acknowedged by the device.
             */
            SUCCESS,

            /**
             * The attribute write failed either in the Afero Cloud, the hub, or the device.
             */
            FAILURE,

            /**
             * The attribute write timed out waiting for a response from the device.
             */
            TIMEOUT,

            /**
             * The attribute write was not attempted due to an earlier failure.
             */
            NOT_ATTEMPTED
        }

        public final int attributeId;
        public final Status status;
        public final long timestampMs;

        /**
         * @return {@code true} if the {@link Result#status} equals {@link Status#SUCCESS};
         *          otherwise {@code false}
         */
        public boolean isSuccess() {
            return Status.SUCCESS.equals(status);
        }

        private Result(int attrId, WriteResponse r, Status s) {
            attributeId = attrId;
            status = s != null ? s : r.isSuccess() ? Status.SUCCESS : Status.FAILURE;
            timestampMs = r.timestampMs;
        }

        private Result(int attrId) {
            attributeId = attrId;
            status = Status.NOT_ATTEMPTED;
            timestampMs = 0;
        }
    }

    /**
     * Exception returned via {@link rx.Observer#onError(Throwable)} when any of the write requests
     * fail at the Afero Cloud API level before reaching the device.
     */
    public static class AttributeWriteRequestFailure extends RuntimeException {

        private final int mAttributeId;

        AttributeWriteRequestFailure(int attrId) {
            super("Write for attribute " + attrId + " failed within the Afero Cloud");
            mAttributeId = attrId;
        }

        /**
         * @return Id of the device attribute for which the write failed.
         */
        public int getAttributeId() {
            return mAttributeId;
        }
    }

    /**
     * @param deviceModel {@link DeviceModel} on which attributes will be written
     */
    AttributeWriter(DeviceModel deviceModel) {
        this(deviceModel, DEFAULT_TIMEOUT);
    }

    /**
     * This constructor is used for unit testing the timeout functionality
     *
     * @param deviceModel {@link DeviceModel} on which attributes will be written
     * @param timeoutSeconds max duration in seconds we'll wait for a response from the device
     *                       before emitting an error.
     */
    AttributeWriter(DeviceModel deviceModel, long timeoutSeconds) {
        mDeviceModelRef = new WeakReference<>(deviceModel);
        mTimeoutSeconds = timeoutSeconds;
    }

    /**
     * Private default constructor stub to prevent unauthorized usage.
     */
    private AttributeWriter() {
        mDeviceModelRef = null;
        mTimeoutSeconds = 0;
    }

    /**
     * Adds an attribute value to be written.
     *
     * @param attrId Id of the attribute
     * @param value  {@link AttributeValue} to write to the specified attribute
     * @return this AttributeWriter instance
     */
    public AttributeWriter put(int attrId, AttributeValue value) {
        mWriteRequests.put(attrId, new WriteRequest(attrId, value.toString()));
        return this;
    }

    /**
     * @return {@code true} if this AttributeWriter contains no attributes; false otherwise.
     */
    public boolean isEmpty() {
        return mWriteRequests.isEmpty();
    }

    /**
     * Starts execution of the write operation.
     *
     * @return {@link Observable} the emits a {@link Result} for each attribute written. A result is
     * emitted when the device acknowledges that the attribute has been changed, or an
     * error has occurred in transit.
     */
    public Observable<Result> commit() {

        if (mWriteRequests.isEmpty()) {
            throw new IllegalArgumentException("Must put at least one attribute");
        }

        return Observable.from(mWriteRequests.values())

            // Since requests may be issued in batches, an error could abort the process before
            // we even issue the batch request, but we still need to emit an appropriate Result.
            // So we add a NOT_ATTEMPTED Result to this list for each request, then as each request
            // is issued, the corresponding Result is removed from this list. When an error occurs
            // any Results remaining in this list will be emitted.
            .doOnNext(new Action1<WriteRequest>() {
                @Override
                public void call(WriteRequest writeRequest) {
                    mResultsNotAttempted.put(writeRequest.attrId, new Result(writeRequest.attrId));
                }
            })

            // Issue requests in chunks so we don't overwhelm the device with attribute spray
            .buffer(REQUEST_BATCH_SIZE)

            // Map each chunk of requests to an Observable that will execute that chunk
            .map(new Func1<List<WriteRequest>, Observable<Result>>() {
                @Override
                public Observable<Result> call(List<WriteRequest> writeRequests) {
                    return deviceBatchWrite(writeRequests);
                }
            })

            // Concat each batch together so they execute serially
            .reduce(new Func2<Observable<Result>, Observable<Result>, Observable<Result>>() {
                @Override
                public Observable<Result> call(Observable<Result> resultObservable, Observable<Result> resultObservable2) {
                    return resultObservable.concatWith(resultObservable2);
                }
            })

            // Execute the Observable chain
            .flatMap(new Func1<Observable<Result>, Observable<Result>>() {
                @Override
                public Observable<Result> call(Observable<Result> resultObservable) {
                    return Observable.mergeDelayError(mDeviceResultObservable, resultObservable);
                }
            })

            .timeout(mTimeoutSeconds, TimeUnit.SECONDS)

            .onErrorResumeNext(emitNotYetAttemptedResultsAlongWithError())
            .doOnSubscribe(onSubscribeAction())
            .doOnTerminate(onTerminateAction())
            .doOnError(notifyDeviceOfError())
            ;
    }

    private Observable<Result> deviceBatchWrite(final List<WriteRequest> writeRequests) {

        DeviceModel deviceModel = getDevice();
        if (deviceModel == null) {
            return Observable.error(new IllegalArgumentException("DeviceModel is null"));
        }

        /*
         * For successful requests we're expecting a corresponding device update to
         * arrive later, so put the request/response in a list so we can match it up
         * with the update when it arrives and emit an appropriate result.
         *
         * For a failed request we emit a FAILURE Result for that request and NOT_ATTEMPTED for
         * any that follow. Any successful requests that preceded the failure, we wait for the device
         * response before issuing the AttributeWriteFailure to the main Observable.
         */
        // See http://wiki.afero.io/display/CD/Batch+Attribute+Requests
        return deviceModel.postBatchAttributeWrite(writeRequests, WRITE_ATTRIBUTE_RETRY_COUNT, HTTP_LOCKED)
            .flatMap(new Func1<WriteResponse[], Observable<Result>>() {
                @Override
                public Observable<Result> call(WriteResponse[] writeResponses) {

                    // This Observable emits Results as they come in:
                    //  - Results for failed requests (rare) are emitted immediately
                    //  - Results for successful requests are emitted when the DeviceModel receives
                    //    an update with the corresponding requestId
                    return Observable.from(writeResponses)

                        // Requests and responses match up in order in their respective lists
                        // so zip them together into one object for easier processing...
                        .zipWith(Observable.from(writeRequests), zipDeviceRequestWithRequestResponse())

                        .doOnNext(processResponse())

                        // let failed responses fall through and be emitted
                        .filter(new Func1<WriteRequestResponsePair, Boolean>() {
                            @Override
                            public Boolean call(WriteRequestResponsePair wrrp) {
                                return !wrrp.writeResponse.isSuccess();
                            }
                        })

                        .map(toWriteResult());
                }
            });
    }

    private Func2<WriteResponse, WriteRequest, WriteRequestResponsePair> zipDeviceRequestWithRequestResponse() {
        return new Func2<WriteResponse, WriteRequest, WriteRequestResponsePair>() {
            @Override
            public WriteRequestResponsePair call(WriteResponse writeResponse, WriteRequest writeRequest) {
                return new WriteRequestResponsePair(writeRequest, writeResponse);
            }
        };
    }

    private Action1<WriteRequestResponsePair> processResponse() {
        return new Action1<WriteRequestResponsePair>() {
            @Override
            public void call(WriteRequestResponsePair wrrp) {
                mResultsNotAttempted.remove(wrrp.writeRequest.attrId);

                if (wrrp.writeResponse.isSuccess()) {
                    mPendingResponses.put(wrrp.writeResponse.requestId, wrrp);
                } else {
                    mResultEmitter.onNext(new Result(wrrp.writeRequest.attrId, wrrp.writeResponse, Result.Status.FAILURE));

                    if (mPendingResponses.isEmpty()) {
                        mResultEmitter.onCompleted();
                    }

                    throw new AttributeWriteRequestFailure(wrrp.writeRequest.attrId);
                }
            }
        };
    }

    private Observable<Result> deviceResultObservable() {
        return Observable.create(
                new Action1<Emitter<Result>>() {
                    @Override
                    public void call(Emitter<Result> emitter) {
                        mResultEmitter = emitter;
                    }
                }, Emitter.BackpressureMode.BUFFER)
                .doOnNext(notifyDeviceOfResult());
    }

    // tell device about this Result so it can update its Device.UpdateState
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

    // tell device about this error so it can update its Device.UpdateState
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

    private Func1<Throwable, Observable<Result>> emitNotYetAttemptedResultsAlongWithError() {
        return new Func1<Throwable, Observable<Result>>() {
            @Override
            public Observable<Result> call(Throwable throwable) {
                if (!mResultsNotAttempted.isEmpty()) {
                    Result[] results = new Result[mResultsNotAttempted.size()];
                    mResultsNotAttempted.values().toArray(results);
                    mResultsNotAttempted.clear();
                    return Observable.from(results)
                            .concatWith(Observable.<Result>error(throwable));
                }

                if (throwable instanceof TimeoutException) {
                    if (!mPendingResponses.isEmpty()) {
                        return Observable.from(mPendingResponses.values())
                            .map(new Func1<WriteRequestResponsePair, Result>() {
                                @Override
                                public Result call(WriteRequestResponsePair wrrp) {
                                    return new Result(wrrp.writeRequest.attrId, wrrp.writeResponse, Result.Status.TIMEOUT);
                                }
                            })
                            .concatWith(Observable.<Result>error(throwable));
                    }
                }

                return Observable.error(throwable);
            }
        };
    }

    private Func1<WriteRequestResponsePair, Result> toWriteResult() {
        return new Func1<WriteRequestResponsePair, Result>() {
            @Override
            public Result call(WriteRequestResponsePair wrrp) {
                return new Result(wrrp.writeRequest.attrId, wrrp.writeResponse, null);
            }
        };
    }

    private Action0 onSubscribeAction() {
        return new Action0() {
            @Override
            public void call() {
                DeviceModel deviceModel = getDevice();
                if (deviceModel == null) {
                    return;
                }

                deviceModel.onWriteStart(mWriteRequests.values());

                mDeviceSyncSubscription = deviceModel.getDeviceSyncPostUpdateObservable()
                        .subscribe(new Action1<DeviceSync>() {
                            @Override
                            public void call(DeviceSync deviceSync) {
                                if (deviceSync.hasRequestId()) {
                                    emitResult(deviceSync.requestId, Result.Status.SUCCESS);
                                }
                            }
                        });

                mDeviceErrorSubscription = deviceModel.getErrorObservable()
                        .subscribe(new Action1<AferoError>() {
                            @Override
                            public void call(AferoError error) {
                                WriteRequestResponsePair wrrp = getResponseFromError(error);
                                if (wrrp != null) {
                                    emitResult(wrrp.writeResponse.requestId, Result.Status.FAILURE);
                                }
                            }
                        });
            }
        };
    }

    private Action0 onTerminateAction() {
        return new Action0() {
            @Override
            public void call() {
                mDeviceSyncSubscription = RxUtils.safeUnSubscribe(mDeviceSyncSubscription);
                mDeviceErrorSubscription = RxUtils.safeUnSubscribe(mDeviceErrorSubscription);
            }
        };
    }

    private void emitResult(int requestId, Result.Status status) {
        if (mResultEmitter == null) {
            return;
        }

        // if this deviceSync matches one of our requestIds, emit it
        WriteRequestResponsePair wrrp = mPendingResponses.remove(requestId);
        if (wrrp != null) {
            mResultEmitter.onNext(new Result(wrrp.writeRequest.attrId, wrrp.writeResponse, status));
        }

        // no more responses left means we're done
        if (mPendingResponses.isEmpty()) {
            mResultEmitter.onCompleted();
        }
    }

    private WriteRequestResponsePair getResponseFromError(AferoError error) {
        if (error instanceof DeviceError) {
            int reqId = ((DeviceError) error).requestId;
            return mPendingResponses.get(reqId);
        }

        return null;
    }

    private DeviceModel getDevice() {
        return mDeviceModelRef != null ? mDeviceModelRef.get() : null;
    }

    private class WriteRequestResponsePair {
        final WriteRequest writeRequest;
        final WriteResponse writeResponse;

        private WriteRequestResponsePair(WriteRequest dr, WriteResponse rr) {
            writeRequest = dr;
            writeResponse = rr;
        }
    }

}
