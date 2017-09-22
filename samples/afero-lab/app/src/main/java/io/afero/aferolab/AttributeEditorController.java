package io.afero.aferolab;


import java.math.BigDecimal;

import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.device.AttributeWriter;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceProfile;
import io.afero.sdk.utils.RxUtils;
import rx.Observer;
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
    private Subscription mAttributeWriteSubscription;

    AttributeEditorController(AttributeEditorView view) {
        mView = view;
    }

    void start(DeviceModel deviceModel, DeviceProfile.Attribute attribute) {
        mDeviceModel = deviceModel;
        mAttribute = attribute;

        DeviceProfile.Presentation presentation = mDeviceModel.getPresentation();
        DeviceProfile.AttributeOptions options = presentation != null
                ? presentation.getAttributeOptionsById(mAttribute.getId()) : null;

        mValueOptions = options != null ? options.getValueOptions() : null;

        if (mValueOptions != null) {
            mRange = makeRangeFromValueOptions();
            mView.setAttributeValueSliderMax(mValueOptions.length - 1);

            for (DeviceProfile.DisplayRule vo : mValueOptions) {
                mView.addEnumItem(vo.getApplyLabel(), vo.match);
            }
        } else {
            mRange = options != null ? options.getRangeOptions() : null;

            if (mRange == null) {
                mRange = makeRangeFromDataType();
            }
        }

        mView.setAttributeIdText(mAttribute.getId());
        mView.setAttributeLabelText(mAttribute.getSemanticType());
        mView.setAttributeDataTypeText(mAttribute.getDataType().toString());
        mView.setAttributeValueEditorType(getAttributeValueEditorType());

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
        AttributeValue value = new AttributeValue(mAttribute.getDataType());
        value.setValue(numValue);

        mView.setAttributeValueText(value.toString());
        mView.setAttributeValueEnumText(getValueOptionsLabelFromValue(value));
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

    void onAttributeValueSelected(String itemValue) {
        AttributeValue value = new AttributeValue(itemValue, mAttribute.getDataType());

        updateDeviceModel(value);
    }

    private void updateDeviceModel(AttributeValue value) {
        mView.setAttributeValueText(value.toString());

        mAttributeWriteSubscription = RxUtils.safeUnSubscribe(mAttributeWriteSubscription);
        mAttributeWriteSubscription = mDeviceModel.writeAttributes()
                .put(mAttribute.getId(), value)
                .commit()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<AttributeWriter.Result>() {
                    @Override
                    public void onCompleted() {
                        mAttributeWriteSubscription = null;
                        onDeviceUpdate();
                    }

                    @Override
                    public void onError(Throwable e) {
                        mAttributeWriteSubscription = null;
                        onDeviceUpdate();
                    }

                    @Override
                    public void onNext(AttributeWriter.Result result) {
                        if (!result.isSuccess()) {
                            mDeviceModel.cancelAttributePendingValue(mDeviceModel.getAttributeById(result.attributeId));
                        }
                    }
                });
    }

    private void onDeviceUpdate() {

        mView.setEditorEnabled(mDeviceModel.isAvailable() && mAttribute.isWritable());

        if (mAttributeWriteSubscription != null) {
            // write already in progress...
            return;
        }

        AttributeValue value = mDeviceModel.getAttributeCurrentValue(mAttribute);
        mView.setAttributeValueText(value != null ? value.toString() : null);
        mView.setAttributeValueEnumText(getValueOptionsLabelFromValue(value));

        if (mAttribute.isNumericType()) {
            BigDecimal numericValue = value != null ? value.numericValue() : BigDecimal.ZERO;

            if (mValueOptions != null) {
                numericValue = new BigDecimal(getValueOptionsIndex(value));
            }

            mView.setAttributeValueSliderProportion(mRange.getProportionByValue(numericValue));
        }
    }

    private String getValueOptionsLabelFromValue(AttributeValue value) {
        if (mValueOptions != null) {
            String stringValue = value.toString();

            for (DeviceProfile.DisplayRule vo : mValueOptions) {
                if (stringValue.equals(vo.match)) {
                    return vo.getApplyLabel();
                }
            }
        }

        return "";
    }

    private int getValueOptionsIndex(AttributeValue value) {
        String stringValue = value.toString();
        int i = 0;

        for (DeviceProfile.DisplayRule vo : mValueOptions) {
            if (stringValue.equals(vo.match)) {
                break;
            }
            ++i;
        }

        return i;
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
            } else if (mAttribute.getDataType() == AttributeValue.DataType.BYTES) {
                editorType = AttributeEditorView.ValueEditorType.BYTES;
            } else if (mAttribute.getDataType() == AttributeValue.DataType.BOOLEAN) {
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
            case FIXED_32_32:
                min = BigDecimal.valueOf(Integer.MIN_VALUE);
                max = BigDecimal.valueOf(Integer.MAX_VALUE);
                break;

            case SINT64:
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
