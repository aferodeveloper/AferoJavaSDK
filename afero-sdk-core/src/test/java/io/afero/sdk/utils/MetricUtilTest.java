/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.List;

import io.afero.sdk.AferoTest;
import io.afero.sdk.BuildConfig;
import io.afero.sdk.conclave.ConclaveMessage;
import io.afero.sdk.device.DeviceModel;
import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk=21)
public class MetricUtilTest extends AferoTest {
    MetricUtil util;

    @Before
    public void beforeTests() {
        util = new MetricUtil();
    }

    @Test
    public void testSimpleHappyCase() throws IOException {

        TestSubscriber<ConclaveMessage.Metric> subscriber = new TestSubscriber<>();
        util.getEventObservable().subscribe(subscriber);

        util.begin(1,"1",200);
        util.end(1,300,true,null);

        subscriber.assertNoErrors();
        List<ConclaveMessage.Metric> events = subscriber.getOnNextEvents();

        assertEquals(1, events.size());
        ConclaveMessage.Metric metric = events.get(0);
        Assert.assertEquals(1, metric.peripherals.size());
        ConclaveMessage.Metric.MetricsFields fields = metric.peripherals.get(0);
        Assert.assertEquals(100, fields.elapsed);
    }

    @Test
    public void testEndBeforeBegin() throws IOException {

        TestSubscriber<ConclaveMessage.Metric> subscriber = new TestSubscriber<>();
        util.getEventObservable().subscribe(subscriber);
        util.end(1,300,true,null);
        util.begin(1,"1",200);
        subscriber.assertNoErrors();
        List<ConclaveMessage.Metric> events = subscriber.getOnNextEvents();

        assertEquals(1, events.size());
        ConclaveMessage.Metric metric = events.get(0);
        Assert.assertEquals(1, metric.peripherals.size());
        ConclaveMessage.Metric.MetricsFields fields = metric.peripherals.get(0);
        Assert.assertEquals(100, fields.elapsed);
    }

    @Test
    public void testNegativeTime() throws IOException {
        TestSubscriber<ConclaveMessage.Metric> subscriber = new TestSubscriber<>();
        util.getEventObservable().subscribe(subscriber);
        util.end(1,200,true,null);
        util.begin(1,"1",300);
        List<Throwable> errors = subscriber.getOnErrorEvents();
        List<ConclaveMessage.Metric> events = subscriber.getOnNextEvents();

        assertEquals(1, errors.size());
        assertEquals(0, events.size());
        assertTrue(errors.get(0) instanceof AssertionError);
    }

    @Test
    public void testPurgeRolloverMetric() throws IOException {
        TestSubscriber<ConclaveMessage.Metric> subscriber = new TestSubscriber<>();
        util.getEventObservable().subscribe(subscriber);
        util.begin(1,"1",300);
        util.begin(1,"1",200);
        List<ConclaveMessage.Metric> events = subscriber.getOnNextEvents();

        assertEquals(events.size(),1);

        ConclaveMessage.Metric.MetricsFields fields = events.get(0).peripherals.get(0);
        assertFalse(fields.success);
        Assert.assertEquals(30000, fields.elapsed);
        Assert.assertEquals(ConclaveMessage.Metric.FailureReason.SERVICE_API_TIMEOUT.toString(), fields.failure_reason);
    }


    @Test
    public void testPurgeTimeoutMetric() throws IOException {
        TestSubscriber<ConclaveMessage.Metric> subscriber = new TestSubscriber<>();
        util.getEventObservable().subscribe(subscriber);
        util.begin(1,"1",400);

        util.purgeTimedOutWrites(40000);
        List<ConclaveMessage.Metric> events = subscriber.getOnNextEvents();

        assertEquals(events.size(),1);

        ConclaveMessage.Metric.MetricsFields fields = events.get(0).peripherals.get(0);
        assertFalse(fields.success);
        Assert.assertEquals(30000, fields.elapsed);
        Assert.assertEquals(ConclaveMessage.Metric.FailureReason.APP_TIMEOUT.toString(), fields.failure_reason);

    }

    @Test
    public void testPurgeTimeoutMetric_negative() throws IOException {
        TestSubscriber<ConclaveMessage.Metric> subscriber = new TestSubscriber<>();
        util.getEventObservable().subscribe(subscriber);
        util.begin(1,"1",10001);

        util.purgeTimedOutWrites(40000);
        List<ConclaveMessage.Metric> events = subscriber.getOnNextEvents();

        assertEquals(events.size(),0);
    }
}
