package com.moko.bxp.a.c.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.elvishew.xlog.XLog;
import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.bxp.a.c.AppConstants;
import com.moko.bxp.a.c.BuildConfig;
import com.moko.bxp.a.c.R;
import com.moko.bxp.a.c.adapter.DeviceListAdapter;
import com.moko.bxp.a.c.databinding.ACActivityMainBinding;
import com.moko.bxp.a.c.dialog.ScanFilterDialog;
import com.moko.bxp.a.c.entity.AdvInfo;
import com.moko.bxp.a.c.utils.AdvInfoAnalysisImpl;
import com.moko.bxp.a.c.utils.SPUtiles;
import com.moko.bxp.a.c.utils.ToastUtils;
import com.moko.lib.bxpui.dialog.AlertMessageDialog;
import com.moko.lib.bxpui.dialog.LoadingDialog;
import com.moko.lib.bxpui.dialog.LoadingMessageDialog;
import com.moko.lib.bxpui.dialog.PasswordDialog;
import com.moko.support.ac.AOAMokoSupport;
import com.moko.support.ac.MokoBleScanner;
import com.moko.support.ac.OrderTaskAssembler;
import com.moko.support.ac.callback.MokoScanDeviceCallback;
import com.moko.support.ac.entity.DeviceInfo;
import com.moko.support.ac.entity.OrderCHAR;
import com.moko.support.ac.entity.ParamsKeyEnum;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

