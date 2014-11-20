package com.mbonnin.treedo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewPropertyAnimator;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

/**
 * Created by martin on 21/08/14.
 * XXX: use a listview instead so that it scales correctly
 */
public class ItemListView extends LinearLayout {
    private final Context mContext;
    private Item mParent;
    private Item mNewItem;

    private static final long NO_POINTER_ID = Long.MAX_VALUE;
    private long mPointerID = NO_POINTER_ID;
    private ItemFrontView mTouchedView;
    private Listener mListener;
    private long mTouchedTime;
    private final int longPressTimeout;
    private ItemFrontView mTouchedDirectory;

    private static LinkedList<ItemView> sRecycledViews = new LinkedList<ItemView>();
    private ArrayList<ItemView> mItemViewList = new ArrayList<ItemView>();

    public String getTitle() {
        return mParent.text;
    }

    public Item getParentItem() {
        return mParent;
    }

    static interface Listener {
        void onDirectoryClicked(Item item);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    private Item createItem() {
        Item item = Database.createItem();
        item.parent = mParent.id;
        return item;
    }

    private ItemView getItemView() {
        if (sRecycledViews.size() > 0) {
            Utils.log("reuse View");
            return sRecycledViews.pop();
        } else {
            Utils.log("create View");
            return createItemView();
        }

    }

