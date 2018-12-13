package com.star.contacts;

import com.tencent.bugly.Bugly;
import com.umeng.analytics.MobclickAgent;
import com.umeng.commonsdk.UMConfigure;

/**
 * Created by xiongxingxing on 16/12/26.
 */

public class Application extends android.app.Application{
    @Override
    public void onCreate() {
        super.onCreate();
        Bugly.init(getApplicationContext(), "5c27ffd25b", true);
        UMConfigure.init(this, "5c11d780f1f556acc40000b3", null, UMConfigure.DEVICE_TYPE_PHONE, null);
        MobclickAgent.setScenarioType(this, MobclickAgent.EScenarioType.E_UM_NORMAL);
    }
}
