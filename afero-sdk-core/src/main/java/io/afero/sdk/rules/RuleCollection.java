/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.rules;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.client.afero.models.DeviceRules;
import io.afero.sdk.log.AfLog;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.PublishSubject;

public class RuleCollection {

    private final HashMap<String, DeviceRules.Rule> mRules = new HashMap<String, DeviceRules.Rule>();

    protected final AferoClient mAferoClient;

    private final PublishSubject<DeviceRules.Rule[]> mCreateSubject = PublishSubject.create();
    private final PublishSubject<DeviceRules.Rule[]> mResetSubject = PublishSubject.create();
    private final PublishSubject<DeviceRules.Rule[]> mUpdateSubject = PublishSubject.create();
    private final PublishSubject<DeviceRules.Rule[]> mDeleteSubject = PublishSubject.create();

    public RuleCollection(AferoClient aferoClient) {
        mAferoClient = aferoClient;
    }

    public Observable<DeviceRules.Rule[]> getCreateObservable() {
        return mCreateSubject;
    }

    public Observable<DeviceRules.Rule[]> getResetObservable() {
        return mResetSubject;
    }

    public Observable<DeviceRules.Rule[]> getUpdateObservable() {
        return mUpdateSubject;
    }

    public Observable<DeviceRules.Rule[]> getDeleteObservable() {
        return mDeleteSubject;
    }

    public boolean isEmpty() {
        return mRules.isEmpty();
    }

    public rx.Observable<DeviceRules.Rule[]> fetchRules() {
        return getRules().doOnNext(new FetchRulesOnNext());
    }

    public rx.Observable<DeviceRules.Rule> saveRule(DeviceRules.Rule rule) {
        DeviceRules.Rule saveRule = new DeviceRules.Rule(rule);
        Observable<DeviceRules.Rule> ruleObservable;
        Action1<DeviceRules.Rule> onNext;

        // remove empty actions (the service doesn't support them)
        Vector<DeviceRules.DeviceAction> actions = saveRule.getDeviceActions();
        if (actions != null) {
            Iterator<DeviceRules.DeviceAction> iter = actions.iterator();
            while (iter.hasNext()) {
                DeviceRules.DeviceAction action = iter.next();
                if (!action.hasAttributes()) {
                    iter.remove();
                }
            }

            if (actions.isEmpty()) {
                saveRule.setDeviceActions(null);
            }
        }

        // save the schedule, if any
        rx.Observable<DeviceRules.Schedule> scheduleObservable = null;
        DeviceRules.Schedule schedule = saveRule.getSchedule();
        if (schedule != null) {
            String scheduleId = schedule.getScheduleId();

            if (scheduleId != null) {
                scheduleObservable = mAferoClient.putSchedule(scheduleId, schedule);
            } else {
                scheduleObservable = mAferoClient.postSchedule(schedule);
            }
        }

        if (rule.isSaved()) {

            if (scheduleObservable != null) {
                ruleObservable = scheduleObservable
                        .zipWith(Observable.just(saveRule), new Func2<DeviceRules.Schedule, DeviceRules.Rule, DeviceRules.Rule>() {
                            @Override
                            public DeviceRules.Rule call(DeviceRules.Schedule newSchedule, DeviceRules.Rule r) {
                                AfLog.i("saveRule: newSchedule.getScheduleId()=" + newSchedule.getScheduleId());
                                return r;
                            }
                        })
                        .concatMap(new Func1<DeviceRules.Rule, Observable<DeviceRules.Rule>>() {
                            @Override
                            public Observable<DeviceRules.Rule> call(DeviceRules.Rule r) {
                                return putRule(r.getRuleId(), r);
                            }
                        });
            } else {
                ruleObservable = putRule(saveRule.getRuleId(), saveRule);
            }

            onNext = new UpdateRuleOnNext();

        } else {

            if (scheduleObservable != null) {
                ruleObservable = scheduleObservable
                        .zipWith(Observable.just(saveRule), new Func2<DeviceRules.Schedule, DeviceRules.Rule, DeviceRules.Rule>() {
                            @Override
                            public DeviceRules.Rule call(DeviceRules.Schedule newSchedule, DeviceRules.Rule r) {
                                String sId = newSchedule.getScheduleId();
                                r.setScheduleId(sId);
                                DeviceRules.Schedule s = r.getSchedule();
                                if (s != null) {
                                    s.setScheduleId(sId);
                                }
                                return r;
                            }
                        })
                        .concatMap(new Func1<DeviceRules.Rule, Observable<DeviceRules.Rule>>() {
                            @Override
                            public Observable<DeviceRules.Rule> call(DeviceRules.Rule r) {
                                return postRule(r);
                            }
                        });
            } else {
                ruleObservable = postRule(saveRule);
            }

//            ruleObservable = ruleObservable.zipWith(Observable.just(saveRule),
//                new Func2<DeviceRules.Rule, DeviceRules.Rule, DeviceRules.Rule>() {
//                    @Override
//                    public DeviceRules.Rule call(DeviceRules.Rule resultRule, DeviceRules.Rule r) {
//                        r.setRuleId(resultRule.getRuleId());
//                        return r;
//                    }
//                });

            onNext = new CreateRuleOnNext();
        }

        return ruleObservable
                .zipWith(Observable.just(rule), new Func2<DeviceRules.Rule, DeviceRules.Rule, DeviceRules.Rule>() {
                    @Override
                    public DeviceRules.Rule call(DeviceRules.Rule responseRule, DeviceRules.Rule originalRule) {
                        originalRule.setRuleId(responseRule.getRuleId());
                        originalRule.setScheduleId(responseRule.getScheduleId());
                        return originalRule;
                    }
                })
                .doOnNext(onNext);
    }

