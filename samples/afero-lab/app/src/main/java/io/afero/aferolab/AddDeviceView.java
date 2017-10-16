/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import java.util.Collections;

import butterknife.ButterKnife;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class AddDeviceView extends FrameLayout implements ZXingScannerView.ResultHandler {

    private AddDeviceController mController;
    private ZXingScannerView mScannerView;
    private boolean mIsCameraStarted;

    public static AddDeviceView create(ViewGroup parent) {
        AddDeviceView view = (AddDeviceView)LayoutInflater.from(parent.getContext())
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

        mScannerView = new ZXingScannerView(getContext());
        mScannerView.setFormats(Collections.singletonList(BarcodeFormat.QR_CODE));
        mScannerView.setClickable(true);
        mScannerView.setMaskColor(Color.TRANSPARENT);
        mScannerView.setBorderColor(Color.TRANSPARENT);
        mScannerView.setAutoFocus(true);
        mScannerView.setShouldScaleToFill(true);
        mScannerView.setSquareViewFinder(true);

        mScannerView.setResultHandler(this);

        addView(mScannerView);
    }

    void start() {
        mController = new AddDeviceController(this);
        mController.start();

        startCamera();
    }

    void stop() {
        stopCamera();

        if (mController != null) {
            mController.stop();
        }

        ViewParent view = getParent();
        if (view != null) {
            ((ViewGroup)view).removeView(this);
        }
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.camera_access_title)
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

    }
}
