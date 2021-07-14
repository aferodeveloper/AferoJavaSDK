/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;


public class AferoEditText extends androidx.appcompat.widget.AppCompatEditText {
    public AferoEditText(Context context) {
        super(context);
    }

    public AferoEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AferoEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void showKeyboard() {
        requestFocus();
        InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(this, InputMethodManager.SHOW_FORCED);
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getApplicationWindowToken(), 0);
    }

    public static boolean isDone(int actionId, KeyEvent event) {

        switch (actionId) {
            case EditorInfo.IME_NULL:
            case EditorInfo.IME_ACTION_DONE:
            case EditorInfo.IME_ACTION_NEXT:
                if (event == null || event.getAction() == KeyEvent.ACTION_DOWN) {
                    return true;
                }
                break;
        }

        return false;
    }

}


