package com.mbonnin.treedo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
    private Paint mPaint;

    public NodeView(Context context) {
        super(context);
    }

    public NodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float h = Utils.toPixels(0.5f);
        canvas.drawRect(0, 0, canvas.getWidth(), h, mPaint);
        canvas.drawRect(0, canvas.getHeight() - h, canvas.getWidth(), canvas.getHeight(), mPaint);
    }

    @AfterViews
    void afterViews() {
        mPaint = new Paint();
        mPaint.setColor(Color.LTGRAY);
        mPaint.setStyle(Paint.Style.FILL);
        setWillNotDraw(false);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
    }

}
