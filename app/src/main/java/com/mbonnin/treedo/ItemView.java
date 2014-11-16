package com.mbonnin.treedo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static android.util.TypedValue.applyDimension;

/**
 * Created by martin on 12/11/14.
 */
public class ItemView extends SwipableView implements View.OnClickListener, ItemFrontView.Listener {
    private final ItemBackView mBack;
    private final ItemFrontView mFront;
    public static final int FLAG_LAST = 0x01;
    public static final int FLAG_SHOW_SPINNER = 0x02;
    private Listener mListener;

    @Override
    public void onNewItem(String text) {
        if (mListener != null) {
            mListener.onNewItem(text);
        }
    }

    private void updateSwipableState() {
        Item item = mFront.getItem();
        if (item == null) {
            setSwipable(false);
            return;
        }
        if (!item.isADirectory) {
            setSwipable(false);
            return;
        }

        if ((mFront.getFlags() & ItemView.FLAG_LAST) != 0) {
            setSwipable(false);
            return;
        }

        setSwipable(true);
    }

    @Override
    public void onDeleteItem(String remainingText) {
        if (mListener != null) {
            mListener.onDeleteItem(remainingText);
        }
    }

    @Override
    public void onArrowClicked() {
        if (mListener != null) {
            mListener.onArrowClicked();
        }
    }

    public static interface Listener {
        public void onNewItem(String text);
        public void onDeleteItem(String remainingText);
        public void onArrowClicked();
        public void onTrashClicked();
    }

    public ItemView(Context context) {
        super(context);
        mBack = new ItemBackView(context);
        mFront = new ItemFrontView(context);
        mBack.mImageView.setOnClickListener(this);
        super.setViews(mBack, mFront);
    }

    private int toPixels(int dp) {
        return (int)applyDimension(COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setSwipeOffset(mBack.getSwipeOffset());
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (true /*(mFront.getFlags() & FLAG_LAST) == 0*/) {
            Paint p = new Paint();
            p.setStyle(Paint.Style.FILL);
            p.setColor(getResources().getColor(R.color.light_gray));
            canvas.drawRect(0, getMeasuredHeight() - toPixels(1), getMeasuredWidth(), getMeasuredHeight(), p);
        }
    }

    public void setFlags(int flags) {
        mFront.setFlags(flags);
        updateSwipableState();
    }

    public void setItem(Item item) {
        mFront.setItem(item);
        updateSwipableState();
    }

    public Item getItem() {
        return mFront.getItem();
    }

    public void appendAndFocus(String remainingText, boolean b) {
        mFront.appendAndFocus(remainingText, b);
    }

    public int getFlags() {
        return mFront.getFlags();
    }

    public void setListener(Listener listener) {
        mListener = listener;
        mFront.setListener(this);
    }

    @Override
    public void onClick(View v) {
        if (mFront.getTranslationX() > 0){
            if (mListener != null) {
                mListener.onTrashClicked();
            }
        }
    }
}
