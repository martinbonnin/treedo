package com.mbonnin.treedo;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

/**
 * Created by martin on 8/23/16.
 */

@EViewGroup(R.layout.node_view)
public class NodeView extends LinearLayout {
    @ViewById
    CheckBox checkbox;
    @ViewById
    ImageView folder;
    @ViewById
    ItemEditText editText;
    @ViewById
    ImageView arrow;

    public NodeView(Context context) {
        super(context);
    }

    public NodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NodeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setNode(Node node, boolean trash, boolean grabable) {
        checkbox.setVisibility(node.folder ? GONE : VISIBLE);
        checkbox.setChecked(node.checked);
        folder.setVisibility(node.folder ? VISIBLE : GONE);
        folder.setImageResource(trash ? R.drawable.trash:R.drawable.folder);
        /**
         * be paranoid about carriage return, we don't want setText() to trigger the listener
         */
        editText.setText(node.text.replaceAll("\n", ""));
        boolean editable = !node.folder && !grabable;
        editText.setEditable(editable);
        editText.setFocusable(editable);
        editText.setFocusableInTouchMode(editable);
        editText.setHint("");
        arrow.setVisibility(node.folder || grabable ? VISIBLE : GONE);
        arrow.setImageResource(grabable ? R.drawable.handle : R.drawable.arrow);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    public void setNewNode() {
        checkbox.setVisibility(GONE);
        folder.setVisibility(VISIBLE);
        folder.setImageResource(R.drawable.plus_gray);
        editText.setEditable(true);
        editText.setFocusable(true);
        editText.setFocusableInTouchMode(true);
        editText.setText("");
        editText.setHint(getContext().getString(R.string.new_item));
        arrow.setVisibility(GONE);
    }

    @AfterViews
    void afterViews() {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
    }

}
