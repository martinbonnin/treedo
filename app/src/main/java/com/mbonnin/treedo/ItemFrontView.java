package com.mbonnin.treedo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import static android.util.TypedValue.*;

/**
 * Created by martin on 18/08/14.
 */
public class ItemFrontView extends ViewGroup implements View.OnFocusChangeListener, ItemEditText.Listener {
    private Item mItem;
    public EditText mEditText;
    private ItemSpinner mSpinner;
    private ImageView mPlus;
    public CheckBox mCheckBox;
    private MeasureSpec mMeasureSpec;

    private Listener mListener;

    private int mFlags;
    private ImageView mArrow;
    private ImageView mDirectory;
    private int mSpinnerWidth;
    private int mHeight;
    private int mRemainingWidth;
    private int mCheckBoxWidth;
    private int mCheckBoxHeight;
    private boolean mArrowClicked;

    private final int PADDING = 2;
    private final int EDIT_TEXT_CORRECTION = 8;

    private int mEditTextHeight;
    private int mPadding;
    private int mEditTextCorrection;

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

        mPlus = new ImageView(getContext());
        mDirectory = new ImageView(getContext());
        mCheckBox = new CheckBox(getContext());
        mEditText = (ItemEditText)inflater.inflate(R.layout.item_edit_text, null);
        mSpinner = (ItemSpinner)inflater.inflate(R.layout.item_spinner, null);
        mArrow = new ImageView(getContext());


        mCheckBox.setTextColor(getResources().getColor(R.color.dark_gray));

        mEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        mEditText.setFocusable(true);
        mEditText.setBackground(null);
        mEditText.setInputType(EditorInfo.TYPE_TEXT_FLAG_IME_MULTI_LINE);
        mEditText.setSingleLine(false);
        mEditText.setTextColor(getResources().getColor(R.color.dark_gray));
        mEditText.setGravity(Gravity.CENTER_VERTICAL);
        mEditText.setHintTextColor(getResources().getColor(R.color.medium_gray));

        setWillNotDraw(false);
        setBackgroundColor(Color.WHITE);

        mMeasureSpec = new MeasureSpec();

        mPadding = toPixels(PADDING);
        mEditTextCorrection = toPixels(EDIT_TEXT_CORRECTION);

        mPlus.setImageResource(R.drawable.plus_gray);
        mDirectory.setImageResource(R.drawable.directory);
        mArrow.setImageResource(R.drawable.arrow);
        mArrow.setClickable(false);

        addView(mEditText);
        addView(mPlus);
        addView(mSpinner);
        addView(mArrow);
        addView(mDirectory);
        addView(mCheckBox);

        mCheckBox.setGravity(Gravity.CENTER);

        int widthSpec = mMeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int heightSpec = mMeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

        mCheckBox.measure(widthSpec, heightSpec);
        mCheckBoxWidth = mCheckBox.getMeasuredWidth();
        mCheckBoxHeight = mCheckBox.getMeasuredHeight();

        mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mItem.checked = isChecked;
            }
        });

        ((ItemEditText)mEditText).setListener(this);
        mEditText.setOnFocusChangeListener(this);

        mSpinner.setListener(new ItemSpinner.Listener() {
            @Override
            public void onSelectionChanged(boolean isFolder) {
                if (isFolder) {
                    mItem.isADirectory = true;
                } else {
                    mItem.isADirectory = false;
                }
                updateHint();

            }
        });
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

        if (mItem.isADirectory && ((flags & ItemView.FLAG_LAST) == 0)) {
            // directory
            mArrow.setVisibility(VISIBLE);
            mPlus.setVisibility(GONE);
            mCheckBox.setVisibility(INVISIBLE);
            mDirectory.setVisibility(VISIBLE);
        } else if ((flags & ItemView.FLAG_LAST) != 0){
            // plus item
            mArrow.setVisibility(GONE);
            mPlus.setVisibility(VISIBLE);
            mCheckBox.setVisibility(GONE);
            mDirectory.setVisibility(GONE);

        } else {
            // normal item
            mArrow.setVisibility(GONE);
            mPlus.setVisibility(GONE);
            mCheckBox.setVisibility(VISIBLE);
            mDirectory.setVisibility(GONE);
        }

        if (mItem.isADirectory) {
            mSpinner.setFolder(true);
        } else {
            mSpinner.setFolder(false);
        }

        mFlags = flags;

        updateHint();
        onFocusChange(mEditText, mEditText.isFocused());
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

    private int toPixels(int dp) {
        return (int)applyDimension(COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int childWidthMeasureSpec = mMeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int childHeightMeasureSpec = mMeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

        int width = 200;
        if (mMeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST
                || mMeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
            width = mMeasureSpec.getSize(widthMeasureSpec);
        }

        mSpinner.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        mSpinnerWidth = mSpinner.getMeasuredWidth();

        mRemainingWidth = width;

        mRemainingWidth -= mCheckBoxWidth;
        mRemainingWidth -= (mSpinner.getVisibility() == GONE) ? mCheckBoxWidth:mSpinnerWidth;

        childWidthMeasureSpec = mMeasureSpec.makeMeasureSpec(mRemainingWidth, MeasureSpec.AT_MOST);

        mEditText.measure(childWidthMeasureSpec, childHeightMeasureSpec);

        mEditTextHeight = mEditText.getMeasuredHeight() + mEditTextCorrection;

        mHeight = mCheckBoxHeight;
        if (mEditTextHeight > mHeight) {
            mHeight = mEditTextHeight;
        }
        /*if (mSpinner.getMeasuredHeight() > mHeight) {
            mHeight = mSpinner.getMeasuredHeight();
        }*/

        mHeight += 2 * mPadding;
        setMeasuredDimension(width, mHeight);
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int x = 0;
        int w = r -l;

        mPlus.layout(x, 0, mCheckBoxWidth, mHeight);
        mDirectory.layout(x, 0, mCheckBoxWidth, mHeight);
        mCheckBox.layout(x, 0, mCheckBoxWidth, mHeight);

        x += mCheckBoxWidth;
        mEditText.layout(x, mPadding + mEditTextCorrection, x + mRemainingWidth, mHeight - mPadding);

        mSpinner.layout(r - mSpinnerWidth, mPadding, r - mPadding, mHeight - 2*mPadding);
        mArrow.layout(r - mCheckBoxWidth, 0, r, mHeight);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (true) {
            Paint p = new Paint();
            p.setStyle(Paint.Style.FILL);
            /*p.setARGB(255, 255, 0, 0);
            canvas.drawRect(0, 0, mCheckBoxWidth, mHeight, p);
            p.setARGB(255, 0, 255, 0);
            canvas.drawRect(0, mPadding, canvas.getWidth(), mEditTextHeight, p);
            p.setARGB(255, 0, 0, 255);
            View view = mEditText;
            canvas.drawRect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom(), p);*/
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Utils.log("onInterceptTouchEvent" + ev.getActionMasked());
        if (mArrow.getVisibility() == VISIBLE) {
            if (ev.getActionMasked() == ev.ACTION_DOWN) {
                if (ev.getX() > mCheckBoxWidth + mEditText.getMeasuredWidth()) {
                    mArrowClicked = true;
                    setBackgroundColor(Color.argb(255, 220, 220, 220));
                    return true;
                } else {
                    mArrowClicked = false;
                }
            }
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == ev.ACTION_UP) {
            if (mArrowClicked) {
                if (ev.getX() > mCheckBoxWidth + mEditText.getMeasuredWidth()) {
                    mListener.onArrowClicked();
                }

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
