/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.rules;

import io.afero.sdk.client.afero.models.DeviceRules;

public class RuleBuilder {
    private DeviceRules.Rule rule = new DeviceRules.Rule();
    private DeviceRules.Schedule schedule = new DeviceRules.Schedule();
    private DeviceRules.Schedule.DayOfWeek[] daysOfWeek = new DeviceRules.Schedule.DayOfWeek[7];

    public static RuleBuilder start() {
        return new RuleBuilder();
    }

    public RuleBuilder withDay(DeviceRules.Schedule.DayOfWeek day) {
        daysOfWeek[day.ordinal()] = day;
        return this;
    }

    public RuleBuilder withDays(DeviceRules.Schedule.DayOfWeek[] days) {
        for (DeviceRules.Schedule.DayOfWeek d : days) {
            withDay(d);
        }
        return this;
    }

    public RuleBuilder atHour(int hour) {
        getTime().hour = hour;
        return this;
    }

    public RuleBuilder atMinute(int minute) {
        getTime().minute = minute;
        return this;
    }

    public DeviceRules.Rule build() {

        int dayCount = 0;
        for (DeviceRules.Schedule.DayOfWeek d : daysOfWeek) {
            if (d != null) {
                ++dayCount;
            }
        }

        if (dayCount > 0) {
            int i = 0;
            DeviceRules.Schedule.DayOfWeek[] days = new DeviceRules.Schedule.DayOfWeek[dayCount];
            for (DeviceRules.Schedule.DayOfWeek d : daysOfWeek) {
                if (d != null) {
                    days[i] = d;
                    ++i;
                }
            }
            schedule.setDayOfWeek(days);
        }

        rule.setSchedule(schedule);

        return rule;
    }

    private DeviceRules.Time getTime() {
        DeviceRules.Time t = schedule.getTime();
        if (t == null) {
            t = new DeviceRules.Time();
            schedule.setTime(t);
        }
        return t;
    }
}
