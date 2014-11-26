package com.mbonnin.treedo;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;

/**
 * Created by martin on 06/10/14.
 */
public class ItemEditText extends EditText implements TextWatcher {
    private Listener mListener;
    private int mNewLinePosition = -1;

    @Override
    public void onTextChanged(CharSequence s, int start, int count, int after) {
        if (after == 1) {
            char c = s.charAt(start);
            if (c == '\n') {
                mNewLinePosition = start;
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        if (mNewLinePosition != -1) {
            int position = mNewLinePosition;
            mNewLinePosition = -1;
            s.delete(position, position + 1);
            if (mListener != null) {
                mListener.onNewItem(position);
            }
        }

    }

    public interface Listener {
        void onNewItem(int position);
        void onDeleteItem();
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public ItemEditText(Context context) {
        super(context);
        init();
    }

    private void init() {
        addTextChangedListener(this);
    }

    public ItemEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ItemEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
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
                        mListener.onDeleteItem();
                        return false;
                    }
                }
            }

            return super.sendKeyEvent(event);
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            // magic: in latest Android, deleteSurroundingText(1, 0) will be called for backspace
            if (beforeLength == 1 && afterLength == 0) {
                // backspace
                return sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                        && sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
            }

            return super.deleteSurroundingText(beforeLength, afterLength);
        }
    }
}
