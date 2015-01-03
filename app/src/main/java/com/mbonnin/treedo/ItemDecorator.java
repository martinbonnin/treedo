package com.mbonnin.treedo;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by martin on 1/3/15.
 */
public class ItemDecorator extends RecyclerView.ItemDecoration {
    public void onDrawOver (Canvas c, RecyclerView parent, RecyclerView.State state) {
        Paint p = new Paint();
        p.setColor(parent.getResources().getColor(R.color.black_26));
        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            c.drawRect(0, child.getBottom() - Utils.toPixels(1f), c.getWidth(), child.getBottom(), p);
        }
    }
}
