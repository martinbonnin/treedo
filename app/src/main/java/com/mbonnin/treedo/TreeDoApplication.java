package com.mbonnin.treedo;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.support.multidex.MultiDex;

import io.paperdb.Paper;

/**
 * Created by martin on 8/23/16.
 */

public class TreeDoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Utils.init(getApplicationContext(), (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);

        Paper.init(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
