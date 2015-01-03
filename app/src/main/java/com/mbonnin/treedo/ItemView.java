package com.mbonnin.treedo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;

/**
 * Created by martin on 23/11/14.
 */
public class ItemView extends FrameLayout implements ItemEditText.Listener, AdapterView.OnItemSelectedListener, View.OnClickListener {
    private static final int SPINNER_POSITION_DIRECTORY = 1;
    private static final int SPINNER_POSITION_ITEM = 0;
    private ViewGroup mLeft;
    public ItemEditText mEditText;
    private ViewGroup mRight;
    private Listener mListener;
    private Item mItem;
    private ImageView mPlusImageView;
    private int mFlags = -1;
    private ImageView mDirectoryImageView;
    private CheckBox mCheckBox;
    private Spinner mSpinner;
    private View mContainer;
    private float mDownX;
    private float mLastScrollX;
    private boolean mSwiping;
    private float mScrollX;
    private int mScaledTouchSlop;
    private Button mCancelButton;
    private View mTrashIcons;
    private boolean mPressed;
    public static final int FLAG_LAST = 1;
    public static final int FLAG_SHOW_SPINNER = 2;
    public static final int FLAG_SHOW_HANDLE = 4;
    private int mLongPressTimeout;
    private Handler mHandler;
    private static int sId = 0;

    private Runnable mLongPressRunnable = new Runnable() {
        @Override
        public void run() {
            mEditText.requestFocus();
            InputMethodManager inputMethodManager=(InputMethodManager)(getContext().getSystemService(Context.INPUT_METHOD_SERVICE));
            inputMethodManager.showSoftInput(mEditText, 0);

            cancelPress();
        }
    };

    public ItemView(Context context) {
        super(context);
    }

    public ItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onNewItem(int position) {
        if (mListener != null) {
            mListener.onNewItem(position);
        }
        mEditText.clearFocus();
    }

    @Override
    public void onDeleteItem() {
        String text = mEditText.getText().toString();
        mListener.onDeleteItem(text);
    }

