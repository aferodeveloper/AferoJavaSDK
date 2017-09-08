/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.scheduler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.conclave.models.DeviceSync;
import io.afero.sdk.device.AttributeWriter;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceProfile;
import io.afero.sdk.log.AfLog;
import io.afero.sdk.utils.RxUtils;
import rx.Emitter;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

public class OfflineScheduler {

    public static final int CALENDAR_DAY = Calendar.DAY_OF_WEEK;
    public static final int CALENDAR_HOUR = Calendar.HOUR_OF_DAY;
    public static final int CALENDAR_MINUTE = Calendar.MINUTE;

    private static final int SCHEDULE_FLAGS_ENABLED = 1;

    private DeviceModel mDeviceModel;
    private final HashMap<Integer,OfflineScheduleEvent> mScheduleItems = new HashMap<>();
    private int mEventMaxCount = 0;
    private Subscription mDeviceSyncSubscription;
    private final AttributeValue mNullValue = new AttributeValue(OfflineScheduleEvent.ZERO_DATA, AttributeValue.DataType.BYTES);
    private PublishSubject<OfflineScheduleEvent> mUpdateSubject = PublishSubject.create();
    private boolean mHasNonLocalTimeEvents;

    public OfflineScheduler() {
    }

    public static boolean hasOfflineScheduleCapability(DeviceModel deviceModel) {
        return deviceModel.getAttributeById(DeviceProfile.SCHEDULE_ATTRIBUTE_ID) != null;
    }

    public void start(DeviceModel deviceModel) {
        if (deviceModel == mDeviceModel) {
            return;
        }

        if (mDeviceModel != null) {
            stop();
        }
        mDeviceModel = deviceModel;

        mEventMaxCount = mDeviceModel.getProfile().getScheduleAttributeCount();

        mDeviceSyncSubscription = RxUtils.safeUnSubscribe(mDeviceSyncSubscription);
        mDeviceSyncSubscription = mDeviceModel.getDeviceSyncPreUpdateObservable()
            .filter(new Func1<DeviceSync, Boolean>() {
                @Override
                public Boolean call(DeviceSync deviceSync) {
                    final int attrId = deviceSync.attribute != null ? deviceSync.attribute.id : 0;
                    return attrId >= DeviceProfile.SCHEDULE_ATTRIBUTE_ID && attrId <= (DeviceProfile.SCHEDULE_ATTRIBUTE_ID + mEventMaxCount - 1);
                }
            })
            .subscribe(new Action1<DeviceSync>() {
                @Override
                public void call(DeviceSync deviceSync) {
                    onDeviceSync(deviceSync);
                }
            });
    }

    public synchronized void stop() {
        mDeviceModel = null;
        mScheduleItems.clear();
        mDeviceSyncSubscription = RxUtils.safeUnSubscribe(mDeviceSyncSubscription);
    }

    public Observable<OfflineScheduleEvent> getUpdateObservable() {
        return mUpdateSubject;
    }

    public synchronized OfflineScheduleEvent getById(int id) {
        return mScheduleItems.get(id);
    }

    public synchronized int getCount() {
        return mScheduleItems.size();
    }

    public int getMaxCount() {
        return mEventMaxCount;
    }

    public int getMaxCountPerDay() {
        return mEventMaxCount / 7;
    }

    public synchronized boolean isDayFullOfEvents(int day) {
        final int maxCountPerDay = getMaxCountPerDay();
        int count = 0;

        for (OfflineScheduleEvent event : mScheduleItems.values()) {
            if (event.getDay() == day) {
                ++count;
                if (count >= maxCountPerDay) {
                    return true;
                }
            }
        }

        return count >= maxCountPerDay;
    }

    public synchronized int getEventCountForDay(int day) {
        int count = 0;

        for (OfflineScheduleEvent event : mScheduleItems.values()) {
            if (event.getDay() == day) {
                ++count;
            }
        }

        return count;
    }

    public synchronized void readFromDevice() {
        mScheduleItems.clear();
        DeviceProfile deviceProfile = mDeviceModel.getProfile();

        for (int i = 0; i < mEventMaxCount; ++i) {
            final int attrId = getEventIdAtIndex(i);

            DeviceProfile.Attribute attribute = mDeviceModel.getAttributeById(attrId);
            AttributeValue value = attribute != null ? mDeviceModel.getAttributePendingValue(attribute) : null;

            boolean isValueValid = !isValueNullOrEmpty(value);
            if (isValueValid) {
                try {
                    OfflineScheduleEvent event = new OfflineScheduleEvent(attrId, value, deviceProfile);
                    mScheduleItems.put(attrId, event);
                    mHasNonLocalTimeEvents = mHasNonLocalTimeEvents || !event.isInLocalTime();
                } catch (Exception e) {
                    AfLog.e(e);
                }
            }
        }
    }

