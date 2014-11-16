package com.mbonnin.treedo;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static android.util.TypedValue.applyDimension;

/**
 * Created by martin on 12/11/14.
 */
public class ItemBackView extends LinearLayout {
    private final int trashSize = 21;
    public ImageView mImageView;

    private int toPixels(int dp) {
        return (int)applyDimension(COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    public ItemBackView(Context context) {
        super(context);
        setBackgroundColor(getResources().getColor(R.color.light_gray));

        LinearLayout.LayoutParams layoutParams;

        mImageView = new ImageView(context);
        mImageView.setImageResource(R.drawable.trash);
        setGravity(Gravity.CENTER_VERTICAL);

        layoutParams = new LinearLayout.LayoutParams(
                toPixels(trashSize), toPixels(trashSize));
        layoutParams.leftMargin = toPixels(trashSize);
        layoutParams.rightMargin = toPixels(trashSize);
        addView(mImageView, layoutParams);

        /*layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.leftMargin = 0;
        layoutParams.topMargin = 0;
        View space = new View(context);
        space.setBackgroundColor(Color.WHITE);
        addView(space, layoutParams);

        setWillNotDraw(false);*/
    }

    public int getSwipeOffset() {
        return toPixels(trashSize*3);
    }
}
