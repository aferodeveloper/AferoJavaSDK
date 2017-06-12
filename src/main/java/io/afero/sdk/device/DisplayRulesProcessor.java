/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;

import java.math.BigInteger;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.afero.sdk.client.afero.models.AttributeValue;

public class DisplayRulesProcessor {

    private static final Pattern sSubExpressionPattern = Pattern.compile("^\\((.*)\\)(\\|\\||\\&\\&|\\^)\\((.*)\\)$");
    private static final Pattern sEqualPattern = Pattern.compile("^(0x\\d+|[-]?\\d+[.]?\\d*)$");
    private static final Pattern sClosedRangeCommaExpressionPattern = Pattern.compile("^(0x\\d+|[-]?\\d+[.]?\\d*),(0x\\d+|[-]?\\d+[.]?\\d*)$");
    private static final Pattern sClosedRangeExpressionPattern = Pattern.compile("^(0x\\d+|[-]?\\d+[.]?\\d*)\\.\\.\\.(0x\\d+|[-]?\\d+[.]?\\d*)$");
    private static final Pattern sLeftOpenRangeExpressionPattern = Pattern.compile("^(0x\\d+|[-]?\\d+[.]?\\d*)<\\.\\.(0x\\d+|[-]?\\d+[.]?\\d*)$");
    private static final Pattern sRightOpenRangeExpressionPattern = Pattern.compile("^(0x\\d+|[-]?\\d+[.]?\\d*)\\.\\.<(0x\\d+|[-]?\\d+[.]?\\d*)$");
    private static final Pattern sFullOpenRangeExpressionPattern = Pattern.compile("^(0x\\d+|[-]?\\d+[.]?\\d*)<\\.<(0x\\d+|[-]?\\d+[.]?\\d*)$");
    private static final Pattern sBitwiseAndExpressionPattern = Pattern.compile("^\\&(0x\\d+|[-]?\\d+[.]?\\d*)$");
    private static final Pattern sBitwiseXorExpressionPattern = Pattern.compile("^\\^(0x\\d+|[-]?\\d+[.]?\\d*)$");

    public static class RuleMatcher {

        static abstract class Operator {
            abstract boolean test(AttributeValue value);
        }

        static class True extends Operator {
            @Override
            boolean test(AttributeValue value) {
                return true;
            }
        }

        static class False extends Operator {
            @Override
            boolean test(AttributeValue value) {
                return false;
            }
        }

        static class Equals extends Operator {
            AttributeValue mValue;

            Equals(AttributeValue value) {
                mValue = value;
            }

            @Override
            boolean test(AttributeValue value) {
                return mValue.compareTo(value) == 0;
            }
        }

        static class InClosedRange extends Operator {
            AttributeValue mLowerBound;
            AttributeValue mUpperBound;

            InClosedRange(AttributeValue lowerBound, AttributeValue upperBound) {
                mLowerBound = lowerBound;
                mUpperBound = upperBound;
            }

            @Override
            boolean test(AttributeValue value) {
                return mLowerBound.compareTo(value) <= 0 && mUpperBound.compareTo(value) >= 0;
            }
        }

        static class InLeftOpenRange extends Operator {
            AttributeValue mLowerBound;
            AttributeValue mUpperBound;

            InLeftOpenRange(AttributeValue lowerBound, AttributeValue upperBound) {
                mLowerBound = lowerBound;
                mUpperBound = upperBound;
            }

            @Override
            boolean test(AttributeValue value) {
                return mLowerBound.compareTo(value) < 0 && mUpperBound.compareTo(value) >= 0;
            }
        }

        static class InRightOpenRange extends Operator {
            AttributeValue mLowerBound;
            AttributeValue mUpperBound;

            InRightOpenRange(AttributeValue lowerBound, AttributeValue upperBound) {
                mLowerBound = lowerBound;
                mUpperBound = upperBound;
            }

