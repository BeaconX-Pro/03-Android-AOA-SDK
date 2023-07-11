package com.moko.support.d.callback;

import com.moko.support.d.entity.DeviceInfo;

public interface MokoScanDeviceCallback {
    void onStartScan();

    void onScanDevice(DeviceInfo device);

    void onStopScan();
}
