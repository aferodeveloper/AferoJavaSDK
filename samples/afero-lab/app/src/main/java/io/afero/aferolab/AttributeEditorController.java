package io.afero.aferolab;


import java.math.BigDecimal;

import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.device.AttributeWriter;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceProfile;
import io.afero.sdk.utils.RxUtils;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

class AttributeEditorController {

    private final AttributeEditorView mView;
    private DeviceModel mDeviceModel;
    private DeviceProfile.Attribute mAttribute;
    private Subscription mDeviceUpdateSubscription;
    private DeviceProfile.RangeOptions mRange;
    private DeviceProfile.DisplayRule[] mValueOptions;

    AttributeEditorController(AttributeEditorView view) {
        mView = view;
    }

    void start(DeviceModel deviceModel, DeviceProfile.Attribute attribute) {
        mDeviceModel = deviceModel;
        mAttribute = attribute;

        mView.setAttributeIdText(mAttribute.getId());
        mView.setAttributeLabelText(mAttribute.getSemanticType());
        mView.setAttributeDataTypeText(mAttribute.getDataType().toString());
        mView.setAttributeValueEditorType(getAttributeValueEditorType());

        DeviceProfile.Presentation presentation = mDeviceModel.getPresentation();
        DeviceProfile.AttributeOptions options = presentation != null
                ? presentation.getAttributeOptionsById(mAttribute.getId()) : null;

        mValueOptions = options != null ? options.getValueOptions() : null;
        mRange = options != null ? options.getRangeOptions() : null;
        if (mRange == null) {
            mRange = mValueOptions != null ? makeRangeFromValueOptions() : makeRangeFromDataType();
        }

        mDeviceUpdateSubscription = deviceModel.getUpdateObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<DeviceModel>() {
                    @Override
                    public void call(DeviceModel deviceModel) {
                        onDeviceUpdate();
                    }
                });

        onDeviceUpdate();
    }

    void stop() {
        mDeviceUpdateSubscription = RxUtils.safeUnSubscribe(mDeviceUpdateSubscription);
    }

    boolean isStarted() {
        return mDeviceUpdateSubscription != null;
    }

    void onAttributeValueTextEditorChanged(String text) {
        AttributeValue value = new AttributeValue(mAttribute.getDataType());
        value.setValue(text);

        updateDeviceModel(value);
    }

    void onAttributeValueNumberEditorChanging(double sliderProportion) {
        BigDecimal numValue = getNumericValueFromProportion(sliderProportion);
        mView.setAttributeValueText(numValue.toString());
    }

    void onAttributeValueNumberEditorChangeComplete(double sliderProportion) {
        BigDecimal numValue = getNumericValueFromProportion(sliderProportion);
        AttributeValue value = new AttributeValue(mAttribute.getDataType());
        value.setValue(numValue);

        updateDeviceModel(value);
    }

    void onAttributeValueBooleanEditorChanged(boolean b) {
        AttributeValue value = new AttributeValue(mAttribute.getDataType());
        value.setValue(b);

        updateDeviceModel(value);
    }

    private void updateDeviceModel(AttributeValue value) {
        mView.setAttributeValueText(value.toString());

        mDeviceModel.writeAttributes()
                .put(mAttribute.getId(), value)
                .commit()
                .subscribe(new RxUtils.IgnoreResponseObserver<AttributeWriter.Result>());
    }

    private void onDeviceUpdate() {
        AttributeValue value = mDeviceModel.getAttributeCurrentValue(mAttribute);
        mView.setAttributeValueText(value != null ? value.toString() : null);

        if (mAttribute.isNumericType()) {
            BigDecimal numericValue = value != null ? value.numericValue() : BigDecimal.ZERO;
            mView.setAttributeValueSliderProportion(mRange.getProportionByValue(numericValue));
        }
    }

    private AttributeEditorView.ValueEditorType getAttributeValueEditorType() {
        AttributeEditorView.ValueEditorType editorType = AttributeEditorView.ValueEditorType.NONE;

        if (mAttribute.isWritable()) {
            if (mAttribute.isNumericType()) {
                if (mAttribute.isNumericDecimalType()) {
                    editorType = AttributeEditorView.ValueEditorType.NUMBER_DECIMAL;
                } else {
                    editorType = AttributeEditorView.ValueEditorType.NUMBER;
                }
            }
            else if (mAttribute.getDataType() == AttributeValue.DataType.BYTES) {
                editorType = AttributeEditorView.ValueEditorType.BYTES;
            }
            else if (mAttribute.getDataType() == AttributeValue.DataType.BOOLEAN) {
                editorType = AttributeEditorView.ValueEditorType.BOOLEAN;
            } else {
                editorType = AttributeEditorView.ValueEditorType.TEXT;
            }
        }

        return editorType;
    }

    private DeviceProfile.RangeOptions makeRangeFromValueOptions() {
        DeviceProfile.RangeOptions range = new DeviceProfile.RangeOptions();
        range.setMin(BigDecimal.ZERO);
        range.setMax(BigDecimal.valueOf(mValueOptions.length - 1));
        range.setStep(BigDecimal.ONE);
        return range;
    }

    private DeviceProfile.RangeOptions makeRangeFromDataType() {
        DeviceProfile.RangeOptions range = new DeviceProfile.RangeOptions();
        BigDecimal min = BigDecimal.ZERO;
        BigDecimal max = BigDecimal.ONE;

        switch (mAttribute.getDataType()) {
            case SINT8:
                min = BigDecimal.valueOf(Byte.MIN_VALUE);
                max = BigDecimal.valueOf(Byte.MAX_VALUE);
                break;

            case SINT16:
            case Q_15_16:
            case FIXED_16_16:
                min = BigDecimal.valueOf(Short.MIN_VALUE);
                max = BigDecimal.valueOf(Short.MAX_VALUE);
                break;

            case SINT32:
            case Q_31_32:
                min = BigDecimal.valueOf(Integer.MIN_VALUE);
                max = BigDecimal.valueOf(Integer.MAX_VALUE);
                break;

            case SINT64:
            case FIXED_32_32:
                min = BigDecimal.valueOf(Long.MIN_VALUE);
                max = BigDecimal.valueOf(Long.MAX_VALUE);
                break;
        }

        range.setMin(min);
        range.setMax(max);
        range.setStep(BigDecimal.ONE);

        return range;
    }

    private BigDecimal getNumericValueFromProportion(double proportion) {
        BigDecimal numValue = null;

        if (mValueOptions != null) {
            try {
                int index = (int) mRange.getIndexByProportion(proportion);
                numValue = new BigDecimal(mValueOptions[index].match);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            numValue = mRange.getValueByProportion(proportion);
        }

        if (numValue == null) {
            numValue = BigDecimal.ZERO;
        }

        return numValue;
    }
}
