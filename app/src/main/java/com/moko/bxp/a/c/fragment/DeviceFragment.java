package com.moko.bxp.a.c.fragment;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.moko.bxp.a.c.databinding.ACFragmentDeviceBinding;

public class DeviceFragment extends BaseFragment<ACFragmentDeviceBinding> {

    public DeviceFragment() {
    }

    public static DeviceFragment newInstance() {
        return new DeviceFragment();
    }

    @Override
    protected ACFragmentDeviceBinding getViewBind(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return ACFragmentDeviceBinding.inflate(inflater, container, false);
    }

    public void setBattery(int battery) {
        mBind.tvBattery.setText(battery + "mV");
    }

    public void setMacAddress(String macAddress){
        mBind.tvMacAddress.setText(macAddress);
    }

    public void setProductMode(String productMode){
        mBind.tvDeviceModel.setText(productMode);
    }

    public void setSoftwareVersion(String softwareVersion){
        mBind.tvSoftwareVersion.setText(softwareVersion);
    }

    public void setFirmwareVersion(String firmwareVersion){
        mBind.tvFirmwareVersion.setText(firmwareVersion);
    }

    public void setHardwareVersion(String hardwareVersion){
        mBind.tvHardwareVersion.setText(hardwareVersion);
    }

    public void setProductDate(String productDate){
        mBind.tvProductDate.setText(productDate);
    }

    public void setManufacturer(String manufacturer){
        mBind.tvManufacturer.setText(manufacturer);
    }
}
