/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.TimeZone;
import java.util.TreeMap;

import io.afero.sdk.AferoTest;
import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.client.mock.MockAferoClient;
import io.afero.sdk.client.mock.MockDeviceEventSource;
import io.afero.sdk.client.mock.ResourceLoader;
import io.afero.sdk.conclave.models.DeviceSync;
import rx.Observer;
import rx.functions.Action1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DeviceModelTest extends AferoTest {

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
        DeviceProfile dp = loadDeviceProfile("resources/deviceModelTestProfile.json");
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
        DeviceSync ds = loadDeviceSync("resources/deviceSync.json");
        DeviceProfile dp = loadDeviceProfile("resources/deviceModelTestProfile.json");
        DeviceModel dm = new DeviceModel(DEVICE_ID, dp, false, null);

        dm.update(ds);

        assertEquals("deviceModel-model-id", dm.getId());
        assertEquals("device-profile-id", dm.getProfileID());
        assertEquals("device-name", dm.getName());

        String tagValue = dm.getTag("tag-id-1");
        assertEquals("tag-1", tagValue);

        tagValue = dm.getTag("tag-id-2");
        assertEquals("tag-2", tagValue);

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
        BigDecimal actual = av != null ? av.numericValue() : null;;
        assertTrue(actual.compareTo(expected) == 0);
    }

    private void testAttribute(DeviceModel dm, DeviceProfile.Attribute attribute, String expected) {
        AttributeValue av = dm.getAttributePendingValue(attribute);
        String actual = av != null ? av.toString() : null;;
        assertEquals(expected, actual);
    }

    @Test
    public void testWriteAttribute() throws IOException {
        final int ATTRIBUTE_ID = 100;
        final String ATTRIBUTE_VALUE = "32";

        makeWriteAttributeTester()
                .deviceModelWriteAttribute(ATTRIBUTE_ID, ATTRIBUTE_VALUE, AttributeValue.DataType.SINT8)
                .deviceModelUpdate(1, ATTRIBUTE_ID, ATTRIBUTE_VALUE)

                .verifyWriteResultStatus(ATTRIBUTE_ID, AttributeWriter.Result.Status.SUCCESS)
                ;
    }

    private WriteAttributeTester makeWriteAttributeTester() throws IOException {
        return new WriteAttributeTester();
    }

    private static class WriteAttributeTester {
        final ResourceLoader resourceLoader = new ResourceLoader("resources/");
        final DeviceProfile deviceProfile;
        final MockAferoClient aferoClient = new MockAferoClient();
        final DeviceModel deviceModel;
        TreeMap<Integer, AttributeWriter.Result> writeResults = new TreeMap<>();

        WriteAttributeTester() throws IOException {
            deviceProfile = loadDeviceProfile("deviceModelTestProfile.json");
            deviceModel = new DeviceModel(DEVICE_ID, deviceProfile, false, aferoClient);
        }

        DeviceProfile loadDeviceProfile(String path) throws IOException {
            return resourceLoader.createObjectFromJSONResource(path, DeviceProfile.class);
        }

        WriteAttributeTester deviceModelWriteAttribute(int attrId, String value, AttributeValue.DataType type) {
            deviceModel.writeAttributes()
                .put(attrId, new AttributeValue(value, type))
                .commit()
                .subscribe(new Action1<AttributeWriter.Result>() {
                    @Override
                    public void call(AttributeWriter.Result wr) {
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

        WriteAttributeTester verifyWriteResultStatus(int attrId, AttributeWriter.Result.Status resultStatus) {
            assertEquals(resultStatus, writeResults.get(attrId).status);
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
        final ResourceLoader resourceLoader = new ResourceLoader("resources/");
        final DeviceProfile deviceProfile;
        final MockAferoClient aferoClient = new MockAferoClient();
        final DeviceModel deviceModel;
        final TimeZoneObserver getTimeZoneObserver = new TimeZoneObserver();
        final TimeZoneObserver setTimeZoneObserver = new TimeZoneObserver();

        TimeZoneTester() throws IOException {
            deviceProfile = loadDeviceProfile("deviceModelTestProfile.json");
            deviceModel = new DeviceModel(DEVICE_ID, deviceProfile, false, aferoClient);
        }

        DeviceProfile loadDeviceProfile(String path) throws IOException {
            return resourceLoader.createObjectFromJSONResource(path, DeviceProfile.class);
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

    private class TagTester {
        static final String PATH_PREFIX = "resources/deviceModel/";
        final ResourceLoader resourceLoader = new ResourceLoader(PATH_PREFIX);
        final MockAferoClient aferoClient = new MockAferoClient(PATH_PREFIX);
        final MockDeviceEventSource deviceEventSource = new MockDeviceEventSource();
        final DeviceCollection deviceCollection;

        Throwable thrown;
        DeviceModel deviceModelReturnedFromAddDevice;

        TagTester() {
            deviceCollection = new DeviceCollection(deviceEventSource, aferoClient);
        }

        TagTester deviceCollectionStartWithNoDevices() {
            aferoClient.setFileGetDevices("getDevicesEmpty.json");
            deviceCollection.start().subscribe(new Observer<DeviceCollection>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    thrown = e;
                }

                @Override
                public void onNext(DeviceCollection deviceCollection) {

                }
            });

            return this;
        }


        TagTester deviceCollectionStop() {
            deviceCollection.stop();
            return this;
        }

        TagTester deviceCollectionStopWithCatch() {
            try {
                deviceCollection.stop();
            } catch (Throwable t) {
                thrown = t;
            }
            return this;
        }

        TagTester deviceCollectionAddDevice() {
            deviceCollection.addDevice("genericDevice", false)
                    .subscribe(new Action1<DeviceModel>() {
                        @Override
                        public void call(DeviceModel deviceModel) {
                            deviceModelReturnedFromAddDevice = deviceModel;
                        }
                    });
            return this;
        }
    }
}
