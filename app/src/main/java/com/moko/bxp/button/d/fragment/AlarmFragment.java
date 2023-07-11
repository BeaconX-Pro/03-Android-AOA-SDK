package com.moko.bxp.button.d.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.moko.bxp.button.d.activity.DeviceInfoActivity;
import com.moko.bxp.button.d.databinding.FragmentDAlarmBinding;

public class AlarmFragment extends Fragment {

    private FragmentDAlarmBinding mBind;
    private DeviceInfoActivity activity;

    public AlarmFragment() {
    }

    public static AlarmFragment newInstance() {
        AlarmFragment fragment = new AlarmFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBind = FragmentDAlarmBinding.inflate(inflater, container, false);
        activity = (DeviceInfoActivity) getActivity();
        return mBind.getRoot();
    }


    public void setSinglePressModeSwitch(int onOff) {
        mBind.tvSinglePressModeSwitch.setText(onOff == 1 ? "ON" : "OFF");
    }

    public void setDoublePressModeSwitch(int onOff) {
        mBind.tvDoublePressModeSwitch.setText(onOff == 1 ? "ON" : "OFF");
    }

    public void setLongPressModeSwitch(int onOff) {
        mBind.tvLongPressModeSwitch.setText(onOff == 1 ? "ON" : "OFF");
    }

    public void setAbnormalInactivityModeSwitch(int onOff) {
        mBind.tvAbnormalInactivityModeSwitch.setText(onOff == 1 ? "ON" : "OFF");
    }

    public void setAbnormalInactivityModeVisibility(boolean hasAcc) {
        mBind.layoutAbnormalInactivityModeSwitch.setVisibility(hasAcc ? View.VISIBLE : View.GONE);
    }
}
