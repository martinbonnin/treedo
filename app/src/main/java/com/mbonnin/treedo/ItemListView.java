package com.mbonnin.treedo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.LayoutInflater;
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
    private Listener mListener;

    private static LinkedList<ItemView2> sRecycledViews = new LinkedList<ItemView2>();
    private ArrayList<ItemView2> mItemViewList = new ArrayList<ItemView2>();

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

    private void collapseSpacerView(final SpacerView spacerView) {
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

    private void addItemViewAfter(ItemView2 itemView, ItemView2 after) {
        int index2;
        final LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

        if (after == null) {
            index2 = -1;
            mItemViewList.add(itemView);
        } else {
            index2 = indexOfChild(after) + 1;
            int index = getItemViewIndex(after);
            mItemViewList.add(index + 1, itemView);
        }

        addView(itemView, index2, layoutParams);
    }

    private void removeItemView(ItemView2 itemView) {
        mItemViewList.remove(itemView);
        removeView(itemView);
    }

    private int getItemViewCount() {
        return mItemViewList.size();
    }

    private ItemView2 getItemViewAt(int index) {
        return mItemViewList.get(index);
    }

    private int getItemViewIndex(ItemView2 itemView) {
        for (int i =0; i < mItemViewList.size(); i++) {
            if (mItemViewList.get(i) == itemView) {
                return i;
            }
        }

        return -1;
    }

    private void onNewItem(ItemView2 itemView, String remainingText) {
        int index = getItemViewIndex(itemView);

        itemView.setFlags(0);

        Item item2 = createItem();
        ItemView2 itemView2 = getItemView();
        item2.isADirectory = itemView.getItem().isADirectory;
        itemView2.setItem(item2);
        if (index == getItemViewCount() -1) {
            itemView2.setFlags(ItemView.FLAG_LAST);
        }
        addItemViewAfter(itemView2, itemView);
        itemView2.appendAndFocus(remainingText, true);
    }

    private ItemView2 getItemView() {
        final ItemView2 itemView;

        if (sRecycledViews.size() > 0) {
            itemView = sRecycledViews.pop();
        } else {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            itemView = (ItemView2)inflater.inflate(R.layout.item_view, null);
        }
        final LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

        itemView.setListener(new ItemView2.Listener() {
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
                    ItemView2 previousView = getItemViewAt(index - 1);

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
                            ItemView2 firstItemView = getItemViewAt(0);
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

            @Override
            public void onCancelTrash() {

            }
        });

        return itemView;
    }

    public ItemListView(Context context, Item parent) {
        super(context);
        long start = System.currentTimeMillis();

        mContext = context;

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

        for (int i = 0; i < mParent.children.size(); i++) {
            final ItemView2 itemView = getItemView();
            itemView.setItem(mParent.children.get(i));
            addItemViewAfter(itemView, null);
        }

        // Add the new item view
        mNewItem = createItem();
        ItemView2 newItemView = getItemView();
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
            ItemView2 itemView = getItemViewAt(i);
            itemView.recycle();
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
            ItemView2 itemView = getItemViewAt(i);
            Item item = itemView.getItem();
            item.order = order++;

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
}
