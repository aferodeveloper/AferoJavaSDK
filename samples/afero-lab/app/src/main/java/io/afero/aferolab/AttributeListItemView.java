/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.afero.sdk.client.afero.models.AttributeValue;

class AttributeListItemView extends LinearLayout {

    @BindView(R.id.attribute_label_text)
    TextView mLabelText;

    @BindView(R.id.attribute_value_text)
    TextView mValueText;

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
        ButterKnife.bind(this);
    }

    public void setLabelAndValue(String label, AttributeValue value) {
        mLabelText.setText(label);
        mValueText.setText(value != null ? value.toString() : "<null>");
    }
}
