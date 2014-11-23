package com.mbonnin.treedo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;

/**
 * Created by martin on 23/11/14.
 */
public class ItemView2 extends LinearLayout implements ItemEditText.Listener, View.OnFocusChangeListener, AdapterView.OnItemSelectedListener {
    private ViewGroup mLeft;
    private ItemEditText mEditText;
    private ViewGroup mRight;
    private Listener mListener;
    private Item mItem;
    private ImageView mPlusImageView;
    private int mFlags = -1;
    private ImageView mDirectoryImageView;
    private CheckBox mCheckBox;
    private Spinner mSpinner;
    private ImageView mArrowImageView;
    private View mContainer;
    private float mDownX;
    private float mLastScrollX;
    private boolean mSwiping;
    private float mScrollX;
    private int mScaledTouchSlop;

    public ItemView2(Context context) {
        super(context);
    }

    public ItemView2(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ItemView2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onNewItem(String text) {
        if (mListener != null) {
            mListener.onNewItem(text);
        }
        mEditText.clearFocus();
    }

    @Override
    public void onDeleteItem() {
        String text = mEditText.getText().toString();
        mListener.onDeleteItem(text);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {

    }

    private void updateHint() {
        if (mPlusImageView == null) {
            mEditText.setHint("");
            return;
        }

        if (mItem.isADirectory) {
            mEditText.setHint(R.string.new_directory);
        } else {
            mEditText.setHint(R.string.new_item);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position == 0) {
            mItem.isADirectory = true;
        } else {
            mItem.isADirectory = false;
        }
        updateHint();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public static interface Listener {
        public void onNewItem(String text);
        public void onDeleteItem(String remainingText);
        public void onArrowClicked();
        public void onTrashClicked();
        public void onCancelTrash();

    }

    private void init() {
        mLeft = (ViewGroup)findViewById(R.id.left);
        mRight = (ViewGroup)findViewById(R.id.right);
        mEditText = (ItemEditText)findViewById(R.id.edit_text);
        mContainer = findViewById(R.id.container);

        mEditText.setListener(this);
        mEditText.setOnFocusChangeListener(this);
        setWillNotDraw(false);

        mScaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        init();
    }

    public void setFlags(int flags) {
        if (mFlags == flags) {
            return;
        }

        mLeft.removeAllViews();
        mRight.removeAllViews();
        mPlusImageView = null;
        mDirectoryImageView = null;
        mArrowImageView = null;
        mCheckBox = null;
        mSpinner = null;

        if (mItem.isADirectory && ((flags & ItemView.FLAG_LAST) == 0)) {
            // directory
            mArrowImageView = new ImageView(getContext());
            mDirectoryImageView = new ImageView(getContext());

            mArrowImageView.setImageDrawable(new BitmapDrawable(getResources(), Utils.getBitmap(getContext(), R.drawable.arrow)));
            mDirectoryImageView.setImageDrawable(new BitmapDrawable(getResources(), Utils.getBitmap(getContext(), R.drawable.directory)));

            LayoutParams layoutParams = new LayoutParams(Utils.getCheckBoxHeight(), Utils.getCheckBoxHeight());

            mLeft.addView(mDirectoryImageView, layoutParams);
            mRight.addView(mArrowImageView, layoutParams);

        } else if ((flags & ItemView.FLAG_LAST) != 0){
            // plus item
            mPlusImageView = new ImageView(getContext());
            mPlusImageView.setImageDrawable(new BitmapDrawable(getResources(), Utils.getBitmap(getContext(), R.drawable.plus_gray)));

            LayoutParams layoutParams = new LayoutParams(Utils.getCheckBoxHeight(), Utils.getCheckBoxHeight());
            mLeft.addView(mPlusImageView, layoutParams);

            layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mSpinner = new Spinner(getContext());
            mSpinner.setOnItemSelectedListener(this);
            mRight.addView(mSpinner, layoutParams);
        } else {
            mCheckBox = new CheckBox(getContext());
            LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mLeft.addView(mCheckBox, layoutParams);
            mCheckBox.setChecked(mItem.checked);
        }

        mFlags = flags;

        updateHint();
        updateSwipableState();

        onFocusChange(mEditText, mEditText.isFocused());
    }

    private void updateSwipableState() {

    }

    public void setItem(Item item) {
        mItem = item;
        mEditText.setText(mItem.text);

        setFlags(0);
    }

    public Item getItem() {
        return mItem;
    }

    public void appendAndFocus(String remainingText, boolean focusAtStart) {
        int focusIndex = mItem.text.length();
        mItem.text += remainingText;
        mEditText.setText(mItem.text);
        mEditText.requestFocus();
        if (!focusAtStart) {
            focusIndex = mItem.text.length();
        }
        mEditText.setSelection(focusIndex);
    }

    public int getFlags() {
        return mFlags;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!mItem.isADirectory || (mFlags & ItemView.FLAG_LAST) != 0) {
            return false;
        }

        if (ev.getActionMasked() == ev.ACTION_DOWN) {
            mDownX = ev.getRawX();
            mLastScrollX = mContainer.getTranslationX();
            mSwiping = false;
            Utils.log("down");
            setPressed(true);
            return true;
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!mItem.isADirectory || (mFlags & ItemView.FLAG_LAST) != 0) {
            return false;
        }

        if (ev.getActionMasked() == ev.ACTION_MOVE) {
            if (mSwiping == false) {
                if (Math.abs(ev.getRawX() - mDownX) > mScaledTouchSlop) {
                    Utils.log("start swipe");
                    mSwiping = true;
                    setPressed(false);
                }
            } else {
                mScrollX = mLastScrollX + ev.getRawX() - mDownX;
                Utils.log("scroll: " + mScrollX);
                mContainer.setTranslationX(mScrollX);
            }
        } else if (ev.getActionMasked() == ev.ACTION_UP || ev.getActionMasked() == ev.ACTION_CANCEL) {
            if (mSwiping) {
                ViewPropertyAnimator animator = mContainer.animate();
                float target = 0;
                if (mScrollX > getWidth()/3) {
                    target = getWidth();
                } else if (mScrollX < -getWidth()/3) {
                    target = -getWidth();
                } else {
                    target = 0;
                }
                if (ev.getActionMasked() == ev.ACTION_CANCEL) {
                    Utils.log("Canceled");
                    target = 0;
                }

                animator.setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                    }
                });
                animator.translationX(target).setDuration(100).start();
                mSwiping = false;
            } else {
                setPressed(false);
                if (ev.getActionMasked() == ev.ACTION_UP) {
                    if (mListener != null) {
                        mListener.onArrowClicked();
                    }
                }
            }
        }

        return true;
    }

    public void recycle() {
        mFlags = -1;
    }
}
