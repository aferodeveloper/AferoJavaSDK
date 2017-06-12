/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.afero.models;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;

public class DeviceInfoBody {

    public static class ExtendedData {
        public String app_version;
        public int app_build_number;
        public String app_identifier;
        public String app_build_type;

        public String hardware_manufacturer = Build.MANUFACTURER;
        public String hardware_model_name = Build.MODEL;
        public String hardware_serial_number = Build.SERIAL;
        public String hardware_board = Build.BOARD;
        public String hardware_bootloader = Build.BOOTLOADER;
        public String hardware_brand = Build.BRAND;
        public String hardware_device = Build.DEVICE;
        public String hardware_display = Build.DISPLAY;
        public String hardware_fingerprint = Build.FINGERPRINT;
        public String hardware_radio_version = Build.getRadioVersion();
        public String hardware_product = Build.PRODUCT;
        public String hardware = Build.HARDWARE;
        public String hardware_type = Build.TYPE;
        public String hardware_cpu_abi = Build.CPU_ABI;
        public String hardware_cpu_abi2 = Build.CPU_ABI2;
        public String hardware_id = Build.ID;
        public String hardware_host = Build.HOST;
        public String hardware_user = Build.USER;
        public String system_os_version = Build.VERSION.RELEASE;
        public String system_os_codename = Build.VERSION.CODENAME;
        public String system_os_incremental = Build.VERSION.INCREMENTAL;
        public int system_os_sdk_int = Build.VERSION.SDK_INT;

        public String screen_size;
        public int screen_dpi;
        public float screen_density;
        public float screen_scaled_density;
        public float screen_xdpi;
        public float screen_ydpi;

        ExtendedData(Context context) {
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            screen_size = dm.widthPixels + "x" + dm.heightPixels;
            screen_dpi = dm.densityDpi;
            screen_density = dm.density;
            screen_scaled_density = dm.scaledDensity;
            screen_xdpi = dm.xdpi;
            screen_ydpi = dm.ydpi;
        }
    }

    public String pushId;
    public String mobileDeviceId;
    public String platform = "ANDROID";
    public ExtendedData extendedData;

    public DeviceInfoBody(Context context, String pId, String mobileId) {
        extendedData = new ExtendedData(context);
        pushId = pId;
        mobileDeviceId = mobileId;
    }
}
