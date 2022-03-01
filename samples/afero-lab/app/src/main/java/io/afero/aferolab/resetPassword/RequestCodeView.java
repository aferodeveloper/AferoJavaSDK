/*
 * Copyright (c) 2014-2019 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.resetPassword;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.afero.aferolab.R;
import io.afero.aferolab.widget.AferoEditText;
import io.afero.aferolab.widget.ProgressSpinnerView;
import io.afero.aferolab.widget.ScreenView;
import io.afero.sdk.client.afero.AferoClient;

public class RequestCodeView extends ScreenView {

    @BindView(R.id.edit_text_email)
    AferoEditText emailEditText;

    @BindView(R.id.progress_request_code)
    ProgressSpinnerView progressView;

    private RequestCodeController mController;


    public RequestCodeView(@NonNull Context context) {
        super(context);
    }

    public RequestCodeView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RequestCodeView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public static RequestCodeView create(@NonNull View contextView) {
        return inflateView(R.layout.view_request_code, contextView);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.bind(this);
    }

    public RequestCodeView start(AferoClient aferoClient) {
        pushOnBackStack();

        mController = new RequestCodeController(this, aferoClient);
        mController.start();

        emailEditText.showKeyboard();

        return this;
    }

    @Override
    public void stop() {
        mController.stop();

        super.stop();
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @OnClick(R.id.button_request_code)
    void onClickRequestCode() {
        emailEditText.hideKeyboard();
        mController.onClickRequestCode(emailEditText.getText().toString());
    }

    @OnClick(R.id.button_already_have_code)
    void onClickAlreadyHaveCode() {
        emailEditText.hideKeyboard();
        mController.onClickAlreadyHaveCode();
    }

    void showProgress() {
        progressView.show();
    }

    void hideProgress() {
        progressView.hide();
    }
}
