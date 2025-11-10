package com.moko.bxp.a.c.activity;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.bxp.a.c.R;
import com.moko.bxp.a.c.databinding.ACActivityPowerSavingConfigBinding;
import com.moko.bxp.a.c.entity.TxPowerEnum;
import com.moko.bxp.a.c.utils.ToastUtils;
import com.moko.lib.bxpui.dialog.BottomDialog;
import com.moko.support.ac.AOAMokoSupport;
import com.moko.support.ac.OrderTaskAssembler;
import com.moko.support.ac.entity.OrderCHAR;
import com.moko.support.ac.entity.ParamsKeyEnum;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class PowerSavingConfigActivity extends BaseActivity<ACActivityPowerSavingConfigBinding> implements CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener {
    private int version;
    private final String[] mValues = {"10", "20", "50", "100", "200", "250", "500", "1000", "2000", "5000", "10000", "20000", "50000", "100000"};
    private int mSelected;
    private static final int NEW_VERSION = 204;
    private boolean saveParamsError;

    @SuppressLint("StringFormatInvalid")
    @Override
    protected void onCreate() {
        version = getIntent().getIntExtra("version", 0);
        mBind.etStaticTriggerTime.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String triggerTime = editable.toString();
                mBind.tvTips.setText(getString(version >= NEW_VERSION ? R.string.static_trigger_time_tips_new : R.string.static_trigger_time_tips, triggerTime));
            }
        });
        mBind.tvTips.setText(getString(version >= NEW_VERSION ? R.string.static_trigger_time_tips_new : R.string.static_trigger_time_tips, ""));
        if (!AOAMokoSupport.getInstance().isBluetoothOpen()) {
            // 蓝牙未打开，开启蓝牙
            AOAMokoSupport.getInstance().enableBluetooth();
        } else {
            showSyncingProgressDialog();
            ArrayList<OrderTask> orderTasks = new ArrayList<>(2);
            orderTasks.add(OrderTaskAssembler.getPowerSavingStaticTriggerTime());
            if (version >= NEW_VERSION) {
                mBind.cbAdvPowerSaveMode.setVisibility(View.VISIBLE);
                mBind.cbAdvPowerSaveMode.setOnCheckedChangeListener(this);
                mBind.sbTxPower.setOnSeekBarChangeListener(this);
                mBind.tvAdvInterval.setOnClickListener(v -> onAdvIntervalClick());
                orderTasks.add(OrderTaskAssembler.getPowerSavingAdvParams());
            } else {
                mBind.cbAdvPowerSaveMode.setVisibility(View.GONE);
                mBind.group.setVisibility(View.GONE);
            }
            AOAMokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        }
        mBind.cbPowerSaveMode.setOnCheckedChangeListener(this);
    }

    @Override
    protected ACActivityPowerSavingConfigBinding getViewBinding() {
        return ACActivityPowerSavingConfigBinding.inflate(getLayoutInflater());
    }

    private void onAdvIntervalClick() {
        if (isWindowLocked()) return;
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(new ArrayList<>(Arrays.asList(mValues)), mSelected);
        dialog.setListener(i -> {
            mSelected = i;
            mBind.tvAdvInterval.setText(mValues[i]);
        });
        dialog.show(getSupportFragmentManager());
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 200)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        final String action = event.getAction();
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
                // 设备断开，通知页面更新
                finish();
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 200)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        EventBus.getDefault().cancelEventDelivery(event);
        final String action = event.getAction();
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
                dismissSyncProgressDialog();
            }
            if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                OrderTaskResponse response = event.getResponse();
                OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
                byte[] value = response.responseValue;
                if (orderCHAR == OrderCHAR.CHAR_PARAMS) {
                    if (value.length > 4) {
                        int header = value[0] & 0xFF;// 0xEB
                        int flag = value[1] & 0xFF;// read or write
                        int cmd = value[2] & 0xFF;
                        if (header != 0xEB) return;
                        ParamsKeyEnum configKeyEnum = ParamsKeyEnum.fromParamKey(cmd);
                        if (configKeyEnum == null) return;
                        int length = value[3] & 0xFF;
                        if (flag == 0x01 && length == 0x01) {
                            // write
                            int result = value[4] & 0xFF;
                            switch (configKeyEnum) {
                                case KEY_POWER_SAVING_ADV_PARAMS:
                                    if (result != 0xAA) saveParamsError = true;
                                    break;
                                case KEY_POWER_SAVING_STATIC_TRIGGER_TIME:
                                    if (result != 0xAA) saveParamsError = true;
                                    ToastUtils.showToast(this, saveParamsError ? "Opps！Save failed. Please check the input characters and try again." : "Success");
                                    break;
                            }
                        } else if (flag == 0x00) {
                            // read
                            switch (configKeyEnum) {
                                case KEY_POWER_SAVING_STATIC_TRIGGER_TIME:
                                    if (length == 2) {
                                        int time = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 6));
                                        mBind.cbPowerSaveMode.setChecked(time > 0);
                                        if (time > 0) {
                                            mBind.etStaticTriggerTime.setText(String.valueOf(time));
                                            mBind.etStaticTriggerTime.setSelection(mBind.etStaticTriggerTime.getText().length());
                                        }
                                    }
                                    break;

                                case KEY_POWER_SAVING_ADV_PARAMS:
                                    int interval = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 6));
                                    int txPower = value[6];
                                    int advDuration = MokoUtils.toInt(Arrays.copyOfRange(value, 7, 9));
                                    int standby = MokoUtils.toInt(Arrays.copyOfRange(value, 9, 11));
                                    int enable = value[11];
                                    mBind.cbAdvPowerSaveMode.setChecked(enable == 1);
                                    for (int i = 0; i < mValues.length; i++) {
                                        if ((interval == Integer.parseInt(mValues[i]))) {
                                            mSelected = i;
                                            break;
                                        }
                                    }
                                    mBind.tvAdvInterval.setText(mValues[mSelected]);
                                    int txPowerProgress = Objects.requireNonNull(TxPowerEnum.fromTxPower(txPower)).ordinal();
                                    mBind.sbTxPower.setProgress(txPowerProgress);
                                    mBind.tvTxPower.setText(TxPowerEnum.fromTxPower(txPower).getTxPower() + "dBm");
                                    mBind.etAdvDuration.setText(String.valueOf(advDuration));
                                    mBind.etAdvDuration.setSelection(mBind.etAdvDuration.getText().length());
                                    mBind.etStandbyDuration.setText(String.valueOf(standby));
                                    mBind.etStandbyDuration.setSelection(mBind.etStandbyDuration.getText().length());
                                    break;
                            }
                        }
                    }
                }
            }
        });
    }

    public void onSave(View view) {
        if (isWindowLocked()) return;
        if (isValid()) {
            showSyncingProgressDialog();
            saveParamsError = false;
            int time = mBind.cbPowerSaveMode.isChecked() ? Integer.parseInt(mBind.etStaticTriggerTime.getText().toString()) : 0;
            ArrayList<OrderTask> orderTasks = new ArrayList<>(2);
            if (version >= NEW_VERSION) {
                //新版本
                int duration = TextUtils.isEmpty(mBind.etAdvDuration.getText()) ? 7 : Integer.parseInt(mBind.etAdvDuration.getText().toString());
                int standby = TextUtils.isEmpty(mBind.etStandbyDuration.getText()) ? 60 : Integer.parseInt(mBind.etStandbyDuration.getText().toString());
                int interval = Integer.parseInt(mValues[mSelected]);
                int txPower = TxPowerEnum.fromOrdinal(mBind.sbTxPower.getProgress()).getTxPower();
                orderTasks.add(OrderTaskAssembler.setPowerSavingAdvParams(interval, txPower, duration, standby, mBind.cbAdvPowerSaveMode.isChecked() ? 1 : 0));
            }
            orderTasks.add(OrderTaskAssembler.setPowerSavingStaticTriggerTime(time));
            AOAMokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        } else {
            ToastUtils.showToast(this, "Opps！Save failed. Please check the input characters and try again.");
        }
    }

    public void onBack(View view) {
        finish();
    }

    private boolean isValid() {
        if (mBind.cbPowerSaveMode.isChecked()) {
            if (TextUtils.isEmpty(mBind.etStaticTriggerTime.getText())) return false;
            String timeStr = mBind.etStaticTriggerTime.getText().toString();
            int time = Integer.parseInt(timeStr);
            return time >= 1 && time <= 65535;
        }
        if (version >= NEW_VERSION && mBind.cbAdvPowerSaveMode.isChecked()) {
            if (TextUtils.isEmpty(mBind.etAdvDuration.getText()) || TextUtils.isEmpty(mBind.etStandbyDuration.getText())) {
                return false;
            }
            int duration = Integer.parseInt(mBind.etAdvDuration.getText().toString());
            int standby = Integer.parseInt(mBind.etStandbyDuration.getText().toString());
            return duration >= 1 && duration <= 65535 && standby <= 65535;
        }
        return true;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.cbPowerSaveMode) {
            mBind.clStaticTriggerTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        } else if (buttonView.getId() == R.id.cbAdvPowerSaveMode) {
            mBind.group.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        TxPowerEnum txPowerEnum = TxPowerEnum.fromOrdinal(progress);
        if (null == txPowerEnum) return;
        int txPower = txPowerEnum.getTxPower();
        mBind.tvTxPower.setText(String.format(Locale.getDefault(), "%ddBm", txPower));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
