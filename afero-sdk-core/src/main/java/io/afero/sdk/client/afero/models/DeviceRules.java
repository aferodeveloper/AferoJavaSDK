/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.Vector;

public class DeviceRules {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ActionValue {

        private int mAttributeId;
        private String mValue;
        private long mUpdatedTimestamp;

        public ActionValue() {
        }

        public ActionValue(ActionValue av) {
            setId(av.getId());
            setValue(av.getValue());
            setUpdatedTimestamp(av.getUpdatedTimestamp());
        }

        public ActionValue(int id, String value) {
            mAttributeId = id;
            mValue = value;
        }

        @JsonProperty
        public void setId(int id) {
            mAttributeId = id;
        }

        public int getId() {
            return mAttributeId;
        }

        @JsonProperty
        public void setValue(String value) {
            mValue = value;
        }

        public String getValue() {
            return mValue;
        }

        @JsonProperty
        public void setUpdatedTimestamp(long ts) {
            mUpdatedTimestamp = ts;
        }

        public long getUpdatedTimestamp() {
            return mUpdatedTimestamp;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeviceAction {

        private String mDeviceId;
        private final HashMap<Integer,ActionValue> mAttributes = new HashMap<>();
        private int mDurationSeconds;

        public DeviceAction() {
        }

        public DeviceAction(DeviceAction action) {
            setDeviceId(action.getDeviceId());

            setDurationSeconds(getDurationSeconds());

            for (ActionValue av : action.mAttributes.values()) {
                mAttributes.put(av.getId(), new ActionValue(av));
            }
        }

        @JsonProperty
        public void setDeviceId(String id) {
            mDeviceId = id;
        }

        public String getDeviceId() {
            return mDeviceId;
        }

        @JsonProperty
        public void setAttributes(ActionValue[] as) {
            for (ActionValue a : as) {
                mAttributes.put(a.getId(), a);
            }
        }

        @JsonProperty
        public ActionValue[] getAttributes() {
            final int size = mAttributes.size();

            if (size > 0) {
                ActionValue[] as = new ActionValue[size];
                int i = 0;
                for (ActionValue av : mAttributes.values()) {
                    as[i] = av;
                    ++i;
                }
                return as;
            }

            return new ActionValue[0];
        }

        @JsonIgnore
        public ActionValue getAttributeById(int id) {
            return mAttributes != null ? mAttributes.get(id) : null;
        }

        @JsonIgnore
        public boolean hasAttribute(int id) {
            return getAttributeById(id) != null;
        }

        @JsonIgnore
        public void addAttribute(int id, String value) {
            mAttributes.put(id, new ActionValue(id, value));
        }

        @JsonIgnore
        public void addAttribute(ActionValue av) {
            mAttributes.put(av.getId(), av);
        }

        @JsonIgnore
        public ActionValue getFirstAttribute() {
            return mAttributes.size() > 0 ? mAttributes.values().iterator().next() : null;
        }

        @JsonIgnore
        public void clearAttributes() {
            mAttributes.clear();
        }

        @JsonIgnore
        public int getAttributeCount() {
            return mAttributes.size();
        }

        @JsonIgnore
        public void copyAttributesFrom(DeviceAction otherAction) {
            clearAttributes();
            mAttributes.putAll(otherAction.mAttributes);
        }

        @JsonProperty
        public void setDurationSeconds(int seconds) {
            mDurationSeconds = seconds;
        }

        public int getDurationSeconds() {
            return mDurationSeconds;
        }

        @JsonIgnore
        public boolean hasAttributes() {
            return mAttributes != null && mAttributes.size() != 0;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeviceFilterCriteria {

        public enum Operation {
            NONE,
            EQUALS,
            GREATER_THAN,
            LESS_THAN
        }

        private ActionValue mActionValue;
        private Operation mOperation = Operation.EQUALS;
        private String mDeviceId;
        private boolean mTrigger = true;

        public DeviceFilterCriteria() {
        }

        public DeviceFilterCriteria(DeviceFilterCriteria dfc) {
            mActionValue = dfc.mActionValue != null ? new ActionValue(dfc.mActionValue) : null;
            mOperation = dfc.mOperation;
            mDeviceId = dfc.mDeviceId;
            mTrigger = dfc.mTrigger;
        }

        @JsonProperty
        public void setAttribute(ActionValue av) {
            mActionValue = av;
        }

        public ActionValue getAttribute() {
            return mActionValue;
        }

        @JsonProperty
        public void setOperation(Operation op) {
            mOperation = op;
        }

        public Operation getOperation() {
            return mOperation;
        }

        @JsonProperty
        public void setDeviceId(String id) {
            mDeviceId = id;
        }

        public String getDeviceId() {
            return mDeviceId;
        }

        @JsonProperty
        public void setTrigger(boolean t) {
            mTrigger = t;
        }

        public boolean getTrigger() {
            return mTrigger;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Schedule {

        public enum DayOfWeek {
            SUN,
            MON,
            TUE,
            WED,
            THU,
            FRI,
            SAT
        }

        private String mScheduleId;
        private DayOfWeek[] mDaysOfWeek;
        private Time mTime;

        public Schedule() {
        }

        public Schedule(Schedule s) {
            mScheduleId = s.mScheduleId;
            mTime = new Time(s.mTime);
            if (s.mDaysOfWeek != null) {
                mDaysOfWeek = s.mDaysOfWeek.clone();
            }
        }

        @JsonProperty
        public void setScheduleId(String id) {
            mScheduleId = id;
        }

        public String getScheduleId() {
            return mScheduleId;
        }

        @JsonProperty
        public void setDaysOfWeek(DayOfWeek[] daysOfWeek) {
            mDaysOfWeek = daysOfWeek;
        }

        public DayOfWeek[] getDaysOfWeek() {
            return mDaysOfWeek;
        }

//        @JsonIgnore
//        public void addDay(DayOfWeek day) {
//            if (mDaysOfWeek == null) {
//                mDaysOfWeek = new Vector<DayOfWeek>(1);
//            }
//            if (!mDaysOfWeek.contains(day)) {
//                mDaysOfWeek.add(day);
//            }
//        }
//
//        @JsonIgnore
//        public void removeDay(DayOfWeek day) {
//            if (mDaysOfWeek != null) {
//                mDaysOfWeek.remove(day);
//            }
//        }

        @JsonProperty
        public void setDayOfWeek(DayOfWeek[] daysOfWeek) {
            mDaysOfWeek = daysOfWeek;
        }

        public DayOfWeek[] getDayOfWeek() {
            return mDaysOfWeek;
        }

        @JsonProperty
        public void setTime(Time time) {
            mTime = time;
        }

        public Time getTime() {
            return mTime;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Rule {

        public enum RuleType {
            NONE,
            ON_DEMAND,
            SCHEDULE,
            DEVICE_LINK
        }

        public static final String DEFAULT_ACCOUNT_NOTIFICATION_ID = "85c796b4-c08c-4bcd-bb9f-2f5fa850e5f9";

        private RuleType mType;
        private String mRuleId;
        private long mLocalId;
        private String mLabel;
        private boolean mEnabled = true;
        private Vector<DeviceFilterCriteria> mDeviceFilterCriteria;
        private Vector<DeviceAction> mDeviceActions;
        private Schedule mSchedule;
        private String mScheduleId;
        private String mDeviceGroupId;
        private String mAccountNotificationId;


        public Rule(RuleType ruleType) {
            mType = ruleType;
            initLocalId();
        }

        public Rule() {
            initLocalId();
        }

        private void initLocalId() {
            mLocalId = UUID.randomUUID().getLeastSignificantBits();
        }

        public Rule(Rule r) {
            copyFrom(r);

            mLocalId = UUID.randomUUID().getLeastSignificantBits();
        }

        @JsonIgnore
        public void copyFrom(Rule r) {
            mType = r.getType();
            mRuleId = r.mRuleId;
//            mLocalId = r.mLocalId;
            mLabel = r.mLabel;
            mEnabled = r.mEnabled;
            mScheduleId = r.getScheduleId();
            mDeviceGroupId = r.mDeviceGroupId;
            mAccountNotificationId = r.mAccountNotificationId;

            if (r.mSchedule != null) {
                mSchedule = new Schedule(r.mSchedule);
            }

            if (r.getDeviceFilterCriteria() != null) {
                mDeviceFilterCriteria = new Vector<>(r.getDeviceFilterCriteria().size());
                for (DeviceFilterCriteria dfc : r.getDeviceFilterCriteria()) {
                    mDeviceFilterCriteria.add(new DeviceFilterCriteria(dfc));
                }
            } else {
                mDeviceFilterCriteria = null;
            }

            if (r.getDeviceActions() != null) {
                mDeviceActions = new Vector<>(r.getDeviceActions().size());
                for (DeviceAction action : r.getDeviceActions()) {
                    mDeviceActions.add(new DeviceAction(action));
                }
            } else {
                mDeviceActions = null;
            }
        }


        @JsonIgnore
        public RuleType getType() {

            if (mType == null) {
                if (getSchedule() != null) {
                    mType = RuleType.SCHEDULE;
                }
                else if (getDeviceFilterCriteria() != null) {
                    mType = RuleType.DEVICE_LINK;
                }
                else {
                    mType = RuleType.ON_DEMAND;
                }
            }

            return mType;
        }

        @JsonIgnore
        public boolean isSaved() {
            return mRuleId != null;
        }

        @JsonIgnore
        public void setLocalId(long id) {
            mLocalId = id;
        }

        @JsonIgnore
        public long getLocalId() {
            return mLocalId;
        }

        @JsonProperty
        public void setRuleId(String id) {
            mRuleId = id;
        }

        public String getRuleId() {
            return mRuleId;
        }

        @JsonProperty
        public void setLabel(String label) {
            mLabel = label;
        }

        public String getLabel() {
            return mLabel;
        }

        @JsonIgnore
        public boolean hasLabel() {
            return mLabel != null && !mLabel.isEmpty();
        }

        @JsonProperty
        public void setEnabled(boolean enabled) {
            mEnabled = enabled;
        }

        public boolean getEnabled() {
            return mEnabled;
        }

        @JsonProperty
        public void setDeviceActions(Vector<DeviceAction> actions) {
            mDeviceActions = actions;
        }

        public Vector<DeviceAction> getDeviceActions() {
            return mDeviceActions;
        }

        @JsonProperty
        public void addDeviceAction(DeviceAction action) {
            if (mDeviceActions == null) {
                mDeviceActions = new Vector<DeviceAction>(1);
            }
            mDeviceActions.add(action);
        }

        @JsonIgnore
        public DeviceAction getAction() {
            return mDeviceActions != null && mDeviceActions.size() > 0 ? mDeviceActions.firstElement() : null;
        }

        @JsonIgnore
        public int getActionCount() {
            return mDeviceActions != null ? mDeviceActions.size() : 0;
        }

        @JsonIgnore
        public boolean hasActions() {
            return getActionCount() > 0;
        }

        @JsonProperty
        public void setDeviceFilterCriteria(Vector<DeviceFilterCriteria> filters) {
            mDeviceFilterCriteria = filters;
        }

        public Vector<DeviceFilterCriteria> getDeviceFilterCriteria() {
            return mDeviceFilterCriteria;
        }

        @JsonIgnore
        public boolean hasDeviceFilterCriteria() {
            return mDeviceFilterCriteria != null && !mDeviceFilterCriteria.isEmpty();
        }

        @JsonIgnore
        public boolean hasInvalidTrigger() {
            if (mDeviceFilterCriteria != null) {
                if (mDeviceFilterCriteria.isEmpty()) {
                    return true;
                }
                for (DeviceFilterCriteria dfc : mDeviceFilterCriteria) {
                    if (dfc.getDeviceId() == null) {
                        return true;
                    }
                }
            }

            return false;
        }

        public DeviceFilterCriteria getDeviceFilterCriteriaById(String deviceId, int attributeId) {
            if (mDeviceFilterCriteria != null) {
                for (DeviceFilterCriteria dfc : mDeviceFilterCriteria) {
                    if (deviceId == null || dfc.getDeviceId().equals(deviceId)) {
                        ActionValue av = dfc.getAttribute();
                        if (av != null && av.getId() == attributeId) {
                            return dfc;
                        }
                    }
                }
            }
            return null;
        }

        @JsonIgnore
        public void initDeviceFilterCriteria() {
            if (mDeviceFilterCriteria == null) {
                mDeviceFilterCriteria = new Vector<DeviceFilterCriteria>(1);
            }
        }

        @JsonIgnore
        public void addDeviceFilterCriteria(DeviceFilterCriteria filter) {
            initDeviceFilterCriteria();
            mDeviceFilterCriteria.add(filter);
        }

        @JsonIgnore
        public void clearDeviceFilterCriteria() {
            if (mDeviceFilterCriteria != null) {
                mDeviceFilterCriteria.clear();
            }
        }

        @JsonIgnore
        public void clearDeviceFilterCriteria(String deviceId, int attributeId) {
            if (mDeviceFilterCriteria != null) {
                DeviceFilterCriteria dfc = getDeviceFilterCriteriaById(deviceId, attributeId);
                if (dfc != null) {
                    mDeviceFilterCriteria.remove(dfc);
                }
            }
        }

        @JsonIgnore
        public void copyDeviceFilterCriteriaFrom(Rule rule) {

            Vector<DeviceFilterCriteria> thatCriteria = rule.getDeviceFilterCriteria();

            if (thatCriteria != null) {
                Vector<DeviceFilterCriteria> thisCriteria = getDeviceFilterCriteria();
                if (thisCriteria == null) {
                    thisCriteria = new Vector<>(thatCriteria.size());
                }
                mDeviceFilterCriteria = thisCriteria;

                for (DeviceFilterCriteria dfc : thatCriteria) {
                    thisCriteria.add(new DeviceFilterCriteria(dfc));
                }
            }
        }

        @JsonIgnore
        public DeviceFilterCriteria getDeviceTrigger() {
            if (mDeviceFilterCriteria != null) {
                for (DeviceFilterCriteria dfc : mDeviceFilterCriteria) {
                    if (dfc.getTrigger()) {
                        return dfc;
                    }
                }
            }
            return null;
        }

        @JsonIgnore
        public boolean isAttributeInDeviceFilter(int attributeId) {
            if (mDeviceFilterCriteria != null) {
                for (DeviceFilterCriteria dfc : mDeviceFilterCriteria) {
                    ActionValue av = dfc.getAttribute();
                    if (av != null && av.getId() == attributeId) {
                        return true;
                    }
                }
            }
            return false;
        }

        @JsonProperty
        public void setSchedule(Schedule s) {
            mSchedule = s;
        }

        @JsonIgnore
        public Schedule getSchedule() {
            return mSchedule;
        }

        @JsonProperty
        public void setScheduleId(String s) {
            mScheduleId = s;
        }

        public String getScheduleId() {
            if (mScheduleId == null && mSchedule != null) {
                return mSchedule.getScheduleId();
            }
            return mScheduleId;
        }

        @JsonProperty
        public void setDeviceGroupId(String s) {
            mDeviceGroupId = s;
        }

        public String getDeviceGroupId() {
            return mDeviceGroupId;
        }

        @JsonProperty
        public void setAccountNotificationId(String s) {
            mAccountNotificationId = s;
        }

        public String getAccountNotificationId() {
            return mAccountNotificationId;
        }

        public boolean hasNotificationId() {
            return mAccountNotificationId != null && !mAccountNotificationId.isEmpty();
        }

        @JsonIgnore
        public DeviceRules.DeviceAction getActionForDevice(String deviceId) {
            if (mDeviceActions != null) {
                for (DeviceRules.DeviceAction action : mDeviceActions) {
                    if (deviceId.equals(action.getDeviceId())) {
                        return action;
                    }
                }
            }

            return null;
        }

        @JsonIgnore
        public DeviceRules.DeviceAction getActionWithAttributes() {
            if (mDeviceActions != null) {
                for (DeviceRules.DeviceAction action : mDeviceActions) {
                    if (action.hasAttributes()) {
                        return action;
                    }
                }
            }

            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Time {
        public String timeZone;
        public int hour;
        public int minute;
        public int seconds;

        public Time(Time t) {
            timeZone = t.timeZone;
            hour = t.hour;
            minute = t.minute;
            seconds = t.seconds;
        }

        public Time() {}
    }

    public static class CreateScheduleBody {

        public ArrayList<Schedule.DayOfWeek> dayOfWeek = new ArrayList<Schedule.DayOfWeek>();
        public ArrayList<ActionValue> attributes = new ArrayList<ActionValue>();
        public Time time = new Time();

        public CreateScheduleBody() {}
    }

    public DeviceRules() {}
}
