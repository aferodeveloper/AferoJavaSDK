/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

import android.support.annotation.NonNull;

import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import io.afero.sdk.log.AfLog;
import io.afero.sdk.utils.HexUtils;

public class AttributeValue implements Comparable<AttributeValue> {

    public enum DataType {
        UNKNOWN,
        BOOLEAN,
        UINT8,
        UINT16,
        UINT32,
        UINT64,
        SINT8,
        SINT16,
        SINT32,
        SINT64,
        FLOAT32,
        FLOAT64,
        FIXED_16_16,
        FIXED_32_32,
        Q_15_16,
        Q_31_32,
        UTF8S,
        BYTES
        ;
    }

    private static final BigDecimal Q_16_FACTOR = BigDecimal.valueOf(65536);
    private static final BigDecimal Q_32_FACTOR = BigDecimal.valueOf(4294967296L);

    private Boolean mBooleanValue;
    private BigDecimal mNumericValue;
    private String mStringValue;
    private byte[] mByteArrayValue;

    private DataType mDataType;

    public AttributeValue(DataType dataType) {
        mDataType = dataType;
    }

    public AttributeValue(String value, DataType dataType) {
        mDataType = dataType;

        switch (dataType) {

            case UNKNOWN:
                break;

            case BOOLEAN:
                if (value.matches("\\d+")) {
                    mBooleanValue = new BigDecimal(value).compareTo(BigDecimal.ZERO) != 0;
                } else {
                    mBooleanValue = Boolean.valueOf(value);
                }
                break;

            case UINT8:
            case UINT16:
            case UINT32:
            case UINT64:
            case SINT8:
            case SINT16:
            case SINT32:
            case SINT64:
            case FLOAT32:
            case FLOAT64:
            case FIXED_16_16:
            case FIXED_32_32:
            case Q_15_16:
            case Q_31_32:
                try {
                    if (value.startsWith("0x")) {
                        mNumericValue = new BigDecimal(Long.decode(value));
                    } else {
                        mNumericValue = new BigDecimal(value);
                    }
                } catch (NumberFormatException e) {
                    mStringValue = value;
                }
                break;

            case UTF8S:
                mStringValue = value;
                break;

            case BYTES:
                try {
                    mByteArrayValue = value == null || value.isEmpty() ? new byte[0] : HexUtils.parseHexBinary(value);
                } catch (Exception e) {
                    AfLog.e(e);
                    mByteArrayValue = new byte[0];
                }
                break;
        }
    }

    public AttributeValue(ByteBuffer bb, DataType dataType) {
        mDataType = dataType;

        switch (dataType) {

            case UNKNOWN:
                break;

            case BOOLEAN:
                mBooleanValue = bb.get() != 0;
                break;

            case UINT8:
            case SINT8:
                mNumericValue = new BigDecimal(bb.get());
                break;

            case UINT16:
            case SINT16:
                mNumericValue = new BigDecimal(bb.getShort());
                break;

            case UINT32:
            case SINT32:
                mNumericValue = new BigDecimal(bb.getInt());
                break;

            case UINT64:
            case SINT64:
                mNumericValue = new BigDecimal(bb.getLong());
                break;

            case FLOAT32:
                mNumericValue = new BigDecimal(bb.getFloat());
                break;

            case FLOAT64:
                mNumericValue = new BigDecimal(bb.getDouble());
                break;

            case FIXED_16_16:
            case Q_15_16:
                mNumericValue = BigDecimal.valueOf(bb.getInt()).divide(Q_16_FACTOR, MathContext.DECIMAL64);
                break;

            case FIXED_32_32:
            case Q_31_32:
                mNumericValue = BigDecimal.valueOf(bb.getLong()).divide(Q_32_FACTOR, MathContext.DECIMAL128);
                break;

            case UTF8S:
                byte[] bytes = HexUtils.toBytes(bb);
                mStringValue = new String(bytes, StandardCharsets.UTF_8);
                bb.position(bb.position() + bytes.length);
                break;

            case BYTES:
                mByteArrayValue = HexUtils.toBytes(bb);
                break;
        }
    }

    public DataType getDataType() {
        return mDataType;
    }

    public void setValue(Boolean value) {
        mBooleanValue = value;
    }

    public void setValue(BigDecimal value) {
        mNumericValue = value;
    }

    public void setValue(String value) {
        mStringValue = value;
    }

    public Boolean booleanValue() {
        if (mBooleanValue != null) {
            return mBooleanValue;
        }
        else if (mNumericValue != null) {
            return mNumericValue.compareTo(BigDecimal.ZERO) != 0;
        }
        else if (mStringValue != null) {
            try {
                return Boolean.parseBoolean(mStringValue);
            } catch (NumberFormatException nfe) {
                // ignore
            }
        }

        return false;
    }

