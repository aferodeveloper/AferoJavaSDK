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
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk=21)
public class ActionValueTest {

    @Test
    public void verifyDefaultConstructor() {
        DeviceRules.ActionValue av = new DeviceRules.ActionValue();
        assertEquals(0, av.getId());
        assertNull(av.getValue());
        assertEquals(0, av.getUpdatedTimestamp());
    }

    @Test
    public void verifyIdAndDataConstructor() {
        final int ATTRIBUTE_ID = 100;
        final String ATTRIBUTE_VALUE = "1";

        DeviceRules.ActionValue av = new DeviceRules.ActionValue(ATTRIBUTE_ID, ATTRIBUTE_VALUE);
        assertEquals(ATTRIBUTE_ID, av.getId());
        assertEquals(ATTRIBUTE_VALUE, av.getValue());
        assertEquals(0, av.getUpdatedTimestamp());
    }

    @Test
    public void verifyCopyConstructor() {
        final int ATTRIBUTE_ID = 100;
        final String ATTRIBUTE_VALUE = "1";

        DeviceRules.ActionValue sourceValue = new DeviceRules.ActionValue(ATTRIBUTE_ID, ATTRIBUTE_VALUE);
        DeviceRules.ActionValue destValue = new DeviceRules.ActionValue(sourceValue);
        assertEquals(ATTRIBUTE_ID, destValue.getId());
        assertEquals(ATTRIBUTE_VALUE, destValue.getValue());
        assertEquals(0, destValue.getUpdatedTimestamp());
    }
}
