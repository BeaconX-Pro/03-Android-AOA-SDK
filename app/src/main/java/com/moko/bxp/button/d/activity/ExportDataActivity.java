package com.moko.bxp.button.d.activity;


import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.bxp.button.d.AppConstants;
import com.moko.bxp.button.d.R;
import com.moko.bxp.button.d.adapter.ExportDataListAdapter;
import com.moko.bxp.button.d.databinding.DActivityExportDataBinding;
import com.moko.bxp.button.d.dialog.LoadingMessageDialog;
import com.moko.bxp.button.d.utils.Utils;
import com.moko.support.d.DMokoSupport;
import com.moko.support.d.entity.ExportData;
import com.moko.support.d.entity.OrderCHAR;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class ExportDataActivity extends BaseActivity {

    private static final String EXPORT_FILE_SINGLE = "Single_press_trigger_event.txt";
    private static final String EXPORT_FILE_DOUBLE = "Double_press_trigger_event.txt";
    private static final String EXPORT_FILE_LONG = "Long_press_trigger_event.txt";
    private static final String EXPORT_FILE_SINGLE_TITLE = "Single_press_trigger_event";
    private static final String EXPORT_FILE_DOUBLE_TITLE = "Double_press_trigger_event";
    private static final String EXPORT_FILE_LONG_TITLE = "Long_press_trigger_event";

    private static String PATH_LOGCAT;
    private DActivityExportDataBinding mBind;

    private StringBuilder storeString;
    private ArrayList<ExportData> exportDatas;
    private boolean mIsSync;
    private ExportDataListAdapter adapter;
    private SimpleDateFormat sdf;
    private TimeZone timeZone;
    private boolean mIsShown;
    private String exportTitle;


    public int slotType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBind = DActivityExportDataBinding.inflate(getLayoutInflater());
        setContentView(mBind.getRoot());
        if (getIntent() != null && getIntent().getExtras() != null) {
            slotType = getIntent().getIntExtra(AppConstants.EXTRA_KEY_SLOT_TYPE, 0);
        }
        switch (slotType) {
            case 0:
                mBind.tvTitle.setText("Single press event");
                exportDatas = DMokoSupport.getInstance().exportSingleEvents;
                storeString = DMokoSupport.getInstance().storeSingleEventString;
                PATH_LOGCAT = DMainActivity.PATH_LOGCAT + File.separator + EXPORT_FILE_SINGLE;
                exportTitle = EXPORT_FILE_SINGLE_TITLE;
                break;
            case 1:
                mBind.tvTitle.setText("Double press event");
                exportDatas = DMokoSupport.getInstance().exportDoubleEvents;
                storeString = DMokoSupport.getInstance().storeDoubleEventString;
                PATH_LOGCAT = DMainActivity.PATH_LOGCAT + File.separator + EXPORT_FILE_DOUBLE;
                exportTitle = EXPORT_FILE_DOUBLE_TITLE;
                break;
            case 2:
                mBind.tvTitle.setText("Long press event");
                exportDatas = DMokoSupport.getInstance().exportLongEvents;
                storeString = DMokoSupport.getInstance().storeLongEventString;
                PATH_LOGCAT = DMainActivity.PATH_LOGCAT + File.separator + EXPORT_FILE_LONG;
                exportTitle = EXPORT_FILE_LONG_TITLE;
                break;
        }
        if (exportDatas != null && exportDatas.size() > 0 && storeString != null) {
            mBind.tvExport.setEnabled(true);
        } else {
            exportDatas = new ArrayList<>();
            storeString = new StringBuilder();
        }
        adapter = new ExportDataListAdapter();
        adapter.openLoadAnimation();
        adapter.replaceData(exportDatas);
        mBind.rvExportData.setLayoutManager(new LinearLayoutManager(this));
        mBind.rvExportData.setAdapter(adapter);
        timeZone = TimeZone.getTimeZone("GMT");
        sdf = new SimpleDateFormat(AppConstants.PATTERN_YYYY_MM_DD_T_HH_MM_SS_Z, Locale.US);
        sdf.setTimeZone(timeZone);
        EventBus.getDefault().register(this);
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 300)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        final String action = event.getAction();
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
                finish();
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 300)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        final String action = event.getAction();
        if (!MokoConstants.ACTION_CURRENT_DATA.equals(action))
            EventBus.getDefault().cancelEventDelivery(event);
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_CURRENT_DATA.equals(action)) {
                OrderTaskResponse response = event.getResponse();
                OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
                int responseType = response.responseType;
                byte[] value = response.responseValue;
                switch (orderCHAR) {
                    // eb0201090000017f87a9be8b
                    case CHAR_SINGLE_TRIGGER:
                    case CHAR_DOUBLE_TRIGGER:
                    case CHAR_LONG_TRIGGER:
                        int header = value[0] & 0xFF;// 0xEB
                        int flag = value[1] & 0xFF;// read or write
                        int cmd = value[2] & 0xFF;
                        if (header != 0xEB)
                            return;
                        int length = value[3] & 0xFF;
                        if (flag == 0x02 && cmd == (slotType + 1) && length == 0x09) {
                            if (!mIsShown) {
                                mIsShown = true;
                                mBind.tvExport.setEnabled(true);
                            }
                            ExportData exportData = new ExportData();
                            byte[] timeBytes = Arrays.copyOfRange(value, 4, 12);
                            ByteBuffer byteBuffer = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).put(timeBytes, 0, timeBytes.length);
                            byteBuffer.flip();
                            long time = byteBuffer.getLong();
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTimeZone(timeZone);
                            calendar.setTimeInMillis(time);

                            String timestampStr = sdf.format(calendar.getTime());
                            exportData.timestamp = timestampStr;
                            switch (slotType) {
                                case 0:
                                    exportData.triggerMode = "Single press mode";
                                    break;
                                case 1:
                                    exportData.triggerMode = "Double press mode";
                                    break;
                                case 2:
                                    exportData.triggerMode = "Long press mode";
                                    break;
                            }
                            exportDatas.add(0, exportData);
                            storeString.insert(0, String.format("%s  %s\n", timestampStr, exportData.triggerMode));
                        }
                        adapter.replaceData(exportDatas);
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

    private void back() {
        if (mIsSync) {
            if (slotType == 0) {
                DMokoSupport.getInstance().disableSingleTriggerNotify();
                DMokoSupport.getInstance().exportSingleEvents = exportDatas;
                DMokoSupport.getInstance().storeSingleEventString = storeString;
            }
            if (slotType == 1) {
                DMokoSupport.getInstance().disableDoubleTriggerNotify();
                DMokoSupport.getInstance().exportDoubleEvents = exportDatas;
                DMokoSupport.getInstance().storeDoubleEventString = storeString;
            }
            if (slotType == 2) {
                DMokoSupport.getInstance().disableLongTriggerNotify();
                DMokoSupport.getInstance().exportLongEvents = exportDatas;
                DMokoSupport.getInstance().storeLongEventString = storeString;
            }
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        back();
    }

    public void onSync(View view) {
        if (isWindowLocked())
            return;
        if (!mIsSync) {
            mIsSync = true;
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.rotate_refresh);
            mBind.ivSync.startAnimation(animation);
            mBind.tvSync.setText("Stop");
            if (slotType == 0) {
                DMokoSupport.getInstance().enableSingleTriggerNotify();
            }
            if (slotType == 1) {
                DMokoSupport.getInstance().enableDoubleTriggerNotify();
            }
            if (slotType == 2) {
                DMokoSupport.getInstance().enableLongTriggerNotify();
            }
        } else {
            mIsSync = false;
            mBind.ivSync.clearAnimation();
            mBind.tvSync.setText("Sync");
            if (slotType == 0) {
                DMokoSupport.getInstance().disableSingleTriggerNotify();
            }
            if (slotType == 1) {
                DMokoSupport.getInstance().disableDoubleTriggerNotify();
            }
            if (slotType == 2) {
                DMokoSupport.getInstance().disableLongTriggerNotify();
            }
        }
    }

    public void onEmpty(View view) {
        if (isWindowLocked())
            return;
        storeString = new StringBuilder();
        mBind.tvExport.setEnabled(false);
        exportDatas.clear();
        adapter.replaceData(exportDatas);
        switch (slotType) {
            case 0:
                if (DMokoSupport.getInstance().exportSingleEvents != null) {
                    DMokoSupport.getInstance().exportSingleEvents.clear();
                    DMokoSupport.getInstance().storeSingleEventString = null;
                }
                break;
            case 1:
                if (DMokoSupport.getInstance().exportDoubleEvents != null) {
                    DMokoSupport.getInstance().exportDoubleEvents.clear();
                    DMokoSupport.getInstance().storeDoubleEventString = null;
                }
                break;
            case 2:
                if (DMokoSupport.getInstance().exportLongEvents != null) {
                    DMokoSupport.getInstance().exportLongEvents.clear();
                    DMokoSupport.getInstance().storeLongEventString = null;
                }
                break;
        }
    }

    public void onExport(View view) {
        if (isWindowLocked())
            return;
        showSyncingProgressDialog();
        writeTrackedFile("");
        mBind.tvExport.postDelayed(() -> {
            dismissSyncProgressDialog();
            final String log = storeString.toString();
            if (!TextUtils.isEmpty(log)) {
                writeTrackedFile(log);
                File file = getTrackedFile();
                // 发送邮件
                String address = "Development@mokotechnology.com";
                Utils.sendEmail(ExportDataActivity.this, address, exportTitle, exportTitle, "Choose Email Client", file);
            }
        }, 500);
    }

    public void onBack(View view) {
        back();
    }


    public static void writeTrackedFile(String thLog) {
        File file = new File(PATH_LOGCAT);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(thLog);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static File getTrackedFile() {
        File file = new File(PATH_LOGCAT);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
}
