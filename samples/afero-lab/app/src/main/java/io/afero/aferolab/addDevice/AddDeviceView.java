/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.addDevice;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.afero.aferolab.R;
import io.afero.aferolab.widget.ProgressSpinnerView;
import io.afero.aferolab.widget.ScreenView;
import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.device.DeviceCollection;
import io.afero.sdk.log.AfLog;
import me.dm7.barcodescanner.zxing.ZXingScannerView;
import rx.Observable;
import rx.subjects.PublishSubject;

public class AddDeviceView extends ScreenView implements ZXingScannerView.ResultHandler {

    @BindView(R.id.scanner_view_container)
    ViewGroup mScannerViewContainer;

    @BindView(R.id.add_device_progress)
    ProgressSpinnerView mProgressView;

    private AddDeviceController mController;
    private ZXingScannerView mScannerView;
    private boolean mIsCameraStarted;
    private final PublishSubject<AddDeviceView> mViewSubject = PublishSubject.create();

    public static AddDeviceView create(ViewGroup parent) {
        AddDeviceView view = (AddDeviceView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_add_device, parent, false);
        parent.addView(view);

        return view;
    }

    public AddDeviceView(@NonNull Context context) {
        super(context);
    }

    public AddDeviceView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AddDeviceView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.bind(this);

        mScannerView = new ZXingScannerView(mScannerViewContainer.getContext());
        mScannerView.setFormats(Collections.singletonList(BarcodeFormat.QR_CODE));
        mScannerView.setClickable(true);
        mScannerView.setMaskColor(Color.TRANSPARENT);
        mScannerView.setBorderColor(Color.TRANSPARENT);
        mScannerView.setAutoFocus(true);
        mScannerView.setShouldScaleToFill(true);
        mScannerView.setSquareViewFinder(true);

        mScannerView.setResultHandler(this);

        mScannerViewContainer.addView(mScannerView);
    }

    public void start(DeviceCollection deviceCollection, AferoClient aferoClient) {
        pushOnBackStack();

        mController = new AddDeviceController(this, deviceCollection, aferoClient);
        mController.start();

        startCamera();
    }

    public void stop() {
        stopCamera();

        if (mController != null) {
            mController.stop();
        }

        super.stop();
    }

    public Observable<AddDeviceView> getObservable() {
        return mViewSubject;
    }

    private void startCamera() {
        boolean hasCameraPermission = ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        if (hasCameraPermission) {
            mScannerView.startCamera();
            mIsCameraStarted = true;
        } else {
            showPermissionAlert();
        }
    }

    public void resumeCamera() {
        if (mIsCameraStarted) {
            mScannerView.resumeCameraPreview(this);
        } else {
            mScannerView.startCamera();
        }
    }

    public void stopCamera() {
        mScannerView.stopCamera();
        mIsCameraStarted = false;
    }

    public void showPermissionAlert() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.camera_access_title)
                .setMessage(R.string.permission_camera_rationale)
                .setPositiveButton(R.string.button_title_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }).show();
    }

    @Override
    public void handleResult(Result result) {
        try {
            String associationId = result.getText();
            mController.addDevice(associationId);
        } catch (Exception e) {
            AfLog.d("Scan error: " + e.getLocalizedMessage());

            new AlertDialog.Builder(getContext())
                    .setMessage(R.string.error_qr_code_scan_generic_failure)
                    .setPositiveButton(R.string.button_title_ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    resumeCamera();
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
                        resumeAfterFailedAssociate();
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

    private void resumeAfterFailedAssociate() {
        resumeCamera();
    }

    public void showProgress() {
        mProgressView.show();
    }

    public void hideProgress() {
        mProgressView.hide();
    }

    public void showErrorAlert(@StringRes int messageStringId) {
        new AlertDialog.Builder(getContext())
                .setMessage(messageStringId)
                .setPositiveButton(R.string.button_title_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        resumeAfterFailedAssociate();
                    }
                })
                .show();
    }

    public void onCompleted() {
        mViewSubject.onCompleted();
    }
}
