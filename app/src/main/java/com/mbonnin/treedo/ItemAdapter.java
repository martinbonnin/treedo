package com.mbonnin.treedo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

/**
 * Created by martin on 1/3/15.
 */
public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {
    private final Context mContext;
    private Item mItem;
    OnFolderClickedListener mOnFolderClickedListener;
    private boolean mGrabable;

    public ItemAdapter(Context context, Item item) {
        mItem = item;
        mContext = context;
    }

    public void setOnFolderClickedListener(OnFolderClickedListener onFolderClickedListener) {
        mOnFolderClickedListener = onFolderClickedListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ItemView mItemView;
        public ViewHolder(ItemView itemView) {
            super(itemView);
            mItemView = itemView;
        }
    }
    @Override
    public ItemAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemView itemView = (ItemView)LayoutInflater.from(mContext).inflate(R.layout.item_view, null);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ItemAdapter.ViewHolder holder, int position) {
        Item item = mItem.children.get(position);
        holder.mItemView.cancelTranslation();
        holder.mItemView.setItem(item);
        holder.mItemView.setGrabable(mGrabable);
        holder.mItemView.setListener(new ItemView.Listener() {

            @Override
            public void onArrowClicked() {
                mOnFolderClickedListener.onFolderClicked(holder.mItemView.getItem());
            }
        });

    }

    @Override
    public int getItemCount() {
        return mItem.children.size();
    }

    public void add(Item item) {
        mItem.children.add(item);
        notifyDataSetChanged();
    }

    public abstract static class OnFolderClickedListener {
        public abstract void onFolderClicked(Item item);
    }

    public void setGrablable(boolean grabable) {
        mGrabable = grabable;
        notifyDataSetChanged();
    }
}
