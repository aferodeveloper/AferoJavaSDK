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


class DeviceTagEditor {

    private DeviceTagCollection.Tag mTag;

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

        enum Event {
            ADD,
            UPDATE,
            DELETE
        }

        Result(Event e) {
            event = e;
        }

        public final Event event;
        public DeviceTagCollection.Tag tag;
        public String key;
        public String value;
    }


    DeviceTagEditor(ViewGroup ownerView, @StringRes int titleResId) {
        mOwnerView = ownerView;
        mTitleResId = titleResId;
    }

    public Observable<Result> start(DeviceTagCollection.Tag tag) {

        mTag = tag;

        return Observable.create(new Action1<Emitter<Result>>() {
            @Override
            public void call(final Emitter<Result> emitter) {

                View view = LayoutInflater.from(mOwnerView.getContext())
                        .inflate(R.layout.view_tag_editor, mOwnerView, false);
                mTagKeyEditText = ButterKnife.findById(view, R.id.tag_key_edit_text);
                mTagValueEditText = ButterKnife.findById(view, R.id.tag_value_edit_text);

                if (mTag != null) {
                    if (mTag.getKey() != null) {
                        mTagKeyEditText.setText(mTag.getKey());
                    }

                    if (mTag.getValue() != null) {
                        mTagValueEditText.setText(mTag.getValue());
                    }
                }

                mTagKeyEditText.addTextChangedListener(mTextWatcher);
                mTagValueEditText.addTextChangedListener(mTextWatcher);

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(view.getContext())
                        .setView(view)
                        .setCancelable(false)
                        .setTitle(mTitleResId)
                        .setPositiveButton(R.string.button_title_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String key = mTagKeyEditText.getText().toString();
                                mTagKeyEditText.hideKeyboard();

                                Result result = new Result(mTag != null ? Result.Event.UPDATE : Result.Event.ADD);
                                result.tag = mTag;
                                result.key = key.isEmpty() ? null : key;
                                result.value = mTagValueEditText.getText().toString();

                                emitter.onNext(result);
                                emitter.onCompleted();

                                dialogInterface.dismiss();
                            }
                        })
                        .setNegativeButton(R.string.button_title_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mTagKeyEditText.hideKeyboard();
                                emitter.onCompleted();

                                dialogInterface.cancel();
                            }
                        });

                if (mTag != null) {
                    dialogBuilder.setNeutralButton(R.string.button_title_tag_remove, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mTagKeyEditText.hideKeyboard();

                            Result result = new Result(Result.Event.DELETE);
                            result.tag = mTag;

                            emitter.onNext(result);
                            emitter.onCompleted();

                            dialogInterface.dismiss();
                        }
                    });
                }

                mAlertDialog = dialogBuilder.create();

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
