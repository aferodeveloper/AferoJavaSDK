/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.deviceInspector;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import io.afero.aferolab.databinding.ViewAttributeListItemBinding;
import io.afero.sdk.client.afero.models.AttributeValue;
import io.afero.sdk.device.DeviceModel;
import io.afero.sdk.device.DeviceProfile;

public class AttributeListItemView extends LinearLayout {

    private ViewAttributeListItemBinding binding;

    public AttributeListItemView(Context context) {
        super(context);
    }

    public AttributeListItemView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AttributeListItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        binding = ViewAttributeListItemBinding.bind(this);
    }

    public void update(DeviceModel deviceModel, DeviceProfile.Attribute attribute) {
        binding.attributeIdText.setText(Integer.toString(attribute.getId()));
        binding.attributeLabelText.setText(attribute.getSemanticType() != null ? attribute.getSemanticType() : "");

        AttributeValue value = deviceModel.getAttributeCurrentValue(attribute);
        binding.attributeValueText.setText(value != null ? value.toString() : "<null>");
    }
}
