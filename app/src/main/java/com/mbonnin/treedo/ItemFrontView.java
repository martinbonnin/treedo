package com.mbonnin.treedo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;

/**
 * Created by martin on 18/08/14.
 */
public class ItemFrontView extends ViewGroup implements View.OnFocusChangeListener, ItemEditText.Listener {
    private Item mItem;
    public ItemEditText mEditText;
    private Spinner mSpinner;
    private ImageView mPlusImageView;
    public CheckBox mCheckBox;
    private View mLeftView;
    private View mRightView;

    private Listener mListener;

    int mRightWidthMeasureSpec;
    int mRightHeightMeasureSpec;

    private int mFlags;
    private ImageView mArrowImageView;
    private ImageView mDirectoryImageView;
    private int mSpinnerWidth;
    private int mHeight;
    private int mRemainingWidth;
    private boolean mArrowClicked;

    private int mEditTextHeight;
    private int mPaddingTop;
    private int mPaddingBottom;
    private int mPaddingLeft;
    private int mPaddingRight;

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

    public static interface Listener {
        public void onNewItem(String text);
        public void onDeleteItem(String remainingText);
        public void onArrowClicked();
    }

    public ItemFrontView(Context context) {
        super(context);
        init();
    }

    public ItemFrontView(Context context, AttributeSet attr) {
        super(context, attr);
        init();
    }

    public ItemFrontView(Context context, AttributeSet attr, int style) {
        super(context, attr, style);
        init();
    }

    private void init() {
        LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mEditText = (ItemEditText)inflater.inflate(R.layout.item_edit_text, null);

        mEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        mEditText.setFocusable(true);
        mEditText.setBackground(null);
        mEditText.setPadding(0, 0, 0, 0);
        mEditText.setInputType(EditorInfo.TYPE_TEXT_FLAG_IME_MULTI_LINE);
        mEditText.setSingleLine(false);
        mEditText.setTextColor(getResources().getColor(R.color.dark_gray));
        mEditText.setGravity(Gravity.CENTER_VERTICAL);
        mEditText.setHintTextColor(getResources().getColor(R.color.medium_gray));

        setWillNotDraw(false);
        setBackgroundColor(Color.WHITE);

        mPaddingTop = Utils.toPixels(2);
        mPaddingBottom = Utils.toPixels(2);
        mPaddingLeft = Utils.toPixels(8);
        mPaddingRight = Utils.toPixels(2);

        addView(mEditText);

        mEditText.setListener(this);
        mEditText.setOnFocusChangeListener(this);

        /*mSpinner.setListener(new ItemSpinner.Listener() {
            @Override
            public void onSelectionChanged(boolean isFolder) {
                if (isFolder) {
                    mItem.isADirectory = true;
                } else {
                    mItem.isADirectory = false;
                }
                updateHint();

            }
        });*/
    }

    private void updateHint() {
        if ((mFlags & ItemView.FLAG_LAST) == 0) {
            mEditText.setHint("");
            return;
        }

        if (mItem.isADirectory) {
            mEditText.setHint(R.string.new_directory);
        } else {
            mEditText.setHint(R.string.new_item);
        }
    }

    public int getFlags() {
        return mFlags;
    }

    public void setFlags(int flags) {
        mSpinner.setVisibility(GONE);

        if (mLeftView != null) {
            removeView(mLeftView);
            mLeftView = null;
        }
        if (mRightView != null) {
            removeView(mRightView);
            mRightView = null;
        }

        if (mItem.isADirectory && ((flags & ItemView.FLAG_LAST) == 0)) {
            // directory
            mArrowImageView = new ImageView(getContext());
            mDirectoryImageView = new ImageView(getContext());

            mArrowImageView.setImageDrawable(new BitmapDrawable(getResources(), Utils.getBitmap(getContext(), R.drawable.arrow)));
            mDirectoryImageView.setImageDrawable(new BitmapDrawable(getResources(), Utils.getBitmap(getContext(), R.drawable.directory)));

            mLeftView = mDirectoryImageView;
            mRightView = mArrowImageView;

            mRightWidthMeasureSpec = MeasureSpec.makeMeasureSpec(Utils.getCheckBoxHeight(), MeasureSpec.EXACTLY);
            mRightHeightMeasureSpec = MeasureSpec.makeMeasureSpec(Utils.getCheckBoxHeight(), MeasureSpec.EXACTLY);
        } else if ((flags & ItemView.FLAG_LAST) != 0){
            // plus item
            mPlusImageView = new ImageView(getContext());
            mPlusImageView.setImageDrawable(new BitmapDrawable(getResources(), Utils.getBitmap(getContext(), R.drawable.plus_gray)));

            mLeftView = mPlusImageView;

            mSpinner = new Spinner(getContext());
            //mSpinner.setOnItemSelectedListener(this);
            mRightView = mSpinner;
            mRightWidthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            mRightHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

            onFocusChange(mEditText, mEditText.isFocused());
        } else {
            mCheckBox = new CheckBox(getContext());
            mLeftView = mCheckBox;
            mCheckBox.setChecked(mItem.checked);
        }

        mFlags = flags;

        if (mLeftView != null){
            addView(mLeftView);
        }
        if (mRightView != null) {
            addView(mRightView);
        }
        updateHint();
    }

