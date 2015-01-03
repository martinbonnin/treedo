package com.mbonnin.treedo;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

/**
 * Created by martin on 15/11/14.
 */
public class ProgressBar extends View implements ValueAnimator.AnimatorUpdateListener {
    private final Paint mPainter;
    float mOffset;
    ValueAnimator mValueAnimator;

    public ProgressBar(Context context) {
        super(context);

        setBackgroundColor(getResources().getColor(R.color.vibrant_100));
        mPainter = new Paint();
        mPainter.setStyle(Paint.Style.FILL);
        mPainter.setColor(getResources().getColor(R.color.vibrant_200));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int d = Utils.toPixels(4);
        int width = d;
        MeasureSpec measureSpec = new MeasureSpec();

        int mode = measureSpec.getMode(widthMeasureSpec);
        int w = measureSpec.getSize(widthMeasureSpec);
        if (mode == MeasureSpec.AT_MOST
                || mode == MeasureSpec.EXACTLY) {
            width = w;
        }

        setMeasuredDimension(width, d);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        restartAnimation();
    }

    private void restartAnimation() {
        mValueAnimator = new ValueAnimator().ofFloat(0.0f, 1.0f);
        mValueAnimator.addUpdateListener(this);
        mValueAnimator.setInterpolator(new AccelerateInterpolator());
        mValueAnimator.setRepeatCount(mValueAnimator.INFINITE);
        mValueAnimator.setDuration(1000).start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = canvas.getWidth();
        int start;
        if (mOffset < 0.5) {
            start = (int)(width * 2 * mOffset * mOffset );
        } else {
            float x = mOffset - 0.5f;
            start = (int)(width * (0.5 + 2.0 * x - 2 * x * x));
        }
        int end = (int)(width * (2.0 * mOffset - mOffset * mOffset * mOffset * mOffset));

        canvas.drawRect(start, 0, end, canvas.getHeight(), mPainter);
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animator) {
        mOffset = (Float)animator.getAnimatedValue();
        invalidate();
    }
}
