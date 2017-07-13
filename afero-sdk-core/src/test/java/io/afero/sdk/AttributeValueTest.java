/*
 * Copyright (c) 2014-2016 Afero, Inc. All rights reserved.
 */

package io.afero.sdk;

import org.junit.Test;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.utils.HexUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AttributeValueTest {

    @Test
    public void testDataTypeEnumHasChanged() {
        assertEquals("AttributeValue.DataType enum changed. Please update tests.", 18, AttributeValue.DataType.values().length);
    }

    @Test
    public void testInitialValue() {
        AttributeValue av = new AttributeValue(AttributeValue.DataType.SINT8);

        assertTrue(av.numericValue().compareTo(BigDecimal.ZERO) == 0);
        assertTrue(av.toString().equals(""));
        assertFalse(av.booleanValue());
    }

    @Test
    public void testString() {
        AttributeValue.DataType dataType = AttributeValue.DataType.UTF8S;
        AttributeValue av = new AttributeValue(dataType);

        final String primValue = "string-value";
        final String hexValueAsString = "737472696e672d76616c7565";
        av.setValue(primValue);
        assertEquals(primValue, av.toString());
        assertTrue(av.compareTo(new AttributeValue(primValue, dataType)) == 0);
        assertFalse(av.compareTo(new AttributeValue("foo", dataType)) == 0);
        assertFalse(av.compareTo(new AttributeValue("123", AttributeValue.DataType.SINT8)) == 0);
        assertTrue(av.numericValue().compareTo(BigDecimal.ZERO) == 0);
        assertFalse(av.booleanValue());
    }

    @Test
    public void testBoolean() {
        AttributeValue.DataType dataType = AttributeValue.DataType.BOOLEAN;
        AttributeValue av = new AttributeValue(dataType);

        final boolean primValue = true;
        final String primValueAsString = Boolean.toString(primValue);
        final String hexValueAsString = "01";
        Boolean value = primValue;
        av.setValue(value);
        assertTrue(value == (av.numericValue().compareTo(BigDecimal.ZERO) != 0));
        assertTrue(av.compareTo(new AttributeValue("true", dataType)) == 0);
        assertFalse(av.compareTo(new AttributeValue("false", dataType)) == 0);
        assertTrue(av.compareTo(new AttributeValue("123", dataType)) == 0);
        assertFalse(av.compareTo(new AttributeValue("0", dataType)) == 0);
        assertTrue(av.toString().equals(Boolean.toString(primValue)));
        assertTrue(av.booleanValue());
    }

    @Test
    public void testByte() {
        AttributeValue.DataType dataType = AttributeValue.DataType.SINT8;
        AttributeValue av = new AttributeValue(dataType);

        final byte primValue = 0x1D;
        final String primValueAsString = Byte.toString(primValue);
        final String hexValueAsString = "1D";
        BigDecimal value = new BigDecimal(primValue);
        av.setValue(value);
        assertTrue(av.numericValue().compareTo(value) == 0);
        assertTrue(av.compareTo(new AttributeValue(primValueAsString, dataType)) == 0);
        assertTrue(av.toString().equals(Byte.toString(primValue)));
        assertTrue(av.booleanValue());
    }

    @Test
    public void testShort() {
        AttributeValue.DataType dataType = AttributeValue.DataType.SINT16;
        AttributeValue av = new AttributeValue(dataType);

        final short primValue = 0x1D1D;
        final String primValueAsString = Short.toString(primValue);
        final String hexValueAsString = "1D1D";
        BigDecimal value = new BigDecimal(primValue);
        av.setValue(value);
        assertTrue(av.numericValue().compareTo(value) == 0);
        assertTrue(av.compareTo(new AttributeValue(primValueAsString, dataType)) == 0);
        assertTrue(av.toString().equals(Short.toString(primValue)));
        assertTrue(av.booleanValue());
    }

    @Test
    public void testInteger() {
        AttributeValue.DataType dataType = AttributeValue.DataType.SINT32;
        AttributeValue av = new AttributeValue(dataType);

        final int primValue = 0x1D1D1D1D;
        final String primValueAsString = Integer.toString(primValue);
        final String hexValueAsString = Integer.toHexString(primValue);
        BigDecimal value = new BigDecimal(primValue);
        av.setValue(value);
        assertTrue(av.numericValue().compareTo(value) == 0);
        assertTrue(av.compareTo(new AttributeValue(primValueAsString, dataType)) == 0);
        assertTrue(av.toString().equals(Integer.toString(primValue)));
        assertTrue(av.booleanValue());
    }

    @Test
    public void testLong() {
        AttributeValue.DataType dataType = AttributeValue.DataType.SINT64;
        AttributeValue av = new AttributeValue(dataType);

        final long primValue = 0x1D1D1D1D1D1D1D1DL;
        final String primValueAsString = Long.toString(primValue);
        final String hexValueAsString = Long.toHexString(primValue);
        BigDecimal value = new BigDecimal(primValue);
        av.setValue(value);
        assertTrue(av.numericValue().compareTo(value) == 0);
        assertTrue(av.compareTo(new AttributeValue(primValueAsString, dataType)) == 0);
        assertTrue(av.toString().equals(Long.toString(primValue)));
        assertTrue(av.booleanValue());
    }

    @Test
    public void testFloat() {
        final AttributeValue.DataType dataType = AttributeValue.DataType.FLOAT32;
        AttributeValue av = new AttributeValue(dataType);

        BigDecimal primValue = new BigDecimal(12345.12345f);
        av.setValue(primValue);
        assertTrue(av.numericValue().compareTo(primValue) == 0);
        assertTrue(av.compareTo(new AttributeValue(primValue.toString(), dataType)) == 0);
        assertEquals(primValue.toString(), av.toString());
        assertTrue(av.booleanValue());
    }

    @Test
    public void testDouble() {
        final AttributeValue.DataType dataType = AttributeValue.DataType.FLOAT64;
        AttributeValue av = new AttributeValue(dataType);

        BigDecimal primValue = new BigDecimal(12345.12345678f);
        av.setValue(primValue);
        assertTrue(av.numericValue().compareTo(primValue) == 0);
        assertTrue(av.compareTo(new AttributeValue(primValue.toString(), dataType)) == 0);
        assertEquals(primValue.toString(), av.toString());
        assertTrue(av.booleanValue());
    }

    @Test
    public void testFixed16() {
        final AttributeValue.DataType dataType = AttributeValue.DataType.FIXED_16_16;
        AttributeValue av = new AttributeValue(dataType);

        BigDecimal primValue = new BigDecimal(12345.12345f);
        av.setValue(primValue);
        assertTrue(av.numericValue().compareTo(primValue) == 0);
        assertTrue(av.compareTo(new AttributeValue(primValue.toString(), dataType)) == 0);
        assertEquals(primValue.toString(), av.toString());
        assertTrue(av.booleanValue());
    }

    @Test
    public void testFixed32() {
        final AttributeValue.DataType dataType = AttributeValue.DataType.FIXED_32_32;
        AttributeValue av = new AttributeValue(dataType);

        BigDecimal primValue = new BigDecimal(12345.12345678f);
        av.setValue(primValue);
        assertTrue(av.numericValue().compareTo(primValue) == 0);
        assertTrue(av.compareTo(new AttributeValue(primValue.toString(), dataType)) == 0);
        assertEquals(primValue.toString(), av.toString());
        assertTrue(av.booleanValue());
    }

    @Test
    public void testGetValueBytes() {
        ByteBuffer bb = makeAttributeValueBytes("123", AttributeValue.DataType.SINT8);
        assertEquals(123, bb.get());

        bb = makeAttributeValueBytes("12345", AttributeValue.DataType.SINT16);
        assertEquals(12345, bb.getShort());

        bb = makeAttributeValueBytes("12345678", AttributeValue.DataType.SINT32);
        assertEquals(12345678, bb.getInt());

        bb = makeAttributeValueBytes("123456780123456789", AttributeValue.DataType.SINT64);
        assertEquals(123456780123456789L, bb.getLong());

        bb = makeAttributeValueBytes("18837.84149169922", AttributeValue.DataType.Q_15_16);
        assertEquals(1234556780, bb.getInt());

        bb = makeAttributeValueBytes("354864272.0663846470415592193603516", AttributeValue.DataType.Q_31_32);
        assertEquals(1234556780L * 1234556780L, bb.getLong());

        bb = makeAttributeValueBytes("true", AttributeValue.DataType.BOOLEAN);
        assertTrue(bb.get() != 0);

        String testString = "This is a test";
        bb = makeAttributeValueBytes(testString, AttributeValue.DataType.UTF8S);
        assertEquals(testString, new String(bb.array(), StandardCharsets.UTF_8));

        testString = "C0EDBABEDEADBEEF";
        bb = makeAttributeValueBytes(testString, AttributeValue.DataType.BYTES);
        assertEquals(testString, HexUtils.hexEncode(bb));
    }

    private ByteBuffer makeAttributeValueBytes(String value, AttributeValue.DataType dataType) {
        AttributeValue av = new AttributeValue(value, dataType);
        ByteBuffer bb = av.getValueBytes(null);
        bb.position(0);
        return bb;
    }

    @Test
    public void testGetValueByteCount() {
        assertEquals(1, new AttributeValue(AttributeValue.DataType.SINT8).getByteCount());
        assertEquals(1, new AttributeValue(AttributeValue.DataType.UINT8).getByteCount());
        assertEquals(1, new AttributeValue(AttributeValue.DataType.BOOLEAN).getByteCount());

        assertEquals(2, new AttributeValue(AttributeValue.DataType.SINT16).getByteCount());
        assertEquals(2, new AttributeValue(AttributeValue.DataType.UINT16).getByteCount());

        assertEquals(4, new AttributeValue(AttributeValue.DataType.SINT32).getByteCount());
        assertEquals(4, new AttributeValue(AttributeValue.DataType.UINT32).getByteCount());
        assertEquals(4, new AttributeValue(AttributeValue.DataType.Q_15_16).getByteCount());
        assertEquals(4, new AttributeValue(AttributeValue.DataType.FLOAT32).getByteCount());

        assertEquals(8, new AttributeValue(AttributeValue.DataType.SINT64).getByteCount());
        assertEquals(8, new AttributeValue(AttributeValue.DataType.UINT64).getByteCount());
        assertEquals(8, new AttributeValue(AttributeValue.DataType.Q_31_32).getByteCount());
        assertEquals(8, new AttributeValue(AttributeValue.DataType.FLOAT64).getByteCount());

        String testString = "This is a test";
        assertEquals(testString.getBytes().length, new AttributeValue(testString, AttributeValue.DataType.UTF8S).getByteCount());

        testString = "C0EDBABEDEADBEEF";
        assertEquals(HexUtils.parseHexBinary(testString).length, new AttributeValue(testString, AttributeValue.DataType.BYTES).getByteCount());
    }

    @Test
    public void testIsNumeric() {
        assertFalse(AttributeValue.isNumericType(AttributeValue.DataType.BOOLEAN));
        assertFalse(AttributeValue.isNumericType(AttributeValue.DataType.UTF8S));
        assertFalse(AttributeValue.isNumericType(AttributeValue.DataType.BYTES));

        assertTrue(AttributeValue.isNumericType(AttributeValue.DataType.SINT8));
        assertTrue(AttributeValue.isNumericType(AttributeValue.DataType.UINT8));

        assertTrue(AttributeValue.isNumericType(AttributeValue.DataType.SINT16));
        assertTrue(AttributeValue.isNumericType(AttributeValue.DataType.UINT16));

        assertTrue(AttributeValue.isNumericType(AttributeValue.DataType.SINT32));
        assertTrue(AttributeValue.isNumericType(AttributeValue.DataType.UINT32));
        assertTrue(AttributeValue.isNumericType(AttributeValue.DataType.Q_15_16));
        assertTrue(AttributeValue.isNumericType(AttributeValue.DataType.FLOAT32));

        assertTrue(AttributeValue.isNumericType(AttributeValue.DataType.SINT64));
        assertTrue(AttributeValue.isNumericType(AttributeValue.DataType.UINT64));
        assertTrue(AttributeValue.isNumericType(AttributeValue.DataType.Q_31_32));
        assertTrue(AttributeValue.isNumericType(AttributeValue.DataType.FLOAT64));
    }

    @Test
    public void testIsNumericDecimal() {
        assertFalse(AttributeValue.isNumericDecimalType(AttributeValue.DataType.BOOLEAN));
        assertFalse(AttributeValue.isNumericDecimalType(AttributeValue.DataType.UTF8S));
        assertFalse(AttributeValue.isNumericDecimalType(AttributeValue.DataType.BYTES));

        assertFalse(AttributeValue.isNumericDecimalType(AttributeValue.DataType.SINT8));
        assertFalse(AttributeValue.isNumericDecimalType(AttributeValue.DataType.UINT8));

        assertFalse(AttributeValue.isNumericDecimalType(AttributeValue.DataType.SINT16));
        assertFalse(AttributeValue.isNumericDecimalType(AttributeValue.DataType.UINT16));

        assertFalse(AttributeValue.isNumericDecimalType(AttributeValue.DataType.SINT32));
        assertFalse(AttributeValue.isNumericDecimalType(AttributeValue.DataType.UINT32));
        assertTrue(AttributeValue.isNumericDecimalType(AttributeValue.DataType.Q_15_16));
        assertTrue(AttributeValue.isNumericDecimalType(AttributeValue.DataType.FLOAT32));

        assertFalse(AttributeValue.isNumericDecimalType(AttributeValue.DataType.SINT64));
        assertFalse(AttributeValue.isNumericDecimalType(AttributeValue.DataType.UINT64));
        assertTrue(AttributeValue.isNumericDecimalType(AttributeValue.DataType.Q_31_32));
        assertTrue(AttributeValue.isNumericDecimalType(AttributeValue.DataType.FLOAT64));
    }

}
