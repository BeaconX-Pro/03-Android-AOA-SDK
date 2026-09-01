package com.moko.bxp.a.c.fragment;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.moko.bxp.a.c.R;
import com.moko.bxp.a.c.databinding.AQhFragmentAdvertisementBinding;
import com.moko.lib.bxpui.dialog.BottomDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class AdvertisementQHFragment extends BaseFragment<AQhFragmentAdvertisementBinding> implements SeekBar.OnSeekBarChangeListener {
    private final String[] advIntervalVal = {"10", "20", "50", "100", "200", "250", "333", "500", "1000", "2000", "5000", "10000", "20000", "50000"};
    private int mSelectedAdvInterval = -1;
    private int mSelectedAdvInterval2 = -1;
    private FragmentActivity activity;
    private final int[] txPowerArray = {-40, -30, -20, -16, -12, -8, -4, 0, 3, 4};
    private int mTxPower;
    private int mTxPower2;

    public AdvertisementQHFragment() {
    }

    public static AdvertisementQHFragment newInstance() {
        return new AdvertisementQHFragment();
    }

    @Override
    protected void onCreateView() {
        activity = getActivity();
        mBind.sbTxPower.setOnSeekBarChangeListener(this);
        mBind.sbTxPower2.setOnSeekBarChangeListener(this);
        mBind.tvAdvInterval.setOnClickListener(v -> onAdvIntervalClick(0));
        mBind.tvAdvInterval2.setOnClickListener(v -> onAdvIntervalClick(1));
    }

    @Override
    protected AQhFragmentAdvertisementBinding getViewBind(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return AQhFragmentAdvertisementBinding.inflate(inflater, container, false);
    }


    public int getSelectedAdvInterval() {
        return Integer.parseInt(advIntervalVal[mSelectedAdvInterval]);
    }

    public int getSelectedAdvInterval2() {
        return Integer.parseInt(advIntervalVal[mSelectedAdvInterval2]);
    }

    public int getTxPower() {
        return mTxPower;
    }

    public int getTxPower2() {
        return mTxPower2;
    }


    public boolean isValid() {
        if (mSelectedAdvInterval == -1 || mSelectedAdvInterval2 == -1) return false;
        return true;
    }

    /**
     * 广播间隔
     */
    private void onAdvIntervalClick(int type) {
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(new ArrayList<>(Arrays.asList(advIntervalVal)), type == 0 ? mSelectedAdvInterval : mSelectedAdvInterval2);
        dialog.setListener(value -> {
            if (type == 0) {
                mSelectedAdvInterval = value;
                mBind.tvAdvInterval.setText(advIntervalVal[value]);
            } else {
                mSelectedAdvInterval2 = value;
                mBind.tvAdvInterval2.setText(advIntervalVal[value]);
            }
        });
        dialog.show(activity.getSupportFragmentManager());
    }

    public void setAdvInterval(int type, int interval) {
        for (int i = 0; i < advIntervalVal.length; i++) {
            if (interval == Integer.parseInt(advIntervalVal[i])) {
                if (type == 0)
                    mSelectedAdvInterval = i;
                else
                    mSelectedAdvInterval2 = i;
                break;
            }
        }
        if (type == 0) {
            if (mSelectedAdvInterval != -1)
                mBind.tvAdvInterval.setText(advIntervalVal[mSelectedAdvInterval]);
        } else {
            if (mSelectedAdvInterval2 != -1)
                mBind.tvAdvInterval2.setText(advIntervalVal[mSelectedAdvInterval2]);
        }
    }

    public void updateAdvTxPower(int type, int progress) {
        if (type == 0) {
            mBind.sbTxPower.setProgress(getProgress(progress));
            mBind.tvTxPower.setText(String.format(Locale.getDefault(), "%ddBm", progress));
            mTxPower = progress;
        } else {
            mBind.sbTxPower2.setProgress(getProgress(progress));
            mBind.tvTxPower2.setText(String.format(Locale.getDefault(), "%ddBm", progress));
            mTxPower2 = progress;
        }
    }

    private int getProgress(int progress) {
        int index = 0;
        for (int i = 0; i < txPowerArray.length; i++) {
            if (progress == txPowerArray[i]) {
                index = i;
                break;
            }
        }
        return index;
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar.getId() == R.id.sb_tx_power) {
            mBind.tvTxPower.setText(String.format(Locale.getDefault(), "%ddBm", txPowerArray[progress]));
            mTxPower = txPowerArray[progress];
        } else if (seekBar.getId() == R.id.sb_tx_power2) {
            mBind.tvTxPower2.setText(String.format(Locale.getDefault(), "%ddBm", txPowerArray[progress]));
            mTxPower2 = txPowerArray[progress];
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
