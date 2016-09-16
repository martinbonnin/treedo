package com.mbonnin.treedo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;

import static java.security.AccessController.getContext;

/**
 * Created by martin on 9/15/16.
 */

public class ShortcutAdapter extends RecyclerView.Adapter {
    ArrayList<Integer> mColorList = new ArrayList<>();
    int mSelectedPosition = 0;

    static class MyImageView extends ImageView {
        public MyImageView(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
        }
    }

    public ShortcutAdapter(Context context) {
        mColorList.add(Color.parseColor("#F44336"));
        mColorList.add(Color.parseColor("#E91E63"));
        mColorList.add(Color.parseColor("#9C27B0"));
        mColorList.add(Color.parseColor("#673AB7"));
        mColorList.add(Color.parseColor("#3F51B5"));
        mColorList.add(Color.parseColor("#2196F3"));
        mColorList.add(Color.parseColor("#03A9F4"));
        mColorList.add(Color.parseColor("#00BCD4"));
        mColorList.add(Color.parseColor("#009688"));
        mColorList.add(Color.parseColor("#4CAF50"));
        mColorList.add(Color.parseColor("#8BC34A"));
        mColorList.add(Color.parseColor("#CDDC39"));
        mColorList.add(Color.parseColor("#FFEB3B"));
        mColorList.add(Color.parseColor("#FFC107"));
        mColorList.add(Color.parseColor("#FF9800"));

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ImageView imageView = new MyImageView(parent.getContext());
        int px = (int)Utils.toPixels(5);
        imageView.setPadding(px, px, px, px);
        RecyclerView.ViewHolder holder = new RecyclerView.ViewHolder(imageView) {};

        RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        imageView.setLayoutParams(layoutParams);

        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ImageView imageView = (ImageView)holder.itemView;
        int resId;
        if (position == mSelectedPosition) {
            //imageView.setBackgroundColor(Color.LTGRAY);
            resId = R.drawable.ic_treedo_checked;
        } else {
            //imageView.setBackgroundColor(Color.TRANSPARENT);
            resId = R.drawable.ic_treedo_empty;
        }
        Drawable drawable = imageView.getContext().getResources().getDrawable(resId);
        DrawableCompat.setTint(DrawableCompat.wrap(drawable).mutate(), mColorList.get(position));
        imageView.setImageDrawable(drawable);
        imageView.setOnClickListener(v-> {
            if (position != mSelectedPosition) {
                notifyItemChanged(position);
                notifyItemChanged(mSelectedPosition);
                mSelectedPosition = position;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mColorList.size();
    }

    public int getSelectedColor() {
        return mColorList.get(mSelectedPosition);
    }
}