    public void setItem(Item item) {
        mItem = item;
        mEditText.setText(item.text);
        mCheckBox.setChecked(item.checked);

        setFlags(0);
    }

    public Item getItem() {
        mItem.text = mEditText.getText().toString();
        mItem.checked = mCheckBox.isChecked();
        return mItem;
    }

    @Override
    public void onFocusChange(View view, boolean focused) {
        if (focused) {
            if ((mFlags & ItemView.FLAG_SHOW_SPINNER) != 0) {
                mSpinner.setVisibility(VISIBLE);
            }
        } else {
            if ((mFlags & ItemView.FLAG_SHOW_SPINNER) != 0) {
                mSpinner.setVisibility(GONE);
            }
        }
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 200;
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST
                || MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
            width = MeasureSpec.getSize(widthMeasureSpec);
        }


        mRemainingWidth = width - mPaddingLeft - mPaddingRight;

        int checkBoxHeight = Utils.getCheckBoxHeight();

        mRemainingWidth -= checkBoxHeight;
        if (mRightView != null) {
            mRightView.measure(mRightWidthMeasureSpec, mRightHeightMeasureSpec);
            mRemainingWidth -= mRightView.getMeasuredWidth();
        }

        int wms = MeasureSpec.makeMeasureSpec(mRemainingWidth, MeasureSpec.AT_MOST);
        int hms = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

        mEditText.measure(wms, hms);

        mEditTextHeight = mEditText.getMeasuredHeight();

        mHeight = checkBoxHeight;
        if (mEditTextHeight > mHeight) {
            mHeight = mEditTextHeight;
        }
        if (mRightView != null && mRightView.getMeasuredHeight() > mHeight) {
            mHeight = mRightView.getMeasuredHeight();
        }

        mHeight += mPaddingTop + mPaddingBottom;
        setMeasuredDimension(width, mHeight);
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int x = 0;
        int w = r - l;
        int h = b - t;

        if (mLeftView != null) {
            //mLeftView.layout(mPaddingLeft);
        }

        /*mPlusImageView.layout(x, 0, mCheckBoxWidth, mHeight);
        mDirectoryImageView.layout(x, 0, mCheckBoxWidth, mHeight);
        mCheckBox.layout(x, 0, mCheckBoxWidth, mHeight);

        x += mCheckBoxWidth;
        int y = (mHeight - mEditTextHeight) / 2;
        mEditText.layout(x, y, x + mRemainingWidth, y + mEditTextHeight);

        mSpinner.layout(r - mSpinnerWidth, mPaddingTop, r - mPaddingTop, mHeight - 2* mPaddingTop);
        mArrowImageView.layout(r - mCheckBoxWidth, 0, r, mHeight);*/
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (true) {
            Paint p = new Paint();
            p.setStyle(Paint.Style.FILL);
            /*p.setARGB(255, 255, 0, 0);
            canvas.drawRect(0, 0, mCheckBoxWidth, mHeight, p);
            p.setARGB(255, 0, 255, 0);
            canvas.drawRect(0, mPaddingTop, canvas.getWidth(), mEditTextHeight, p);*/
            p.setARGB(255, 0, 0, 255);
            View view = mEditText;
            canvas.drawRect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom(), p);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Utils.log("onInterceptTouchEvent" + ev.getActionMasked());
        if (mArrowImageView.getVisibility() == VISIBLE) {
            if (ev.getActionMasked() == ev.ACTION_DOWN) {
                /*if (ev.getX() > mCheckBoxWidth + mEditText.getMeasuredWidth()) {
                    mArrowClicked = true;
                    setBackgroundColor(Color.argb(255, 220, 220, 220));
                    return true;
                } else {
                    mArrowClicked = false;
                }*/
            }
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == ev.ACTION_UP) {
            if (mArrowClicked) {
                /*if (ev.getX() > mCheckBoxWidth + mEditText.getMeasuredWidth()) {
                    mListener.onArrowClicked();
                }*/

                setBackgroundColor(Color.WHITE);
                mArrowClicked = false;
                return true;
            }


        } else if (ev.getActionMasked() == ev.ACTION_CANCEL) {
            mArrowClicked = false;
            setBackgroundColor(Color.WHITE);
        }

        return true;
    }
}
