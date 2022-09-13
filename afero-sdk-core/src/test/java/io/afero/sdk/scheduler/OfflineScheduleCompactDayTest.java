/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.scheduler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import io.afero.sdk.AferoTest;
import io.afero.sdk.device.DeviceProfile;


public class OfflineScheduleCompactDayTest extends AferoTest {
    DeviceProfile deviceProfile;

    @Before
    public void setup() throws IOException {
        deviceProfile = loadDeviceProfile("offlineScheduleEvent/deviceProfile.json");
    }

    @Test
    public void addSomeDays() throws Exception {

        OfflineScheduleEvent event = new OfflineScheduleEvent(true);

        event.setDay(OfflineScheduleEvent.MONDAY);
        event.setDay(OfflineScheduleEvent.TUESDAY);
        final String hexData = event.toAttributeValueString();
        OfflineScheduleEvent newEvent = OfflineScheduleEvent.fromAttributeValueString(0, hexData, deviceProfile);
        assertTrue(newEvent.hasDay(OfflineScheduleEvent.MONDAY));
        assertTrue(newEvent.hasDay(OfflineScheduleEvent.TUESDAY));

        assertFalse(newEvent.hasDay(OfflineScheduleEvent.SUNDAY));
        assertFalse( newEvent.hasDay(OfflineScheduleEvent.WEDNESDAY));
        assertFalse(newEvent.hasDay(OfflineScheduleEvent.THURSDAY));
        assertFalse( newEvent.hasDay(OfflineScheduleEvent.FRIDAY));
        assertFalse(newEvent.hasDay(OfflineScheduleEvent.SATURDAY));
    }


    @Test
    public void addAllDays() throws Exception {

        OfflineScheduleEvent event = new OfflineScheduleEvent(true);

        event.setDay(OfflineScheduleEvent.SUNDAY);
        event.setDay(OfflineScheduleEvent.MONDAY);
        event.setDay(OfflineScheduleEvent.TUESDAY);
        event.setDay(OfflineScheduleEvent.WEDNESDAY);
        event.setDay(OfflineScheduleEvent.THURSDAY);
        event.setDay(OfflineScheduleEvent.FRIDAY);
        event.setDay(OfflineScheduleEvent.SATURDAY);

        final String hexData = event.toAttributeValueString();

        OfflineScheduleEvent newEvent = OfflineScheduleEvent.fromAttributeValueString(0, hexData, deviceProfile);
        assertTrue(newEvent.hasDay(OfflineScheduleEvent.SUNDAY));
        assertTrue( newEvent.hasDay(OfflineScheduleEvent.MONDAY));
        assertTrue(newEvent.hasDay(OfflineScheduleEvent.TUESDAY));
        assertTrue( newEvent.hasDay(OfflineScheduleEvent.WEDNESDAY));
        assertTrue( newEvent.hasDay(OfflineScheduleEvent.THURSDAY));
        assertTrue( newEvent.hasDay(OfflineScheduleEvent.FRIDAY));
        assertTrue( newEvent.hasDay(OfflineScheduleEvent.SATURDAY));

    }

    @Test
    public void getDayOnCompactEvent() throws Exception {

        OfflineScheduleEvent event = new OfflineScheduleEvent(true);

        event.setDay(OfflineScheduleEvent.MONDAY);

        final String hexData = event.toAttributeValueString();

        OfflineScheduleEvent newEvent = OfflineScheduleEvent.fromAttributeValueString(0, hexData, deviceProfile);

        try {
            newEvent.getDay();
            throw new Exception("This should not be allowed");
        } catch (IllegalStateException e) {
            // Expected
        }
    }

    @Test
    public void isCompactEvent() throws Exception {

        OfflineScheduleEvent event = new OfflineScheduleEvent(true);

        event.setDay(OfflineScheduleEvent.MONDAY);

        final String hexData = event.toAttributeValueString();

        OfflineScheduleEvent newEvent = OfflineScheduleEvent.fromAttributeValueString(0, hexData, deviceProfile);

        assertTrue( newEvent.hasCompactDayRepresentation());
    }

    @Test
    public void isNotCompactEvent() throws Exception {

        OfflineScheduleEvent event = new OfflineScheduleEvent();

        event.setDay(OfflineScheduleEvent.MONDAY);

        final String hexData = event.toAttributeValueString();

        OfflineScheduleEvent newEvent = OfflineScheduleEvent.fromAttributeValueString(0, hexData, deviceProfile);

        assertFalse(newEvent.hasCompactDayRepresentation());
    }
}