    public synchronized void writeToDevice() {
        AttributeWriter writer = mDeviceModel.writeAttributes();

        for (int i = 0; i < mEventMaxCount; ++i) {
            final int onAttrId = getEventIdAtIndex(i);

            DeviceProfile.Attribute onAttribute = mDeviceModel.getAttributeById(onAttrId);
            if (onAttribute != null) {

                OfflineScheduleEvent onEvent = mScheduleItems.get(onAttrId);
                AttributeValue onValue = onEvent != null ? makeAttributeValue(onEvent) : null;

                if (hasAttributeChanged(onAttribute, onValue)) {
                    writer.put(onAttrId, new AttributeValue(
                            onValue != null ? onValue.toString() : OfflineScheduleEvent.ZERO_DATA,
                            onAttribute.getDataType()));
                }
            }
        }

        if (!writer.isEmpty()) {
            writer.commit().subscribe(new RxUtils.IgnoreResponseObserver<>());
        }
    }

    public void writeMasterSwitchFlag(boolean isOn) {
        DeviceProfile.Attribute attribute = mDeviceModel.getAttributeById(DeviceProfile.SCHEDULE_FLAGS_ATTRIBUTE_ID);
        if (attribute != null) {
            AttributeValue newValue = new AttributeValue(isOn ? "1" : "0", attribute.getDataType());
            mDeviceModel.writeAttributes()
                .put(attribute.getId(), newValue)
                .commit()
                .subscribe(new RxUtils.IgnoreResponseObserver<AttributeWriter.Result>());
        }
    }

    public synchronized void addEvent(OfflineScheduleEvent event) {
        for (int i = 0; i < mEventMaxCount; ++i) {
            final int attrId = getEventIdAtIndex(i);
            if (mScheduleItems.get(attrId) == null) {
                mScheduleItems.put(attrId, event);
                event.setId(attrId);
                break;
            }
        }
    }

    public synchronized void removeEvent(OfflineScheduleEvent event) {
        final int id = event.getId();
        if (id != 0 && mScheduleItems.get(id) != null) {
            mScheduleItems.remove(event.getId());
        }
    }

    public DeviceModel getDeviceModel() {
        return mDeviceModel;
    }

    public synchronized boolean isEventCountAtMaximum() {
        return mScheduleItems.size() == mEventMaxCount;
    }

    public static Calendar getCalendarGMT() {
        return Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    }

    public static Calendar getCalendarGMT(int dayOfWeekGMT, int hourGMT, int minuteGMT) {
        Calendar calendar = getCalendarGMT();
        calendar.set(CALENDAR_DAY, dayOfWeekGMT);
        calendar.set(CALENDAR_HOUR, hourGMT);
        calendar.set(CALENDAR_MINUTE, minuteGMT);
        return calendar;
    }

    public static Calendar getLocalCalendarFromGMT(int dayOfWeekGMT, int hourGMT, int minuteGMT) {
        return getLocalCalendarFromGMT(dayOfWeekGMT, hourGMT, minuteGMT, TimeZone.getDefault());
    }

    public static Calendar getLocalCalendarFromGMT(int dayOfWeekGMT, int hourGMT, int minuteGMT, TimeZone localTimezone) {
        Calendar calGMT = OfflineScheduler.getCalendarGMT(dayOfWeekGMT, hourGMT, minuteGMT);
        Calendar calLocal = Calendar.getInstance(localTimezone);

        calLocal.setTimeInMillis(calGMT.getTimeInMillis());

        return calLocal;
    }

    public static Calendar getGMTCalendarFromLocal(int dayOfWeekLocal, int hourLocal, int minuteLocal) {
        return getGMTCalendarFromLocal(dayOfWeekLocal, hourLocal, minuteLocal, TimeZone.getDefault());
    }

    public static Calendar getGMTCalendarFromLocal(int dayOfWeekLocal, int hourLocal, int minuteLocal, TimeZone localTimezone) {
        Calendar calLocal = Calendar.getInstance(localTimezone);
        calLocal.set(CALENDAR_DAY, dayOfWeekLocal);
        calLocal.set(CALENDAR_HOUR, hourLocal);
        calLocal.set(CALENDAR_MINUTE, minuteLocal);

        Calendar calGMT = getCalendarGMT();
        calGMT.setTimeInMillis(calLocal.getTimeInMillis());

        return calGMT;
    }

