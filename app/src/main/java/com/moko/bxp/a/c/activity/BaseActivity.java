package com.moko.bxp.a.c.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;

import com.elvishew.xlog.XLog;
import com.moko.lib.bxpui.dialog.LoadingMessageDialog;

import java.util.List;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import org.greenrobot.eventbus.EventBus;

public abstract class BaseActivity<VB extends ViewBinding> extends FragmentActivity {
    // 记录上次页面控件点击时间,屏蔽无效点击事件
    protected VB mBind;
    protected long mLastOnClickTime = 0;
    private LoadingMessageDialog mLoadingMessageDialog;
    private boolean mReceiverTag;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // targetSdk 35+ / Android 16：预测性返回不会再走 Activity.onBackPressed()，
        // 在此统一接管系统返回，并继续回调子类已有的 onBackPressed() 实现。
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                BaseActivity.this.onBackPressed();
            }
        });
        // 设置全屏
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
        // 透明导航栏
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        // Android P及以上支持刘海屏
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        getWindow().setAttributes(params);

        // 设置WindowInsets监听
        getWindow().getDecorView().setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            private int lastOrientation = -1;

            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                DisplayCutout cutout = insets.getDisplayCutout();
                if (cutout != null) {
                    // 获取当前方向
                    int currentOrientation = getResources().getConfiguration().orientation;

                    // 只有当方向改变时才重新设置padding
                    if (currentOrientation != lastOrientation) {
                        lastOrientation = currentOrientation;

                        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                            // 横屏：只考虑左右安全区域
                            v.setPadding(cutout.getSafeInsetLeft(), 0,
                                    cutout.getSafeInsetRight(), 0);
                        } else {
                            int bottomInset = 0;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                            }
                            // 竖屏：使用全部安全区域
                            v.setPadding(cutout.getSafeInsetLeft(), cutout.getSafeInsetTop(),
                                    cutout.getSafeInsetRight(), cutout.getSafeInsetBottom() + bottomInset);
                        }

                        // 请求重新布局
                        v.requestLayout();
                    }
                } else {
                    int bottomInset = 0;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                    }
                    // 没有刘海屏时重置padding
                    v.setPadding(0, 0, 0, bottomInset);
                    lastOrientation = -1;
                }

                return insets;
            }
        });
        mBind = getViewBinding();
        setContentView(mBind.getRoot());
        if (registerEvent()) {
            EventBus.getDefault().register(this);
            // 注册广播接收器
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mReceiver, filter);
            mReceiverTag = true;
        }
        onCreate();
    }


    /**
     * 覆盖 ComponentActivity 默认实现（会再次进 OnBackPressedDispatcher，可能死循环）。
     * 未重写的页面默认 finish；已重写 onBackPressed 的子类仍走各自逻辑。
     */
    @Override
    public void onBackPressed() {
        finish();
    }

    protected abstract VB getViewBinding();

    protected void onCreate() {
    }

    protected boolean registerEvent() {
        return true;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    if (blueState == BluetoothAdapter.STATE_TURNING_OFF) {
                        onSystemBleTurnOff();
                    }
                }
            }
        }
    };

    protected void onSystemBleTurnOff() {
        dismissSyncProgressDialog();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReceiverTag) {
            mReceiverTag = false;
            // 注销广播
            unregisterReceiver(mReceiver);
        }
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
    }

    public void showSyncingProgressDialog() {
        if (null != mLoadingMessageDialog && mLoadingMessageDialog.isAdded() && !mLoadingMessageDialog.isDetached()) {
            mLoadingMessageDialog.dismissAllowingStateLoss();
        }
        mLoadingMessageDialog = new LoadingMessageDialog();
        mLoadingMessageDialog.setMessage("Syncing..");
        if (!mLoadingMessageDialog.isAdded())
            mLoadingMessageDialog.show(getSupportFragmentManager());
    }

    public void dismissSyncProgressDialog() {
        if (mLoadingMessageDialog != null && mLoadingMessageDialog.isAdded() && !mLoadingMessageDialog.isDetached())
            mLoadingMessageDialog.dismissAllowingStateLoss();
    }

    public boolean isWindowLocked() {
        long current = SystemClock.elapsedRealtime();
        if (current - mLastOnClickTime > 500) {
            mLastOnClickTime = current;
            return false;
        } else {
            return true;
        }
    }

    public boolean isWriteStoragePermissionOpen() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isLocationPermissionOpen() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}
