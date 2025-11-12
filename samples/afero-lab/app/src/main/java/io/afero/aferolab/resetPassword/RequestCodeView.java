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

import io.afero.aferolab.R;
import io.afero.aferolab.databinding.ViewRequestCodeBinding;
import io.afero.aferolab.widget.ScreenView;
import io.afero.sdk.client.afero.AferoClient;

public class RequestCodeView extends ScreenView {

    private ViewRequestCodeBinding binding;

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
        binding = ViewRequestCodeBinding.bind(this);
        binding.buttonRequestCode.setOnClickListener(v -> onClickRequestCode());
        binding.buttonAlreadyHaveCode.setOnClickListener(v -> onClickAlreadyHaveCode());
    }

    public RequestCodeView start(AferoClient aferoClient) {
        pushOnBackStack();

        mController = new RequestCodeController(this, aferoClient);
        mController.start();

        binding.editTextEmail.showKeyboard();

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
        binding.editTextEmail.hideKeyboard();
        mController.onClickRequestCode(binding.editTextEmail.getText().toString());
    }

    void onClickAlreadyHaveCode() {
        binding.editTextEmail.hideKeyboard();
        mController.onClickAlreadyHaveCode();
    }

    void showProgress() {
        binding.progressRequestCode.getRoot().setVisibility(View.VISIBLE);
    }

    void hideProgress() {
        binding.progressRequestCode.getRoot().setVisibility(View.GONE);
    }
}
