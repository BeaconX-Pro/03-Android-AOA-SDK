package com.moko.bxp.button.d.activity;

import android.os.Bundle;
import android.view.View;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.bxp.button.d.databinding.DActivityAlarmEventBinding;
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

public class AlarmEventActivity extends BaseActivity {
    private DActivityAlarmEventBinding mBind;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBind = DActivityAlarmEventBinding.inflate(getLayoutInflater());
        setContentView(mBind.getRoot());

        EventBus.getDefault().register(this);
        if (!DMokoSupport.getInstance().isBluetoothOpen()) {
            // 蓝牙未打开，开启蓝牙
            DMokoSupport.getInstance().enableBluetooth();
        } else {
            showSyncingProgressDialog();
            ArrayList<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.getSinglePressEventCount());
            orderTasks.add(OrderTaskAssembler.getDoublePressEventCount());
            orderTasks.add(OrderTaskAssembler.getLongPressEventCount());
            DMokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        }
    }


    @Subscribe(threadMode = ThreadMode.POSTING, priority = 200)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        final String action = event.getAction();
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
                // 设备断开，通知页面更新
                AlarmEventActivity.this.finish();
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
                                    case KEY_SINGLE_PRESS_EVENT_CLEAR:
                                        if (result == 0) {
                                            ToastUtils.showToast(AlarmEventActivity.this, "Opps！Save failed. Please check the input characters and try again.");
                                        } else {
                                            ToastUtils.showToast(AlarmEventActivity.this, "Success！");
                                            mBind.tvSinglePressEventCount.setText("0");
                                            if (DMokoSupport.getInstance().exportSingleEvents != null) {
                                                DMokoSupport.getInstance().exportSingleEvents.clear();
                                                DMokoSupport.getInstance().storeSingleEventString = null;
                                            }
                                        }
                                        break;
                                    case KEY_DOUBLE_PRESS_EVENT_CLEAR:
                                        if (result == 0) {
                                            ToastUtils.showToast(AlarmEventActivity.this, "Opps！Save failed. Please check the input characters and try again.");
                                        } else {
                                            ToastUtils.showToast(AlarmEventActivity.this, "Success！");
                                            mBind.tvDoublePressEventCount.setText("0");
                                            if (DMokoSupport.getInstance().exportDoubleEvents != null) {
                                                DMokoSupport.getInstance().exportDoubleEvents.clear();
                                                DMokoSupport.getInstance().storeDoubleEventString = null;
                                            }
                                        }
                                        break;
                                    case KEY_LONG_PRESS_EVENT_CLEAR:
                                        if (result == 0) {
                                            ToastUtils.showToast(AlarmEventActivity.this, "Opps！Save failed. Please check the input characters and try again.");
                                        } else {
                                            ToastUtils.showToast(AlarmEventActivity.this, "Success！");
                                            mBind.tvLongPressEventCount.setText("0");
                                            if (DMokoSupport.getInstance().exportLongEvents != null) {
                                                DMokoSupport.getInstance().exportLongEvents.clear();
                                                DMokoSupport.getInstance().storeLongEventString = null;
                                            }
                                        }
                                        break;
                                }
                            }
                            if (flag == 0x00) {
                                // read
                                switch (configKeyEnum) {
                                    case KEY_SINGLE_PRESS_EVENTS:
                                        if (length == 2) {
                                            int count = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 4 + length));
                                            mBind.tvSinglePressEventCount.setText(String.valueOf(count));
                                        }
                                        break;
                                    case KEY_DOUBLE_PRESS_EVENTS:
                                        if (length == 2) {
                                            int count = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 4 + length));
                                            mBind.tvDoublePressEventCount.setText(String.valueOf(count));
                                        }
                                        break;
                                    case KEY_LONG_PRESS_EVENTS:
                                        if (length == 2) {
                                            int count = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 4 + length));
                                            mBind.tvLongPressEventCount.setText(String.valueOf(count));
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

    public void onBack(View view) {
        finish();
    }

    public void onClearSinglePressEvent(View view) {
        if (isWindowLocked())
            return;
        showSyncingProgressDialog();
        DMokoSupport.getInstance().sendOrder(OrderTaskAssembler.setSinglePressEventClear());
    }

    public void onClearDoublePressEvent(View view) {
        if (isWindowLocked())
            return;
        showSyncingProgressDialog();
        DMokoSupport.getInstance().sendOrder(OrderTaskAssembler.setDoublePressEventClear());
    }

    public void onClearLongPressEvent(View view) {
        if (isWindowLocked())
            return;
        showSyncingProgressDialog();
        DMokoSupport.getInstance().sendOrder(OrderTaskAssembler.setLongPressEventClear());
    }
}
