package com.mbonnin.treedo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by martin on 21/08/14.
 * XXX: use a listview instead so that it scales correctly
 */
public class ItemListView extends FrameLayout {
    public final ObservableScrollView mScrollView;
    private Item mParent;
    private Item mNewItem;
    private LinearLayout mLinearLayout;
    private ItemView mGrabbedView;

    private Listener mListener;

    private static LinkedList<ItemView> sRecycledViews = new LinkedList<ItemView>();
    private ArrayList<ItemView> mItemViewList = new ArrayList<ItemView>();
    private ArrayList<ItemView> mTrashedViews = new ArrayList<ItemView>();
    private float mDownY;
    private int mInitialY;
    private LayoutParams mGrabLayoutParams;
    private SpacerView mSpacerView;
    private int mScrollDelta;

    public String getTitle() {
        return mParent.text;
    }

    public Item getParentItem() {
        return mParent;
    }

    public void startReorder() {
        for (int i = 0; i < getItemViewCount() - 1; i++) {
            ItemView itemView = getItemViewAt(i);
            int flags = itemView.getFlags();
            flags |= ItemView.FLAG_SHOW_HANDLE;
            itemView.setFlags(flags);
        }
    }

    private final Runnable scrollRunnable = new Runnable() {
        @Override
        public void run() {
            mScrollView.scrollBy(0, mScrollDelta);
            postDelayed(scrollRunnable, 20);
            //Utils.log("scroll by " + mScrollDelta);
        }
    };

    public void stopReorder() {
        for (int i = 0; i < getItemViewCount() - 1; i++) {
            ItemView itemView = getItemViewAt(i);
            int flags = itemView.getFlags();
            flags &= ~ItemView.FLAG_SHOW_HANDLE;
            itemView.setFlags(flags);
        }
    }

    public void focusLastItem() {
        ItemView itemView = getItemViewAt(getItemViewCount() - 1);
        itemView.mEditText.requestFocus();
        InputMethodManager inputMethodManager=(InputMethodManager)(getContext().getSystemService(Context.INPUT_METHOD_SERVICE));
        inputMethodManager.showSoftInput(itemView.mEditText, 0);
    }

    static interface Listener {
        void onDirectoryClicked(Item item);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    private Item createItem() {
        Item item = new Item();
        return item;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == ev.ACTION_DOWN) {
            for (Iterator<ItemView> it = mTrashedViews.iterator(); it.hasNext();) {
                ItemView itemView = it.next();
                // This uses global coordinates
                int location[] = new int[2];
                itemView.getLocationInWindow(location);
                Rect bounds = new Rect(location[0], location[1], location[0] + itemView.getWidth(), location[1] + itemView.getHeight());
                if (!bounds.contains((int)ev.getRawX(), (int)ev.getRawY())) {
                    it.remove();

                    final SpacerView spacerView = new SpacerView(getContext());
                    spacerView.setBackgroundColor(getResources().getColor(R.color.dark_gray));
                    spacerView.setFixedWidth(itemView.getMeasuredWidth());
                    spacerView.setFixedHeight(itemView.getMeasuredHeight());
                    final int index = mLinearLayout.indexOfChild(itemView);

                    removeItemView(itemView);
                    ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    mLinearLayout.addView(spacerView, index, layoutParams);
                    collapseSpacerView(spacerView);

                    if (getItemViewCount() == 1 && !mParent.isRoot) {
                        ItemView firstItemView = getItemViewAt(0);
                        int flags = firstItemView.getFlags();
                        flags |= ItemView.FLAG_SHOW_SPINNER;
                        firstItemView.setFlags(flags);
                    }
                }
            }
        }

        if (mGrabbedView != null) {
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    private void collapseSpacerView(final SpacerView spacerView) {
        int from = spacerView.getFixedHeight();
        final ValueAnimator collapseAnimator = ValueAnimator.ofInt(from, 0);
        collapseAnimator.setDuration(400);
        collapseAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                spacerView.setFixedHeight((Integer) collapseAnimator.getAnimatedValue());
                spacerView.requestLayout();
            }
        });

        collapseAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLinearLayout.removeView(spacerView);
            }
        });

        collapseAnimator.start();
    }

    private void addItemViewAfter(ItemView itemView, ItemView after) {
        int index2;
        final LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

        if (after == null) {
            index2 = -1;
            mItemViewList.add(itemView);
        } else {
            index2 = mLinearLayout.indexOfChild(after) + 1;
            int index = getItemViewIndex(after);
            mItemViewList.add(index + 1, itemView);
        }

        mLinearLayout.addView(itemView, index2, layoutParams);
    }

    private void removeItemView(ItemView itemView) {
        mItemViewList.remove(itemView);
        mLinearLayout.removeView(itemView);
    }

    private int getItemViewCount() {
        return mItemViewList.size();
    }

    private ItemView getItemViewAt(int index) {
        return mItemViewList.get(index);
    }

    private int getItemViewIndex(ItemView itemView) {
        for (int i =0; i < mItemViewList.size(); i++) {
            if (mItemViewList.get(i) == itemView) {
                return i;
            }
        }

        return -1;
    }

    private void onNewItem(ItemView itemView, int position) {
        int index = getItemViewIndex(itemView);
        Item item = itemView.getItem();
        if (item.isADirectory && ((itemView.getFlags() & ItemView.FLAG_LAST) == 0)) {
            return;
        }

        itemView.setFlags(0);

        Item item2 = createItem();
        ItemView itemView2 = getItemView();
        item2.isADirectory = item.isADirectory;
        itemView2.setItem(item2);
        if (index == getItemViewCount() -1) {
            itemView2.setFlags(ItemView.FLAG_LAST);
        }
        addItemViewAfter(itemView2, itemView);
        String text = itemView.getText();
        String remainingText = text.substring(position);
        itemView.setText(text.substring(0,position));
        itemView2.appendAndFocus(remainingText, true);
    }

    private ItemView getItemView() {
        final ItemView itemView;

        if (sRecycledViews.size() > 0) {
            itemView = sRecycledViews.pop();
        } else {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            itemView = (ItemView)inflater.inflate(R.layout.item_view, null);
        }

        itemView.setListener(new ItemView.Listener() {
            @Override
            public void onNewItem(int position) {
                ItemListView.this.onNewItem(itemView, position);
            }

            @Override
            public void onDeleteItem(String remainingText) {

                int index = getItemViewIndex(itemView);

                if ((itemView.getFlags() & ItemView.FLAG_LAST) != 0 && index == 0) {
                    // we do not allow to delete the first item
                    return;
                }
                if (itemView.getItem().isADirectory) {
                    // do not delete directories
                    return;
                }

                if (index > 0) {
                    ItemView previousView = getItemViewAt(index - 1);

                    Item item = itemView.getItem();
                    mParent.children.remove(item);

                    if (index == getItemViewCount() - 1) {
                        // we were on the last line
                        int flags = ItemView.FLAG_LAST;
                        if (!mParent.isRoot && index == 1) {
                            // we are not at the root and we have no more items
                            // so allow to change from directory to item
                            flags |= ItemView.FLAG_SHOW_SPINNER;
                        }
                        previousView.setFlags(flags);
                    }

                    previousView.appendAndFocus(remainingText, true);

                    // remove the view after setting the focus on the previous item else
                    // we get a glitch in the scrollview
                    removeItemView(itemView);
                }
            }

            @Override
            public void onArrowClicked() {
                if (mListener != null) {
                    mListener.onDirectoryClicked(itemView.getItem());
                }
            }

            @Override
            public void onTrashClicked() {
                Item item = itemView.getItem();
                Database.addToTrash(item);

                // no need to remove from children, this will be done by updateItems
                mTrashedViews.add(itemView);
            }

            @Override
            public void onCancelTrash() {
                Item item = itemView.getItem();
                Database.removeFromTrash(item);
                mTrashedViews.remove(itemView);
            }

            @Override
            public void onGrabTouchEvent(MotionEvent ev) {
                if (ev.getActionMasked() == ev.ACTION_DOWN && mGrabbedView == null) {
                    mGrabbedView = itemView;

                    mSpacerView = new SpacerView(getContext());
                    mSpacerView.setBackgroundColor(getResources().getColor(R.color.dark_gray));
                    mSpacerView.setFixedWidth(itemView.getMeasuredWidth());
                    mSpacerView.setFixedHeight(itemView.getMeasuredHeight());
                    final int index = mLinearLayout.indexOfChild(itemView);

                    int location[] = new int[2];
                    itemView.getLocationInWindow(location);
                    itemView.mEditText.setFocusable(false);

                    ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    mLinearLayout.addView(mSpacerView, index, layoutParams);
                    removeItemView(itemView);

                    int frameLocation[] = new int[2];
                    getLocationInWindow(frameLocation);

                    mGrabLayoutParams = new LayoutParams(itemView.getWidth(), itemView.getHeight());
                    mGrabLayoutParams.leftMargin = location[0] - frameLocation[0];
                    mGrabLayoutParams.topMargin = location[1] - frameLocation[1];

                    mDownY = ev.getRawY();
                    mInitialY = mGrabLayoutParams.topMargin;

                    addView(mGrabbedView, mGrabLayoutParams);

                    /*
                     * Little hack to prevent the item to be on top of the scrollbar
                     */
                    mScrollView.setVerticalScrollBarEnabled(false);
                }
            }
        });

        return itemView;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mGrabbedView == null) {
            return super.onTouchEvent(event);
        }
        if (event.getActionMasked() == event.ACTION_MOVE) {
            mGrabLayoutParams.topMargin = mInitialY + (int)(event.getRawY() - mDownY);
            if (mGrabLayoutParams.topMargin < 0) {
                mGrabLayoutParams.topMargin = 0;
                if (mScrollDelta == 0) {
                    mScrollDelta = -Utils.toPixels(7);
                    Utils.log("start scrolling up");
                    postDelayed(scrollRunnable, 0);
                }
            } else if (mGrabLayoutParams.topMargin + mGrabLayoutParams.height > getHeight()) {
                mGrabLayoutParams.topMargin = getHeight() - mGrabLayoutParams.height;
                if (mScrollDelta == 0) {
                    mScrollDelta = Utils.toPixels(7);
                    postDelayed(scrollRunnable, 0);
                }
            } else {
                mScrollDelta = 0;
                //Utils.log("stop scrolling");
                removeCallbacks(scrollRunnable);
            }

            View child = null;
            child = mLinearLayout.getChildAt(mLinearLayout.getChildCount() - 1);
            Rect lastItemBounds = new Rect();
            child.getHitRect(lastItemBounds);
            if (mGrabLayoutParams.topMargin + mGrabLayoutParams.height + mScrollView.getScrollY() > lastItemBounds.top) {
                mGrabLayoutParams.topMargin = lastItemBounds.top - mScrollView.getScrollY()  - mGrabLayoutParams.height;
                mScrollDelta = 0;
                removeCallbacks(scrollRunnable);
                mScrollView.scrollTo(0, lastItemBounds.top + lastItemBounds.height() - getHeight());
            }

            updateViewLayout(mGrabbedView, mGrabLayoutParams);

            int targetY = mGrabLayoutParams.topMargin + mGrabLayoutParams.height / 2;

            child = null;
            int i = 0;
            int y = -mScrollView.getScrollY();
            for (; i < mLinearLayout.getChildCount(); i++) {
                child = mLinearLayout.getChildAt(i);
                y += child.getHeight();
                if (y > targetY) {
                    break;
                }
            }
            if (!(child instanceof SpacerView)) {
                mLinearLayout.removeView(mSpacerView);
                /*
                 * we have removed a view so we need to recompute i as the spacer view can be
                 * before or after child
                 */
                y = -mScrollView.getScrollY();
                for (i = 0; i < mLinearLayout.getChildCount(); i++) {
                    child = mLinearLayout.getChildAt(i);
                    y += child.getHeight();
                    if (y > targetY) {
                        break;
                    }
                }

                mSpacerView.setFixedHeight(child.getHeight());
                mLinearLayout.addView(mSpacerView, i);

            }
        } else if (event.getActionMasked() == event.ACTION_UP || event.getActionMasked() == event.ACTION_CANCEL) {
            int index = mLinearLayout.indexOfChild(mSpacerView);
            ItemView child = null;
            for (int i = index - 1; i >= 0; i--) {
                if (mLinearLayout.getChildAt(i) instanceof ItemView) {
                    child = (ItemView)mLinearLayout.getChildAt(i);
                    break;
                }
            }

            mGrabbedView.mEditText.setFocusable(false);

            removeView(mGrabbedView);
            mLinearLayout.removeView(mSpacerView);
            if (child != null) {
                addItemViewAfter(mGrabbedView, child);
            } else {
                final LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                mItemViewList.add(0, mGrabbedView);
                mLinearLayout.addView(mGrabbedView, 0, layoutParams);
            }

            mGrabbedView = null;
            mSpacerView = null;
            mScrollView.setVerticalScrollBarEnabled(true);
        }


        return true;
    }

    public ItemListView(Context context, Item parent) {
        super(context);
        long start = System.currentTimeMillis();

        mScrollView = new ObservableScrollView(context);
        mScrollView.setFillViewport(true);

        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        mLinearLayout = new LinearLayout(context);
        mLinearLayout.setOrientation(LinearLayout.VERTICAL);
        mLinearLayout.setBackgroundColor(Color.WHITE);
        mScrollView.addView(mLinearLayout, layoutParams);
        addView(mScrollView, layoutParams);

        mParent = parent;

        for (int i = 0; i < mParent.children.size(); i++) {
            final ItemView itemView = getItemView();
            itemView.setItem(mParent.children.get(i));
            addItemViewAfter(itemView, null);
        }

        // Add the new item view
        mNewItem = createItem();
        ItemView newItemView = getItemView();
        int flags = ItemView.FLAG_LAST;
        if (mParent.isRoot) {
            mNewItem.isADirectory = true;
        } else {
            if (mParent.children.size() == 0) {
                mNewItem.isADirectory = false;
                flags |= ItemView.FLAG_SHOW_SPINNER;
            } else {
                mNewItem.isADirectory = mParent.children.get(0).isADirectory;
            }
        }
        newItemView.setItem(mNewItem);
        newItemView.setFlags(flags);
        addItemViewAfter(newItemView, null);

        SpacerView spacerView = new SpacerView(getContext());
        spacerView.setFixedWidth(LayoutParams.MATCH_PARENT);
        spacerView.setFixedHeight(Utils.toPixels(80));
        spacerView.setBackgroundColor(Color.WHITE);
        mLinearLayout.addView(spacerView, layoutParams);

        Utils.log("ItemListView() took " + (System.currentTimeMillis() - start) + " ms");
    }

    public void recycle() {
        int count = getItemViewCount();
        for (int i = 0; i < count - 1; i++) {
            ItemView itemView = getItemViewAt(i);
            itemView.recycle();
            sRecycledViews.push(itemView);
        }
        mLinearLayout.removeAllViews();
        mItemViewList.clear();
    }

    public void updateItemsTextAndChecked() {
        // we need that because onNew Item does not automatically insert the item at the correct place
        mParent.children.clear();
        int count = getItemViewCount();
        for (int i = 0; i < count; i++) {
            ItemView itemView = getItemViewAt(i);

            if (mTrashedViews.contains(itemView)) {
                continue;
            }

            Item item = itemView.getItem();
            if (i == count -1) {
                if (!item.text.equals("")) {
                    // commit the current item
                    onNewItem(itemView, itemView.getText().length());
                    mParent.children.add(item);
                }
            } else {
                mParent.children.add(item);
            }
        }
    }
}
