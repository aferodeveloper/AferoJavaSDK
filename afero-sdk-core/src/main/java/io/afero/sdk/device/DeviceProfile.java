/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.device;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.log.AfLog;

@JsonIgnoreProperties(ignoreUnknown=true)
public class DeviceProfile {

    public static final int SCHEDULE_FLAGS_ATTRIBUTE_ID = 59001;
    public static final int SCHEDULE_ATTRIBUTE_ID = 59002;
    public static final int SCHEDULE_FLAGS_ATTRIBUTE_ID_END = 59999;

    private String mId;

    private Gauge mGauge;

    private String mDeviceType;
    private String mDeviceTypeId;

    private Presentation mPresentation;

    private Service[] mServices;

    private final HashMap<Integer,Attribute> mAttributeMap = new HashMap<>();

    private HashMap<String, Presentation> mPresentationOverrides;

    private boolean mHasWritableAttributes;
    private boolean mIsWifiSetupCapable;
    private int mScheduleAttributeCount;

    public static final String SEMANTIC_TYPE_POWER = "power";

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class Service {

        private int mId;

        private Attribute[] mAttributes;

        public Service() {
        }

        @JsonProperty
        public void setId(int id) {
            mId = id;
        }

        public int getId() {
            return mId;
        }

        @JsonProperty
        public void setAttributes(Attribute[] attributes) {
            mAttributes = attributes;
        }

        public Attribute[] getAttributes() {
            return mAttributes;
        }

        public int getAttributeCount() {
            return mAttributes != null ? mAttributes.length : 0;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class Attribute {

        private int mId;
        private String mSemanticType;
        private AttributeValue.DataType mDataType = AttributeValue.DataType.UNKNOWN;
        private String[] mOperations;
        private int mLength;
        private String mDefaultValue;

        private Description mDescription;

        private boolean mWritable;

        public Attribute() {
        }

        @JsonProperty
        public void setId(int id) {
            mId = id;
        }

        public int getId() {
            return mId;
        }

        @JsonProperty
        public void setSemanticType(String type) {
            mSemanticType = type;
        }

        public String getSemanticType() {
            return mSemanticType;
        }

        @JsonProperty
        public void setDataType(String dataTypeString) {
            try {
                mDataType = AttributeValue.DataType.valueOf(dataTypeString.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e){
                AfLog.e(e);
            }
        }

        @JsonProperty
        public AttributeValue.DataType getDataType() {
            return mDataType;
        }

        @JsonProperty
        public void setDescription(Description d) {
            mDescription = d;
        }

        @JsonIgnore
        public String getLabel() {
            return mDescription != null ? mDescription.label.base : "";
        }

        @JsonProperty
        public void setOperations(String[] ops) {
            mOperations = ops;
            for (String o : ops) {
                if (o.equalsIgnoreCase("write")) {
                    mWritable = true;
                }
            }
        }

        public String[] getOperations() {
            return mOperations;
        }

        @JsonIgnore
        public boolean isWritable() {
            return mWritable;
        }

        public boolean isNumericType() {
            return AttributeValue.isNumericType(mDataType);
        }

        public boolean isNumericDecimalType() {
            return AttributeValue.isNumericDecimalType(mDataType);
        }

        @JsonProperty
        public int getLength() {
            return mLength;
        }

        public void setLength(int len) {
            mLength = len;
        }

        @JsonProperty
        public void setValue(String value) {
            mDefaultValue = value;
        }

        public AttributeValue getDefaultValue() {
            return mDefaultValue != null ? new AttributeValue(mDefaultValue, getDataType()) : null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class Label {
        public String base;

        Label() {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class Description {
        public Label label;

        Description() {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class Presentation {

        private String mLabel;
        private Gauge mGauge;
        private Group mGroups[];
        private Control mControls[];
        private HashMap<Integer, Control> mControlMap = new HashMap<>();
        private HashMap<Integer, AttributeOptions> mAttributeOptions;
        private DisplayRule[] mDisplayRules;

        public Presentation() {
        }

        @JsonProperty
        public void setLabel(String label) {
            mLabel = label;
        }

        public String getLabel() {
            return mLabel;
        }

        @JsonProperty
        public void setAttributeOptions(HashMap<Integer,AttributeOptions> ao) {
            mAttributeOptions = ao;
        }

        public HashMap<Integer, AttributeOptions> getAttributeOptions() {
            return mAttributeOptions;
        }

        @JsonIgnore
        public AttributeOptions getAttributeOptionsById(int id) {
            return mAttributeOptions.get(id);
        }

        @JsonIgnore
        public AttributeOptions getPrimaryAttributeOptions() {
            for (AttributeOptions ao: mAttributeOptions.values()) {
                if (ao.isPrimaryOperation()) {
                    return ao;
                }
            }
            return null;
        }

        @JsonIgnore
        public int getPrimaryAttributeId() {
            for (Map.Entry<Integer,AttributeOptions> entry: mAttributeOptions.entrySet()) {
                if (entry.getValue().isPrimaryOperation()) {
                    return entry.getKey();
                }
            }

            return 0;
        }

        @JsonProperty
        public void setDisplayRules(DisplayRule[] rules) {
            mDisplayRules = rules;
        }

        public DisplayRule[] getDisplayRules() {
            return mDisplayRules;
        }

        @JsonProperty
        public void setGauge(Gauge gauge) {
            mGauge = gauge;
        }

        public Gauge getGauge() {
            return mGauge;
        }

        @JsonProperty
        public void setGroups(Group[] groups) {
            mGroups = groups;
        }

        public Group[] getGroups() {
            return mGroups;
        }

        @JsonIgnore
        public Group getGroup(int i) {
            return mGroups != null && mGroups.length > i ? mGroups[i] : null;
        }

        @JsonIgnore
        public int getGroupCount() {
            return mGroups != null ? mGroups.length : 0;
        }

        @JsonProperty
        public void setControls(Control[] controls) {
            mControls = controls;
            for (Control c: controls) {
                mControlMap.put(c.getId(), c);
            }
        }

        public Control[] getControls() {
            return mControls;
        }

        @JsonIgnore
        public Control getControlById(int id) {
            return mControlMap.get(id);
        }

        @JsonIgnore
        public Control getControl(int i) {
            return mControls != null && mControls.length > i ? mControls[i] : null;
        }

        @JsonIgnore
        public int getControlCount() {
            return mControls != null ? mControls.length : 0;
        }

        @JsonIgnore
        public Group findGroupThatReferencesControl(Control control) {
            final int controlId = control.getId();

            if (mGroups != null && mControls != null) {
                for (Group g : mGroups) {
                    int[] controlIds = g.getControls();
                    if (controlIds != null) {
                        for (int id : controlIds) {
                            Control ctrl = mControlMap.get(id);
                            if (ctrl != null && id == controlId) {
                                return g;
                            }
                        }
                    }
                }
            }

            return null;
        }

        @JsonIgnore
        public boolean hasGroupsWithWritableControls(DeviceProfile deviceProfile) {
            if (mGroups != null && mControls != null) {
                for (Group g : mGroups) {
                    if (g.hasWritableControls(this, deviceProfile)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public Control findControlThatReferencesAttribute(Attribute attribute, String controlType) {
            final int attrId = attribute.getId();

            for (Control control : mControls) {
                if ((controlType == null || controlType.equalsIgnoreCase(controlType))
                    && control.refersToAttributeId(attrId)) {
                    return control;
                }
            }

            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class AttributeOptions {

        private static final String FLAG_PRIMARY_OPERATION = "primaryOperation";
        private static final String FLAG_LOCAL_SCHEDULABLE = "localSchedulable";

        private String mLabel;
        private boolean mIsPrimaryOperation;
        private boolean mLocalSchedulable;
        private RangeOptions mRangeOptions;
        private DisplayRule[] mValueOptions;

        public AttributeOptions() {
        }

        @JsonProperty
        public void setLabel(String label) {
            mLabel = label;
        }

        public String getLabel() {
            return mLabel;
        }

        @JsonProperty
        public void setFlags(String[] flags) {
            for (int i = 0, n = flags.length; i < n; ++i) {
                String ff = flags[i];
                if (ff.equalsIgnoreCase(FLAG_PRIMARY_OPERATION)) {
                    mIsPrimaryOperation = true;
                }
                if (ff.equalsIgnoreCase(FLAG_LOCAL_SCHEDULABLE)) {
                    mLocalSchedulable = true;
                }
            }
        }

        @JsonIgnore
        public boolean isPrimaryOperation() {
            return mIsPrimaryOperation;
        }

        @JsonIgnore
        public boolean isLocalSchedulable() {
            return mLocalSchedulable;
        }

        @JsonProperty
        public void setRangeOptions(RangeOptions rangeOptions) {
            mRangeOptions = rangeOptions;
        }

        public RangeOptions getRangeOptions() {
            return mRangeOptions;
        }

        @JsonProperty
        public void setValueOptions(DisplayRule[] items) {
            mValueOptions = items;
        }

        public DisplayRule[] getValueOptions() {
            return mValueOptions;
        }

        @JsonIgnore
        public String findValueOptionsLabel(AttributeValue value, String defaultLabel) {
            if (mValueOptions != null) {
                final AttributeValue.DataType dataType = value.getDataType();
                for (DeviceProfile.DisplayRule vo : mValueOptions) {
                    AttributeValue av = new AttributeValue(vo.match, dataType);
                    if (value.compareTo(av) == 0) {
                        return vo.getApplyLabel();
                    }
                }
            }

            return defaultLabel;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class Group {

        private Group mGroups[];
        private int mControlIds[];
        private Gauge mGauge;
        private String mLabel;

        public Group() {
        }

        @JsonProperty
        public void setLabel(String label) {
            mLabel = label;
        }

        public String getLabel() {
            return mLabel;
        }

        @JsonProperty
        public void setGauge(Gauge gauge) {
            mGauge = gauge;
        }

        public Gauge getGauge() {
            return mGauge;
        }

        @JsonProperty
        public void setGroups(Group[] groups) {
            mGroups = groups;
        }

        @JsonIgnore
        public Group getGroup(int i) {
            return mGroups != null && mGroups.length > i ? mGroups[i] : null;
        }

        @JsonIgnore
        public int getGroupCount() {
            return mGroups != null ? mGroups.length : 0;
        }

        @JsonProperty
        public void setControls(int[] controls) {
            mControlIds = controls;
        }

        public int[] getControls() {
            return mControlIds;
        }

        @JsonIgnore
        public int getControlId(int i) {
            return mControlIds != null && mControlIds.length > i ? mControlIds[i] : 0;
        }

        @JsonIgnore
        public int getControlIdCount() {
            return mControlIds != null ? mControlIds.length : 0;
        }

        public boolean hasWritableControls(Presentation presentation, DeviceProfile profile) {
            if (mControlIds != null) {
                for (int ctrlId : mControlIds) {
                    Control ctrl = presentation.getControlById(ctrlId);
                    if (ctrl != null && ctrl.refersToWritableAttribute(profile)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    private static class CurrentAttributeValue implements DisplayRulesProcessor.Value<DeviceModel> {
        Attribute mAttribute;

        CurrentAttributeValue(Attribute attribute) {
            mAttribute = attribute;
        }

        @Override
        public AttributeValue get(DeviceModel model) {
            return model.getAttributeCurrentValue(mAttribute);
        }
    }

    private static class PendingAttributeValue implements DisplayRulesProcessor.Value<DeviceModel> {
        Attribute mAttribute;

        PendingAttributeValue(Attribute attribute) {
            mAttribute = attribute;
        }

        @Override
        public AttributeValue get(DeviceModel model) {
            return model.getAttributePendingValue(mAttribute);
        }
    }

    private static abstract class RuleMaker {
        public abstract DisplayRulesProcessor.Rule makeRule(Attribute attribute, String match, ApplyParams apply);
    }

    private static class DevicePendingValueRuleMaker extends RuleMaker {
        @Override
        public DisplayRulesProcessor.Rule makeRule(Attribute attribute, String match, ApplyParams apply) {
            PendingAttributeValue value = new PendingAttributeValue(attribute);
            return new DisplayRulesProcessor.Rule<>(value, match, apply);
        }
    }

    public static class DeviceCurrentValueRuleMaker extends RuleMaker {
        @Override
        public DisplayRulesProcessor.Rule makeRule(Attribute attribute, String match, ApplyParams apply) {
            CurrentAttributeValue value = new CurrentAttributeValue(attribute);
            return new DisplayRulesProcessor.Rule<>(value, match, apply);
        }
    }

    public static DisplayRulesProcessor.Rule[] createDisplayRules(DisplayRule[] displayRules, DeviceProfile profile, String deviceId, Attribute defaultAttribute, RuleMaker ruleMaker) {
        ArrayList<DisplayRulesProcessor.Rule> rules = new ArrayList<>(displayRules.length);
        if (defaultAttribute == null) {
            defaultAttribute = profile.getPrimaryOperationAttribute(deviceId);
        }

        for (DisplayRule rule : displayRules) {
            Attribute attribute = (rule.attributeId != 0) ? profile.getAttributeById(rule.attributeId) : defaultAttribute;

            ApplyParams apply = ApplyParams.create(rule.apply);

            DisplayRulesProcessor.Rule newRule = ruleMaker.makeRule(attribute, rule.match, apply);
            if (newRule != null) {
                rules.add(newRule);
            }
        }

        return rules.toArray(new DisplayRulesProcessor.Rule[rules.size()]);
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class Gauge {
        private String mType;
        private Icon mBackgroundIcon;
        private Icon mForegroundIcon;
        private DisplayRulesProcessor mDisplayRulesProcessor;
        private DisplayRule[] mDisplayRules;
        private String mLabel;
        private String mLabelSize;
        private String mLabelColor = "#000";

        public Gauge() {
        }

        @JsonProperty
        public void setType(String type) {
            mType = type;
        }

        public String getType() {
            return mType;
        }

        @JsonProperty("background")
        public void setBackgroundIcon(Icon icon) {
            mBackgroundIcon = icon;
        }

        public Icon getBackgroundIcon() {
            return mBackgroundIcon;
        }

        @JsonProperty("foreground")
        public void setForegroundIcon(Icon icon) {
            mForegroundIcon = icon;
        }

        public Icon getForegroundIcon() {
            return mForegroundIcon;
        }

        @JsonProperty
        public void setLabel(String label) {
            mLabel = label;
        }

        public String getLabel() {
            return mLabel;
        }

        @JsonProperty
        public void setLabelSize(String size) {
            mLabelSize = size;
        }

        public String getLabelSize() {
            return mLabelSize;
        }

        @JsonProperty
        public void setLabelColor(String color) {
            mLabelColor = color;
        }

        public String getLabelColor() {
            return mLabelColor;
        }

        @JsonProperty
        public void setDisplayRules(DisplayRule[] rules) {
            mDisplayRules = rules;
        }

        public DisplayRule[] getDisplayRules() {
            return mDisplayRules;
        }

        @JsonIgnore
        public DisplayRulesProcessor getDisplayRulesProcessor(DeviceProfile deviceProfile, String deviceId) {
            if (mDisplayRulesProcessor == null && mDisplayRules != null) {
                mDisplayRulesProcessor = new DisplayRulesProcessor(createDisplayRules(mDisplayRules, deviceProfile, deviceId, null, new DeviceCurrentValueRuleMaker()));
            }

            return mDisplayRulesProcessor;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class DisplayRule {

        public int attributeId;
        public String match;
        public HashMap<String,Object> apply;

        public DisplayRule() {

        }

        public String getApplyLabel() {
            if (apply != null) {
                Object o = apply.get("label");
                if (o != null && o instanceof String) {
                    return (String)o;
                }
            }
            return null;
        }

        public String getApplyImageName() {
            if (apply != null) {
                Object o = apply.get("imageName");
                if (o != null && o instanceof String) {
                    return (String)o;
                }
            }
            return null;
        }

        public void setApplyLabel(String label) {
            if (apply == null) {
                apply = new HashMap<>(1);
            }
            apply.put("label", label);
        }

        @JsonProperty("attrId")
        public void setAttrId(int id) {
            attributeId = id;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class Icon {
        private Image[] mImages;

        public Icon() {
        }

        @JsonProperty
        public void setImages(Image[] images) {
            mImages = images;
        }

        public Image[] getImages() {
            return mImages;
        }

        @JsonIgnore
        public String getURI(int i) {
            return mImages != null && mImages.length > i ? mImages[i].uri : null;
        }

        @JsonIgnore
        public String getCardURI(int i) {
            return mImages != null && mImages.length > i ? mImages[i].cardURI : null;
        }

        @JsonIgnore
        public int getURICount() {
            return mImages != null ? mImages.length : 0;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class Image {

        public String uri;
        public String cardURI;

        public Image() {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class RangeOptions {

        private BigDecimal mMin;
        private BigDecimal mMax;
        private BigDecimal mStep;
        private BigDecimal mCount;

        public String unitLabel;

        public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;


        public RangeOptions() {
            mMin = BigDecimal.ZERO;
            mMax = BigDecimal.ONE;
            mStep = BigDecimal.ONE;
            unitLabel = "";

            updateCount();
        }

        @JsonIgnore
        public void setMin(Number value) {
            mMin = new BigDecimal(value.toString());
            updateCount();
        }

        @JsonProperty
        public void setMinValue(String value) {
            mMin = new BigDecimal(value);
            updateCount();
        }

        @JsonProperty
        public void setMin(BigDecimal value) {
            mMin = value;
            updateCount();
        }

        public BigDecimal getMin() {
            return mMin;
        }

        @JsonIgnore
        public void setMax(Number value) {
            mMax = new BigDecimal(value.toString());
            updateCount();
        }

        @JsonProperty
        public void setMaxValue(String value) {
            mMax = new BigDecimal(value);
            updateCount();
        }

        @JsonProperty
        public void setMax(BigDecimal value) {
            mMax = value;
            updateCount();
        }

        public BigDecimal getMax() {
            return mMax;
        }

        @JsonIgnore
        public void setStep(Number value) {
            mStep = new BigDecimal(value.toString());
            updateCount();
        }

        @JsonProperty
        public void setStepValue(String value) {
            mStep = new BigDecimal(value);
            updateCount();
        }

        @JsonProperty
        public void setStep(BigDecimal value) {
            mStep = value;
            updateCount();
        }

        public BigDecimal getStep() {
            return mStep;
        }

        private void updateCount() {
            BigDecimal range = mMax.subtract(mMin);
            mCount = range.divide(mStep, ROUNDING_MODE).abs().add(BigDecimal.ONE);
        }

        public BigDecimal getCount() {
            return mCount;
        }

        public BigDecimal getValueByIndex(long index) {
            return mMin.add(mStep.multiply(new BigDecimal(index)));
        }

        public long getIndexByValue(BigDecimal value) {
            return getIndexByProportion(getProportionByValue(value));
        }

        public long getIndexByProportion(double proportion) {
            return getBigIndexByProportion(proportion).longValue();
        }

        public BigDecimal getValueByProportion(double proportion) {
            BigDecimal index = getBigIndexByProportion(proportion);
            return mMin.add(mStep.multiply(index));
        }

        public double getProportionByValue(BigDecimal value) {
            if (value.compareTo(mMin) < 0) {
                value = mMin;
            }
            else if (value.compareTo(mMax) > 0) {
                value = mMax;
            }

            BigDecimal prop = value.subtract(mMin).divide(mMax.subtract(mMin), 6, ROUNDING_MODE);

            return prop.doubleValue();
        }

        private BigDecimal getBigIndexByProportion(double proportion) {
            if (mCount.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
            BigDecimal index = mCount.subtract(BigDecimal.ONE);
            return index.multiply(new BigDecimal(proportion)).setScale(0, ROUNDING_MODE);
        }

    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class Control {

        private int mId;
        private String mType;
        private String mLabel;
        private DisplayRulesProcessor mDisplayRulesProcessor;
        private DisplayRule[] mDisplayRules;
        private TreeMap<String,Integer> mAttributeMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        public Control() {
        }

        @JsonProperty
        public void setId(int id) {
            mId = id;
        }

        public int getId() {
            return mId;
        }

        @JsonProperty("controlType")
        public void setType(String type) {
            mType = type;
        }

        public String getType() {
            return mType;
        }

        @JsonProperty
        public void setLabel(String label) {
            mLabel = label;
        }

        public String getLabel() {
            return mLabel;
        }

        @JsonProperty
        public void setAttributeMap(Map<String,Integer> map) {
            mAttributeMap.putAll(map);
        }

        public Map<String,Integer> getAttributeMap() {
            return mAttributeMap;
        }

        @JsonProperty
        public void setDisplayRules(DisplayRule[] rules) {
            mDisplayRules = rules;
        }

        public DeviceProfile.DisplayRule[] getDisplayRules() {
            return mDisplayRules;
        }

        @JsonIgnore
        public DisplayRulesProcessor getDisplayRulesProcessor(DeviceProfile deviceProfile, String deviceId, Attribute defaultAttribute) {
            if (mDisplayRulesProcessor == null && mDisplayRules != null) {
                mDisplayRulesProcessor = new DisplayRulesProcessor(createDisplayRules(mDisplayRules, deviceProfile, deviceId, defaultAttribute, new DevicePendingValueRuleMaker()));
            }
            return mDisplayRulesProcessor;
        }

        @JsonIgnore
        public int getAttributeIdByKey(String key) {
            Integer i = mAttributeMap.get(key);
            return i != null ? i : 0;
        }

        public boolean refersToAttributeId(int attrId) {
            for (int id : mAttributeMap.values()) {
                if (id == attrId) {
                    return true;
                }
            }
            return false;
        }

        public boolean refersToWritableAttribute(DeviceProfile deviceProfile) {
            for (int id : mAttributeMap.values()) {
                Attribute attribute = deviceProfile.getAttributeById(id);
                if (attribute != null && attribute.isWritable()) {
                    return true;
                }
            }
            return false;
        }

        public boolean refersToSchedulableAttribute(DeviceProfile deviceProfile, String deviceId) {
            for (int id : mAttributeMap.values()) {
                AttributeOptions ao = deviceProfile.getPresentation(deviceId).getAttributeOptionsById(id);
                if (ao != null && ao.isLocalSchedulable()) {
                    return true;
                }
            }
            return false;
        }
    }

    public DeviceProfile() {
    }

    @JsonIgnore
    public Attribute getAttributeById(int id) {
        return mAttributeMap.get(id);
    }

    @JsonIgnore
    public Attribute findAttributeWithSemanticType(String semanticType) {
        semanticType = semanticType.toLowerCase(Locale.ROOT);

        for (DeviceProfile.Service service : getServices()) {
            for (Attribute attribute : service.getAttributes()) {
                if (attribute.getSemanticType().equalsIgnoreCase(semanticType)) {
                    return attribute;
                }
            }
        }

        return null;
    }

    public void setDeviceType(String type) {
        mDeviceType = type;
    }

    public String getDeviceType() {
        return mDeviceType;
    }

    public void setDeviceTypeId(String typeId) {
        mDeviceTypeId = typeId;
    }

    public String getDeviceTypeId() {
        return mDeviceTypeId;
    }

    @JsonProperty("uiGauge")
    public void setGauge(Gauge gauge) {
        mGauge = gauge;
    }

    @JsonIgnore
    public Gauge getUIGauge() {
        return mGauge;
    }

    @JsonProperty
    public void setPresentation(Presentation presentation) {
        mPresentation = presentation;
    }

    @JsonIgnore
    public Presentation getPresentation(String deviceId) {
        if (deviceId != null && mPresentationOverrides != null) {
            Presentation p = mPresentationOverrides.get(deviceId);
            if (p != null) {
                return p;
            }
        }

        return mPresentation;
    }

    @JsonProperty
    public void setServices(Service[] services) {
        mServices = services;
        mScheduleAttributeCount = 0;

        for (Service s : services) {
            for (Attribute attribute : s.getAttributes()) {
                if (attribute.isWritable()) {
                    mHasWritableAttributes = true;
                }

                final int attrId = attribute.getId();
                if (attrId >= DeviceProfile.SCHEDULE_FLAGS_ATTRIBUTE_ID && attrId <= DeviceProfile.SCHEDULE_FLAGS_ATTRIBUTE_ID_END) {
                    ++mScheduleAttributeCount;
                }

                mAttributeMap.put(attribute.getId(), attribute);
            }
        }
    }

    @JsonIgnore
    public boolean hasWritableAttributes() {
        return mHasWritableAttributes;
    }

    @JsonIgnore
    public int getScheduleAttributeCount() {
        return mScheduleAttributeCount;
    }

    public Service[] getServices() {
        return mServices;
    }

    public int getServiceCount() {
        return mServices != null ? mServices.length : 0;
    }

    @JsonProperty("profileId")
    public void setId(String id) {
        mId = id;
    }

    public String getId() {
        return mId;
    }

    @JsonIgnore
    public Attribute getPrimaryOperationAttribute(String deviceId) {
        Presentation presentation = getPresentation(deviceId);
        if (presentation != null) {
            int id = presentation.getPrimaryAttributeId();
            if (id != 0) {
                return getAttributeById(id);
            }
        }

        return null;
    }

    @JsonProperty
    public void setPresentationOverrideMap(HashMap<String,Presentation> po) {
        mPresentationOverrides = po;
    }

    public HashMap<String, Presentation> getPresentationOverrideMap() {
        return mPresentationOverrides;
    }
}
