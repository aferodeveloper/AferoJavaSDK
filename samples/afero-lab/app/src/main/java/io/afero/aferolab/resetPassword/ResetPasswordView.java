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

public class ResetPasswordView extends ScreenView {

    @BindView(R.id.edit_text_reset_code)
    AferoEditText resetCodeEditText;

    @BindView(R.id.edit_text_password)
    AferoEditText passwordEditText;

    @BindView(R.id.progress_reset_password)
    ProgressSpinnerView progressView;

    private ResetPasswordController mController;


    public ResetPasswordView(@NonNull Context context) {
        super(context);
    }

    public ResetPasswordView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ResetPasswordView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public static ResetPasswordView create(@NonNull View contextView) {
        return inflateView(R.layout.view_reset_password, contextView);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.bind(this);
    }

    public ResetPasswordView start(AferoClient aferoClient) {
        pushOnBackStack();

        mController = new ResetPasswordController(this, aferoClient);
        mController.start();

        resetCodeEditText.showKeyboard();

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

    @OnClick(R.id.button_reset_password)
    void onClickRequestCode() {
        resetCodeEditText.hideKeyboard();
        passwordEditText.hideKeyboard();
        mController.onClickResetPassword(resetCodeEditText.getText().toString(), passwordEditText.getText().toString());
    }

    void showProgress() {
        progressView.show();
    }

    void hideProgress() {
        progressView.hide();
    }
}