            @Override
            boolean test(AttributeValue value) {
                return mLowerBound.compareTo(value) <= 0 && mUpperBound.compareTo(value) > 0;
            }
        }

        static class InFullOpenRange extends Operator {
            AttributeValue mLowerBound;
            AttributeValue mUpperBound;

            InFullOpenRange(AttributeValue lowerBound, AttributeValue upperBound) {
                mLowerBound = lowerBound;
                mUpperBound = upperBound;
            }

            @Override
            boolean test(AttributeValue value) {
                return mLowerBound.compareTo(value) < 0 && mUpperBound.compareTo(value) > 0;
            }
        }

        static class BitwiseAnd extends Operator {
            AttributeValue mValue;

            BitwiseAnd(AttributeValue value) {
                mValue = value;
            }

            @Override
            boolean test(AttributeValue value) {
                return (mValue.numericValue().toBigIntegerExact().and(value.numericValue().toBigIntegerExact())).compareTo(BigInteger.ZERO) != 0;
            }
        }

        static class BitwiseXor extends Operator {
            AttributeValue mValue;

            BitwiseXor(AttributeValue value) {
                mValue = value;
            }

            @Override
            boolean test(AttributeValue value) {
                return (mValue.numericValue().toBigIntegerExact().xor(value.numericValue().toBigIntegerExact())).compareTo(BigInteger.ZERO) != 0;
            }
        }

        static class CombineOr extends Operator {
            RuleMatcher mLhs;
            RuleMatcher mRhs;

            CombineOr(RuleMatcher lhs, RuleMatcher rhs) {
                mLhs = lhs;
                mRhs = rhs;
            }

            @Override
            boolean test(AttributeValue value) {
                return mLhs.match(value) || mRhs.match(value);
            }
        }

        static class CombineAnd extends Operator {
            RuleMatcher mLhs;
            RuleMatcher mRhs;

            CombineAnd(RuleMatcher lhs, RuleMatcher rhs) {
                mLhs = lhs;
                mRhs = rhs;
            }

            @Override
            boolean test(AttributeValue value) {
                return mLhs.match(value) && mRhs.match(value);
            }
        }

        static class CombineXor extends Operator {
            RuleMatcher mLhs;
            RuleMatcher mRhs;

            CombineXor(RuleMatcher lhs, RuleMatcher rhs) {
                mLhs = lhs;
                mRhs = rhs;
            }

            @Override
            boolean test(AttributeValue value) {
                boolean l = mLhs.match(value);
                boolean r = mLhs.match(value);
                return (l || r) && !(l && r);
            }
        }

        Operator combine(String oper, String lhs, String rhs, AttributeValue.DataType dataType) {

            if (oper.equals("||")) {
                return new CombineOr(new RuleMatcher(lhs, dataType), new RuleMatcher(rhs, dataType));
            }
            else if (oper.equals("&&")) {
                return new CombineAnd(new RuleMatcher(lhs, dataType), new RuleMatcher(rhs, dataType));
            }
            else if (oper.equals("^")) {
                return new CombineXor(new RuleMatcher(lhs, dataType), new RuleMatcher(rhs, dataType));
            }

            return new False();
        }

