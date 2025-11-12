package io.afero.aferolab.deviceTag;


import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.afero.aferolab.databinding.ViewTagListItemBinding;
import io.afero.sdk.device.DeviceTagCollection;

public class DeviceTagItemView extends FrameLayout {

    private ViewTagListItemBinding binding;

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
        binding = ViewTagListItemBinding.bind(this);
    }

    public void update(DeviceTagCollection.Tag tag) {
        binding.tagKeyText.setVisibility(tag.getKey() != null ? VISIBLE : GONE);
        binding.tagKeyText.setText(tag.getKey());
        binding.tagValueText.setText(tag.getValue());
    }
}
