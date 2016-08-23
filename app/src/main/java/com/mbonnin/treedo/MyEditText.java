package com.mbonnin.treedo;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;

/**
 * Created by martin on 06/10/14.
 */
public class MyEditText extends AppCompatEditText {
    private Listener mListener;
    private boolean mEditable;

    @Override
    public void onTextChanged(CharSequence s, int start, int lengthBefore, int lengthAfter) {
        if (mListener != null) {
            mListener.onTextChanged(s, start, lengthBefore, lengthAfter);
        }
    }

    public void setEditable(boolean editable) {
        mEditable = editable;
    }


    public interface Listener {
        void onDeleteItem();

        void onTextChanged(CharSequence s, int start, int lengthBefore, int lengthAfter);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public MyEditText(Context context) {
        super(context);
    }

    public MyEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new ItemInputConnection(super.onCreateInputConnection(outAttrs),
                true);
    }

    private class ItemInputConnection extends InputConnectionWrapper {

        public ItemInputConnection(InputConnection target, boolean mutable) {
            super(target, mutable);
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            Log.d("TAG", "sendKeyEvent");
            if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (getSelectionStart() == 0 && getSelectionEnd() == 0) {
                        if (mListener != null) {
                            mListener.onDeleteItem();
                        }
                        return false;
                    }
                }
            }

            return super.sendKeyEvent(event);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mEditable) {
            /**
             * this is so that the EditText does not "eat" the events
             */
            return false;
        } else {
            return super.onTouchEvent(event);
        }
    }
}
