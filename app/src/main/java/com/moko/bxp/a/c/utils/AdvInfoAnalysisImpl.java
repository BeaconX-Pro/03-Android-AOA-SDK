package com.moko.bxp.a.c.utils;

import android.os.ParcelUuid;
import android.os.SystemClock;

import com.moko.ble.lib.utils.MokoUtils;
import com.moko.bxp.a.c.entity.AdvInfo;
import com.moko.support.d.entity.DeviceInfo;
import com.moko.support.d.entity.OrderServices;
import com.moko.support.d.service.DeviceInfoAnalysis;

import java.util.Arrays;
import java.util.HashMap;

import no.nordicsemi.android.support.v18.scanner.ScanRecord;
import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class AdvInfoAnalysisImpl implements DeviceInfoAnalysis<AdvInfo> {
    private final HashMap<String, AdvInfo> beaconXInfoHashMap;

    public AdvInfoAnalysisImpl() {
        this.beaconXInfoHashMap = new HashMap<>();
    }

    private String getProductMHZ(int paramsInfo) {
        int bit0 = paramsInfo;
        int bit1 = paramsInfo >> 1;
        int bit2 = paramsInfo >> 2;
        StringBuilder builder = new StringBuilder();
        builder.append(bit0).append(bit1).append(bit2);
        String result = builder.toString();
        if ("000".equals(result)) return "2401MHZ";
        if ("001".equals(result)) return "2402MHZ";
        if ("010".equals(result)) return "2480MHZ";
        if ("100".equals(result)) return "2481MHZ";
        return null;
    }

    private String getDeviceInfoMHZ(int paramsInfo) {
        int bit0 = paramsInfo;
        int bit1 = paramsInfo >> 1;
        int bit2 = paramsInfo >> 2;
        StringBuilder builder = new StringBuilder();
        builder.append(bit0).append(bit1).append(bit2);
        String result = builder.toString();
        if ("000".equals(result)) return "2401MHZ";
        if ("001".equals(result)) return "2402MHZ";
        if ("010".equals(result)) return "2426MHZ";
        if ("011".equals(result)) return "2480MHZ";
        if ("100".equals(result)) return "2481MHZ";
        return null;
    }

    private String getDbm(int paramsInfo) {
        int bit4 = paramsInfo >> 4;
        int bit5 = paramsInfo >> 5;
        int bit6 = paramsInfo >> 6;
        int bit7 = paramsInfo >> 7;
        StringBuilder builder = new StringBuilder();
        builder.append(bit4).append(bit5).append(bit6).append(bit7);
        String result = builder.toString();
        if ("0000".equals(result)) return "0dBm";
        if ("0001".equals(result)) return "+3dBm";
        if ("0010".equals(result)) return "+4dBm";
        if ("0011".equals(result)) return "-40dBm";
        if ("0100".equals(result)) return "-20dBm";
        if ("0101".equals(result)) return "-16dBm";
        if ("0110".equals(result)) return "-12dBm";
        if ("0111".equals(result)) return "-8dBm";
        if ("1000".equals(result)) return "-4dBm";
        if ("1001".equals(result)) return "-30dBm";
        return null;
    }

    private int getAdInterval(int interval) {
        int bit0 = interval;
        int bit1 = interval >> 1;
        int bit2 = interval >> 2;
        int bit3 = interval >> 3;
        int bit4 = interval >> 4;
        int bit5 = interval >> 5;
        int bit6 = interval >> 6;
        StringBuilder builder = new StringBuilder();
        builder.append(bit0).append(bit1).append(bit2).append(bit3).append(bit4).append(bit5).append(bit6);
        String result = builder.toString();
        if ("0101011".equals(result)) return 10;
        if ("1010011".equals(result)) return 20;
        if ("0010101".equals(result)) return 50;
        if ("0101001".equals(result)) return 100;
        if ("1010001".equals(result)) return 200;
        if ("0010001".equals(result)) return 250;
        if ("0100001".equals(result)) return 500;
        if ("1000001".equals(result)) return 1000;
        if ("0100000".equals(result)) return 2000;
        if ("1010000".equals(result)) return 5000;
        if ("0101000".equals(result)) return 10000;
        if ("0010100".equals(result)) return 20000;
        if ("1010010".equals(result)) return 50000;
        if ("0101010".equals(result)) return 100000;
        return -1;
    }

    @Override
    public AdvInfo parseDeviceInfo(DeviceInfo deviceInfo) {
        ScanResult result = deviceInfo.scanResult;
        ScanRecord record = result.getScanRecord();
        if (null == record) return null;
        int battery = -1;
        int batterPercent = -1;
        String productMhz = null;
        String productTxPower = null;
        int productAdvInterval = -1;
        String deviceInfoMhz = null;
        String deviceInfoTxPower = null;
        int deviceInfoAdvInterval = -1;
        double temperature = -100;
        int alarmCount = -1;
        String alarmStatus = null;
        int advType = -1;
        //产测信息帧
        byte[] serviceData = record.getServiceData(new ParcelUuid(OrderServices.SERVICE_ADV_PRODUCT_TEST.getUuid()));
        if (null != serviceData && serviceData.length == 13) {
            battery = MokoUtils.toInt(Arrays.copyOfRange(serviceData, 0, 2));
            productMhz = getProductMHZ(serviceData[8] & 0xff);
            productTxPower = getDbm(serviceData[8] & 0xff);
            productAdvInterval = getAdInterval(serviceData[9] & 0xff);
            advType = 1;
        }
        //设备信息帧
        byte[] specificData = record.getManufacturerSpecificData(0x000D);
        if (null != serviceData && serviceData.length == 27) {
            if (specificData[0] != 0x04) return null;
            int key = specificData[1] & 0xff;
            advType = 2;
            if (key == 0x10) {
                //参数信息定位包
                deviceInfoMhz = getDeviceInfoMHZ(specificData[2] & 0xff);
                deviceInfoTxPower = getDbm(specificData[2] & 0xff);
                deviceInfoAdvInterval = getAdInterval(serviceData[4] & 0xff);
                //报警状态
                int i = specificData[3] & 0xff;
                alarmStatus = i >> 3 == 1 ? "Triggerd" : "Standy";
                //电量百分比
                int batterInfo = specificData[3] & 0xff;
                int bit4 = batterInfo >> 4;
                int bit5 = batterInfo >> 5;
                int bit6 = batterInfo >> 6;
                int bit7 = batterInfo >> 7;
                batterPercent = Integer.parseInt("" + bit4 + bit5 + bit6 + bit7, 2);
            } else if (key == 0x1C) {
                temperature = (specificData[2] + 200) / 10.0;
                String count = MokoUtils.bytesToHexString(Arrays.copyOfRange(specificData, 3, 5));
                alarmCount = Integer.parseInt(count, 16);
            }
        }
        AdvInfo advInfo;
        if (beaconXInfoHashMap.containsKey(deviceInfo.mac)) {
            advInfo = beaconXInfoHashMap.get(deviceInfo.mac);
            if (advInfo == null) return null;
            advInfo.rssi = deviceInfo.rssi;
            advInfo.battery = battery;
            advInfo.connectable = result.isConnectable();
            advInfo.productAdvInterval = productAdvInterval;
            advInfo.productMhz = productMhz;
            advInfo.productTxPower = productTxPower;
            advInfo.deviceInfoAdvInterval = deviceInfoAdvInterval;
            advInfo.deviceInfoMhz = deviceInfoMhz;
            advInfo.deviceInfoTxPower = deviceInfoTxPower;
            advInfo.txPower = record.getTxPowerLevel();
            advInfo.temperature = temperature;
            advInfo.alarmCount = alarmCount;
            advInfo.alarmStatus = alarmStatus;
            advInfo.scanRecord = deviceInfo.scanRecord;
            long currentTime = SystemClock.elapsedRealtime();
            advInfo.intervalTime = currentTime - advInfo.scanTime;
            advInfo.scanTime = currentTime;
            advInfo.advType = advType;
            advInfo.batterPercent = batterPercent;
        } else {
            advInfo = new AdvInfo();
            advInfo.name = deviceInfo.name;
            advInfo.mac = deviceInfo.mac;
            advInfo.rssi = deviceInfo.rssi;
            advInfo.battery = battery;
            advInfo.connectable = result.isConnectable();
            advInfo.txPower = record.getTxPowerLevel();
            advInfo.productAdvInterval = productAdvInterval;
            advInfo.productMhz = productMhz;
            advInfo.productTxPower = productTxPower;
            advInfo.deviceInfoAdvInterval = deviceInfoAdvInterval;
            advInfo.deviceInfoMhz = deviceInfoMhz;
            advInfo.deviceInfoTxPower = deviceInfoTxPower;
            advInfo.temperature = temperature;
            advInfo.alarmCount = alarmCount;
            advInfo.alarmStatus = alarmStatus;
            advInfo.scanRecord = deviceInfo.scanRecord;
            advInfo.advType = advType;
            advInfo.batterPercent = batterPercent;
            advInfo.scanTime = SystemClock.elapsedRealtime();
            beaconXInfoHashMap.put(deviceInfo.mac, advInfo);
        }
        return advInfo;
    }
}