    private void onDeviceSync(DeviceSync deviceSync) {
        OfflineScheduleEvent event;

        synchronized (this) {
            final String data = deviceSync.attribute != null ? deviceSync.attribute.value : null;
            final int attrId = deviceSync.attribute != null ? deviceSync.attribute.id : 0;
            if (!OfflineScheduleEvent.isDataStringNullOrEmpty(data)) {
                event = mScheduleItems.get(attrId);
                if (event == null) {
                    event = OfflineScheduleEvent
                            .fromAttributeValueString(attrId, data, mDeviceModel.getProfile());
                    if (event != null) {
                        put(attrId, event);
                    }
                } else {
                    AttributeValue value = new AttributeValue(data, AttributeValue.DataType.BYTES);
                    event.read(value, mDeviceModel.getProfile());
                }
            } else {
                event = getById(attrId);
                if (event != null) {
                    removeEvent(event);
                }
            }
        }

        if (event != null) {
            mUpdateSubject.onNext(event);
        }
    }

    private synchronized boolean put(int id, OfflineScheduleEvent event) {
        OfflineScheduleEvent curEvent = mScheduleItems.get(id);

        if (curEvent != null || mScheduleItems.size() < getMaxCount()) {
            mScheduleItems.put(id, event);
            return true;
        }

        return false;
    }

    private boolean isValueNullOrEmpty(AttributeValue av) {
        return av == null || av.getByteCount() == 0 || mNullValue.compareTo(av) == 0;
    }

    private AttributeValue makeAttributeValue(OfflineScheduleEvent event) {
        return new AttributeValue(event.toAttributeValueString(), AttributeValue.DataType.BYTES);
    }

    private int getEventIdAtIndex(int index) {
        return DeviceProfile.SCHEDULE_ATTRIBUTE_ID + index;
    }

    private boolean writeToAttributeIfChanged(DeviceProfile.Attribute attribute, OfflineScheduleEvent event) {
        String data = event != null ? event.toAttributeValueString() : null;
        AttributeValue newValue = data != null ? new AttributeValue(data, AttributeValue.DataType.BYTES) : null;

        return writeToAttributeIfChanged(attribute, newValue);
    }

    private boolean writeToAttributeIfChanged(DeviceProfile.Attribute attribute, AttributeValue newValue) {
        AttributeValue currentValue = mDeviceModel.getAttributePendingValue(attribute);
        boolean isCurrentValueEmpty = isValueNullOrEmpty(currentValue);
        boolean isNewValueEmpty = isValueNullOrEmpty(newValue);
        boolean areValuesNotEqual = (!(isCurrentValueEmpty || isNewValueEmpty)) && currentValue.compareTo(newValue) != 0;

        if ((isCurrentValueEmpty != isNewValueEmpty) || areValuesNotEqual) {
            if (isNewValueEmpty) {
                newValue = mNullValue;
            }
            mDeviceModel.writeAttributes()
                .put(attribute.getId(), newValue)
                .commit()
                .subscribe(new RxUtils.IgnoreResponseObserver<AttributeWriter.Result>());
            return true;
        }

        return false;
    }

    private boolean hasAttributeChanged(DeviceProfile.Attribute attribute, AttributeValue newValue) {
        AttributeValue currentValue = mDeviceModel.getAttributePendingValue(attribute);
        boolean isCurrentValueEmpty = isValueNullOrEmpty(currentValue);
        boolean isNewValueEmpty = isValueNullOrEmpty(newValue);
        boolean areValuesNotEqual = (!(isCurrentValueEmpty || isNewValueEmpty)) && currentValue.compareTo(newValue) != 0;

        if ((isCurrentValueEmpty != isNewValueEmpty) || areValuesNotEqual) {
            return true;
        }

        return false;
    }

    private AttributeValue findValueWithState(DeviceModel deviceModel, DeviceProfile.Attribute attribute, String state) {
        DeviceProfile.Presentation presentation = deviceModel.getPresentation();

        if (presentation != null) {
            int attributeId = attribute.getId();
            DeviceProfile.AttributeOptions ao = presentation.getAttributeOptionsById(attributeId);
            DeviceProfile.DisplayRule[] valueOptions = ao != null ? ao.getValueOptions() : null;

            if (valueOptions != null) {
                for (DeviceProfile.DisplayRule vo : valueOptions) {
                    AttributeValue matchValue = new AttributeValue(vo.match, attribute.getDataType());

                    Object value = vo.apply.get("state");
                    if (value != null && value instanceof String && state.equalsIgnoreCase((String)value)) {
                        return matchValue;
                    }
                }
            }
        }

        return null;
    }

