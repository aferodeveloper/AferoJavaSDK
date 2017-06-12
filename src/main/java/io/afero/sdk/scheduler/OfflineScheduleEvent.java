/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.scheduler;

import android.support.annotation.NonNull;
import android.util.SparseArray;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;

import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.utils.AfLog;
import io.afero.sdk.device.DeviceProfile;
import io.afero.sdk.utils.HexUtils;

public class OfflineScheduleEvent implements Comparable<OfflineScheduleEvent> {

    public static final int SUNDAY = 1;
    public static final int MONDAY = 2;
    public static final int TUESDAY = 3;
    public static final int WEDNESDAY = 4;
    public static final int THURSDAY = 5;
    public static final int FRIDAY = 6;
    public static final int SATURDAY = 7;

    public static final int FIRST_DAY = SUNDAY;
    public static final int LAST_DAY = SATURDAY;

    public static final String ZERO_DATA = "00";

    private static byte EVENT_FLAGS_REPEATS = 1;

    /*
        From http://wiki.afero.io/display/FIR/AfRac+Offline+Schedule+Design

        Time Specification

        Name            Data Type   Description
        ------------------------------------------------------------------------
        repeats         bool	    True if the event should repeat weekly.
        day of the week	uint8_t	    Day of the week where 1 = Sunday and Saturday = 7.
        hour	        uint8_t	    Hour of the day that the event should occur on.
        minute	        uint8_t	    Minute of the day that the event should occur on.
    */

    public static final int TIME_SPEC_BYTE_COUNT = 4;
    public static final int ATTRIBUTE_ID_BYTE_COUNT = Short.SIZE / Byte.SIZE;

    private int mId;

    private byte mFlags = EVENT_FLAGS_REPEATS;
    private int mDayGMT;
    private int mHourGMT;
    private int mMinuteGMT;
    private SparseArray<AttributeValue> mAttributeValues = new SparseArray<>();


    public OfflineScheduleEvent() {
    }

    public OfflineScheduleEvent(int id, @NonNull AttributeValue value, @NonNull DeviceProfile dp) {
        setId(id);
        read(value, dp);
    }

    public OfflineScheduleEvent(int id, @NonNull ByteBuffer bb, @NonNull DeviceProfile dp) {
        setId(id);
        read(bb, dp);
    }

    public void read(@NonNull AttributeValue value, @NonNull DeviceProfile dp) {
        ByteBuffer bb = ByteBuffer.wrap(value.getByteValue()).order(ByteOrder.LITTLE_ENDIAN);
        read(bb, dp);
    }

    public void read(@NonNull ByteBuffer bb, @NonNull DeviceProfile dp) {
        mFlags = bb.get();
        setDayGMT(bb.get());
        setHourGMT(bb.get());
        setMinuteGMT(bb.get());

        byte[] b = new byte[4];

        while (bb.remaining() > 0) {

            // attribute id is really an *unsigned* short
            // so read it out and convert it to an int manually
            b[0] = bb.get();
            b[1] = bb.get();
            b[2] = 0;
            b[3] = 0;

            int attrId = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getInt();

            DeviceProfile.Attribute attribute = dp.getAttributeById(attrId);
            if (attribute != null) {

                AttributeValue av = new AttributeValue(bb, attribute.getDataType());
                addAttributeValue(attrId, av);

            } else {
                throw new IllegalArgumentException("Couldn't find attribute id=" + attrId);
            }
        }
    }

    public static boolean isDataStringNullOrEmpty(String data) {
        return data == null || data.isEmpty() || ZERO_DATA.equals(data);
    }

    public void setId(int attrId) {
        mId = attrId;
    }

    public int getId() {
        return mId;
    }

    public void setRepeats(boolean repeats) {
        if (repeats) {
            mFlags |= EVENT_FLAGS_REPEATS;
        } else {
            mFlags &= ~EVENT_FLAGS_REPEATS;
        }
    }

    public boolean getRepeats() {
        return (mFlags & EVENT_FLAGS_REPEATS) != 0;
    }

    public void setDayGMT(int dayGMT) {
        mDayGMT = dayGMT;
    }

    public int getDayGMT() {
        return mDayGMT;
    }

    public int getDayLocal() {
        return getLocalCalendar().get(OfflineScheduler.CALENDAR_DAY);
    }

    public Calendar getLocalCalendar() {
        return OfflineScheduler.getLocalCalendarFromGMT(getDayGMT(), getHourGMT(), getMinuteGMT());
    }

    public Calendar getGMTCalendar() {
        return OfflineScheduler.getCalendarGMT(getDayGMT(), getHourGMT(), getMinuteGMT());
    }

    public void setHourGMT(int t) {
        mHourGMT = t;
    }

    public int getHourGMT() {
        return mHourGMT;
    }

    public void setMinuteGMT(int t) {
        mMinuteGMT = t;
    }

    public int getMinuteGMT() {
        return mMinuteGMT;
    }

    public void addAttributeValue(int id, @NonNull AttributeValue value) {
        mAttributeValues.put(id, value);
    }

    public int getAttributeIdAt(int index) {
        return mAttributeValues.keyAt(index);
    }

    public AttributeValue getAttributeValueAt(int index) {
        return mAttributeValues.valueAt(index);
    }

    public AttributeValue getAttributeValue(int id) {
        return mAttributeValues.get(id);
    }

    public int getAttributeValueCount() {
        return mAttributeValues.size();
    }

    public void clearAttributes() {
        mAttributeValues.clear();
    }

    public static OfflineScheduleEvent fromAttributeValueString(int id, @NonNull String s, @NonNull DeviceProfile dp) {

        try {
            ByteBuffer bb = HexUtils.hexDecode(s);
            bb.position(0);
            return new OfflineScheduleEvent(id, bb, dp);
        } catch (Exception e) {
            AfLog.e(e);
        }

        return null;
    }

    public String toAttributeValueString() {

        int byteCount = TIME_SPEC_BYTE_COUNT;
        for (int i = 0, n = mAttributeValues.size(); i < n; ++i) {
            AttributeValue av = mAttributeValues.valueAt(i);
            byteCount += ATTRIBUTE_ID_BYTE_COUNT;
            byteCount += av.getByteCount();
        }

        ByteBuffer bb = ByteBuffer.allocate(byteCount).order(ByteOrder.LITTLE_ENDIAN);

        bb.put(mFlags);
        bb.put((byte) mDayGMT);
        bb.put((byte) mHourGMT);
        bb.put((byte) mMinuteGMT);

        for (int i = 0, n = mAttributeValues.size(); i < n; ++i) {
            AttributeValue av = mAttributeValues.valueAt(i);
            bb.putShort((short)mAttributeValues.keyAt(i));
            av.getValueBytes(bb);
        }

        bb.position(0);

        return HexUtils.hexEncode(bb);
    }

    @Override
    public int compareTo(@NonNull OfflineScheduleEvent another) {
        if (this == another) return 0;

        int result = Integer.compare(getDayGMT(), another.getDayGMT());

        if (result == 0) {
            result = Integer.compare(getHourGMT(), another.getHourGMT());
        }

        if (result == 0) {
            result = Integer.compare(getMinuteGMT(), another.getMinuteGMT());
        }

        return result;
    }

    public boolean isSaved() {
        return mId != 0;
    }
}
