package com.mbonnin.treedo;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
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
    private static int mCheckBoxWidth;
    private static int mCheckBoxHeight;
    private static SparseArray<Bitmap> sCachedBitmaps = new SparseArray<Bitmap>();
    private static Context mContext;

    public static synchronized void log(String message) {
        if (sDebuggable)
            Log.d(TAG, message);
    }

    public static void init(Context context, boolean debuggable) {
        sDebuggable = debuggable;
        sDisplayMetrics = context.getResources().getDisplayMetrics();

        int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

        CheckBox checkBox = new CheckBox(context);
        checkBox.measure(widthSpec, heightSpec);
        mCheckBoxWidth = checkBox.getMeasuredWidth();
        mCheckBoxHeight = checkBox.getMeasuredHeight();

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
}
