package com.mbonnin.treedo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static android.util.TypedValue.applyDimension;

/**
 * Created by martin on 15/11/14.
 */
public class ItemSpinner extends RelativeLayout implements  AdapterView.OnItemSelectedListener {
    private Spinner mSpinner;
    private Listener mListener;
    private TextView mTextView;

    public ItemSpinner(Context context) {
        super(context);
    }

    public ItemSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ItemSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mSpinner = (Spinner)findViewById(R.id.spinner);
        SpinnerAdapter spinnerAdapter = TypeSpinnerAdapter.getTypeSpinnerAdapter(getContext());
        mSpinner.setAdapter(spinnerAdapter);
        mTextView = (TextView)findViewById(R.id.text);

        mSpinner.setOnItemSelectedListener(this);

        setWillNotDraw(false);
    }

    public Spinner getSpinner() {
        return mSpinner;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mListener.onSelectionChanged((position == TypeSpinnerAdapter.POSITION_ITEM)? false : true);
        if (position == TypeSpinnerAdapter.POSITION_DIRECTORY) {
            mTextView.setText(getResources().getString(R.string.directory));
        } else {
            mTextView.setText(getResources().getString(R.string.item));
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    interface Listener {
        public void onSelectionChanged(boolean isFolder);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void setFolder(boolean isFolder) {
        if (isFolder) {
            mSpinner.setSelection(TypeSpinnerAdapter.POSITION_DIRECTORY);
            mTextView.setText(getResources().getString(R.string.directory));
        } else {
            mSpinner.setSelection(TypeSpinnerAdapter.POSITION_ITEM);
            mTextView.setText(getResources().getString(R.string.item));
        }
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (true) {
            Paint p = new Paint();
            p.setStyle(Paint.Style.FILL);
            /*p.setARGB(255, 255, 0, 0);
            canvas.drawRect(0, 0, mCheckBoxWidth, mHeight, p);
            p.setARGB(255, 0, 255, 0);
            canvas.drawRect(mCheckBoxWidth, 0, mCheckBoxWidth + mEditText.getMeasuredWidth(), mHeight, p);*/
        }
    }

    private int toPixels(int dp) {
        return (int)applyDimension(COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int h = mTextView.getMeasuredHeight();
        int y = ((bottom - top) - h)/2;
        mTextView.layout(toPixels(5), y, right - left, h + y);
        mSpinner.layout(0, 0, right-left, bottom - top);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == ev.ACTION_DOWN) {
            setBackgroundColor(getResources().getColor(R.color.vibrant_light));
        } else if (ev.getActionMasked() == ev.ACTION_CANCEL || ev.getActionMasked() == ev.ACTION_UP) {
            setBackgroundColor(Color.TRANSPARENT);
        }
        return super.dispatchTouchEvent(ev);
    }
}
