package com.moko.bxp.button.d.activity;

import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.elvishew.xlog.XLog;
import com.moko.bxp.button.d.BuildConfig;
import com.moko.bxp.button.d.R;
import com.moko.bxp.button.d.databinding.ActivityAboutDBinding;
import com.moko.bxp.button.d.utils.ToastUtils;
import com.moko.bxp.button.d.utils.Utils;

import java.io.File;
import java.util.Calendar;


public class AboutActivity extends BaseActivity {
    private ActivityAboutDBinding mBind;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBind = ActivityAboutDBinding.inflate(getLayoutInflater());
        setContentView(mBind.getRoot());
        if (!BuildConfig.IS_LIBRARY) {
            mBind.appVersion.setText(String.format("Version:V%s", Utils.getVersionInfo(this)));
        }
        mBind.tvCompanyWebsite.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
    }


    public void onBack(View view) {
        finish();
    }

    public void onCompanyWebsite(View view) {
        if (isWindowLocked())
            return;
        Uri uri = Uri.parse("https://" + getString(R.string.company_website));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    public void onFeedback(View view) {
        if (isWindowLocked())
            return;
        File trackerLog = new File(DMainActivity.PATH_LOGCAT + File.separator + (BuildConfig.IS_LIBRARY ? "MKButton.txt" : "MKButtonD.txt"));
        File trackerLogBak = new File(DMainActivity.PATH_LOGCAT + File.separator + (BuildConfig.IS_LIBRARY ? "MKButton.txt.bak" : "MKButtonD.txt.bak"));
        File trackerCrashLog = new File(DMainActivity.PATH_LOGCAT + File.separator + (BuildConfig.IS_LIBRARY ? "crash_log.txt" : "d_crash_log.txt"));
        if (!trackerLog.exists() || !trackerLog.canRead()) {
            ToastUtils.showToast(this, "File is not exists!");
            return;
        }
        XLog.i("333333mail="+trackerLog.getPath());
        String address = "Development@mokotechnology.com";
        StringBuilder mailContent = new StringBuilder("MKButtonD_");
        Calendar calendar = Calendar.getInstance();
        String date = Utils.calendar2strDate(calendar, "yyyyMMdd");
        mailContent.append(date);
        String title = mailContent.toString();
        if ((!trackerLogBak.exists() || !trackerLogBak.canRead())
                && (!trackerCrashLog.exists() || !trackerCrashLog.canRead())) {
            Utils.sendEmail(this, address, "", title, "Choose Email Client", trackerLog);
        } else if (!trackerCrashLog.exists() || !trackerCrashLog.canRead()) {
            Utils.sendEmail(this, address, "", title, "Choose Email Client", trackerLog, trackerLogBak);
        } else if (!trackerLogBak.exists() || !trackerLogBak.canRead()) {
            Utils.sendEmail(this, address, "", title, "Choose Email Client", trackerLog, trackerCrashLog);
        } else {
            Utils.sendEmail(this, address, "", title, "Choose Email Client", trackerLog, trackerLogBak, trackerCrashLog);
        }
    }
}
