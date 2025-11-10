package com.moko.bxp.a.c.fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.moko.bxp.a.c.databinding.ACFragmentAoaSettingBinding;

public class SettingFragment extends BaseFragment<ACFragmentAoaSettingBinding> {
    private boolean showPwd;

    public SettingFragment() {
    }

    public static SettingFragment newInstance() {
        return new SettingFragment();
    }

    @Override
    protected void onCreateView() {
        setPwdShown(showPwd);
    }

    @Override
    protected ACFragmentAoaSettingBinding getViewBind(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return ACFragmentAoaSettingBinding.inflate(inflater, container, false);
    }

    public void setAcc(int accEnable) {
        if (accEnable == 0) {
            //无三轴
            mBind.tvAcc.setVisibility(View.GONE);
            mBind.lineAcc.setVisibility(View.GONE);
            mBind.tvPowerSave.setVisibility(View.GONE);
            mBind.linePowerSave.setVisibility(View.GONE);
        }
    }

    public void setPwdShown(boolean showPwd) {
        this.showPwd = showPwd;
        if (null == mBind) return;
        mBind.tvModifyPwd.setVisibility(showPwd ? View.VISIBLE : View.GONE);
        mBind.lineModifyPwd.setVisibility(showPwd ? View.VISIBLE : View.GONE);
    }
}
