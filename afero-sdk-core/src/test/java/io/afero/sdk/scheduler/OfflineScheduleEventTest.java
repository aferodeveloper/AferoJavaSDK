/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.scheduler;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import io.afero.sdk.AferoTest;
import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.device.DeviceProfile;
import io.afero.sdk.utils.HexUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class OfflineScheduleEventTest extends AferoTest {

    DeviceProfile deviceProfile;

    @Before
    public void setup() throws IOException {
        deviceProfile = loadDeviceProfile("resources/offlineScheduleEvent/deviceProfile.json");
    }

    @Test
    public void addAttribute() throws Exception {
        OfflineScheduleEvent event = new OfflineScheduleEvent();
        final int value = 100;
        AttributeValue av = new AttributeValue(Integer.toString(value), AttributeValue.DataType.SINT32);

        assertEquals(0, event.getAttributeValueCount());

        event.addAttributeValue(100, av);

        assertEquals(1, event.getAttributeValueCount());
    }

    @Test
    public void getAttributeValue() throws Exception {
        OfflineScheduleEvent event = new OfflineScheduleEvent();
        final int value = 100;
        AttributeValue av = new AttributeValue(Integer.toString(value), AttributeValue.DataType.SINT32);
        event.addAttributeValue(100, av);

        assertEquals(av, event.getAttributeValue(100));
    }

    @Test
    public void fromAttributeValueStringEmptyEvent() throws Exception {

        String hexData = "01010000";
        OfflineScheduleEvent event = OfflineScheduleEvent.fromAttributeValueString(0, hexData, deviceProfile);

        verifyEvent(event, 0, hexData, null);
    }

    @Test
    public void fromAttributeValueStringTimeSpec() throws Exception {

        String hexData = "00030C22";
        OfflineScheduleEvent event = OfflineScheduleEvent.fromAttributeValueString(0, hexData, deviceProfile);

        assertNotNull(event);
        assertEquals(false, event.getRepeats());
        assertEquals(OfflineScheduleEvent.TUESDAY, event.getDayGMT());
        assertEquals(12, event.getHourGMT());
        assertEquals(34, event.getMinuteGMT());
        assertEquals(0, event.getAttributeValueCount());
        assertEquals(hexData, event.toAttributeValueString());
    }

    @Test
    public void fromAttributeValueStringWithEveryAttributeType() throws Exception {

        assertEquals("AttributeValue.DataType enum changed. Please update test.", 18, AttributeValue.DataType.values().length);

        ByteBuffer bb = ByteBuffer.allocate(1000).order(ByteOrder.LITTLE_ENDIAN);

        // Attribute 100 SINT8
        bb.putShort((short)100);
        bb.put((byte)100);

        // Attribute 200 SINT16
        bb.putShort((short)200);
        bb.putShort((short)12346);

        // Attribute 300 SINT32
        bb.putShort((short)300);
        bb.putInt(1234556780);

        // Attribute 400 SINT64
        bb.putShort((short)400);
        bb.putLong(1234556780L * 1234556780L);

        // Attribute 500 FLOAT32
        bb.putShort((short)500);
        bb.putFloat(12345.6789f);

        // Attribute 600 FLOAT64
        bb.putShort((short)600);
        bb.putDouble(12345.6789);

        // Attribute 700 BOOLEAN
        bb.putShort((short)700);
        bb.put((byte)1);

        // Attribute 800 Q_15_16
        bb.putShort((short)800);
        bb.putInt(1234556780);

        // Attribute 900 Q_31_32
        bb.putShort((short)900);
        bb.putLong(1234556780L * 1234556780L);

        // Attribute 50000 Q_31_32
        bb.putShort((short)50000);
        bb.putLong(1234556780L * 1234556780L);

        byte[] bytes = new byte[bb.position()];
        bb.position(0);
        bb.get(bytes);
        bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        final String hexData = "01010000" + HexUtils.hexEncode(bb);
        OfflineScheduleEvent event = OfflineScheduleEvent.fromAttributeValueString(59002, hexData, deviceProfile);

        assertNotNull(event);
        assertEquals(true, event.getRepeats());
        assertEquals(OfflineScheduleEvent.SUNDAY, event.getDayGMT());
        assertEquals(0, event.getHourGMT());
        assertEquals(0, event.getMinuteGMT());

        // FIXME: rewrite to test this in a more sensible way
//        assertEquals(hexData, event.toAttributeValueString());

        assertEquals(10, event.getAttributeValueCount());

        AttributeValue av = event.getAttributeValue(100);
        assertNotNull(av);
        assertEquals(0, av.numericValue().compareTo(new BigDecimal(100)));

        av = event.getAttributeValue(200);
        assertNotNull(av);
        assertEquals(0, av.numericValue().compareTo(new BigDecimal(12346)));

        av = event.getAttributeValue(300);
        assertNotNull(av);
        assertEquals(0, av.numericValue().compareTo(new BigDecimal(1234556780)));

        av = event.getAttributeValue(400);
        assertNotNull(av);
        assertEquals(0, av.numericValue().compareTo(new BigDecimal(1234556780L * 1234556780L)));

// floats aren't really supported anyway
//        av = event.getAttributeValue(500);
//        assertNotNull(av);
//        assertEquals(0, av.numericValue().compareTo(new BigDecimal(12345.6789f)));
//
//        av = event.getAttributeValue(600);
//        assertNotNull(av);
//        assertEquals(0, av.numericValue().compareTo(new BigDecimal(12345.6789)));

        av = event.getAttributeValue(700);
        assertNotNull(av);
        assertTrue(av.booleanValue());

        av = event.getAttributeValue(800);
        assertNotNull(av);
        assertEquals(0, av.numericValue().compareTo(new BigDecimal("18837.84149169922")));

        av = event.getAttributeValue(900);
        assertNotNull(av);
        assertEquals(0, av.numericValue().compareTo(new BigDecimal("354864272.0663846470415592193603516")));

        av = event.getAttributeValue(50000);
        assertNotNull(av);
        assertEquals(0, av.numericValue().compareTo(new BigDecimal("354864272.0663846470415592193603516")));

//        assertEquals(hexData, event.toAttributeValueString());
    }

    private void verifyEvent(OfflineScheduleEvent event, int attributeId, String hexData, BigDecimal value) {
        assertNotNull(event);
        assertEquals(true, event.getRepeats());
        assertEquals(OfflineScheduleEvent.SUNDAY, event.getDayGMT());
        assertEquals(0, event.getHourGMT());
        assertEquals(0, event.getMinuteGMT());

        assertEquals(hexData, event.toAttributeValueString());

        assertEquals(attributeId != 0 ? 1 : 0, event.getAttributeValueCount());
        if (attributeId != 0) {
            assertNotNull(event.getAttributeValue(attributeId));
            assertEquals(0, event.getAttributeValue(attributeId).numericValue().compareTo(value));
        }
    }

    @Test
    public void toAttributeValueStringWithEmptyEvent() throws Exception {
        OfflineScheduleEvent event = new OfflineScheduleEvent();

        String expected = "01000000";
        String s = event.toAttributeValueString();
        assertEquals(expected, s);
    }

}
