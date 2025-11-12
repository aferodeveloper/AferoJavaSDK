/*
 * Copyright (c) 2014-2019 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.resetPassword;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.afero.aferolab.R;
import io.afero.aferolab.databinding.ViewResetPasswordBinding;
import io.afero.aferolab.widget.ScreenView;
import io.afero.sdk.client.afero.AferoClient;

public class ResetPasswordView extends ScreenView {

    private ViewResetPasswordBinding binding;

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
        binding = ViewResetPasswordBinding.bind(this);
        binding.buttonResetPassword.setOnClickListener(v -> onClickRequestCode());
    }

    public ResetPasswordView start(AferoClient aferoClient) {
        pushOnBackStack();

        mController = new ResetPasswordController(this, aferoClient);
        mController.start();

        binding.editTextResetCode.showKeyboard();

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

    void onClickRequestCode() {
        binding.editTextResetCode.hideKeyboard();
        binding.editTextPassword.hideKeyboard();
        mController.onClickResetPassword(binding.editTextResetCode.getText().toString(), binding.editTextPassword.getText().toString());
    }

    void showProgress() {
        binding.progressResetPassword.getRoot().setVisibility(View.VISIBLE);
    }

    void hideProgress() {
        binding.progressResetPassword.getRoot().setVisibility(View.GONE);
    }
}
