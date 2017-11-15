package io.afero.aferolab.deviceTag;


import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.afero.aferolab.R;
import io.afero.sdk.device.DeviceTagCollection;

public class DeviceTagItemView extends LinearLayout {

    @BindView(R.id.tag_key_text)
    TextView mKeyText;

    @BindView(R.id.tag_value_text)
    TextView mValueText;

    public DeviceTagItemView(@NonNull Context context) {
        super(context);
    }

    public DeviceTagItemView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DeviceTagItemView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.bind(this);
    }

    public void update(DeviceTagCollection.Tag tag) {
        mKeyText.setText(tag.getKey());
        mValueText.setText(tag.getValue());
    }
}