public class AOACMainActivity extends BaseActivity implements MokoScanDeviceCallback, BaseQuickAdapter.OnItemChildClickListener {
    private ACActivityMainBinding mBind;
    private boolean mReceiverTag = false;
    private ConcurrentHashMap<String, AdvInfo> advInfoHashMap;
    private ArrayList<AdvInfo> advInfoList;
    private DeviceListAdapter adapter;
    private MokoBleScanner mokoBleScanner;
    private Handler mHandler;
    private boolean isPasswordError;
    private AdvInfoAnalysisImpl advInfoAnalysisImpl;
    public static String PATH_LOGCAT;
    private boolean enablePwd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBind = ACActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBind.getRoot());
        // 初始化Xlog
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // 优先保存到SD卡中
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                PATH_LOGCAT = getExternalFilesDir(null).getAbsolutePath() + File.separator + (BuildConfig.IS_LIBRARY ? "mokoBeaconXPro" : "BXP_A_C");
            } else {
                PATH_LOGCAT = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + (BuildConfig.IS_LIBRARY ? "mokoBeaconXPro" : "BXP_A_C");
            }
        } else {
            // 如果SD卡不存在，就保存到本应用的目录下
            PATH_LOGCAT = getFilesDir().getAbsolutePath() + File.separator + (BuildConfig.IS_LIBRARY ? "mokoBeaconXPro" : "BXP_A_C");
        }
        AOAMokoSupport.getInstance().init(getApplicationContext());
        advInfoHashMap = new ConcurrentHashMap<>();
        advInfoList = new ArrayList<>();
        adapter = new DeviceListAdapter();
        adapter.replaceData(advInfoList);
        adapter.setOnItemChildClickListener(this);
        adapter.openLoadAnimation();
        mBind.rvDevices.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        itemDecoration.setDrawable(ContextCompat.getDrawable(this, R.drawable.shape_recycleview_divider));
        mBind.rvDevices.addItemDecoration(itemDecoration);
        mBind.rvDevices.setAdapter(adapter);

        mHandler = new Handler(Looper.getMainLooper());
        mokoBleScanner = new MokoBleScanner(this);
        EventBus.getDefault().register(this);
        mSavedPassword = SPUtiles.getStringValue(this, AppConstants.SP_KEY_SAVED_PASSWORD, "");
        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        mReceiverTag = true;
        if (!AOAMokoSupport.getInstance().isBluetoothOpen()) {
            // 蓝牙未打开，开启蓝牙
            AOAMokoSupport.getInstance().enableBluetooth();
        } else {
            if (animation == null) startScan();
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch (blueState) {
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            if (animation != null) {
                                mHandler.removeMessages(0);
                                mokoBleScanner.stopScanDevice();
                                onStopScan();
                            }
                            break;
                        case BluetoothAdapter.STATE_ON:
                            if (animation == null) startScan();
                            break;

                    }
                }
            }
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        String action = event.getAction();
        if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
            mPassword = "";
            // 设备断开，通知页面更新
            dismissLoadingProgressDialog();
            dismissLoadingMessageDialog();
            if (animation == null) {
                if (isPasswordError) {
                    isPasswordError = false;
                } else {
                    if (disconnectType == 1) {
                        disconnectType = 0;
                    } else {
                        ToastUtils.showToast(this, "Connection failed");
                    }
                }
                if (null == animation) startScan();
            }
        }
        if (MokoConstants.ACTION_DISCOVER_SUCCESS.equals(action)) {
            // 设备连接成功，通知页面更新
            AOAMokoSupport.getInstance().sendOrder(OrderTaskAssembler.getVerifyPasswordEnable());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        final String action = event.getAction();
        if (MokoConstants.ACTION_ORDER_TIMEOUT.equals(action)) {
            AOAMokoSupport.getInstance().disConnectBle();
        }
        if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
            dismissLoadingProgressDialog();
        }
        if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
            OrderTaskResponse response = event.getResponse();
            OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
            byte[] value = response.responseValue;
            if (orderCHAR == OrderCHAR.CHAR_PASSWORD) {
                if (value.length == 5) {
                    int header = value[0] & 0xFF;// 0xEB
                    int flag = value[1] & 0xFF;// read or write
                    int cmd = value[2] & 0xFF;
                    ParamsKeyEnum configKeyEnum = ParamsKeyEnum.fromParamKey(cmd);
                    if (header != 0xEB || null == configKeyEnum) return;
                    int length = value[3] & 0xFF;
                    if (configKeyEnum == ParamsKeyEnum.KEY_PASSWORD) {
                        if (flag == 1 && length == 1) {
                            int result = value[4] & 0xFF;
                            dismissLoadingMessageDialog();
                            if (result == 0xAA) {
                                mSavedPassword = mPassword;
                                SPUtiles.setStringValue(this, AppConstants.SP_KEY_SAVED_PASSWORD, mSavedPassword);
                                XLog.i("Success");
                                Intent intent = new Intent(this, DeviceInfoActivity.class);
                                intent.putExtra("pwdEnable", enablePwd);
                                startActivity(intent);
                            } else {
                                isPasswordError = true;
                                ToastUtils.showToast(this, "Password incorrect！");
                                AOAMokoSupport.getInstance().disConnectBle();
                            }
                        }
                    } else if (configKeyEnum == ParamsKeyEnum.KEY_VERIFY_PASSWORD_ENABLE) {
                        if (flag == 0 && length == 1) {
                            int enable = value[4] & 0xff;
                            if (enable == 1) {
                                // 开启验证
                                enablePwd = true;
                                //开启密码验证的监听
                                showPasswordDialog();
                            } else {
                                enablePwd = false;
                                Intent intent = new Intent(this, DeviceInfoActivity.class);
                                intent.putExtra("pwdEnable", enablePwd);
                                startActivity(intent);
                            }
                        }
                    }
                }
            }
        } else if (MokoConstants.ACTION_CURRENT_DATA.equals(action)) {
            //监听密码连接超时
            OrderTaskResponse response = event.getResponse();
            OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
            byte[] value = response.responseValue;
            if (orderCHAR == OrderCHAR.CHAR_DISCONNECT) {
                if (null != value && value.length == 5) {
                    disconnectType = value[4] & 0xff;
                    if (disconnectType == 1) {
                        //密码验证超时
                        XLog.i("333333*******************type=" + disconnectType);
                        if (null != dialog && dialog.isAdded() && dialog.isVisible())
                            dialog.dismiss();
                        ToastUtils.showToast(this, "Password entry timed out！");
                    }
                }
            }
        }
    }

    private int disconnectType;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefresh(String flag) {
        if ("refresh".equals(flag)) {
            mPassword = "";
            if (animation == null) startScan();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReceiverTag) {
            mReceiverTag = false;
            // 注销广播
            unregisterReceiver(mReceiver);
        }
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onStartScan() {
        advInfoHashMap.clear();
        new Thread(() -> {
            while (animation != null) {
                runOnUiThread(() -> {
                    adapter.replaceData(advInfoList);
                    mBind.tvDeviceNum.setText(String.format("DEVICE(%d)", advInfoList.size()));
                });
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                updateDevices();
            }
        }).start();
    }

    @Override
    public void onScanDevice(DeviceInfo deviceInfo) {
        AdvInfo advInfo = advInfoAnalysisImpl.parseDeviceInfo(deviceInfo);
        if (advInfo == null) return;
        advInfoHashMap.put(advInfo.mac, advInfo);
    }

    @Override
    public void onStopScan() {
        findViewById(R.id.iv_refresh).clearAnimation();
        animation = null;
    }

    private void updateDevices() {
        advInfoList.clear();
        if (!TextUtils.isEmpty(filterMac) || filterRssi != -100) {
            ArrayList<AdvInfo> advInfoListFilter = new ArrayList<>(advInfoHashMap.values());
            Iterator<AdvInfo> iterator = advInfoListFilter.iterator();
            while (iterator.hasNext()) {
                AdvInfo advInfo = iterator.next();
                if (advInfo.rssi > filterRssi) {
                    if (!TextUtils.isEmpty(filterMac) && TextUtils.isEmpty(advInfo.mac)) {
                        iterator.remove();
                    } else if (!TextUtils.isEmpty(filterMac) && advInfo.mac.toLowerCase().replaceAll(":", "").contains(filterMac.toLowerCase())) {
                        continue;
                    } else if (!TextUtils.isEmpty(filterMac) && !advInfo.mac.toLowerCase().replaceAll(":", "").contains(filterMac.toLowerCase(Locale.ROOT))) {
                        iterator.remove();
                    }
                } else {
                    iterator.remove();
                }
            }
            advInfoList.addAll(advInfoListFilter);
        } else {
            advInfoList.addAll(advInfoHashMap.values());
        }
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        Collections.sort(advInfoList, (lhs, rhs) -> {
            if (lhs.rssi > rhs.rssi) {
                return -1;
            } else if (lhs.rssi < rhs.rssi) {
                return 1;
            }
            return 0;
        });
    }

    private Animation animation = null;
    public String filterMac;
    public int filterRssi = -100;

    private void startScan() {
        if (!AOAMokoSupport.getInstance().isBluetoothOpen()) {
            // 蓝牙未打开，开启蓝牙
            AOAMokoSupport.getInstance().enableBluetooth();
            return;
        }
        animation = AnimationUtils.loadAnimation(this, R.anim.rotate_refresh);
        findViewById(R.id.iv_refresh).startAnimation(animation);
        advInfoAnalysisImpl = new AdvInfoAnalysisImpl();
        mokoBleScanner.startScanDevice(this);
    }

    private LoadingDialog mLoadingDialog;

    private void showLoadingProgressDialog() {
        mLoadingDialog = new LoadingDialog();
        mLoadingDialog.show(getSupportFragmentManager());
    }

    private void dismissLoadingProgressDialog() {
        if (mLoadingDialog != null)
            mLoadingDialog.dismissAllowingStateLoss();
    }

    private LoadingMessageDialog mLoadingMessageDialog;

    private void showLoadingMessageDialog() {
        mLoadingMessageDialog = new LoadingMessageDialog();
        mLoadingMessageDialog.setMessage("Verifying..");
        mLoadingMessageDialog.show(getSupportFragmentManager());
    }

    private void dismissLoadingMessageDialog() {
        if (mLoadingMessageDialog != null)
            mLoadingMessageDialog.dismissAllowingStateLoss();
    }

    @Override
    public void onBackPressed() {
        back();
    }

    private String mPassword;
    private String mSavedPassword;
    private String mSelectedDeviceMac;

    @Override
    public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
        //防止重复点击
        if (isWindowLocked()) return;
        if (!AOAMokoSupport.getInstance().isBluetoothOpen()) {
            // 蓝牙未打开，开启蓝牙
            AOAMokoSupport.getInstance().enableBluetooth();
            return;
        }
        final AdvInfo advInfo = (AdvInfo) adapter.getItem(position);
        if (advInfo != null && !isFinishing()) {
            if (animation != null) {
                mHandler.removeMessages(0);
                mokoBleScanner.stopScanDevice();
            }
            mSelectedDeviceMac = advInfo.mac;
            showLoadingProgressDialog();
            mBind.ivRefresh.postDelayed(() -> AOAMokoSupport.getInstance().connDevice(mSelectedDeviceMac), 500);
        }
    }

    private PasswordDialog dialog;

    private void showPasswordDialog() {
        // show password
        dialog = new PasswordDialog();
        dialog.setPassword(mSavedPassword);
        dialog.setOnPasswordClicked(new PasswordDialog.PasswordClickListener() {
            @Override
            public void onEnsureClicked(String password) {
                if (!AOAMokoSupport.getInstance().isBluetoothOpen()) {
                    AOAMokoSupport.getInstance().enableBluetooth();
                    return;
                }
                XLog.i(password);
                mPassword = password;
                showLoadingMessageDialog();
                mBind.ivRefresh.postDelayed(() -> AOAMokoSupport.getInstance().sendOrder(OrderTaskAssembler.setPassword(password)), 200);
            }

            @Override
            public void onDismiss() {
                AOAMokoSupport.getInstance().disConnectBle();
                startScan();
            }
        });
        dialog.show(getSupportFragmentManager());
    }

    public void onBack(View view) {
        if (isWindowLocked()) return;
        back();
    }

    public void onAbout(View view) {
        if (isWindowLocked()) return;
        startActivity(new Intent(this, AboutActivity.class));
    }

    public void onFilter(View view) {
        if (isWindowLocked()) return;
        if (animation != null) {
            mHandler.removeMessages(0);
            mokoBleScanner.stopScanDevice();
        }
        ScanFilterDialog scanFilterDialog = new ScanFilterDialog(this);
        scanFilterDialog.setFilterMac(filterMac);
        scanFilterDialog.setFilterRssi(filterRssi);
        scanFilterDialog.setOnScanFilterListener((filterMac, filterRssi) -> {
            AOACMainActivity.this.filterMac = filterMac;
            String showFilterMac;
            if (filterMac.length() == 12) {
                StringBuilder stringBuffer = new StringBuilder(filterMac);
                stringBuffer.insert(2, ":");
                stringBuffer.insert(5, ":");
                stringBuffer.insert(8, ":");
                stringBuffer.insert(11, ":");
                stringBuffer.insert(14, ":");
                showFilterMac = stringBuffer.toString();
            } else {
                showFilterMac = filterMac;
            }
            AOACMainActivity.this.filterRssi = filterRssi;
            if (!TextUtils.isEmpty(showFilterMac) || filterRssi != -100) {
                mBind.rlFilter.setVisibility(View.VISIBLE);
                mBind.rlEditFilter.setVisibility(View.GONE);
                StringBuilder stringBuilder = new StringBuilder();
                if (!TextUtils.isEmpty(showFilterMac)) {
                    stringBuilder.append(showFilterMac);
                    stringBuilder.append(";");
                }
                if (filterRssi != -100) {
                    stringBuilder.append(String.format("%sdBm", filterRssi + ""));
                    stringBuilder.append(";");
                }
                mBind.tvFilter.setText(stringBuilder.toString());
            } else {
                mBind.rlFilter.setVisibility(View.GONE);
                mBind.rlEditFilter.setVisibility(View.VISIBLE);
            }
            if (isWindowLocked()) return;
            if (animation == null) startScan();
        });
        scanFilterDialog.setOnDismissListener(dialog -> {
            if (isWindowLocked()) return;
            if (animation == null) startScan();
        });
        scanFilterDialog.show();
    }

    private void back() {
        if (animation != null) {
            mHandler.removeMessages(0);
            mokoBleScanner.stopScanDevice();
        }
        if (BuildConfig.IS_LIBRARY) {
            finish();
        } else {
            AlertMessageDialog dialog = new AlertMessageDialog();
            dialog.setMessage(R.string.main_exit_tips);
            dialog.setOnAlertConfirmListener(this::finish);
            dialog.show(getSupportFragmentManager());
        }
    }

    public void onRefresh(View view) {
        if (isWindowLocked())
            return;
        if (!AOAMokoSupport.getInstance().isBluetoothOpen()) {
            // 蓝牙未打开，开启蓝牙
            AOAMokoSupport.getInstance().enableBluetooth();
            return;
        }
        if (animation == null) {
            startScan();
        } else {
            mHandler.removeMessages(0);
            mokoBleScanner.stopScanDevice();
        }
    }

    public void onFilterDelete(View view) {
        if (animation != null) {
            mHandler.removeMessages(0);
            mokoBleScanner.stopScanDevice();
        }
        mBind.rlFilter.setVisibility(View.GONE);
        mBind.rlEditFilter.setVisibility(View.VISIBLE);
        filterMac = "";
        filterRssi = -100;
        if (isWindowLocked()) return;
        if (animation == null) startScan();
    }
}
