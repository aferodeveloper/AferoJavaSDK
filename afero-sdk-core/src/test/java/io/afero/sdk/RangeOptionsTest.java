/*
 * Copyright (c) 2014-2016 Afero, Inc. All rights reserved.
 */

package io.afero.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

import io.afero.sdk.device.DeviceProfile;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RangeOptionsTest {

    @Test
    public void testDefaults() {
        DeviceProfile.RangeOptions ro = new DeviceProfile.RangeOptions();
        assertTrue(ro.getMin().compareTo(BigDecimal.ZERO) == 0);
        assertTrue(ro.getMax().compareTo(BigDecimal.ONE) == 0);
        assertTrue(ro.getStep().compareTo(BigDecimal.ONE) == 0);
    }

    @Test
    public void testSetAndGetMin() {
        DeviceProfile.RangeOptions ro = new DeviceProfile.RangeOptions();

        int min = 123;
        BigDecimal bdMin = new BigDecimal(min);
        ro.setMin(bdMin);
        assertTrue(ro.getMin().compareTo(bdMin) == 0);

        ro = new DeviceProfile.RangeOptions();
        ro.setMin(min);
        assertTrue(ro.getMin().compareTo(bdMin) == 0);

        ro = new DeviceProfile.RangeOptions();
        ro.setMinValue(Integer.toString(min, 10));
        assertTrue(ro.getMin().compareTo(bdMin) == 0);
    }

    @Test
    public void testSetAndGetMax() {
        DeviceProfile.RangeOptions ro = new DeviceProfile.RangeOptions();
        int max = 456;
        BigDecimal bdMax = new BigDecimal(max);

        ro.setMax(bdMax);
        assertTrue(ro.getMax().compareTo(bdMax) == 0);

        ro = new DeviceProfile.RangeOptions();
        ro.setMax(max);
        assertTrue(ro.getMax().compareTo(bdMax) == 0);

        ro = new DeviceProfile.RangeOptions();
        ro.setMaxValue(Integer.toString(max, 10));
        assertTrue(ro.getMax().compareTo(bdMax) == 0);
    }

    @Test
    public void testSetAndGetStep() {
        DeviceProfile.RangeOptions ro = new DeviceProfile.RangeOptions();
        int step = 78;
        BigDecimal bdStep = new BigDecimal(step);

        ro.setStep(bdStep);
        assertTrue(ro.getStep().compareTo(bdStep) == 0);

        ro = new DeviceProfile.RangeOptions();
        ro.setStep(step);
        assertTrue(ro.getStep().compareTo(bdStep) == 0);

        ro = new DeviceProfile.RangeOptions();
        ro.setStepValue(Integer.toString(step, 10));
        assertTrue(ro.getStep().compareTo(bdStep) == 0);
    }

    @Test
    public void testIndexAndProportionWithIntegers() {
        DeviceProfile.RangeOptions ro = new DeviceProfile.RangeOptions();

        ro.setMin(BigDecimal.ZERO);
        ro.setMax(new BigDecimal(100));
        ro.setStep(BigDecimal.ONE);

        assertTrue(ro.getCount().compareTo(new BigDecimal(100)) == 0);

        BigDecimal value = ro.getValueByIndex(50);
        assertTrue(value.compareTo(new BigDecimal(50)) == 0);

        value = ro.getValueByProportion(0);
        assertTrue(value.compareTo(BigDecimal.ZERO) == 0);
        value = ro.getValueByProportion(0.5);
        assertTrue(value.compareTo(new BigDecimal(50)) == 0);
        value = ro.getValueByProportion(1.0);
        assertTrue(value.compareTo(new BigDecimal(100)) == 0);

        double proportion = ro.getProportionByValue(new BigDecimal(50));
        assertTrue(proportion == 0.5);
    }

    @Test
    public void testIndexAndProportionWithDecimals() {
        DeviceProfile.RangeOptions ro = new DeviceProfile.RangeOptions();

        BigDecimal min = new BigDecimal("1.000");
        BigDecimal max = new BigDecimal("1.100");
        BigDecimal step = new BigDecimal("0.025");
        BigDecimal mid = new BigDecimal("1.050");

        ro.setMin(min);
        ro.setMax(max);
        ro.setStep(step);

        assertTrue(ro.getCount().compareTo(new BigDecimal(4)) == 0);

        BigDecimal value = ro.getValueByIndex(1);
        assertTrue(value.compareTo(new BigDecimal("1.025")) == 0);

        value = ro.getValueByProportion(0);
        assertTrue(value.compareTo(min) == 0);
        value = ro.getValueByProportion(0.5);
        assertTrue(value.compareTo(mid) == 0);
        value = ro.getValueByProportion(1.0);
        assertTrue(value.compareTo(max) == 0);

        double proportion = ro.getProportionByValue(mid);
        assertTrue(proportion == 0.5);
    }

    @Test
    public void testFromJson() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("resources/rangeOptions.json");
        assertNotNull(is);

        ObjectMapper objectMapper = new ObjectMapper();
        DeviceProfile.RangeOptions[] roArray = objectMapper.readValue(is, DeviceProfile.RangeOptions[].class);

        DeviceProfile.RangeOptions ro = roArray[0];
        assertTrue(ro.getMin().compareTo(new BigDecimal(12)) == 0);
        assertTrue(ro.getMax().compareTo(new BigDecimal(23)) == 0);
        assertTrue(ro.getStep().compareTo(new BigDecimal(3)) == 0);

        ro = roArray[1];
        assertTrue(ro.getMin().compareTo(new BigDecimal(123)) == 0);
        assertTrue(ro.getMax().compareTo(new BigDecimal(456)) == 0);
        assertTrue(ro.getStep().compareTo(new BigDecimal(34)) == 0);
    }
}
