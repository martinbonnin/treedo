package com.mbonnin.treedo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by martin on 17/11/14.
 */
public class DialogBuilder {
    AlertDialog.Builder mBuilder;
    LinearLayout mViewHolder;
    String mButtonLabel;
    ImageView mIcon;
    TextView mTitle;
    Listener mListener;

    interface Listener {
        void onButtonClick();
    }

    public DialogBuilder(Activity activity) {
        mBuilder = new AlertDialog.Builder(activity);
        View view = activity.getLayoutInflater().inflate(R.layout.dialog, null);
        mViewHolder = (LinearLayout)view.findViewById(R.id.view_holder);
        mIcon = (ImageView)view.findViewById(R.id.icon);
        mTitle = (TextView)view.findViewById(R.id.title);

        mBuilder.setView(view);
        mButtonLabel = activity.getString(R.string.ok);
    }

    public void setView(View view) {
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mViewHolder.removeAllViews();
        mViewHolder.addView(view,layoutParams);
    }

    public void setButtonLabel(String label) {
        mButtonLabel = label;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void setTitle(String title) {
        mTitle.setText(title);
    }

    public void setIcon(int id) {
        mIcon.setImageResource(id);
    }

    public AlertDialog show() {
        mBuilder.setNeutralButton(mButtonLabel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if (mListener != null) {
                    mListener.onButtonClick();
                }
            }
        });

        return mBuilder.show();

    }
}
