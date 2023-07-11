package com.moko.bxp.button.d.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.moko.ble.lib.task.OrderTask;
import com.moko.bxp.button.d.activity.DeviceInfoActivity;
import com.moko.bxp.button.d.databinding.FragmentDSettingBinding;
import com.moko.support.d.DMokoSupport;
import com.moko.support.d.OrderTaskAssembler;

import java.util.ArrayList;
import java.util.List;

public class SettingFragment extends Fragment {

    private FragmentDSettingBinding mBind;
    private DeviceInfoActivity activity;

    public SettingFragment() {
    }

    public static SettingFragment newInstance() {
        SettingFragment fragment = new SettingFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBind = FragmentDSettingBinding.inflate(inflater, container, false);
        activity = (DeviceInfoActivity) getActivity();
        return mBind.getRoot();
    }


    public void setEffectiveClickInterval(int interval) {
        mBind.etEffectiveClickInterval.setText(String.valueOf(interval / 100));
    }

    public boolean isValid() {
        String intervalStr = mBind.etEffectiveClickInterval.getText().toString();
        if (TextUtils.isEmpty(intervalStr))
            return false;
        int interval = Integer.parseInt(intervalStr);
        if (interval < 5 || interval > 15)
            return false;
        return true;
    }

    public void saveParams() {
        String intervalStr = mBind.etEffectiveClickInterval.getText().toString();
        int interval = Integer.parseInt(intervalStr) * 100;
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.setEffectiveClickInterval(interval));
        DMokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    public void setAccAndPowerSaveVisibility(boolean hasAcc){
        if (hasAcc){
            mBind.tvAcc.setVisibility(View.VISIBLE);
            mBind.lineAcc.setVisibility(View.VISIBLE);
            mBind.tvPowerSave.setVisibility(View.VISIBLE);
            mBind.linePowerSave.setVisibility(View.VISIBLE);
        }else {
            mBind.tvAcc.setVisibility(View.GONE);
            mBind.lineAcc.setVisibility(View.GONE);
            mBind.tvPowerSave.setVisibility(View.GONE);
            mBind.linePowerSave.setVisibility(View.GONE);
        }
    }
}
