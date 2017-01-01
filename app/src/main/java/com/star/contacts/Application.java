package com.star.contacts;

import com.tencent.bugly.Bugly;
import com.tencent.bugly.crashreport.CrashReport;

/**
 * Created by xiongxingxing on 16/12/26.
 */

public class Application extends android.app.Application{
    @Override
    public void onCreate() {
        super.onCreate();
        Bugly.init(getApplicationContext(), "5c27ffd25b", true);
    }
}
