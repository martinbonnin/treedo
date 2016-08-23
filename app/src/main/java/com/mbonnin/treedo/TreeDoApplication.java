package com.mbonnin.treedo;

import android.app.Application;

import io.paperdb.Paper;

/**
 * Created by martin on 8/23/16.
 */

public class TreeDoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Paper.init(this);
    }
}
