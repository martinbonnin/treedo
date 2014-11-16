package com.mbonnin.treedo;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;

/**
 * Created by martin on 12/11/14.
 */
public class SwipableView extends ViewGroup {
    private View mBack;
    private View mFront;
    private int mScaledTouchSlop;
    private float mDownX;
    private boolean mSwiping;
    private float mScrollX;
    private float mLastScrollX;
    private int mSwipeOffset = 0;
    private MeasureSpec mMeasureSpec;
    private boolean mSwipable;

    public void setSwipeOffset(int offset) {
        mSwipeOffset = offset;
    }

    public void setSwipable(boolean swipable) {
        mSwipable = swipable;
    }

    public boolean isSwipable() {
        return mSwipable;
    }

    public SwipableView(Context context) {
        super(context);
        mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setWillNotDraw(false);
        mMeasureSpec = new MeasureSpec();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!mSwipable) {
            return false;
        }

        if (ev.getActionMasked() == ev.ACTION_DOWN) {
            mDownX = ev.getRawX();
            mLastScrollX = mFront.getTranslationX();
            mSwiping = false;
        } else if (ev.getActionMasked() == ev.ACTION_MOVE) {
            if (mSwiping == false) {
                if (Math.abs(ev.getRawX() - mDownX) > mScaledTouchSlop) {
                    mSwiping = true;
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mSwiping == false) {
            return false;
        }

        if (ev.getActionMasked() == ev.ACTION_MOVE) {
            mScrollX = mLastScrollX + ev.getRawX() - mDownX;
            if (mScrollX < 0) {
                mScrollX = 0;
            }
            mFront.setTranslationX(mScrollX);
        } else if (ev.getActionMasked() == ev.ACTION_UP || ev.getActionMasked() == ev.ACTION_CANCEL) {
            ViewPropertyAnimator animator = mFront.animate();
            float target = 0;
            if (ev.getRawX() > mDownX) {
                target = mSwipeOffset;
            } else {
                target = 0;
            }

            animator.translationX(target).setDuration(100).start();
            mSwiping = false;
        }
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mFront.measure(widthMeasureSpec, heightMeasureSpec);
        widthMeasureSpec = mMeasureSpec.makeMeasureSpec(mFront.getMeasuredWidth(), MeasureSpec.EXACTLY);
        heightMeasureSpec = mMeasureSpec.makeMeasureSpec(mFront.getMeasuredHeight(), MeasureSpec.EXACTLY);
        mBack.measure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(mFront.getMeasuredWidth(), mFront.getMeasuredHeight());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mBack.layout(0, 0, r - l, b - t);
        mFront.layout(0, 0, r - l, b - t);
    }

    public void setViews(View back, View front) {
        removeAllViews();
        mBack = back;
        mFront = front;
        addView(mBack);
        addView(mFront);
    }

    public void cancelSwipe() {
        ViewPropertyAnimator animator = mFront.animate();
        animator.translationX(0).setDuration(100).start();
        mSwiping = false;
    }
}
