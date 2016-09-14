package com.mbonnin.treedo;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

import org.androidannotations.annotations.EBean;

/**
 * Created by martin on 9/14/16.
 */

@EBean
public class FirebaseUtils {
    private FirebaseAnalytics mFirebaseAnalytics;

    public static String EVENT_SCREEN = "screen";

    public FirebaseUtils(Context context) {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
    }

    public void logScreen() {
        Bundle bundle = new Bundle();

        mFirebaseAnalytics.logEvent(EVENT_SCREEN, bundle);
    }
}
