/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.afero.aferolab.databinding.ViewProgressBinding;

public class ProgressSpinnerView extends FrameLayout {

    private ViewProgressBinding binding;

    public ProgressSpinnerView(@NonNull Context context) {
        super(context);
    }

    public ProgressSpinnerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ProgressSpinnerView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        binding = ViewProgressBinding.bind(this);
    }

    public void show() {
        setVisibility(VISIBLE);
        binding.viewProgress.setVisibility(VISIBLE);
    }

    public void hide() {
        setVisibility(GONE);
        binding.viewProgress.setVisibility(GONE);
    }
}
