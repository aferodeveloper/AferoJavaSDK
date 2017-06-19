/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client.retrofit2.models;

public class DeviceInfoBody {

    public static class ExtendedData {
        public String app_version;
        public int app_build_number;
        public String app_identifier;
        public String app_build_type;

        public String hardware_manufacturer;
        public String hardware_model_name;
        public String hardware_serial_number;
        public String hardware_board;
        public String hardware_bootloader;
        public String hardware_brand;
        public String hardware_device;
        public String hardware_display;
        public String hardware_fingerprint;
        public String hardware_radio_version;
        public String hardware_product;
        public String hardware;
        public String hardware_type;
        public String hardware_cpu_abi;
        public String hardware_cpu_abi2;
        public String hardware_id;
        public String hardware_host;
        public String hardware_user;
        public String system_os_version;
        public String system_os_codename;
        public String system_os_incremental;
        public int system_os_sdk_int;

        public String screen_size;
        public int screen_dpi;
        public float screen_density;
        public float screen_scaled_density;
        public float screen_xdpi;
        public float screen_ydpi;
    }

    public String pushId;
    public String mobileDeviceId;
    public String platform;
    public ExtendedData extendedData;

    public DeviceInfoBody(String platform, String pId, String clientId) {
        extendedData = new ExtendedData();
        this.platform = platform;
        this.pushId = pId;
        this.mobileDeviceId = clientId;
    }
}
