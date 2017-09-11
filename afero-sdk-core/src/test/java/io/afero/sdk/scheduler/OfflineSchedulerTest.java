/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.scheduler;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

import io.afero.sdk.AferoTest;
import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.client.mock.MockAferoClient;
import io.afero.sdk.conclave.models.DeviceSync;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceModelTest;
import io.afero.sdk.device.DeviceProfile;
import io.afero.sdk.log.AfLog;
import io.afero.sdk.log.JavaLog;

import static org.junit.Assert.assertEquals;


public class OfflineSchedulerTest extends AferoTest {

    DeviceProfile deviceProfile;

    @Before
    public void setup() throws IOException {
        AfLog.init(new JavaLog());
        deviceProfile = loadDeviceProfile("resources/offlineScheduleEvent/deviceProfile.json");
    }

    @Test
    public void constructor() throws Exception {
        MockAferoClient aferoClient = new MockAferoClient();
        DeviceModel dm = DeviceModelTest.createDeviceModel(deviceProfile, aferoClient);
        OfflineScheduler os = new OfflineScheduler();
        os.start(dm);

        AttributeValue av = new AttributeValue("0102092D640001640002", AttributeValue.DataType.BYTES);

        OfflineScheduleEvent event = new OfflineScheduleEvent(59002, av, dm.getProfile());

        os.addEvent(event);
    }

    @Test
    public void readFromDevice() throws Exception {
        MockAferoClient aferoClient = new MockAferoClient();
        DeviceModel dm = DeviceModelTest.createDeviceModel(deviceProfile, aferoClient);
        OfflineScheduler os = new OfflineScheduler();
        os.start(dm);
        os.readFromDevice();
    }

    @Test
    public void writeToDevice() throws Exception {
        MockAferoClient aferoClient = new MockAferoClient();
        DeviceModel dm = DeviceModelTest.createDeviceModel(deviceProfile, aferoClient);
        OfflineScheduler os = new OfflineScheduler();
        os.start(dm);

        DeviceProfile.Attribute attribute = deviceProfile.getAttributeById(59002);
        AttributeValue av = new AttributeValue("00", AttributeValue.DataType.BYTES);
        dm.writeAttributes().put(attribute.getId(), av).commit().subscribe();

        os.readFromDevice();

        OfflineScheduleEvent event = new OfflineScheduleEvent();

        av = new AttributeValue("01", AttributeValue.DataType.BOOLEAN);
        event.addAttributeValue(850, av);
        av = new AttributeValue("22", AttributeValue.DataType.SINT8);
        event.addAttributeValue(100, av);
        av = new AttributeValue("22", AttributeValue.DataType.SINT8);
        event.addAttributeValue(120, av);
        av = new AttributeValue("33.33", AttributeValue.DataType.FIXED_16_16);
        event.addAttributeValue(800, av);
        os.addEvent(event);

//        av = new AttributeValue("01020D2A640000640000640000640000001000", AttributeValue.DataType.BYTES);

        byte[] bytes = av.getByteValue();

//        OfflineScheduleEvent event = new OfflineScheduleEvent(59002, ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN), deviceProfile);
//        os.addEvent(event);

        os.writeToDevice();
    }

    @Test
    public void getLocalCalendarFromGMT() throws Exception {

        String[] timeZoneIds = TimeZone.getAvailableIDs(-8000 * 3600);
        TimeZone tz = TimeZone.getTimeZone(timeZoneIds[0]);

        // same day
        Calendar calendar = OfflineScheduler.getLocalCalendarFromGMT(OfflineScheduleEvent.TUESDAY, 17, 46, tz);
        int offset = tz.getOffset(calendar.getTimeInMillis()) / 3600000;

        assertEquals(OfflineScheduleEvent.TUESDAY, calendar.get(OfflineScheduler.CALENDAR_DAY));
        assertEquals(17 + offset, calendar.get(OfflineScheduler.CALENDAR_HOUR));
        assertEquals(46, calendar.get(OfflineScheduler.CALENDAR_MINUTE));

        // crossing day boundary
        calendar = OfflineScheduler.getLocalCalendarFromGMT(OfflineScheduleEvent.TUESDAY, 4, 32, tz);
        offset = tz.getOffset(calendar.getTimeInMillis()) / 3600000;

        assertEquals(OfflineScheduleEvent.MONDAY, calendar.get(OfflineScheduler.CALENDAR_DAY));
        assertEquals((4 + offset) + 24, calendar.get(OfflineScheduler.CALENDAR_HOUR));
        assertEquals(32, calendar.get(OfflineScheduler.CALENDAR_MINUTE));
    }

    @Test
    public void getGMTCalendarFromLocal() throws Exception {

        String[] timeZoneIds = TimeZone.getAvailableIDs(-8000 * 3600);
        TimeZone tz = TimeZone.getTimeZone(timeZoneIds[0]);

        // same day
        Calendar calendar = OfflineScheduler.getGMTCalendarFromLocal(OfflineScheduleEvent.TUESDAY, 9, 46, tz);
        int offset = tz.getOffset(calendar.getTimeInMillis()) / 3600000;

        assertEquals(OfflineScheduleEvent.TUESDAY, calendar.get(OfflineScheduler.CALENDAR_DAY));
        assertEquals(9 - offset, calendar.get(OfflineScheduler.CALENDAR_HOUR));
        assertEquals(46, calendar.get(OfflineScheduler.CALENDAR_MINUTE));

        // crossing day boundary
        calendar = OfflineScheduler.getGMTCalendarFromLocal(OfflineScheduleEvent.TUESDAY, 20, 32, tz);
        offset = tz.getOffset(calendar.getTimeInMillis()) / 3600000;

        assertEquals(OfflineScheduleEvent.WEDNESDAY, calendar.get(OfflineScheduler.CALENDAR_DAY));
        assertEquals((20 - offset) % 24, calendar.get(OfflineScheduler.CALENDAR_HOUR));
        assertEquals(32, calendar.get(OfflineScheduler.CALENDAR_MINUTE));
    }

    @Test
    public void migrateAllToDeviceTimeZone() throws Exception {
        MockAferoClient aferoClient = new MockAferoClient();
        DeviceSync data = loadDeviceSync("resources/offlineScheduleEvent/migrateDeviceSync.json");
        DeviceModel deviceModel = DeviceModelTest.createDeviceModel(deviceProfile, aferoClient, data);
        DeviceProfile.Attribute attr1 = deviceModel.getAttributeById(59002);
        DeviceProfile.Attribute attr2 = deviceModel.getAttributeById(59003);
        DeviceProfile.Attribute attr3 = deviceModel.getAttributeById(59004);

        OfflineScheduler.migrateToDeviceTimeZone(deviceModel);

        AttributeValue av = deviceModel.getAttributePendingValue(attr1);
        assertEquals("0302092D", av.toString());

        av = deviceModel.getAttributePendingValue(attr2);
        assertEquals("0303021D", av.toString());

        av = deviceModel.getAttributePendingValue(attr3);
        assertEquals("0304030D", av.toString());
    }

}