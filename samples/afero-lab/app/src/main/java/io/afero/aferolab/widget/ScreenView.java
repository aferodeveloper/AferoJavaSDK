/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.widget;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import io.afero.aferolab.R;

public class ScreenView extends FrameLayout {

    public ScreenView(@NonNull Context context) {
        super(context);
    }

    public ScreenView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ScreenView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * @return true if the backPress was handled by the ScreenView,
     *         false if the ScreenView should be stopped
     */
    public boolean onBackPressed() {
        return false;
    }

    public void stop() {
        if (getParent() != null) {
            ((ViewGroup) getParent()).removeView(this);
        }
    }

    public static ViewGroup getRootContainer(View v) {
        return (ViewGroup)v.getRootView().findViewById(R.id.root_view);
    }

    public static <T extends ScreenView> T inflateView(int layoutResId, View contextView) {
        final ViewGroup parent = ScreenView.getRootContainer(contextView);
        final View v = LayoutInflater.from(contextView.getContext()).inflate(layoutResId, parent, false);
        parent.addView(v);

        //noinspection unchecked
        return (T) v;
    }
}
