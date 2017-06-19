/*
 * Copyright (c) 2014-2016 Afero, Inc. All rights reserved.
 */

package io.afero.sdk;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import io.afero.sdk.client.afero.models.DeviceRules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk=21)
public class DeviceActionTest {

    @Test
    public void testInitialValue() {
        DeviceRules.DeviceAction action = new DeviceRules.DeviceAction();
        assertNull(action.getDeviceId());
        assertTrue(action.getDurationSeconds() == 0);
        assertTrue(action.getAttributeCount() == 0);
        assertFalse(action.hasAttributes());
    }

    @Test
    public void testCopyConstructor() {
        final String DEVICE_ID = "device-id";
        final int ATTRIBUTE_ID = 100;
        final String ATTRIBUTE_VALUE = "1";

        DeviceRules.DeviceAction sourceAction = new DeviceRules.DeviceAction();

        sourceAction.setDeviceId(DEVICE_ID);
        sourceAction.addAttribute(ATTRIBUTE_ID, ATTRIBUTE_VALUE);
        assertNotNull(sourceAction.getAttributeById(ATTRIBUTE_ID));
        assertEquals(ATTRIBUTE_VALUE, sourceAction.getFirstAttribute().getValue());

        DeviceRules.DeviceAction destAction = new DeviceRules.DeviceAction(sourceAction);
        assertEquals(1, destAction.getAttributeCount());
        assertNotNull(destAction.getAttributeById(ATTRIBUTE_ID));
        assertEquals(ATTRIBUTE_VALUE, sourceAction.getFirstAttribute().getValue());
    }

    @Test
    public void testCopyAttributes() {
        final int ATTRIBUTE_ID = 100;
        final String ATTRIBUTE_VALUE = "1";

        DeviceRules.DeviceAction sourceAction = new DeviceRules.DeviceAction();
        DeviceRules.DeviceAction destAction = new DeviceRules.DeviceAction();

        sourceAction.addAttribute(ATTRIBUTE_ID, ATTRIBUTE_VALUE);
        assertEquals(ATTRIBUTE_VALUE, sourceAction.getFirstAttribute().getValue());

        destAction.copyAttributesFrom(sourceAction);
        assertEquals(1, destAction.getAttributeCount());
        assertNotNull(destAction.getAttributeById(ATTRIBUTE_ID));
        assertEquals(ATTRIBUTE_VALUE, sourceAction.getFirstAttribute().getValue());
    }

}
