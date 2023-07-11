package com.moko.bxp.button.d.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.bxp.button.d.databinding.DActivityRemoteReminderNotifyTypeBinding;
import com.moko.bxp.button.d.dialog.LoadingMessageDialog;
import com.moko.bxp.button.d.utils.ToastUtils;
import com.moko.support.d.DMokoSupport;
import com.moko.support.d.OrderTaskAssembler;
import com.moko.support.d.entity.OrderCHAR;
import com.moko.support.d.entity.ParamsKeyEnum;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;

public class RemoteReminderActivity extends BaseActivity {

    private DActivityRemoteReminderNotifyTypeBinding mBind;
    public boolean isConfigError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBind = DActivityRemoteReminderNotifyTypeBinding.inflate(getLayoutInflater());
        setContentView(mBind.getRoot());
        EventBus.getDefault().register(this);
        if (!DMokoSupport.getInstance().isBluetoothOpen()) {
            // 蓝牙未打开，开启蓝牙
            DMokoSupport.getInstance().enableBluetooth();
        } else {
            showSyncingProgressDialog();
            ArrayList<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.getRemoteLEDNotifyAlarmParams());
            orderTasks.add(OrderTaskAssembler.getRemoteBuzzerNotifyAlarmParams());
            DMokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        }
    }


    @Subscribe(threadMode = ThreadMode.POSTING, priority = 200)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        final String action = event.getAction();
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
                // 设备断开，通知页面更新
                RemoteReminderActivity.this.finish();
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 200)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        EventBus.getDefault().cancelEventDelivery(event);
        final String action = event.getAction();
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_ORDER_TIMEOUT.equals(action)) {
            }
            if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
                dismissSyncProgressDialog();
            }
            if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                OrderTaskResponse response = event.getResponse();
                OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
                int responseType = response.responseType;
                byte[] value = response.responseValue;
                switch (orderCHAR) {
                    case CHAR_PARAMS:
                        if (value.length > 4) {
                            int header = value[0] & 0xFF;// 0xEB
                            int flag = value[1] & 0xFF;// read or write
                            int cmd = value[2] & 0xFF;
                            if (header != 0xEB)
                                return;
                            ParamsKeyEnum configKeyEnum = ParamsKeyEnum.fromParamKey(cmd);
                            if (configKeyEnum == null) {
                                return;
                            }
                            int length = value[3] & 0xFF;
                            if (flag == 0x01 && length == 0x01) {
                                // write
                                int result = value[4] & 0xFF;
                                switch (configKeyEnum) {
                                    case KEY_REMOTE_LED_NOTIFY_ALARM_PARAMS:
                                    case KEY_REMOTE_BUZZER_NOTIFY_ALARM_PARAMS:
                                        if (result == 0) {
                                            isConfigError = true;
                                        }
                                        if (isConfigError) {
                                            ToastUtils.showToast(RemoteReminderActivity.this, "Opps！Save failed. Please check the input characters and try again.");
                                        } else {
                                            ToastUtils.showToast(this, "Success");
                                        }
                                        break;
                                }
                            }
                            if (flag == 0x00) {
                                // read
                                switch (configKeyEnum) {
                                    case KEY_REMOTE_LED_NOTIFY_ALARM_PARAMS:
                                        if (length == 4) {
                                            int time = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 6));
                                            int interval = MokoUtils.toInt(Arrays.copyOfRange(value, 6, 8));
                                            mBind.etBlinkingTime.setText(String.valueOf(time));
                                            mBind.etBlinkingInterval.setText(String.valueOf(interval));
                                        }
                                        break;
                                    case KEY_REMOTE_BUZZER_NOTIFY_ALARM_PARAMS:
                                        if (length == 4) {
                                            int time = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 6));
                                            int interval = MokoUtils.toInt(Arrays.copyOfRange(value, 6, 8));
                                            mBind.etRingingTime.setText(String.valueOf(time));
                                            mBind.etRingingInterval.setText(String.valueOf(interval));
                                        }
                                        break;
                                }
                            }
                        }
                        break;
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private LoadingMessageDialog mLoadingMessageDialog;

    public void showSyncingProgressDialog() {
        mLoadingMessageDialog = new LoadingMessageDialog();
        mLoadingMessageDialog.setMessage("Syncing..");
        mLoadingMessageDialog.show(getSupportFragmentManager());
    }

    public void dismissSyncProgressDialog() {
        if (mLoadingMessageDialog != null)
            mLoadingMessageDialog.dismissAllowingStateLoss();
    }

    public void onLedNotifyRemind(View view) {
        if (isWindowLocked()) return;
        if (isLEDValid()) {
            showSyncingProgressDialog();
            String ledTimeStr = mBind.etBlinkingTime.getText().toString();
            String ledIntervalStr = mBind.etBlinkingInterval.getText().toString();
            int ledTime = Integer.parseInt(ledTimeStr);
            int ledInterval = Integer.parseInt(ledIntervalStr);
            ArrayList<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.setRemoteLEDNotifyAlarmParams(ledTime, ledInterval));
            DMokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        } else {
            ToastUtils.showToast(this, "Opps！Save failed. Please check the input characters and try again.");
        }
    }

    public void onBuzzerNotifyRemind(View view) {
        if (isWindowLocked())
            return;
        if (isBuzzerValid()) {
            showSyncingProgressDialog();
            String buzzerTimeStr = mBind.etRingingTime.getText().toString();
            String buzzerIntervalStr = mBind.etRingingInterval.getText().toString();
            int buzzerTime = Integer.parseInt(buzzerTimeStr);
            int buzzerInterval = Integer.parseInt(buzzerIntervalStr);
            ArrayList<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.setRemoteBuzzerNotifyAlarmParams(buzzerTime, buzzerInterval));
            DMokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        } else {
            ToastUtils.showToast(this, "Opps！Save failed. Please check the input characters and try again.");
        }
    }

    public void onBack(View view) {
        finish();
    }

    private boolean isBuzzerValid() {
        String buzzerTimeStr = mBind.etRingingTime.getText().toString();
        String buzzerIntervalStr = mBind.etRingingInterval.getText().toString();
        if (TextUtils.isEmpty(buzzerTimeStr) || TextUtils.isEmpty(buzzerIntervalStr)) {
            return false;
        }
        int buzzerTime = Integer.parseInt(buzzerTimeStr);
        if (buzzerTime < 1 || buzzerTime > 6000)
            return false;
        int buzzerInterval = Integer.parseInt(buzzerIntervalStr);
        if (buzzerInterval < 0 || buzzerInterval > 100)
            return false;
        return true;
    }

    private boolean isLEDValid() {
        String ledTimeStr = mBind.etBlinkingTime.getText().toString();
        String ledIntervalStr = mBind.etBlinkingInterval.getText().toString();
        if (TextUtils.isEmpty(ledTimeStr) || TextUtils.isEmpty(ledIntervalStr)) {
            return false;
        }
        int ledTime = Integer.parseInt(ledTimeStr);
        if (ledTime < 1 || ledTime > 6000)
            return false;
        int ledInterval = Integer.parseInt(ledIntervalStr);
        if (ledInterval < 0 || ledInterval > 100)
            return false;
        return true;
    }
}
