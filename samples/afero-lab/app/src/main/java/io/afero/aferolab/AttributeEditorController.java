package io.afero.aferolab;


import android.text.InputType;

import java.math.BigDecimal;

import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceProfile;
import io.afero.sdk.utils.RxUtils;
import rx.Subscription;
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

        mView.setAttributeValueEditEnabled(mAttribute.isWritable());
        mView.setAttributeValueEditTextConfig(getAttributeValueInputType(), getAttributeValueAllowedDigits());

        mView.setAttributeNumericEditEnabled(mAttribute.isNumericType());

        DeviceProfile.Presentation presentation = mDeviceModel.getPresentation();
        DeviceProfile.AttributeOptions options = presentation != null
                ? presentation.getAttributeOptionsById(mAttribute.getId()) : null;

        mValueOptions = options != null ? options.getValueOptions() : null;
        mRange = options != null ? options.getRangeOptions() : null;
        if (mRange == null) {
            mRange = mValueOptions != null ? makeRangeFromValueOptions() : makeRangeFromDataType();
        }

        mDeviceUpdateSubscription = deviceModel.getUpdateObservable()
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

    void onAttributeValueSliderChanged(double sliderProportion) {
        BigDecimal value = null;

        if (mValueOptions != null) {
            try {
                int index = (int)mRange.getIndexByProportion(sliderProportion);
                value = new BigDecimal(mValueOptions[index].match);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            value = mRange.getValueByProportion(sliderProportion);
        }

        if (value == null) {
            value = BigDecimal.ZERO;
        }

        mView.setAttributeValueText(value.toString());
    }

    private void onDeviceUpdate() {
        AttributeValue value = mDeviceModel.getAttributeCurrentValue(mAttribute);
        mView.setAttributeValueText(value != null ? value.toString() : null);

        if (mAttribute.isNumericType()) {
            BigDecimal numericValue = value != null ? value.numericValue() : BigDecimal.ZERO;
            mView.setAttributeValueSliderProportion(mRange.getProportionByValue(numericValue));
        }
    }

    private int getAttributeValueInputType() {
        int inputType;

        if (mAttribute.isNumericType()) {
            inputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED;
            if (mAttribute.isNumericDecimalType()) {
                inputType |= InputType.TYPE_NUMBER_FLAG_DECIMAL;
            }
        } else {
            inputType = InputType.TYPE_CLASS_TEXT;
            switch (mAttribute.getDataType()) {
                case UTF8S:
                    inputType |= InputType.TYPE_TEXT_FLAG_MULTI_LINE;
                    break;

                case BYTES:
                    inputType |= InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS;
                    break;
            }
        }

        return inputType;
    }

    private String getAttributeValueAllowedDigits() {
        return mAttribute.getDataType() == AttributeValue.DataType.BYTES ? "0123456789ABCDEF" : null;
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
}
