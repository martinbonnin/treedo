package com.mbonnin.treedo;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.gms.drive.MetadataChangeSet;

import java.util.LinkedList;
import java.util.Stack;

/**
 * Created by martin on 8/25/16.
 */

public class ViewStack<T extends View> extends FrameLayout implements ValueAnimator.AnimatorUpdateListener {
    LinkedList<T> viewStack = new LinkedList<>();
    ValueAnimator animator = new ValueAnimator();
    ImageView imageView;
    private int mWidth;
    private int mHeight;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    public ViewStack(Context context) {
        super(context);
        imageView = new ImageView(context);

        FrameLayout.LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.TOP;
        /**
         * we might push Views with the soft keyboard open in which case the bitmap will not contain the whole view
         */
        imageView.setLayoutParams(layoutParams);
        imageView.setBackgroundColor(Color.WHITE);
        animator.addUpdateListener(this);
    }

    void ensureBitmap(int width, int height) {
        if (mWidth != width || mHeight != height || mBitmap == null) {
            if (mBitmap != null) {
                mBitmap.recycle();
            }
            mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
            mWidth = width;
            mHeight = height;
        }
    }

    public void pushView(T newView) {
        View oldView = viewStack.peek();
        viewStack.push(newView);

        removeAllViews();
        addView(newView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (oldView != null) {
            ensureBitmap(oldView.getWidth(), oldView.getHeight());
            oldView.draw(mCanvas);

            /**
             * we're adding the bitmap bellow the newView
             */
            imageView.setImageBitmap(mBitmap);
            imageView.setTranslationX(0.0f);
            addView(imageView, 0);

            animator.setFloatValues(1.0f, 0.0f);
            animator.setDuration(400);
            animator.start();
        }
    }

    public void popView() {
        View oldView = viewStack.peek();

        viewStack.pop();

        removeAllViews();
        View newView = viewStack.peek();
        if (newView != null) {
            addView(newView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        if (oldView != null) {
            ensureBitmap(oldView.getWidth(), oldView.getHeight());
            oldView.draw(mCanvas);

            /**
             * we're adding the bitmap on top of the newView
             */
            imageView.setImageBitmap(mBitmap);
            imageView.setTranslationX(0.0f);
            addView(imageView, 1);

            animator.setFloatValues(0.0f, 1.0f);
            animator.setDuration(300);
            animator.start();
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        float progress = (float)animation.getAnimatedValue();

        View view = getChildAt(getChildCount() - 1);

        view.setTranslationX(progress * getWidth());
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        animator.cancel();
        removeView(imageView);
    }

    public int size() {
        return viewStack.size();
    }

    public T peek() {
        return viewStack.peek();
    }

    public void clear() {
        viewStack.clear();
        removeAllViews();
    }
}
