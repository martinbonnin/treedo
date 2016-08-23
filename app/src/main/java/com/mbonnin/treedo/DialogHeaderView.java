package com.mbonnin.treedo;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;


@EViewGroup(R.layout.dialog_header)
public class DialogHeaderView extends LinearLayout {
    @ViewById
    ViewGroup viewHolder;

    @ViewById
    TextView title;
    @ViewById
    ImageView icon;

    public DialogHeaderView(Context context) {
        super(context);
    }

    @AfterViews
    void afterViews() {
        setOrientation(VERTICAL);
    }
}
