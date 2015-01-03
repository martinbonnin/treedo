package com.mbonnin.treedo;

import android.content.Context;
import android.media.Image;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Created by martin on 1/3/15.
 */
public class ItemView2 extends FrameLayout {
    private static final String TAG = "ItemView2";
    private Item mItem;
    public ImageView mHandle;
    public CheckBox mCheckBox;
    public ImageView mFolder;
    public ItemEditText mEditText;
    public ImageView mArrow;

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
    protected void onFinishInflate() {
        super.onFinishInflate();
        mHandle = (ImageView)findViewById(R.id.handle);
        mCheckBox = (CheckBox)findViewById(R.id.checkbox);
        mFolder = (ImageView)findViewById(R.id.folder);
        mEditText = (ItemEditText)findViewById(R.id.edit_text);
        mArrow = (ImageView)findViewById(R.id.arrow);
    }

    public void cancelTranslation() {
        Log.e(TAG, "cancelTranslation is not implemented");
    }

    public void setItem(Item item) {
        this.mItem = item;
        if (item.isAFolder) {
            mCheckBox.setVisibility(GONE);
            mFolder.setVisibility(VISIBLE);
            if (item.isTrash) {
                mFolder.setImageResource(R.drawable.trash);
            } else {
                mFolder.setImageResource(R.drawable.folder);
            }
            mArrow.setVisibility(VISIBLE);
        } else {
            mCheckBox.setVisibility(VISIBLE);
            mCheckBox.setChecked(item.checked);
            mFolder.setVisibility(GONE);
            mArrow.setVisibility(GONE);
        }
        mEditText.setText(item.text);
    }
}