    public rx.Observable<Void> deleteRule(DeviceRules.Rule rule) {
        return deleteRule(rule.getRuleId())
                .zipWith(Observable.just(rule), new Func2<Void, DeviceRules.Rule, Void>() {
                    @Override
                    public Void call(Void v, DeviceRules.Rule rule) {
                        if (mRules.remove(rule.getRuleId()) != null) {
                            DeviceRules.Rule[] r = {rule};
                            mDeleteSubject.onNext(r);
                        }
                        return v;
                    }
                });
    }

    private class FetchRulesOnNext implements Action1<DeviceRules.Rule[]> {
        @Override
        public void call(DeviceRules.Rule[] rules) {

            mRules.clear();

            for (DeviceRules.Rule r : rules) {
                mRules.put(r.getRuleId(), r);
            }

            mResetSubject.onNext(rules);
        }
    }

    private class CreateRuleOnNext implements Action1<DeviceRules.Rule> {
        @Override
        public void call(DeviceRules.Rule rule) {
            mRules.put(rule.getRuleId(), rule);
            DeviceRules.Rule[] r = { rule };
            mCreateSubject.onNext(r);
        }
    }

    private class UpdateRuleOnNext implements Action1<DeviceRules.Rule> {
        @Override
        public void call(DeviceRules.Rule rule) {
            mRules.put(rule.getRuleId(), rule);
            DeviceRules.Rule[] r = { rule };
            mUpdateSubject.onNext(r);
        }
    }

    protected Observable<DeviceRules.Rule[]> getRules() {
        return mAferoClient.getAccountRules();
    }

    protected Observable<DeviceRules.Rule> postRule(DeviceRules.Rule rule) {
        return mAferoClient.postRule(rule);
    }

    protected Observable<DeviceRules.Rule> putRule(String ruleId, DeviceRules.Rule rule) {
        return mAferoClient.putRule(ruleId, rule);
    }

    protected Observable<Void> deleteRule(String ruleId) {
        return mAferoClient.deleteRule(ruleId);
    }
}
