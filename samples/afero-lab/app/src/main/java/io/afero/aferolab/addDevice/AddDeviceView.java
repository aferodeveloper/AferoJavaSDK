/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.addDevice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.util.AttributeSet;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.afero.aferolab.R;
import io.afero.aferolab.databinding.ViewAddDeviceBinding;
import io.afero.aferolab.widget.ScreenView;
import io.afero.sdk.client.afero.AferoClient;
import io.afero.sdk.device.DeviceCollection;
import io.afero.sdk.log.AfLog;
import rx.Observable;
import rx.subjects.PublishSubject;

public class AddDeviceView extends ScreenView {

    private ViewAddDeviceBinding binding;

    private AddDeviceController mController;
    private final PublishSubject<AddDeviceView> mViewSubject = PublishSubject.create();

    private PreviewView mPreviewView;
    private ProcessCameraProvider mCameraProvider;
    private BarcodeScanner mBarcodeScanner;
    private ExecutorService mCameraExecutor;
    private volatile boolean mIsScanning = true;


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
        binding = ViewAddDeviceBinding.bind(this);

        mCameraExecutor = Executors.newSingleThreadExecutor();
        mPreviewView = new PreviewView(getContext());
        binding.scannerViewContainer.addView(mPreviewView);
    }

    public void start(DeviceCollection deviceCollection, AferoClient aferoClient) {
        pushOnBackStack();

        mIsScanning = true;
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
            final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());
            cameraProviderFuture.addListener(() -> {
                try {
                    mCameraProvider = cameraProviderFuture.get();
                    bindCameraUseCases();
                } catch (ExecutionException | InterruptedException e) {
                    AfLog.e(e.toString());
                }
            }, ContextCompat.getMainExecutor(getContext()));
        } else {
            showPermissionAlert();
        }
    }

    private void bindCameraUseCases() {
        if (mCameraProvider == null) {
            return;
        }

        mCameraProvider.unbindAll();

        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        mBarcodeScanner = BarcodeScanning.getClient(options);

        imageAnalysis.setAnalyzer(mCameraExecutor, imageProxy -> {
            if (!mIsScanning) {
                imageProxy.close();
                return;
            }

            @SuppressLint("UnsafeOptInUsageError")
            android.media.Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                mBarcodeScanner.process(image)
                        .addOnSuccessListener(barcodes -> {
                            if (barcodes.size() > 0) {
                                mIsScanning = false;
                                String associationId = barcodes.get(0).getRawValue();
                                if (associationId != null) {
                                    post(() -> mController.addDevice(associationId));
                                } else {
                                    post(this::showScanError);
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            AfLog.d("Scan error: " + e.getLocalizedMessage());
                            post(this::showScanError);
                        })
                        .addOnCompleteListener(task -> imageProxy.close());
            } else {
                imageProxy.close();
            }
        });

        try {
            LifecycleOwner lifecycleOwner = findLifecycleOwner();
            if (lifecycleOwner == null) {
                AfLog.e("Could not find LifecycleOwner to bind camera.");
                return;
            }

            mCameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis);
            preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());
        } catch (Exception e) {
            AfLog.e(e.toString());
        }
    }

    private void showScanError() {
        new AlertDialog.Builder(getContext())
                .setMessage(R.string.error_qr_code_scan_generic_failure)
                .setPositiveButton(R.string.button_title_ok,
                        (dialog, id) -> {
                            resumeCamera();
                            dialog.cancel();
                        }).show();
    }


    public void resumeCamera() {
        mIsScanning = true;
    }

    public void stopCamera() {
        mIsScanning = false;
        if (mCameraProvider != null) {
            mCameraProvider.unbindAll();
        }
        if (mCameraExecutor != null) {
            mCameraExecutor.shutdown();
        }
    }

    private LifecycleOwner findLifecycleOwner() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof LifecycleOwner) {
                return (LifecycleOwner) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    public void showPermissionAlert() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.camera_access_title)
                .setMessage(R.string.permission_camera_rationale)
                .setPositiveButton(R.string.button_title_ok,
                        (dialog, id) -> dialog.cancel()).show();
    }

    public void askUserForTransferVerification() {
        new AlertDialog.Builder(getContext())
                .setMessage(R.string.error_create_device_transfer)
                .setNegativeButton(R.string.button_title_cancel, (dialogInterface, i) -> resumeAfterFailedAssociate())
                .setPositiveButton(R.string.button_title_transfer, (dialogInterface, i) -> mController.onTransferVerified())
                .show();
    }

    private void resumeAfterFailedAssociate() {
        resumeCamera();
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
                .setPositiveButton(R.string.button_title_ok, (dialogInterface, i) -> resumeAfterFailedAssociate())
                .show();
    }

    public void onCompleted() {
        mViewSubject.onCompleted();
    }
}
