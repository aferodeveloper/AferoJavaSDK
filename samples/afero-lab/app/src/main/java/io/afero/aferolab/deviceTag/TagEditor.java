/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.deviceTag;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.annotation.StringRes;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import butterknife.ButterKnife;
import io.afero.aferolab.R;
import io.afero.aferolab.widget.AferoEditText;
import io.afero.sdk.device.DeviceTagCollection;
import rx.Emitter;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;


public class TagEditor {

    private final ViewGroup mOwnerView;
    private final int mTitleResId;

    private AferoEditText mTagKeyEditText;
    private AferoEditText mTagValueEditText;

    private AlertDialog mAlertDialog;
    private Button mPositiveButton;
    private final TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            mPositiveButton.setEnabled(mTagKeyEditText.length() > 0 || mTagValueEditText.length() > 0);
        }
    };

    public static class Result {
        public String key;
        public String value;
    }

    private final Result mResult = new Result();


    public TagEditor(ViewGroup ownerView, @StringRes int titleResId) {
        mOwnerView = ownerView;
        mTitleResId = titleResId;
    }

    public Observable<Result> start(DeviceTagCollection.Tag tag) {

        if (tag != null) {
            mResult.key = tag.getKey();
            mResult.value = tag.getValue();
        } else {
            mResult.key = null;
            mResult.value = null;
        }

        return Observable.create(new Action1<Emitter<Result>>() {
            @Override
            public void call(final Emitter<Result> emitter) {

                View view = LayoutInflater.from(mOwnerView.getContext())
                        .inflate(R.layout.view_tag_editor, mOwnerView, false);
                mTagKeyEditText = ButterKnife.findById(view, R.id.tag_key_edit_text);
                mTagValueEditText = ButterKnife.findById(view, R.id.tag_value_edit_text);

                if (mResult.key != null) {
                    mTagKeyEditText.setText(mResult.key);
                }

                if (mResult.value != null) {
                    mTagValueEditText.setText(mResult.value);
                }

                mTagKeyEditText.addTextChangedListener(mTextWatcher);

                mAlertDialog = new AlertDialog.Builder(view.getContext())
                        .setView(view)
                        .setCancelable(false)
                        .setTitle(mTitleResId)
                        .setPositiveButton(R.string.button_title_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String password = mTagKeyEditText.getText().toString();

                                if (!password.isEmpty()) {
                                    mTagKeyEditText.hideKeyboard();

                                    mResult.key = mTagKeyEditText.getText().toString();
                                    mResult.value = mTagValueEditText.getText().toString();

                                    emitter.onNext(mResult);
                                    emitter.onCompleted();

                                    dialogInterface.dismiss();
                                }
                            }
                        })
                        .setNegativeButton(R.string.button_title_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mTagKeyEditText.hideKeyboard();
                                emitter.onCompleted();

                                dialogInterface.cancel();
                            }
                        })
                        .create();

                mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialogInterface) {
                        mTagKeyEditText.showKeyboard();
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
