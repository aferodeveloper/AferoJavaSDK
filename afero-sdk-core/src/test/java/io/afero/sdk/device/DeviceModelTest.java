/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Vector;

import io.afero.sdk.AferoTest;
import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.client.afero.models.DeviceTag;
import io.afero.sdk.client.afero.models.WriteResponse;
import io.afero.sdk.client.mock.MockAferoClient;
import io.afero.sdk.client.mock.MockDeviceEventSource;
import io.afero.sdk.client.mock.ResourceLoader;
import io.afero.sdk.conclave.ConclaveMessage;
import io.afero.sdk.conclave.models.DeviceSync;
import io.afero.sdk.utils.MetricUtil;
import io.afero.sdk.utils.RxUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DeviceModelTest extends AferoTest {

    static final String PATH_PREFIX = "deviceModel/";
    private static final String DEVICE_ID = "deviceModel-model-id";

    public static DeviceModel createDeviceModel(DeviceProfile deviceProfile, AferoClient aferoClient) {
        return new DeviceModel(DEVICE_ID, deviceProfile, false, aferoClient);
    }

    public static DeviceModel createDeviceModel(DeviceProfile deviceProfile, MockAferoClient aferoClient, DeviceSync data) {
        DeviceModel deviceModel = createDeviceModel(deviceProfile, aferoClient);
        deviceModel.update(data);
        return deviceModel;
    }

    @Before
    public void beforeTests() {
    }

    @Test
    public void testDefaults() throws IOException {
        DeviceProfile dp = loadDeviceProfile(PATH_PREFIX + "deviceModelTestProfile.json");
        DeviceModel dm = new DeviceModel(DEVICE_ID, dp, false, null);

        assertEquals(DeviceModel.UpdateState.NORMAL, dm.getState());
        assertFalse(dm.isOTAInProgress());
        dm.getOTAProgress().isEmpty().subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean isEmpty) {
                assertTrue(isEmpty);
            }
        });
        assertEquals(dp.getId().length(), dm.getName().length());
    }

    @Test
    public void testUpdateFromJson() throws IOException {
        DeviceSync ds = loadDeviceSync(PATH_PREFIX + "deviceSync.json");
        DeviceProfile dp = loadDeviceProfile(PATH_PREFIX + "deviceModelTestProfile.json");
        DeviceModel dm = new DeviceModel(DEVICE_ID, dp, false, null);

        dm.update(ds);

        assertEquals("deviceModel-model-id", dm.getId());
        assertEquals("device-profile-id", dm.getProfileID());
        assertEquals("device-name", dm.getName());

        DeviceProfile.Attribute a100 = dp.getAttributeById(100);
        testAttribute(dm, a100, new BigDecimal(123));

        DeviceProfile.Attribute a200 = dp.getAttributeById(200);
        testAttribute(dm, a200, new BigDecimal(12345));

        DeviceProfile.Attribute a300 = dp.getAttributeById(300);
        testAttribute(dm, a300, new BigDecimal(123456));

        DeviceProfile.Attribute a400 = dp.getAttributeById(400);
        testAttribute(dm, a400, new BigDecimal(12345678901L));

        DeviceProfile.Attribute a500 = dp.getAttributeById(500);
        testAttribute(dm, a500, new BigDecimal("12345.12345"));

        DeviceProfile.Attribute a600 = dp.getAttributeById(600);
        testAttribute(dm, a600, new BigDecimal("123456.12345678"));

        DeviceProfile.Attribute a700 = dp.getAttributeById(700);
        testAttribute(dm, a700, "string-value");
    }

    private void testAttribute(DeviceModel dm, DeviceProfile.Attribute attribute, BigDecimal expected) {
        AttributeValue av = dm.getAttributePendingValue(attribute);
        BigDecimal actual = av != null ? av.numericValue() : null;
        ;
        assertTrue(actual.compareTo(expected) == 0);
    }

    private void testAttribute(DeviceModel dm, DeviceProfile.Attribute attribute, String expected) {
        AttributeValue av = dm.getAttributePendingValue(attribute);
        String actual = av != null ? av.toString() : null;
        ;
        assertEquals(expected, actual);
    }

    @Test
    public void testWriteAttribute() throws IOException {
        final int ATTRIBUTE_ID = 100;
        final String ATTRIBUTE_VALUE = "32";

        makeWriteAttributeTester()
                .deviceModelWriteAttribute(ATTRIBUTE_ID, ATTRIBUTE_VALUE, AttributeValue.DataType.SINT8)
                .pause()
                .deviceModelUpdate(1, ATTRIBUTE_ID, ATTRIBUTE_VALUE)

                .verifyWriteResultStatus(ATTRIBUTE_ID, AttributeWriter.Result.Status.SUCCESS)
                .verifyWriteMetrics()
                .end()
        ;
    }

    @Test
    public void testWriteAttributeWithError() throws IOException {
        final int ATTRIBUTE_ID = 100;
        final String ATTRIBUTE_VALUE = "32";

        makeWriteAttributeTester()
            .addAttributeWriteResponse(1, WriteResponse.STATUS_FAILURE)
            .commitWriteResponses()

            .deviceModelWriteAttribute(ATTRIBUTE_ID, ATTRIBUTE_VALUE, AttributeValue.DataType.SINT8)

            .deviceModelUpdate(1, ATTRIBUTE_ID, ATTRIBUTE_VALUE)

            .verifyWriteResultStatus(ATTRIBUTE_ID, AttributeWriter.Result.Status.FAILURE)

            .verifyWriteMetric(0, ConclaveMessage.Metric.FailureReason.SERVICE_API_ERROR)

            .end()
        ;
    }

    private WriteAttributeTester makeWriteAttributeTester() throws IOException {
        return new WriteAttributeTester();
    }

    private static class WriteAttributeTester {
        final ResourceLoader resourceLoader = new ResourceLoader(PATH_PREFIX);
        final DeviceProfile deviceProfile;
        final MockAferoClient aferoClient = new MockAferoClient();
        final DeviceModel deviceModel;
        TreeMap<Integer, AttributeWriter.Result> writeResults = new TreeMap<>();
        Subscription metricsSubscription;
        final Vector<ConclaveMessage.Metric> metrics = new Vector<>();
        final ArrayList<WriteResponse> postBatchAttributeWriteResponses = new ArrayList<>();

        WriteAttributeTester() throws IOException {
            deviceProfile = resourceLoader.createObjectFromJSONResource("deviceModelTestProfile.json", DeviceProfile.class);
            deviceModel = new DeviceModel(DEVICE_ID, deviceProfile, false, aferoClient);
            metricsSubscription = MetricUtil.getInstance().getEventObservable().subscribe(new Action1<ConclaveMessage.Metric>() {
                @Override
                public void call(ConclaveMessage.Metric metric) {
                    metrics.add(metric);
                }
            });
        }

        void end() {
            metricsSubscription = RxUtils.safeUnSubscribe(metricsSubscription);
        }

        WriteAttributeTester deviceModelWriteAttribute(int attrId, String value, AttributeValue.DataType type) {
            deviceModel.writeAttributes()
                .put(attrId, new AttributeValue(value, type))
                .commit()
                .subscribe(new Observer<AttributeWriter.Result>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(AttributeWriter.Result wr) {
                        writeResults.put(wr.attributeId, wr);
                    }
                });

            return this;
        }

        WriteAttributeTester deviceModelUpdate(int reqId, int attrId, String value) {
            DeviceSync ds = new DeviceSync();
            ds.requestId = reqId;
            ds.attribute = new DeviceSync.AttributeEntry(attrId, value);
            ds.setDeviceId(deviceModel.getId());
            deviceModel.update(ds);

            return this;
        }

        WriteAttributeTester addAttributeWriteResponse(int reqId, String requestResponseStatus) {

            WriteResponse wr = new WriteResponse();
            wr.requestId = reqId;
            wr.status = requestResponseStatus;
            wr.timestampMs = System.currentTimeMillis();

            postBatchAttributeWriteResponses.add(wr);

            return this;
        }

        WriteAttributeTester commitWriteResponses() {
            WriteResponse[] rr = new WriteResponse[postBatchAttributeWriteResponses.size()];
            postBatchAttributeWriteResponses.toArray(rr);
            aferoClient.setPostBatchAttributeWriteResponse(Observable.just(rr));

            return this;
        }

        WriteAttributeTester verifyWriteResultStatus(int attrId, AttributeWriter.Result.Status resultStatus) {
            assertEquals(resultStatus, writeResults.get(attrId).status);
            return this;
        }

        WriteAttributeTester verifyWriteMetrics() {
            assertEquals(metrics.size(), 1);

            ConclaveMessage.Metric metric = metrics.firstElement();
            assertEquals(metric.peripherals.size(), 1);

            ConclaveMessage.Metric.MetricsFields measurement = metric.peripherals.get(0);
            assertEquals(measurement.name, "AttributeChangeRTT");
            assertEquals(measurement.peripheralId, deviceModel.getId());
            assertNotEquals(measurement.elapsed, 0);
            assertTrue(measurement.success);

            return this;
        }

        WriteAttributeTester verifyWriteMetric(int index, ConclaveMessage.Metric.FailureReason reason) {

            ConclaveMessage.Metric metric = metrics.get(index);
            assertEquals(metric.peripherals.size(), 1);

            ConclaveMessage.Metric.MetricsFields measurement = metric.peripherals.get(0);
            assertEquals(measurement.name, "AttributeChangeRTT");
            assertEquals(measurement.peripheralId, deviceModel.getId());
            assertNotEquals(measurement.elapsed, 0);

            if (reason != null) {
                assertEquals(reason.toString(), measurement.failure_reason);
            } else {
                assertTrue(measurement.success);
            }

            return this;
        }

        WriteAttributeTester pause() {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                // ignore
            }
            return this;
        }
    }


    @Test
    public void testTimeZoneNotSetByDefault() throws IOException {
        makeTimeZoneTester()
                .verifyTimeZoneNotSet()
        ;
    }

    @Test
    public void testSetTimeZone() throws IOException {
        makeTimeZoneTester()

                .setTimeZone()

                .verifySetTimeZoneCompleted()
                .verifyTimeZoneSet()
        ;
    }

    @Test
    public void testGetTimeZoneNotSet() throws IOException {
        makeTimeZoneTester()

                .getTimeZone()

                .verifyGetTimeZoneCompleted()
                .verifyGetTimeZoneReturnedNull()
        ;
    }

    @Test
    public void testSetAndGetTimeZone() throws IOException {
        makeTimeZoneTester()

                .setTimeZone()
                .verifySetTimeZoneCompleted()

                .getTimeZone()
                .verifyGetTimeZoneCompleted()

                .verifyGetTimeZoneReturnedSameTimeZoneFromSet()
        ;
    }

    @Test
    public void testInvalidateTimeZone() throws IOException {
        makeTimeZoneTester()

                .setTimeZone()
                .verifyTimeZoneSet()

                .invalidateTimeZone()
                .verifyTimeZoneSet()

                .getTimeZone()
                .verifyGetTimeZoneReturnedNull()

                .setAferoClientTimeZone()

                .getTimeZone()
                .verifyGetTimeZoneReturnedNotNull()
                .verifyTimeZoneSet()
        ;
    }

    private TimeZoneTester makeTimeZoneTester() throws IOException {
        return new TimeZoneTester();
    }

    private static class TimeZoneTester {
        final ResourceLoader resourceLoader = new ResourceLoader(PATH_PREFIX);
        final DeviceProfile deviceProfile;
        final MockAferoClient aferoClient = new MockAferoClient();
        final DeviceModel deviceModel;
        final TimeZoneObserver getTimeZoneObserver = new TimeZoneObserver();
        final TimeZoneObserver setTimeZoneObserver = new TimeZoneObserver();

        TimeZoneTester() throws IOException {
            deviceProfile = resourceLoader.createObjectFromJSONResource("deviceModelTestProfile.json", DeviceProfile.class);
            deviceModel = new DeviceModel(DEVICE_ID, deviceProfile, false, aferoClient);
        }

        TimeZoneTester setTimeZone() {
            deviceModel.setTimeZone(TimeZone.getDefault()).subscribe(setTimeZoneObserver);
            return this;
        }

        TimeZoneTester getTimeZone() {
            deviceModel.getTimeZone().subscribe(getTimeZoneObserver);
            return this;
        }

        TimeZoneTester invalidateTimeZone() {
            deviceModel.invalidateTimeZone();
            return this;
        }

        TimeZoneTester setAferoClientTimeZone() {
            aferoClient.setDeviceTimeZone(TimeZone.getDefault());
            return this;
        }

        TimeZoneTester verifyTimeZoneSet() {
            assertTrue(deviceModel.isTimeZoneSet());
            return this;
        }

        TimeZoneTester verifyTimeZoneNotSet() {
            assertFalse(deviceModel.isTimeZoneSet());
            return this;
        }

        TimeZoneTester verifySetTimeZoneCompleted() {
            assertTrue(setTimeZoneObserver.isCompleted);
            return this;
        }

        TimeZoneTester verifySetTimeZoneNoError() {
            assertNull(setTimeZoneObserver.error);
            return this;
        }

        TimeZoneTester verifyGetTimeZoneReturnedNull() {
            assertNull(getTimeZoneObserver.timeZone);
            return this;
        }

        TimeZoneTester verifyGetTimeZoneReturnedNotNull() {
            assertNotNull(getTimeZoneObserver.timeZone);
            return this;
        }

        TimeZoneTester verifyGetTimeZoneCompleted() {
            assertTrue(getTimeZoneObserver.isCompleted);
            return this;
        }

        TimeZoneTester verifyGetTimeZoneNoError() {
            assertNull(getTimeZoneObserver.error);
            return this;
        }

        TimeZoneTester verifyGetTimeZoneReturnedSameTimeZoneFromSet() {
            assertEquals(setTimeZoneObserver.timeZone, getTimeZoneObserver.timeZone);
            return this;
        }


        private class TimeZoneObserver implements Observer<TimeZone> {
            TimeZone timeZone;
            Throwable error;
            boolean isCompleted;

            @Override
            public void onCompleted() {
                isCompleted = true;
            }

            @Override
            public void onError(Throwable e) {
                error = e;
            }

            @Override
            public void onNext(TimeZone tz) {
                timeZone = tz;
            }
        }
    }


    // Device Tag Tests ----------------------------------------------------------------

    @Test
    public void testTagsFromDeviceCollectionStart() throws IOException {
        makeTagTester()
                .deviceCollectionStart()

                .verifyAllTags()
        ;
    }

    @Test
    public void testAddTag() throws IOException {
        makeTagTester()
                .makeDeviceModel()

                .addTag("key", "value")
                .verifyTagWasSaved("key", "value")
        ;
    }

    @Test
    public void testDeleteTag() throws IOException {
        makeTagTester()
                .makeDeviceModel()

                .addTag("key", "value")
                .verifyTag("key", "value")

                .deleteTag("key")
                .verifyTagWasDeleted("key")
        ;
    }

    @Test
    public void testReplaceTag() throws IOException {
        makeTagTester()
                .makeDeviceModel()

                .addTag("key", "value")
                .verifyTagWasSaved("key", "value")

                .addTag("key", "newValue")
                .verifyTagWasSaved("key", "newValue")
        ;
    }

    private TagTester makeTagTester() throws IOException {
        return new TagTester();
    }

    private class TagTester {
        final MockAferoClient aferoClient = new MockAferoClient(PATH_PREFIX);
        final MockDeviceEventSource deviceEventSource = new MockDeviceEventSource();
        final DeviceCollection deviceCollection;

        Throwable thrown;
        DeviceModel deviceModel;
        DeviceTagCollection.Tag deletedTag;

        TagTester() {
            deviceCollection = new DeviceCollection(deviceEventSource, aferoClient);
        }

        TagTester deviceCollectionStart() {
            aferoClient.setFileGetDevices("getDevices.json");
            deviceCollection.start().subscribe(new Observer<DeviceCollection>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    thrown = e;
                    e.printStackTrace();
                    assertTrue(false);
                }

                @Override
                public void onNext(DeviceCollection deviceCollection) {
                    deviceCollection.getDevices().take(1)
                            .subscribe(new Action1<DeviceModel>() {
                                @Override
                                public void call(DeviceModel dm) {
                                    deviceModel = dm;
                                }
                            });
                }
            });

            return this;
        }

        TagTester addDevice() {
            deviceCollection.addDevice("genericDevice", false)
                    .subscribe(new Action1<DeviceModel>() {
                        @Override
                        public void call(DeviceModel dm) {
                            deviceModel = dm;
                        }
                    });
            return this;
        }

        TagTester makeDeviceModel() throws IOException {
            DeviceProfile dp = loadDeviceProfile(PATH_PREFIX + "getDeviceProfile/profile-001.json");
            deviceModel = new DeviceModel(DEVICE_ID, dp, false, aferoClient);
            return this;
        }

        TagTester addTag(String key, String value) {
            deviceModel.addTag(key, value).subscribe(
                    new Observer<DeviceTagCollection.Tag>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            assertTrue(false);
                        }

                        @Override
                        public void onNext(DeviceTagCollection.Tag tag) {

                        }
                    }
            );
            return this;
        }

        TagTester deleteTag(String key) {
            deviceModel.removeTag(deviceModel.getTags(key).iterator().next()).subscribe(
                    new Observer<DeviceTagCollection.Tag>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onNext(DeviceTagCollection.Tag tag) {
                            deletedTag = tag;
                        }
                    }
            );
            return this;
        }

        TagTester verifyTag(String key, String value) {
            assertEquals(value, deviceModel.getTags(key).iterator().next().getValue());
            return this;
        }

        TagTester verifyAllTags() throws IOException {
            DeviceSync[] deviceSyncs = loadObject(PATH_PREFIX + "getDevices.json", DeviceSync[].class);
            assertNotEquals(0, deviceSyncs.length);
            assertNotEquals(0, deviceSyncs[0].deviceTags.length);

            for (DeviceTag dt : deviceSyncs[0].deviceTags) {
                DeviceTagCollection.Tag tag = deviceModel.getTagById(dt.deviceTagId);
                assertNotNull(tag);
            }

            return this;
        }

        TagTester verifyTagWasDeleted(String key) {
            assertFalse(deviceModel.getTags(key).iterator().hasNext());
            assertNotNull(deletedTag);
            assertEquals(key, deletedTag.getKey());
            return this;
        }

        TagTester verifyTagWasSaved(final String key, String value) throws JsonProcessingException {
            DeviceTagCollection.Tag tag = deviceModel.getTagInternal(key);
            assertNotNull(tag);
            assertEquals(value, tag.getValue());

            DeviceTag deviceTag = aferoClient.getTagById(tag.getId());
            assertNotNull(deviceTag);
            assertEquals(tag.getKey(), deviceTag.key);
            assertEquals(tag.getValue(), deviceTag.value);

            return this;
        }

        TagTester verifyTagWasNotSaved(final String key, String value) throws JsonProcessingException {
            DeviceTagCollection.Tag tag = deviceModel.getTagInternal(key);
            assertNotNull(tag);
            assertEquals(value, tag.getValue());

            DeviceTag deviceTag = aferoClient.getTagById(tag.getId());
            assertNull(deviceTag);

            return this;
        }
    }
}
