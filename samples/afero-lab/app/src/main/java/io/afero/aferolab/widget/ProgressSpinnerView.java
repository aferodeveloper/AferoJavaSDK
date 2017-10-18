/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.widget;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.afero.aferolab.R;

public class ProgressSpinnerView extends FrameLayout {

    @BindView(R.id.view_progress)
    ProgressBar mProgressBar;

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
        ButterKnife.bind(this);
    }

    public void show() {
        setVisibility(VISIBLE);
        mProgressBar.setVisibility(VISIBLE);
    }

    public void hide() {
        setVisibility(GONE);
        mProgressBar.setVisibility(GONE);
    }
}

