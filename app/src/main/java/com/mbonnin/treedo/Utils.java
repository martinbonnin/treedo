package com.mbonnin.treedo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.CheckBox;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static android.util.TypedValue.applyDimension;

/**
 * Created by martin on 16/10/14.
 */
public class Utils {
    private static final String TAG = "Tree-Do";
    private static boolean sDebuggable;
    private static DisplayMetrics sDisplayMetrics;
    private static Context mContext;

    public static synchronized void log(String message) {
        if (sDebuggable)
            Log.d(TAG, message);
    }

    public static void init(Context context, boolean debuggable) {
        sDebuggable = debuggable;
        sDisplayMetrics = context.getResources().getDisplayMetrics();

        mContext = context;
    }

    public static float toPixels(float dp) {
        return applyDimension(COMPLEX_UNIT_DIP, dp, sDisplayMetrics);
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

    public static Drawable getWhiteDrawable(Context context, int resId) {
        Drawable drawable = DrawableCompat.wrap(context.getResources().getDrawable(resId));
        DrawableCompat.setTint(drawable.mutate(), Color.WHITE);

        return drawable;
    }
}
