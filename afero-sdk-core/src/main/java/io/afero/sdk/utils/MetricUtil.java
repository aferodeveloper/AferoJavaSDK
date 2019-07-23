/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import io.afero.sdk.conclave.ConclaveMessage;
import io.afero.sdk.conclave.ConclaveMessage.Metric.FailureReason;
import io.afero.sdk.device.AttributeWriter;
import rx.Observable;
import rx.subjects.PublishSubject;


public class MetricUtil {
    private static final long WRITE_TIMEOUT_INTERVAL = 30000;


    private final Map<Integer,BeginEvent> beginRequestDeviceMetrics = new HashMap<>();
    private final Map<Integer,EndEvent> endRequestDeviceMetrics = new HashMap<>();

    private PublishSubject<ConclaveMessage.Metric> mEventSubject = PublishSubject.create();

    private static MetricUtil instance;

    public static MetricUtil getInstance() {
        if (instance == null) {
            instance = new MetricUtil();
        }
        return instance;
    }

    public Observable<ConclaveMessage.Metric> getEventObservable() {
        return mEventSubject;
    }

    public void begin(Integer requestId, String deviceId, long time) {
        synchronized (beginRequestDeviceMetrics) {
            synchronized (endRequestDeviceMetrics) {
                if (beginRequestDeviceMetrics.get(requestId) != null) {
                    reportError(deviceId, WRITE_TIMEOUT_INTERVAL, FailureReason.SERVICE_API_TIMEOUT);
                }
                beginRequestDeviceMetrics.put(requestId, new BeginEvent(time, deviceId));
                Set<Integer> beginKeys = beginRequestDeviceMetrics.keySet();
                Set<Integer> endKeys = endRequestDeviceMetrics.keySet();
                Set<Integer> result = new HashSet<>(beginKeys);
                result.retainAll(endKeys);
                reportRequestIds(result);
            }
        }
    }

    public void end(Integer requestId, long time, boolean success, FailureReason reason) {
        synchronized (beginRequestDeviceMetrics) {
            synchronized (endRequestDeviceMetrics) {
                endRequestDeviceMetrics.put(requestId, new EndEvent(time, success, reason));
                Set<Integer> beginKeys = beginRequestDeviceMetrics.keySet();
                Set<Integer> endKeys = endRequestDeviceMetrics.keySet();
                Set<Integer> result = new HashSet<>(beginKeys);
                result.retainAll(endKeys);
                reportRequestIds(result);
            }
        }
    }

    public void reportError(String deviceId, long time, FailureReason reason) {
        ConclaveMessage.Metric metric = new ConclaveMessage.Metric();
        ConclaveMessage.Metric.MetricsFields measurement =
                new ConclaveMessage.Metric.MetricsFields(deviceId, time, false, reason != null ? reason.toString() : null );
        metric.addPeripheralMetric(measurement);
        mEventSubject.onNext(metric);
    }

    public void reportWriteResult(String deviceId, AttributeWriter.Result writeResult) {
        ConclaveMessage.Metric.FailureReason reason = null;

        if (!writeResult.isSuccess()) {
            reason = ConclaveMessage.Metric.FailureReason.SERVICE_API_ERROR;
        }

        ConclaveMessage.Metric metric = new ConclaveMessage.Metric();
        ConclaveMessage.Metric.MetricsFields measurement =
            new ConclaveMessage.Metric.MetricsFields(
                deviceId,
                writeResult.roundTripTimeMs,
                writeResult.isSuccess(),
                reason != null ? reason.toString() : null);
        metric.addPeripheralMetric(measurement);

        mEventSubject.onNext(metric);
    }

    private void reportRequestIds(Set<Integer> requestIds) {
        if (requestIds.size() > 0 ) {
            ConclaveMessage.Metric metric = new ConclaveMessage.Metric();

            for (Integer requestId : requestIds) {
                BeginEvent beginEvent = beginRequestDeviceMetrics.get(requestId);
                EndEvent endEvent = endRequestDeviceMetrics.get(requestId);

                if (beginEvent != null && endEvent != null && beginEvent.time <= endEvent.time) {
                    long elapsed = endEvent.time - beginEvent.time;

                    ConclaveMessage.Metric.MetricsFields measurement =
                        new ConclaveMessage.Metric.MetricsFields(
                            beginEvent.deviceId,
                            elapsed,
                            endEvent.success,
                            endEvent.reason != null ? endEvent.reason.toString() : null);
                    metric.addPeripheralMetric(measurement);
                }

                beginRequestDeviceMetrics.remove(requestId);
                endRequestDeviceMetrics.remove(requestId);
            }

            if (!metric.isEmpty()) {
                mEventSubject.onNext(metric);
            }
        }
    }

    public void purgeTimedOutWrites() {
        purgeTimedOutWrites(Clock.getElapsedMillis());

    }

    public void purgeTimedOutWrites(long time) {
        long timeout = time - WRITE_TIMEOUT_INTERVAL;
        synchronized (beginRequestDeviceMetrics) {
            Iterator<Map.Entry<Integer,BeginEvent>> iter = beginRequestDeviceMetrics.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<Integer,BeginEvent> entry = iter.next();
                long timestamp = entry.getValue().time;
                if (timestamp < timeout) {
                    reportError(entry.getValue().deviceId, WRITE_TIMEOUT_INTERVAL, ConclaveMessage.Metric.FailureReason.APP_TIMEOUT);
                    iter.remove();
                }
            }
        }
    }

    private class BeginEvent {
        long time;
        String deviceId;
        BeginEvent(long time, String deviceId) {
            this.time = time;
            this.deviceId = deviceId;
        }
    }

    private class EndEvent {
        long time;
        boolean success;
        FailureReason reason;

        EndEvent(long time, boolean success, FailureReason reason) {
            this.time = time;
            this.success = success;
            this.reason = reason;
        }
    }
}
