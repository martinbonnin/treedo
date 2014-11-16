package com.mbonnin.treedo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/**
 * Created by martin on 13/11/14.
 */
public class SpacerView extends View {

    private int mWidth;
    private int mHeight;

    public SpacerView(Context context) {
        super(context);
    }

    public void setFixedWidth(int width) {
        mWidth = width;
    }

    public void setFixedHeight(int height) {
        mHeight = height;
    }

    public int getFixedHeight() {
        return mHeight;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Paint p = new Paint();
        p.setStyle(Paint.Style.FILL);
        p.setARGB(255, 255, 255, 255);
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), p);
    }
}
