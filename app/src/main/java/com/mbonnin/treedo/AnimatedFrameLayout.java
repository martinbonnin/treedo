package com.mbonnin.treedo;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class AnimatedFrameLayout<T extends View> extends FrameLayout implements ValueAnimator.AnimatorUpdateListener {
    public static final int ANIMATE_NONE = 0;
    public static final int ANIMATE_ENTER = 1;
    public static final int ANIMATE_EXIT = 2;

    private final Paint mPaint;
    ValueAnimator animator = new ValueAnimator();
    ImageView imageView;
    private int mWidth;
    private int mHeight;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private float mEndValue;
    private float mStartValue;

    public AnimatedFrameLayout(Context context) {
        super(context);
        imageView = new ImageView(context);

        FrameLayout.LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.TOP;
        /**
         * we might push Views with the soft keyboard open in which case the bitmap will not contain the whole view
         */
        imageView.setLayoutParams(layoutParams);
        animator.addUpdateListener(this);

        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.WHITE);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        float value = (float)animation.getAnimatedValue();

        View view = getChildAt(getChildCount() - 1);

        view.setTranslationX(value);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        animator.cancel();
        removeView(imageView);
    }

    public void setView(MainView view, int animate) {
        int width = getWidth();
        int height = getHeight();

        if (width > 0 && height > 0) {
            Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(newBitmap);
            mWidth = width;
            mHeight = height;

            /**
             * we might be partially transparent so we need to clear the canvas first
             */
            mCanvas.drawRect(0, 0, mCanvas.getWidth(), mCanvas.getHeight(), mPaint);
            draw(mCanvas);

            if (mBitmap != null) {
                mBitmap.recycle();
            }
            mBitmap = newBitmap;
        } else {
            /**
             * nothing to draw...
             */
            if (mBitmap != null) {
                mBitmap = null;
                mBitmap.recycle();
            }
        }

        removeAllViews();
        addView(view, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (mBitmap == null || animate == ANIMATE_NONE) {
            return;
        }

        imageView.setImageBitmap(mBitmap);

        switch (animate) {
            case ANIMATE_ENTER:
                /**
                 * we're adding the bitmap below the new View
                 */
                addView(imageView, 0);
                imageView.setTranslationX(0);
                mStartValue = width;
                mEndValue = 0.0f;
                break;
            case ANIMATE_EXIT:
                /**
                 * we're adding the bitmap above the new View
                 */
                addView(imageView, 1);
                view.setTranslationX(0);
                mStartValue = 0.0f;
                mEndValue = width;
                break;
        }

        animator.setFloatValues(mStartValue, mEndValue);
        animator.setDuration(150);
        animator.start();
    }
}
