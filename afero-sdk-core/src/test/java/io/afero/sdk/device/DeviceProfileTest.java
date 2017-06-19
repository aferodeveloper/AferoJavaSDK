/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.InputStream;
import java.math.BigDecimal;

import io.afero.sdk.BuildConfig;
import io.afero.sdk.client.afero.models.AttributeValue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk=21)
public class DeviceProfileTest {

    @Test
    public void testJson() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        InputStream is = getClass().getClassLoader().getResourceAsStream("profile.json");
        assertNotNull(is);

        DeviceProfile profile = objectMapper.readValue(is, DeviceProfile.class);
        assertEquals(1, profile.getServiceCount());

        DeviceProfile.Service service = profile.getServices()[0];
        assertNotNull(service);
        assertEquals(100, service.getId());
        assertEquals(1, service.getAttributeCount());

        DeviceProfile.Attribute attribute = profile.getAttributeById(200);
        assertNotNull(attribute);

        attribute = service.getAttributes()[0];
        assertNotNull(attribute);
        assertEquals(200, attribute.getId());
        assertEquals(AttributeValue.DataType.UINT8, attribute.getDataType());
        assertEquals(DeviceProfile.SEMANTIC_TYPE_POWER, attribute.getSemanticType());
        assertTrue(attribute.isWritable());
//        assertSame(attribute, profile.getPrimaryOperationAttribute(null));
        assertSame(attribute, profile.findAttributeWithSemanticType(DeviceProfile.SEMANTIC_TYPE_POWER));
        assertEquals("Power", attribute.getLabel());

        DeviceProfile.Presentation presentation = profile.getPresentation(null);
        assertNotNull(presentation);
        assertEquals(2, presentation.getGroupCount());
        assertEquals(1, presentation.getControlCount());

        DeviceProfile.AttributeOptions attributeOptions = presentation.getAttributeOptionsById(200);
        assertNotNull(attributeOptions);
        DeviceProfile.DisplayRule[] valueOptions = attributeOptions.getValueOptions();
        assertNotNull(valueOptions);
        assertEquals(2, valueOptions.length);
        DeviceProfile.DisplayRule item = valueOptions[0];
        assertEquals("1", item.match);
        assertEquals("one", item.getApplyLabel());
        item = valueOptions[1];
        assertEquals("2", item.match);
        assertEquals("two", item.getApplyLabel());

        DeviceProfile.RangeOptions rangeOptions = attributeOptions.getRangeOptions();
        assertNotNull(rangeOptions);
        assertTrue(rangeOptions.getMin().compareTo(new BigDecimal(100)) == 0);
        assertTrue(rangeOptions.getMax().compareTo(new BigDecimal(200)) == 0);
        assertTrue(rangeOptions.getStep().compareTo(new BigDecimal(5)) == 0);
        assertEquals("unit", rangeOptions.unitLabel);

        DeviceProfile.Gauge gauge = presentation.getGauge();
        assertNotNull(gauge);
        assertEquals("TestLabel", gauge.getLabel());
        DeviceProfile.Icon icon = gauge.getForegroundIcon();
        assertNotNull(icon);
        assertEquals(1, icon.getURICount());
        assertEquals("https://cdn.afero.io/gaugeIcon.png", icon.getURI(0));
        icon = gauge.getBackgroundIcon();
        assertNull(icon);

        DeviceProfile.Group group = presentation.getGroup(0);
        assertNotNull(group);
        assertEquals(0, group.getGroupCount());
        assertEquals(1, group.getControlIdCount());
        assertEquals(100, group.getControlId(0));
        assertEquals("groupLabel1", group.getLabel());

        gauge = group.getGauge();
        assertNotNull(gauge);
        icon = gauge.getForegroundIcon();
        assertNotNull(icon);
        assertEquals(2, icon.getURICount());
        assertEquals("https://cdn.afero.io/fgIcon0.png", icon.getURI(0));
        assertEquals("https://cdn.afero.io/fgIcon1.png", icon.getURI(1));
        icon = gauge.getBackgroundIcon();
        assertNotNull(icon);
        assertEquals(1, icon.getURICount());
        assertEquals("https://cdn.afero.io/bgIcon.png", icon.getURI(0));

        group = presentation.getGroup(1);
        assertNotNull(group);
        assertEquals(0, group.getGroupCount());
        assertEquals(0, group.getControlIdCount());
        assertEquals("groupLabel2", group.getLabel());
        gauge = group.getGauge();
        assertNotNull(gauge);
        icon = gauge.getForegroundIcon();
        assertNotNull(icon);
        assertEquals(1, icon.getURICount());
        assertEquals("https://cdn.afero.io/fgIcon.png", icon.getURI(0));


        DeviceProfile.Control control = presentation.getControl(0);
        assertNotNull(control);
        assertEquals(100, control.getId());
        assertEquals("menuControl", control.getType());
        assertEquals(200, control.getAttributeIdByKey("value"));
        assertSame(control, presentation.getControlById(100));
    }
}