    public synchronized void clearAll() {

        mScheduleItems.clear();
        AttributeWriter writer = mDeviceModel.writeAttributes();

        for (int i = 0; i < mEventMaxCount; ++i) {
            final int attrId = DeviceProfile.SCHEDULE_ATTRIBUTE_ID + i;

            DeviceProfile.Attribute attribute = mDeviceModel.getAttributeById(attrId);
            AttributeValue av = attribute != null ? mDeviceModel.getAttributePendingValue(attribute) : null;

            if (!isValueNullOrEmpty(av)) {
                writer.put(attribute.getId(), mNullValue);
            }
        }

        if (!writer.isEmpty()) {
            writer.commit().subscribe(new RxUtils.IgnoreResponseObserver<AttributeWriter.Result>());
        }
    }

    public synchronized void forEach(Action1<OfflineScheduleEvent> action) {
        for (OfflineScheduleEvent event : mScheduleItems.values()) {
            action.call(event);
        }
    }

    public synchronized void removeWhere(Func1<OfflineScheduleEvent, Boolean> filterFunc) {
        Iterator<Map.Entry<Integer, OfflineScheduleEvent>> it = mScheduleItems.entrySet().iterator();
        while (it.hasNext()) {
            if (filterFunc.call(it.next().getValue())) {
                it.remove();
            }
        }
    }

    public synchronized OfflineScheduleEvent findEventAtTime(ArrayList<Integer> days, int hour, int minute) {
        for (OfflineScheduleEvent event : mScheduleItems.values()) {
            if ((days == null || days.isEmpty() || days.contains(event.getDay())) &&
                    event.getHour() == hour &&
                    event.getMinute() == minute) {
                return event;
            }
        }

        return null;
    }

    public synchronized Observable<OfflineScheduleEvent> getScheduleEvents() {
        return Observable.create(new Action1<Emitter<OfflineScheduleEvent>>() {
            @Override
            public void call(Emitter<OfflineScheduleEvent> emitter) {
                synchronized (OfflineScheduler.this) {
                    for (OfflineScheduleEvent event : mScheduleItems.values()) {
                        emitter.onNext(event);
                    }
                }
                emitter.onCompleted();
            }
        }, Emitter.BackpressureMode.ERROR);
    }

    private boolean hasNonLocalTimeEvents() {
        return mHasNonLocalTimeEvents;
    }

    public static void migrateAllToDeviceTimeZone(DeviceModel deviceModel) {

        if (deviceModel.isTimeZoneSet() && OfflineScheduler.hasOfflineScheduleCapability(deviceModel)) {

            final OfflineScheduler offlineScheduler = new OfflineScheduler();
            offlineScheduler.start(deviceModel);
            offlineScheduler.readFromDevice();

            if (offlineScheduler.hasNonLocalTimeEvents()) {
                AfLog.w("OfflineScheduler: detected non-local time events - starting migration");

                deviceModel.getTimeZone()
                        .flatMap(new Func1<TimeZone, Observable<OfflineScheduleEvent>>() {
                            @Override
                            public Observable<OfflineScheduleEvent> call(final TimeZone timeZone) {
                                return offlineScheduler.getScheduleEvents()
                                        .filter(new Func1<OfflineScheduleEvent, Boolean>() {
                                            @Override
                                            public Boolean call(OfflineScheduleEvent offlineScheduleEvent) {
                                                return !offlineScheduleEvent.isInLocalTime();
                                            }
                                        })
                                        .doOnNext(new Action1<OfflineScheduleEvent>() {
                                            @Override
                                            public void call(OfflineScheduleEvent offlineScheduleEvent) {

                                                String oldTime = offlineScheduleEvent.getDay() + ":" +
                                                        offlineScheduleEvent.getHour() + ":" +
                                                        offlineScheduleEvent.getMinute();

                                                offlineScheduleEvent.migrateToLocalTimeZone(timeZone);

                                                String newTime = offlineScheduleEvent.getDay() + ":" +
                                                        offlineScheduleEvent.getHour() + ":" +
                                                        offlineScheduleEvent.getMinute();

                                                AfLog.w("OfflineScheduler: migrated event " + offlineScheduleEvent.getId() +
                                                        " from " + oldTime + " to " + newTime + " " + timeZone.getID());
                                            }
                                        });
                            }
                        })
                        .subscribe(new Observer<OfflineScheduleEvent>() {
                            @Override
                            public void onCompleted() {
                                offlineScheduler.writeToDevice();

                                AfLog.w("OfflineScheduler: migration complete");
                            }

                            @Override
                            public void onError(Throwable e) {
                                AfLog.w("OfflineScheduler: migration error");
                                AfLog.e(e);
                            }

                            @Override
                            public void onNext(OfflineScheduleEvent offlineScheduleEvent) {
                            }
                        });
            }

            offlineScheduler.stop();
        }

    }

}