    private void updateHint() {
        if (mPlusImageView == null) {
            mEditText.setHint("");
            return;
        }

        if (mItem.isAFolder) {
            mEditText.setHint(R.string.new_folder);
        } else {
            mEditText.setHint(R.string.new_item);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position == 1) {
            mItem.isAFolder = true;
        } else {
            mItem.isAFolder = false;
        }
        updateHint();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onClick(View v) {
        removeView(mTrashIcons);
        mTrashIcons = null;
        mCancelButton = null;
        ViewPropertyAnimator animator = mContainer.animate();
        mContainer.setTranslationX(getWidth());
        animator.translationX(0).setDuration(100).start();
        Utils.log("cancel trash");

        if (mListener != null) {
            mListener.onCancelTrash();
        }
    }

    public String getText() {
        return mEditText.getText().toString();
    }

    public void setText(String text) {
        mItem.text = text;
        mEditText.setText(text);
    }

    public static interface Listener {
        public void onNewItem(int position);
        public void onDeleteItem(String remainingText);
        public void onArrowClicked();
        public void onTrashClicked();
        public void onCancelTrash();
        public void onGrabTouchEvent(MotionEvent ev);

    }

    private void init() {
        mLeft = (ViewGroup)findViewById(R.id.left);
        mRight = (ViewGroup)findViewById(R.id.right);
        mEditText = (ItemEditText)findViewById(R.id.edit_text);
        mContainer = findViewById(R.id.container);

        mEditText.setListener(this);
        //setWillNotDraw(false);

        mScaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        mLongPressTimeout = ViewConfiguration.get(getContext()).getLongPressTimeout();

        mHandler = new Handler();
        mEditText.setId(sId++);
    }

    private class CanDo {
        public boolean canClick;
        public boolean canSwipe;
        public boolean canGrab;
    }

    private CanDo whanCanGestureDo(MotionEvent ev) {
        CanDo c = new CanDo();
        c.canClick = true;
        c.canSwipe = true;
        c.canGrab = true;

        if (!mItem.isAFolder) {
            c.canClick = false;
            c.canSwipe = false;
        }
        if (mTrashIcons != null) {
            c.canClick = false;
            c.canSwipe = false;
        }
        if ((mFlags & FLAG_LAST) != 0) {
            c.canClick = false;
            c.canSwipe = false;
        }
        if (mEditText.hasFocus()) {
            c.canClick = false;
            c.canSwipe = false;
        }
        if (mItem.isTrash) {
            c.canSwipe = false;
        }

        if ((mFlags & FLAG_SHOW_HANDLE) != 0) {
            c.canGrab = true;
        } else {
            c.canGrab = false;
        }
        return c;
    }

    private void cancelPress() {
        mContainer.setPressed(false);
        mPressed = false;
        if (mHandler != null) {
            mHandler.removeCallbacks(mLongPressRunnable);
        }
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
        mCheckBox = null;
        mSpinner = null;

        if (mItem.isAFolder && ((flags & FLAG_LAST) == 0)) {
            // directory
            ImageView rightImageView = new ImageView(getContext());
            mDirectoryImageView = new ImageView(getContext());

            if ((flags & FLAG_SHOW_HANDLE) != 0) {
                rightImageView.setImageDrawable(new BitmapDrawable(getResources(), Utils.getBitmap(getContext(), R.drawable.handle)));
            } else {
                rightImageView.setImageDrawable(new BitmapDrawable(getResources(), Utils.getBitmap(getContext(), R.drawable.arrow)));
            }
            if (mItem.isTrash) {
                mDirectoryImageView.setImageDrawable(new BitmapDrawable(getResources(), Utils.getBitmap(getContext(), R.drawable.trash)));
            } else {
                mDirectoryImageView.setImageDrawable(new BitmapDrawable(getResources(), Utils.getBitmap(getContext(), R.drawable.folder)));
            }

            LayoutParams layoutParams = new LayoutParams(Utils.getCheckBoxHeight(), Utils.getCheckBoxHeight());

            mLeft.addView(mDirectoryImageView, layoutParams);
            mRight.addView(rightImageView, layoutParams);

        } else if ((flags & FLAG_LAST) != 0){
            // plus item
            mPlusImageView = new ImageView(getContext());
            mPlusImageView.setImageDrawable(new BitmapDrawable(getResources(), Utils.getBitmap(getContext(), R.drawable.plus_gray)));

            LayoutParams layoutParams = new LayoutParams(Utils.getCheckBoxHeight(), Utils.getCheckBoxHeight());
            mLeft.addView(mPlusImageView, layoutParams);
        } else {
            mCheckBox = new CheckBox(getContext());
            LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mLeft.addView(mCheckBox, layoutParams);
            mCheckBox.setChecked(mItem.checked);

            if ((flags & FLAG_SHOW_HANDLE) != 0) {
                layoutParams = new LayoutParams(Utils.getCheckBoxHeight(), Utils.getCheckBoxHeight());
                ImageView rightImageView = new ImageView(getContext());
                rightImageView.setImageDrawable(new BitmapDrawable(getResources(), Utils.getBitmap(getContext(), R.drawable.handle)));
                mRight.addView(rightImageView, layoutParams);
            }
        }

        if ((flags & FLAG_SHOW_SPINNER) != 0) {
            LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mSpinner = new Spinner(getContext());
            mSpinner.setAdapter(new TypeSpinnerAdapter(getContext()));
            if (mItem.isAFolder) {
                mSpinner.setSelection(SPINNER_POSITION_DIRECTORY);
            } else {
                mSpinner.setSelection(SPINNER_POSITION_ITEM);
            }
            mSpinner.setOnItemSelectedListener(this);
            mRight.addView(mSpinner, layoutParams);
        }

        mFlags = flags;

        updateHint();
        updateSwipableState();
    }

    private void updateSwipableState() {

    }

    public void setItem(Item item) {
        mItem = item;
        mEditText.setText(mItem.text);

        setFlags(0);
    }

    public Item getItem() {
        mItem.text = mEditText.getText().toString();
        if (mCheckBox != null) {
            mItem.checked = mCheckBox.isChecked();
        }
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
        CanDo c = whanCanGestureDo(ev);
        if (!c.canSwipe && !c.canClick && !c.canGrab) {
            return false;
        }

        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        CanDo c = whanCanGestureDo(ev);
        if (!c.canSwipe && !c.canClick && !c.canGrab) {
            return false;
        }

        if ((mFlags & FLAG_SHOW_HANDLE) != 0) {
            Rect r = new Rect();
            mRight.getHitRect(r);
            int x = (int)ev.getX();
            int y = (int)ev.getY();
            if (ev.getActionMasked() == ev.ACTION_DOWN) {
                if (!r.contains(x, y)) {
                    return true;
                }
            }
            mListener.onGrabTouchEvent(ev);
            return true;
        }

        if (ev.getActionMasked() == ev.ACTION_DOWN) {
            mDownX = ev.getX();
            mLastScrollX = mContainer.getTranslationX();
            mSwiping = false;
            mContainer.setPressed(true);
            mPressed = true;
            Utils.log("down");
            mHandler.postDelayed(mLongPressRunnable, mLongPressTimeout);
        } else if (ev.getActionMasked() == ev.ACTION_MOVE) {
            if (mSwiping == false) {
                if (Math.abs(ev.getRawX() - mDownX) > mScaledTouchSlop) {
                    if (c.canSwipe) {
                        Utils.log("start swipe");
                        mSwiping = true;
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    cancelPress();
                }
            } else {
                mScrollX = mLastScrollX + ev.getX() - mDownX;
                Utils.log("scroll: " + mScrollX);
                mContainer.setTranslationX(mScrollX);
            }
        } else if (ev.getActionMasked() == ev.ACTION_UP || ev.getActionMasked() == ev.ACTION_CANCEL) {
            if (mSwiping) {
                final ViewPropertyAnimator animator = mContainer.animate();
                float target = 0;
                if (mScrollX > getWidth() / 3) {
                    target = getWidth();
                } else if (mScrollX < -getWidth() / 3) {
                    target = -getWidth();
                } else {
                    target = 0;
                }
                if (ev.getActionMasked() == ev.ACTION_CANCEL) {
                    Utils.log("Canceled");
                    target = 0;
                }

                Utils.log("animate to " + target);

                if (target != 0) {
                    animator.setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            if (mListener != null) {
                                Utils.log("trash clicked");

                                mListener.onTrashClicked();
                                mTrashIcons = LayoutInflater.from(getContext()).inflate(R.layout.trash_icons, null);
                                mCancelButton = (Button) mTrashIcons.findViewById(R.id.cancel_button);
                                mCancelButton.setOnClickListener(ItemView.this);
                                addView(mTrashIcons, 0);
                                animator.setListener(null);
                            }

                        }
                    });
                }
                animator.translationX(target).setDuration(100).start();
                mSwiping = false;
                //getParent().requestDisallowInterceptTouchEvent(fals);
            } else {
                if (mPressed){
                    cancelPress();
                    if (ev.getActionMasked() == ev.ACTION_UP) {
                        if (mListener != null) {
                            mListener.onArrowClicked();
                        }
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
