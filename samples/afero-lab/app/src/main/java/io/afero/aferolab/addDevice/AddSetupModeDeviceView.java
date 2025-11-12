/*
 * Copyright (c) 2014-2019 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.addDevice;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import io.afero.aferolab.R;
import io.afero.aferolab.databinding.ViewAddSetupModeDeviceBinding;
import io.afero.aferolab.widget.ScreenView;
import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.device.DeviceCollection;
import io.afero.sdk.log.AfLog;
import io.afero.sdk.softhub.AferoSofthub;
import rx.Observable;
import rx.subjects.PublishSubject;

public class AddSetupModeDeviceView extends ScreenView {

    private ViewAddSetupModeDeviceBinding binding;

    private AddSetupModeDeviceController mController;
    private final PublishSubject<AddSetupModeDeviceView> mViewSubject = PublishSubject.create();
    private AferoSofthub.SetupModeDeviceInfo mDeviceInfo;

    public static AddSetupModeDeviceView create(ViewGroup parent) {
        AddSetupModeDeviceView view = (AddSetupModeDeviceView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_add_setup_mode_device, parent, false);
        parent.addView(view);

        return view;
    }

    public AddSetupModeDeviceView(@NonNull Context context) {
        super(context);
    }

    public AddSetupModeDeviceView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AddSetupModeDeviceView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        binding = ViewAddSetupModeDeviceBinding.bind(this);
        binding.addDeviceButton.setOnClickListener(v -> onAddDeviceClick());
    }

    public void start(DeviceCollection deviceCollection, AferoClient aferoClient, AferoSofthub.SetupModeDeviceInfo deviceInfo) {
        pushOnBackStack();

        mDeviceInfo = deviceInfo;
        mController = new AddSetupModeDeviceController(this, deviceCollection, aferoClient);
        mController.start();
    }

    public void stop() {
        if (mController != null) {
            mController.stop();
        }

        super.stop();
    }

    public Observable<AddSetupModeDeviceView> getObservable() {
        return mViewSubject;
    }

    public void onAddDeviceClick() {
        try {
            mController.addDevice(mDeviceInfo.associationId);
        } catch (Exception e) {
            AfLog.d("Device Associate Error: " + e.getLocalizedMessage());

            new AlertDialog.Builder(getContext())
                .setMessage(R.string.error_add_device_generic)
                .setPositiveButton(R.string.button_title_ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    }).show();
        }
    }

    public void askUserForTransferVerification() {
        new AlertDialog.Builder(getContext())
                .setMessage(R.string.error_create_device_transfer)
                .setNegativeButton(R.string.button_title_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .setPositiveButton(R.string.button_title_transfer, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mController.onTransferVerified();
                    }
                })
                .show();
    }

    public void showProgress() {
        binding.addDeviceProgress.getRoot().setVisibility(View.VISIBLE);
    }

    public void hideProgress() {
        binding.addDeviceProgress.getRoot().setVisibility(View.GONE);
    }

    public void showErrorAlert(@StringRes int messageStringId) {
        new AlertDialog.Builder(getContext())
                .setMessage(messageStringId)
                .setPositiveButton(R.string.button_title_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .show();
    }

    public void onCompleted() {
        mViewSubject.onCompleted();
    }
}
