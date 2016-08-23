package com.mbonnin.treedo;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;

public class NodeRecyclerView extends RecyclerView {

    private final String mTitle;
    private final NodeAdapter mNodeAdapter;
    private final Handler mHandler;

    Runnable mEnsureFocusVisibleRunnable = new Runnable() {

        @Override
        public void run() {
            LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
            int focusPosition = mNodeAdapter.getFocusPosition();
            if (focusPosition >= 0) {
                if (focusPosition < layoutManager.findFirstVisibleItemPosition()
                        || focusPosition > layoutManager.findLastVisibleItemPosition()) {
                    smoothScrollToPosition(focusPosition);
                }
            }
            mHandler.removeCallbacks(this);
        }
    };

    public NodeRecyclerView(Context context, Node node) {
        super(context);

        setBackgroundColor(Color.WHITE);
        mHandler = new Handler();

        Node trash = null;
        if (node == null) {
            mTitle = context.getString(R.string.app_name);
            node = DB_.getInstance_(context).getRoot();
            trash = DB_.getInstance_(context).getTrash();
        } else {
            mTitle = node.text;
        }

        mNodeAdapter = new NodeAdapter(context, node, trash);
        mNodeAdapter.registerAdapterDataObserver(new AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                mHandler.post(mEnsureFocusVisibleRunnable);
            }
        });
        setLayoutManager(new LinearLayoutManager(context));
        addItemDecoration(new ItemDecorator());
        setAdapter(mNodeAdapter);
    }

    public String getTitle() {
        return mTitle;
    }

    public void setEditMode(boolean editable) {
        mNodeAdapter.setGrabable(editable);
    }

    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    public void createFolder(String text) {
        mNodeAdapter.createFolder(text);
    }
}
