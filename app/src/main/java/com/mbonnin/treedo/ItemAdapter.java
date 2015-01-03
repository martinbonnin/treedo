package com.mbonnin.treedo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by martin on 1/3/15.
 */
public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {
    private final Context mContext;
    private Item mItem;

    public ItemAdapter(Context context, Item item) {
        mItem = item;
        mContext = context;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ItemView2 mItemView;
        public ViewHolder(ItemView2 itemView) {
            super(itemView);
            mItemView = itemView;
        }
    }
    @Override
    public ItemAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemView2 itemView = (ItemView2)LayoutInflater.from(mContext).inflate(R.layout.item_view_2, null);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ItemAdapter.ViewHolder holder, int position) {
        Item item = mItem.children.get(position);
        holder.mItemView.cancelTranslation();
        holder.mItemView.setItem(item);

    }

    @Override
    public int getItemCount() {
        return mItem.children.size();
    }

    public void add(Item item) {
        mItem.children.add(item);
        notifyDataSetChanged();
    }
}