        RuleMatcher(String expression, AttributeValue.DataType dataType) {

            mDataType = dataType;

            mNegate = expression.startsWith("!");
            if (mNegate) {
                expression = expression.substring(1);
            }

            Matcher matcher = sSubExpressionPattern.matcher(expression);

            if (expression.equals("*")) {
                mOperator = new True();
            }
            else if (matcher.matches()) { // subexpressions
                if (matcher.groupCount() == 3) {
                    String lhs = matcher.group(1);
                    String oper = matcher.group(2);
                    String rhs = matcher.group(3);
                    mOperator = combine(oper, lhs, rhs, dataType);
                }
            }
            else if (matcher.usePattern(sEqualPattern).matches()) {
                if (matcher.groupCount() == 1) {
                    String val = matcher.group(1);
                    mOperator = new Equals(makeValue(val));
                }
            }
            else if (matcher.usePattern(sClosedRangeCommaExpressionPattern).matches() ||
                    matcher.usePattern(sClosedRangeExpressionPattern).matches()) {
                if (matcher.groupCount() == 2) {
                    String lo = matcher.group(1);
                    String hi = matcher.group(2);
                    mOperator = new InClosedRange(makeValue(lo), makeValue(hi));
                }
            }
            else if (matcher.usePattern(sLeftOpenRangeExpressionPattern).matches()) {
                if (matcher.groupCount() == 2) {
                    String lo = matcher.group(1);
                    String hi = matcher.group(2);
                    mOperator = new InLeftOpenRange(makeValue(lo), makeValue(hi));
                }
            }
            else if (matcher.usePattern(sRightOpenRangeExpressionPattern).matches()) {
                if (matcher.groupCount() == 2) {
                    String lo = matcher.group(1);
                    String hi = matcher.group(2);
                    mOperator = new InRightOpenRange(makeValue(lo), makeValue(hi));
                }
            }
            else if (matcher.usePattern(sFullOpenRangeExpressionPattern).matches()) {
                if (matcher.groupCount() == 2) {
                    String lo = matcher.group(1);
                    String hi = matcher.group(2);
                    mOperator = new InFullOpenRange(makeValue(lo), makeValue(hi));
                }
            }
            else if (matcher.usePattern(sBitwiseAndExpressionPattern).matches()) {
                if (matcher.groupCount() == 1) {
                    String oper = matcher.group(1);
                    mOperator = new BitwiseAnd(makeValue(oper));
                }
            }
            else if (matcher.usePattern(sBitwiseXorExpressionPattern).matches()) {
                if (matcher.groupCount() == 1) {
                    String oper = matcher.group(1);
                    mOperator = new BitwiseXor(makeValue(oper));
                }
            }
            else {
                mOperator = new False();
            }
        }

        boolean mNegate;
        Operator mOperator;
        AttributeValue.DataType mDataType;

        private AttributeValue makeValue(String s) {
            return new AttributeValue(s, mDataType);
        }

        public boolean match(AttributeValue value) {
            return value != null ? mNegate != mOperator.test(value) : false;
        }
    }

    public interface Value {
        AttributeValue get(ControlModel model);
    }

    public static class Rule {

        private final Value mValue;
        private final ApplyParams mApply;
        private final String mMatchString;
        private RuleMatcher mMatcher;

        public Rule(Value value, String match, ApplyParams apply) {
            mValue = value;
            mMatchString = match;
            mApply = apply;
        }

        void matchAndApply(ApplyParams result, ControlModel model) {

            if (mMatcher == null) {
                AttributeValue av = mValue.get(model);
                if (av == null) {
                    return;
                }
                mMatcher = new RuleMatcher(mMatchString, av.getDataType());
            }

            if (mMatcher.match(mValue.get(model))) {
                for (ApplyParams.Entry<String,Object> applyEntry: mApply.entrySet()) {
                    Object applyValue = applyEntry.getValue();
                    if (applyValue instanceof Map) {
                        Object resultValue = result.get(applyEntry.getKey());
                        if (resultValue != null && resultValue instanceof Map) {
                            // merge the two map values
                            ((Map)resultValue).putAll((Map)applyValue);
                        } else {
                            try {
                                Map<String, Object> map = (Map<String, Object>)applyValue.getClass().newInstance();
                                map.putAll((Map<String, Object>)applyValue);
                                result.put(applyEntry.getKey(), map);
                            } catch (Exception e) {
                                // ignore
                            }
                        }
                    } else {
                        result.put(applyEntry.getKey(), applyValue);
                    }
                }
            }
        }
    }

    private Rule[] mRules;

    public DisplayRulesProcessor(Rule[] rules) {
        mRules = rules;
    }

    public void process(ApplyParams result, ControlModel model) {
        for (Rule rule : mRules) {
            rule.matchAndApply(result, model);
        }
    }
}