    private void collapseSpacerView(final SpacerView spacerView) {
        final AnimatorSet animatorSet = new AnimatorSet();
        int from = spacerView.getFixedHeight();
        final ValueAnimator collapseAnimator = ValueAnimator.ofInt(from, 0);
        collapseAnimator.setDuration(200);
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
                removeView(spacerView);
            }
        });

        collapseAnimator.start();
    }

    private void addItemViewAfter(ItemView itemView, ItemView after) {
        int index2;
        if (after == null) {
            index2 = -1;
            mItemViewList.add(itemView);
        } else {
            index2 = indexOfChild(after) + 1;
            int index = getItemViewIndex(after);
            mItemViewList.add(index + 1, itemView);
        }

        addView(itemView, index2);
    }

    private void removeItemView(ItemView itemView) {
        mItemViewList.remove(itemView);
        removeView(itemView);
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

    private void onNewItem(ItemView itemView, String remainingText) {
        int index = getItemViewIndex(itemView);

        itemView.setFlags(0);

        Item item2 = createItem();
        ItemView itemView2 = getItemView();
        item2.isADirectory = itemView.getItem().isADirectory;
        itemView2.setItem(item2);
        if (index == getItemViewCount() -1) {
            itemView2.setFlags(ItemView.FLAG_LAST);
        }
        addItemViewAfter(itemView2, itemView);
        itemView2.appendAndFocus(remainingText, true);
    }

    private ItemView createItemView() {
        final ItemView itemView = new ItemView(getContext());
        final LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

        itemView.setListener(new ItemView.Listener() {
            @Override
            public void onNewItem(String remainingText) {
                ItemListView.this.onNewItem(itemView, remainingText);
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
                        if (mParent.id != 0 && index == 1) {
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
                final SpacerView spacerView = new SpacerView(getContext());
                spacerView.setFixedWidth(itemView.getMeasuredWidth());
                spacerView.setFixedHeight(itemView.getMeasuredHeight());
                final int index = indexOfChild(itemView);

                ViewPropertyAnimator animator = itemView.animate();
                animator.setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        removeItemView(itemView);
                        addView(spacerView, index, layoutParams);
                        collapseSpacerView(spacerView);

                        if (getItemViewCount() == 1 && mParent.id != 0) {
                            ItemView firstItemView = getItemViewAt(0);
                            int flags = firstItemView.getFlags();
                            flags |= ItemView.FLAG_SHOW_SPINNER;
                            firstItemView.setFlags(flags);
                        }
                    }
                });
                animator.alpha(0.0f).setDuration(200).start();

                Toast toast = Toast.makeText(getContext(), R.string.note_deleted, Toast.LENGTH_SHORT);
                toast.show();

            }
        });

        return itemView;
    }

    public ItemListView(Context context, Item parent) {
        super(context);
        long start = System.currentTimeMillis();

        mContext = context;
        longPressTimeout = ViewConfiguration.get(context).getLongPressTimeout();

        Collections.sort(parent.children, new Comparator<Item>() {
            @Override
            public int compare(Item lhs, Item rhs) {
                if (lhs.isADirectory && !rhs.isADirectory) {
                    return -1;
                } else if (!lhs.isADirectory && rhs.isADirectory) {
                    return 1;
                } else {
                    return lhs.order - rhs.order;
                }
            }
        });

        mParent = parent;

        final LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

        for (int i = 0; i < mParent.children.size(); i++) {
            final ItemView itemView = getItemView();
            itemView.setItem(mParent.children.get(i));
            addItemViewAfter(itemView, null);
        }

        // Add the new item view
        mNewItem = createItem();
        ItemView newItemView = getItemView();
        int flags = ItemView.FLAG_LAST;
        if (mParent.id == 0) {
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

        Utils.log("ItemListView() took " + (System.currentTimeMillis() - start) + " ms");
    }

    public void recycle() {
        int count = getItemViewCount();
        for (int i = 0; i < count - 1; i++) {
            ItemView itemView = getItemViewAt(i);
            sRecycledViews.push(itemView);
        }
        removeAllViews();
        mItemViewList.clear();
    }

    public void sync() {
        mParent.children.clear();
        int count = getItemViewCount();
        int order = 0;
        for (int i = 0; i < count; i++) {
            ItemView itemView = getItemViewAt(i);
            Item item = itemView.getItem();
            item.order = order++;

            itemView.cancelSwipe();

            if (i == count -1) {
                if (!item.text.equals("")) {
                    // commit the current item
                    onNewItem(itemView, "");
                    mParent.children.add(item);
                } else {
                    // do nothing
                }
            } else {
                mParent.children.add(item);
            }

        }
    }

    private String getAction(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                return "DOWN";
            case MotionEvent.ACTION_MOVE:
                return "MOVE";
            case MotionEvent.ACTION_UP:
                return "UP";
            case MotionEvent.ACTION_CANCEL:
                return "CANCEl";
        }

        return "?";
    }

    /*private boolean interceptTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            int count;
            count = getChildCount();
            mTouchedView = null;
            mTouchedDirectory = null;
            int i = 0;
            for (i = 0; i < count; i++) {
                ItemView itemView = (ItemView) getChildAt(i);
                Rect r = new Rect();
                if (itemView.getGlobalVisibleRect(r) && r.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    mTouchedView = itemView;
                    break;
                }
            }

            if (mTouchedView != null) {
                mTouchedTime = System.currentTimeMillis();
                if (mTouchedView.getItem().isADirectory && !mTouchedView.hasFocus()
                        && i != count - 1) {
                    mTouchedDirectory = mTouchedView;
                    mTouchedDirectory.setBackgroundColor(Color.argb(30,0,0,0));
                }
            }
        } else if (ev.getActionMasked() == MotionEvent.ACTION_UP) {
            if (mTouchedDirectory != null) {
                mTouchedDirectory.setBackgroundColor(Color.argb(0, 0, 0, 0));

                if (System.currentTimeMillis() - mTouchedTime < 300) {
                    if (mListener != null) {
                        mListener.onDirectoryClicked(mTouchedDirectory.getItem());
                    }

                    // handle this event. We do not want the edittext to take the focus
                    return true;
                }
            }
        } else if (ev.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            if (mTouchedDirectory != null) {
                mTouchedDirectory.setBackgroundColor(Color.argb(0, 0, 0, 0));
            }
        }

        return false;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Utils.log("onInterceptTouchEvent " + getAction(ev));
        return interceptTouchEvent(ev);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        Utils.log("onTouchEvent " + getAction(ev));
        interceptTouchEvent(ev);
        return true;
    }*/
}
