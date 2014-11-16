package com.mbonnin.treedo;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static android.util.TypedValue.applyDimension;

/**
 * Created by martin on 16/10/14.
 */
public class Utils {
    private static final String TAG = "Tree-Do";
    private static boolean sDebuggable;
    private static DisplayMetrics sDisplayMetrics;

    public static synchronized void log(String message) {
        if (sDebuggable)
            Log.d(TAG, message);
    }

    public static void init(Context context, boolean debuggable) {
        sDebuggable = debuggable;
        sDisplayMetrics = context.getResources().getDisplayMetrics();
    }

    public static int toPixels(int dp) {
        return (int)applyDimension(COMPLEX_UNIT_DIP, dp, sDisplayMetrics);
    }

    public static String encode(String string) {
        int i = 0;
        String result = "";

        if (string.equals("")) {
            return "";
        }

        while (string.charAt(i) == ' ') {
            result += "\\ ";
            i++;
        }

        return result + string.substring(i);
    }

    public static String decode(String string) {
        int i = 0;
        String result = "";

        if (string.equals("")) {
            return "";
        }

        while (string.startsWith("\\ ", i)) {
            result += " ";
            i += 2;
        }
        return result + string.substring(i);
    }
}
