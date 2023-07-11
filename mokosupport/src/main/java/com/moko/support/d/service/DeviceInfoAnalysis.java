package com.moko.support.d.service;

import com.moko.support.d.entity.DeviceInfo;

public interface DeviceInfoAnalysis<T> {
    T parseDeviceInfo(DeviceInfo deviceInfo);
}
