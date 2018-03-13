/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.helper;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;

import io.afero.aferolab.R;
import io.afero.sdk.log.AfLog;

public class PermissionsHelper {

    private static final int PERMISSION_REQUEST_ALL = 1;

    // See http://developer.radiusnetworks.com/2015/09/29/is-your-beacon-app-ready-for-android-6.html
    public static void checkRequiredPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            boolean hasLocation = activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            boolean hasCamera = activity.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

            if (!hasLocation || !hasCamera) {
                askUserForAllPermissions(activity);
            } else {
                AfLog.d("checkRequiredPermissions: permissions granted");
            }
        }
    }

    private static void askUserForAllPermissions(final Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.permission_needed_title);
        builder.setMessage(R.string.permission_needed_message);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    activity.requestPermissions(
                            new String[]{
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.CAMERA
                            },
                            PERMISSION_REQUEST_ALL);
                }
            }
        });
        builder.show();
    }

    public static void onRequestPermissionsResult(Activity activity, int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_ALL: {
                boolean allGranted = true;
                for (int grantResult:grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                    }
                }

                if (!allGranted) {
                    tellUserPermissionDenied(activity);
                }
                break;
            }
        }
    }

    private static void tellUserPermissionDenied(Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.permission_denied_title);
        builder.setMessage(R.string.permission_denied_message);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
            }
        });
        builder.show();
    }
}
