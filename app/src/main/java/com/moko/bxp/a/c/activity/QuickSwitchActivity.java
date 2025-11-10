package com.moko.bxp.a.c.activity;

import android.content.Intent;
import android.view.View;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.bxp.a.c.AppConstants;
import com.moko.bxp.a.c.R;
import com.moko.bxp.a.c.databinding.ACActivityQuickSwitchBinding;
import com.moko.bxp.a.c.utils.ToastUtils;
import com.moko.lib.bxpui.dialog.AlertMessageDialog;
import com.moko.support.ac.AOAMokoSupport;
import com.moko.support.ac.OrderTaskAssembler;
import com.moko.support.ac.entity.OrderCHAR;
import com.moko.support.ac.entity.ParamsKeyEnum;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class QuickSwitchActivity extends BaseActivity<ACActivityQuickSwitchBinding> {
    private boolean enablePasswordVerify;
    private boolean enableLedIndicator;

    @Override
    protected void onCreate() {
        showSyncingProgressDialog();
        ArrayList<OrderTask> orderTasks = new ArrayList<>(2);
        orderTasks.add(OrderTaskAssembler.getTriggerLedStatus());
        orderTasks.add(OrderTaskAssembler.getVerifyPasswordEnable());
        AOAMokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    @Override
    protected ACActivityQuickSwitchBinding getViewBinding() {
        return ACActivityQuickSwitchBinding.inflate(getLayoutInflater());
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
                switch (orderCHAR) {
                    case CHAR_PARAMS:
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
                                if (configKeyEnum == ParamsKeyEnum.KEY_TRIGGER_LED_STATUS) {
                                    if (result != 0xAA) {
                                        ToastUtils.showToast(this, "Opps！Save failed. Please check the input characters and try again.");
                                    } else {
                                        ToastUtils.showToast(this, "Success");
                                    }
                                }
                            }
                            if (flag == 0x00 && length == 1) {
                                // read
                                int result = value[4] & 0xFF;
                                if (configKeyEnum == ParamsKeyEnum.KEY_TRIGGER_LED_STATUS) {
                                    setLedIndicatorEnable(result);
                                }
                            }
                        }
                        break;
                    case CHAR_PASSWORD:
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
                                if (configKeyEnum == ParamsKeyEnum.KEY_VERIFY_PASSWORD_ENABLE) {
                                    if (result != 0xAA) {
                                        ToastUtils.showToast(this, "Opps！Save failed. Please check the input characters and try again.");
                                    } else {
                                        ToastUtils.showToast(this, "Success");
                                    }
                                }
                            }
                            if (flag == 0x00 && length == 1) {
                                // read
                                int result = value[4] & 0xFF;
                                if (configKeyEnum == ParamsKeyEnum.KEY_VERIFY_PASSWORD_ENABLE) {
                                    setPasswordVerify(result);
                                }
                            }
                        }
                        break;
                }
            }
        });
    }

    private void setPasswordVerify(int enable) {
        this.enablePasswordVerify = enable == 1;
        mBind.ivEnablePwd.setImageResource(enable == 1 ? R.drawable.ic_checked : R.drawable.ic_unchecked);
        mBind.tvPwdEnableStatus.setText(enablePasswordVerify ? "Enable" : "Disable");
        mBind.tvPwdEnableStatus.setEnabled(enablePasswordVerify);
    }

    private void setLedIndicatorEnable(int enable) {
        this.enableLedIndicator = enable == 1;
        mBind.ivTriggerLed.setImageResource(enable == 1 ? R.drawable.ic_checked : R.drawable.ic_unchecked);
        mBind.tvTriggerLed.setText(enableLedIndicator ? "Enable" : "Disable");
        mBind.tvTriggerLed.setEnabled(enableLedIndicator);
    }

    public void onChangePwdEnable(View view) {
        if (isWindowLocked()) return;
        if (enablePasswordVerify) {
            final AlertMessageDialog dialog = new AlertMessageDialog();
            dialog.setTitle("Warning！");
            dialog.setMessage("If Password verification is disabled, it will not need password to connect the Beacon.");
            dialog.setConfirm(R.string.ok);
            dialog.setOnAlertConfirmListener(() -> setVerifyPasswordEnable(false));
            dialog.show(getSupportFragmentManager());
        } else {
            setVerifyPasswordEnable(true);
        }
    }

    public void onTriggerLed(View view) {
        if (isWindowLocked()) return;
        showSyncingProgressDialog();
        List<OrderTask> orderTasks = new ArrayList<>(2);
        orderTasks.add(OrderTaskAssembler.setTriggerLedStatus(enableLedIndicator ? 0 : 1));
        orderTasks.add(OrderTaskAssembler.getTriggerLedStatus());
        AOAMokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    public void setVerifyPasswordEnable(boolean enable) {
        showSyncingProgressDialog();
        ArrayList<OrderTask> orderTasks = new ArrayList<>(2);
        orderTasks.add(OrderTaskAssembler.setVerifyPasswordEnable(enable ? 1 : 0));
        orderTasks.add(OrderTaskAssembler.getVerifyPasswordEnable());
        AOAMokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    public void onBack(View view) {
        back();
    }

    @Override
    public void onBackPressed() {
        back();
    }

    private void back() {
        Intent intent = new Intent();
        intent.putExtra(AppConstants.EXTRA_KEY_PASSWORD_VERIFICATION, enablePasswordVerify);
        setResult(RESULT_OK, intent);
        finish();
    }
}
