package io.afero.aferolab.deviceTag;


import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.afero.aferolab.R;
import io.afero.sdk.device.DeviceTagCollection;

public class DeviceTagItemView extends FrameLayout {

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
        mKeyText.setVisibility(tag.getKey() != null ? VISIBLE : GONE);
        mKeyText.setText(tag.getKey());
        mValueText.setText(tag.getValue());
    }
}
