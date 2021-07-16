/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.deviceTag;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

    private AferoEditText mTagKeyEditText;
    private AferoEditText mTagValueEditText;

    private AlertDialog mAlertDialog;

    static class Result {

        enum Event {
            ADD,
            UPDATE,
            DELETE
        }

        Result(Event e) {
            event = e;
        }

        final Event event;
        DeviceTagCollection.Tag tag;

        String key;
        String value;
    }


    DeviceTagEditor(ViewGroup ownerView) {
        mOwnerView = ownerView;
    }

    public Observable<Result> start(DeviceTagCollection.Tag tag) {

        mTag = tag;

        return Observable.create(new Action1<Emitter<Result>>() {
            @Override
            public void call(final Emitter<Result> emitter) {

                View view = LayoutInflater.from(mOwnerView.getContext())
                        .inflate(R.layout.view_tag_editor, mOwnerView, false);

                mTagKeyEditText = view.findViewById(R.id.tag_key_edit_text);
                mTagValueEditText = view.findViewById( R.id.tag_value_edit_text);

                if (mTag != null) {
                    if (mTag.getKey() != null) {
                        mTagKeyEditText.setText(mTag.getKey());
                    }

                    if (mTag.getValue() != null) {
                        mTagValueEditText.setText(mTag.getValue());
                    }

                    mTagKeyEditText.setEnabled(mTag.isEditable());
                    mTagValueEditText.setEnabled(mTag.isEditable());
                }

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(view.getContext())
                        .setView(view)
                        .setCancelable(false)
                        .setTitle(mTag != null ? R.string.tag_editor_title_edit : R.string.tag_editor_title_new)
                        .setPositiveButton(R.string.button_title_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mTagKeyEditText.hideKeyboard();

                                String value = mTagValueEditText.getText().toString();
                                String key = mTagKeyEditText.getText().toString();
                                key = key.isEmpty() ? null : key;

                                Result result;
                                if (mTag != null) {
                                    result = new Result(Result.Event.UPDATE);
                                    result.tag = mTag.cloneWith(key, value);
                                } else {
                                    result = new Result(Result.Event.ADD);
                                    result.key = key;
                                    result.value = value;
                                }

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
                    if (mTag.isEditable()) {
                        dialogBuilder.setNeutralButton(R.string.button_title_tag_remove,
                                new DialogInterface.OnClickListener() {
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
                    } else {
                        dialogBuilder.setMessage(R.string.tag_editor_not_editable_message);
                    }
                }

                mAlertDialog = dialogBuilder.create();

                if (mTag != null && mTag.isEditable()) {
                    mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialogInterface) {
                            mTagKeyEditText.showKeyboard();
                        }
                    });
                }

                mAlertDialog.show();

                if (mTag != null && !mTag.isEditable()) {
                    mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }

            }
        }, Emitter.BackpressureMode.LATEST)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

}
