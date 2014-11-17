package com.mbonnin.treedo;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by martin on 17/11/14.
 */
public class BackupAdapter implements ListAdapter {
    ArrayList<BackupManager.GDrive> mGDrives;
    Context mContext;

    public BackupAdapter(Context context, ArrayList<BackupManager.GDrive> gDrives) {
        mGDrives = gDrives;
        mContext = context;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public int getCount() {
        return mGDrives.size();
    }

    @Override
    public Object getItem(int position) {
        return mGDrives.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.backup_item, null);
        TextView textView = (TextView)view.findViewById(R.id.label);

        textView.setText(mGDrives.get(position).title);
        return view;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }
}