    @Override
    public int compareTo(@NonNull AttributeValue value) {
        int result = 0;

        switch (mDataType) {
            case UNKNOWN:
                break;

            case BOOLEAN:
                return mBooleanValue.compareTo(value.booleanValue());

            case UINT8:
            case UINT16:
            case UINT32:
            case UINT64:
            case SINT8:
            case SINT16:
            case SINT32:
            case SINT64:
            case FLOAT32:
            case FLOAT64:
            case FIXED_16_16:
            case FIXED_32_32:
            case Q_15_16:
            case Q_31_32:
                return mNumericValue.compareTo(value.numericValue());

            case UTF8S:
                return mStringValue.compareTo(value.toString());

            case BYTES:
                return toString().compareTo(value.toString());
        }

        return result;
    }

    public BigDecimal numericValue() {
        if (mNumericValue == null) {
            if (mStringValue != null && !mStringValue.isEmpty()) {
                try {
                    if (mStringValue.startsWith("0x")) {
                        return new BigDecimal(Long.decode(mStringValue));
                    } else {
                        return new BigDecimal(mStringValue);
                    }
                } catch (NumberFormatException nfe) {
                    // ignore
                }
            }
            else if (mBooleanValue != null) {
                return mBooleanValue ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        }

        return mNumericValue != null ? mNumericValue : BigDecimal.ZERO;
    }

    public String toString() {
        String s;

        if (mStringValue != null) {
            s = mStringValue;
        }
        else if (mNumericValue != null) {
            s = mNumericValue.toString();
        }
        else if (mBooleanValue != null) {
            s = mBooleanValue.toString();
        }
        else if (mByteArrayValue != null) {
            s = mByteArrayValue.length > 0 ? HexUtils.printHexBinary(mByteArrayValue) : "";
        }
        else {
            s = "";
        }

        return s;
    }

    public ByteBuffer getValueBytes(ByteBuffer bb) {

        if (bb == null) {
            bb = ByteBuffer.allocate(getByteCount());
            bb.order(ByteOrder.LITTLE_ENDIAN);
        }

        BigDecimal value = numericValue();

        switch (mDataType) {
            case BOOLEAN:
                bb.put((byte)(booleanValue() ? 1 : 0));
                break;

            case UINT8:
            case SINT8:
                bb.put(value.byteValue());
                break;

            case UINT16:
            case SINT16:
                bb.putShort(value.shortValue());
                break;

            case UINT32:
            case SINT32:
                bb.putInt(value.intValue());
                break;

            case FLOAT32:
                bb.putFloat(value.floatValue());
                break;

            case UINT64:
            case SINT64:
                bb.putLong(value.longValue());
                break;

            case FLOAT64:
                bb.putDouble(value.doubleValue());
                break;

            case FIXED_16_16:
            case Q_15_16:
                bb.putInt(value.multiply(Q_16_FACTOR).intValue());
                break;

            case FIXED_32_32:
            case Q_31_32:
                bb.putLong(value.multiply(Q_32_FACTOR).longValue());
                break;

            case UTF8S:
                bb.put(toString().getBytes(StandardCharsets.UTF_8));
                break;

            case BYTES:
                if (mByteArrayValue != null && mByteArrayValue.length > 0) {
                    bb.put(mByteArrayValue);
                }
                break;

            case UNKNOWN:
                break;
        }

        return bb;
    }

    public byte[] getByteValue() {
        return mByteArrayValue;
    }

    public int getByteCount() {

        switch (mDataType) {
            case BOOLEAN:
            case UINT8:
            case SINT8:
                return Byte.SIZE / Byte.SIZE;

            case UINT16:
            case SINT16:
                return Short.SIZE / Byte.SIZE;

            case UINT32:
            case SINT32:
            case FLOAT32:
            case FIXED_16_16:
            case Q_15_16:
                return Integer.SIZE / Byte.SIZE;

            case UINT64:
            case SINT64:
            case FLOAT64:
            case FIXED_32_32:
            case Q_31_32:
                return Long.SIZE / Byte.SIZE;

            case UTF8S:
                return toString().getBytes(StandardCharsets.UTF_8).length;

            case BYTES:
                return mByteArrayValue != null ? mByteArrayValue.length : 0;

            case UNKNOWN:
                break;
        }

        return -1;
    }

    public static boolean isNumericType(DataType type) {
        switch (type) {
            case UNKNOWN:
            case BOOLEAN:
                break;

            case UINT8:
            case UINT16:
            case UINT32:
            case UINT64:
            case SINT8:
            case SINT16:
            case SINT32:
            case SINT64:
            case FLOAT32:
            case FLOAT64:
            case FIXED_16_16:
            case FIXED_32_32:
            case Q_15_16:
            case Q_31_32:
                return true;

            case UTF8S:
            case BYTES:
                break;
        }

        return false;
    }

    public static boolean isNumericDecimalType(DataType type) {
        switch (type) {
            case FLOAT32:
            case FLOAT64:
            case FIXED_16_16:
            case FIXED_32_32:
            case Q_15_16:
            case Q_31_32:
                return true;
        }

        return false;
    }
}
