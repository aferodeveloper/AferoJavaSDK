/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.widget;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.StringRes;

import io.afero.aferolab.R;
import rx.Emitter;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class PasswordDialog {

    private final ViewGroup mViewGroup;
    private final int mTitleResId;

    private AferoEditText mPasswordEditText;
    private AlertDialog mAlertDialog;
    private Button mPositiveButton;


    public PasswordDialog(ViewGroup viewGroup, @StringRes int titleResId) {
        mViewGroup = viewGroup;
        mTitleResId = titleResId;
    }

    public Observable<String> start() {
        return Observable.create(new Action1<Emitter<String>>() {
            @Override
            public void call(final Emitter<String> emitter) {

                View view = LayoutInflater.from(mViewGroup.getContext())
                        .inflate(R.layout.view_password_dialog, mViewGroup, false);
                mPasswordEditText = view.findViewById(R.id.password_edit_text);

                mPasswordEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        mPositiveButton.setEnabled(editable.length() > 0);
                    }
                });

                mAlertDialog = new AlertDialog.Builder(view.getContext())
                        .setView(view)
                        .setCancelable(false)
                        .setTitle(mTitleResId)
                        .setPositiveButton(R.string.button_title_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String password = mPasswordEditText.getText().toString();

                                if (!password.isEmpty()) {
                                    mPasswordEditText.hideKeyboard();

                                    emitter.onNext(password);
                                    emitter.onCompleted();

                                    dialogInterface.dismiss();
                                }
                            }
                        })
                        .setNegativeButton(R.string.button_title_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mPasswordEditText.hideKeyboard();
                                emitter.onCompleted();

                                dialogInterface.cancel();
                            }
                        })
                        .create();

                mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialogInterface) {
                        mPasswordEditText.showKeyboard();
                    }
                });
                mAlertDialog.show();

                mPositiveButton = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                mPositiveButton.setEnabled(false);
            }
        }, Emitter.BackpressureMode.LATEST)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

}
