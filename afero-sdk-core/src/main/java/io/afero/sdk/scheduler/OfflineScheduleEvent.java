/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.scheduler;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.device.DeviceProfile;
import io.afero.sdk.log.AfLog;
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
    private static byte EVENT_FLAGS_USES_DEVICE_TIMEZONE = 2;

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

    private byte mFlags = (byte)(EVENT_FLAGS_REPEATS | EVENT_FLAGS_USES_DEVICE_TIMEZONE);
    private int mDay;
    private int mHour;
    private int mMinute;
    private HashMap<Integer,AttributeValue> mAttributeValues = new HashMap<>();


    public OfflineScheduleEvent() {
    }

    OfflineScheduleEvent(int id, AttributeValue value, DeviceProfile dp) {
        setId(id);
        read(value, dp);
    }

    private OfflineScheduleEvent(int id, ByteBuffer bb, DeviceProfile dp) {
        setId(id);
        read(bb, dp);
    }

    public void read(AttributeValue value, DeviceProfile dp) {
        ByteBuffer bb = ByteBuffer.wrap(value.getByteValue()).order(ByteOrder.LITTLE_ENDIAN);
        read(bb, dp);
    }

    public void read(ByteBuffer bb, DeviceProfile dp) {
        mFlags = bb.get();

        setDay(bb.get());
        setHour(bb.get());
        setMinute(bb.get());

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

    @Deprecated
    public void setDayGMT(int dayGMT) {
        mDay = dayGMT;
        setIsInLocalTime(false);
    }

    @Deprecated
    public int getDayGMT() {
        return mDay;
    }

    @Deprecated
    public int getDayLocal() {
        return getLocalCalendar().get(OfflineScheduler.CALENDAR_DAY);
    }

    @Deprecated
    public void setHourGMT(int t) {
        setIsInLocalTime(false);
        mHour = t;
    }

    @Deprecated
    public int getHourGMT() {
        return mHour;
    }

    @Deprecated
    public void setMinuteGMT(int t) {
        setIsInLocalTime(false);
        mMinute = t;
    }

    @Deprecated
    public int getMinuteGMT() {
        return mMinute;
    }

    public void setDay(int day) {
        setIsInLocalTime(true);
        mDay = day;
    }

    public int getDay() {
        return isInLocalTime() ? mDay : getLocalCalendar().get(OfflineScheduler.CALENDAR_DAY);
    }

    public void setHour(int t) {
        setIsInLocalTime(true);
        mHour = t;
    }

    public int getHour() {
        return isInLocalTime() ? mHour : getLocalCalendar().get(OfflineScheduler.CALENDAR_HOUR);
    }

    public void setMinute(int t) {
        setIsInLocalTime(true);
        mMinute = t;
    }

    public int getMinute() {
        return isInLocalTime() ? mMinute : getLocalCalendar().get(OfflineScheduler.CALENDAR_MINUTE);
    }

    @Deprecated
    public Calendar getLocalCalendar() {
        return OfflineScheduler.getLocalCalendarFromGMT(getDayGMT(), getHourGMT(), getMinuteGMT());
    }

    @Deprecated
    public Calendar getGMTCalendar() {
        return OfflineScheduler.getCalendarGMT(getDayGMT(), getHourGMT(), getMinuteGMT());
    }

    public void addAttributeValue(int id, AttributeValue value) {
        mAttributeValues.put(id, value);
    }

    public Set<Map.Entry<Integer, AttributeValue>> getAttributeEntrySet() {
        return mAttributeValues.entrySet();
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

    public boolean isSaved() {
        return mId != 0;
    }

    @Override
    public int compareTo(OfflineScheduleEvent another) {
        if (this == another) return 0;

        int result = Integer.compare(getDay(), another.getDay());

        if (result == 0) {
            result = Integer.compare(getHour(), another.getHour());
        }

        if (result == 0) {
            result = Integer.compare(getMinute(), another.getMinute());
        }

        return result;
    }

    static OfflineScheduleEvent fromAttributeValueString(int id, String s, DeviceProfile dp) {

        try {
            ByteBuffer bb = HexUtils.hexDecode(s);
            bb.position(0);
            return new OfflineScheduleEvent(id, bb, dp);
        } catch (Exception e) {
            AfLog.e(e);
        }

        return null;
    }

    String toAttributeValueString() {

        int byteCount = TIME_SPEC_BYTE_COUNT;
        for (AttributeValue av : mAttributeValues.values()) {
            byteCount += ATTRIBUTE_ID_BYTE_COUNT;
            byteCount += av.getByteCount();
        }

        ByteBuffer bb = ByteBuffer.allocate(byteCount).order(ByteOrder.LITTLE_ENDIAN);

        bb.put(mFlags);
        bb.put((byte) mDay);
        bb.put((byte) mHour);
        bb.put((byte) mMinute);

        for (Map.Entry<Integer,AttributeValue> entry : mAttributeValues.entrySet()) {
            AttributeValue av = entry.getValue();
            int attrId = entry.getKey();
            bb.putShort((short)attrId);
            av.getValueBytes(bb);
        }

        bb.position(0);

        return HexUtils.hexEncode(bb);
    }

    static boolean isDataStringNullOrEmpty(String data) {
        return data == null || data.isEmpty() || ZERO_DATA.equals(data);
    }

    OfflineScheduleEvent migrateToLocalTimeZone(TimeZone tz) {
        setIsInLocalTime(true);

        Calendar cal = OfflineScheduler.getLocalCalendarFromGMT(mDay, mHour, mMinute, tz);
        mDay = cal.get(OfflineScheduler.CALENDAR_DAY);
        mHour = cal.get(OfflineScheduler.CALENDAR_HOUR);
        mMinute = cal.get(OfflineScheduler.CALENDAR_MINUTE);

        return this;
    }

    boolean isInLocalTime() {
        return (mFlags & EVENT_FLAGS_USES_DEVICE_TIMEZONE) != 0;
    }

    void setIsInLocalTime(boolean b) {
        if (b) {
            mFlags |= EVENT_FLAGS_USES_DEVICE_TIMEZONE;
        } else {
            mFlags &= ~EVENT_FLAGS_USES_DEVICE_TIMEZONE;
        }
    }
}
