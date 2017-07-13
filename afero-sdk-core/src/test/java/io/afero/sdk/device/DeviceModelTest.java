/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;

import io.afero.sdk.AferoTest;
import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.conclave.models.DeviceSync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeviceModelTest extends AferoTest {

    private static final String DEVICE_ID = "device-model-id";

    public static DeviceModel createDeviceModel(DeviceProfile deviceProfile, AferoClient aferoClient) throws IOException {
        return new DeviceModel(DEVICE_ID, deviceProfile, false, aferoClient);
    }

    @Before
    public void beforeTests() {
    }

    @Test
    public void testDefaults() throws IOException {
        DeviceProfile dp = loadDeviceProfile("resources/deviceModelTestProfile.json");
        DeviceModel dm = new DeviceModel(DEVICE_ID, dp, false, null);

        assertEquals(DeviceModel.State.NORMAL, dm.getState());
        assertFalse(dm.isOTAInProgress());
        assertEquals(0, dm.getOTAProgress());
        assertEquals(dp.getId().length(), dm.getName().length());
    }

    @Test
    public void testUpdateFromJson() throws IOException {
        DeviceSync ds = loadDeviceSync("resources/deviceSync.json");
        DeviceProfile dp = loadDeviceProfile("resources/deviceModelTestProfile.json");
        DeviceModel dm = new DeviceModel(DEVICE_ID, dp, false, null);

        dm.update(ds);

        assertEquals("device-model-id", dm.getId());
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
        AttributeValue av = dm.readPendingValue(attribute);
        BigDecimal actual = av != null ? av.numericValue() : null;;
        assertTrue(actual.compareTo(expected) == 0);
    }

    private void testAttribute(DeviceModel dm, DeviceProfile.Attribute attribute, String expected) {
        AttributeValue av = dm.readPendingValue(attribute);
        String actual = av != null ? av.toString() : null;;
        assertEquals(expected, actual);
    }
}
