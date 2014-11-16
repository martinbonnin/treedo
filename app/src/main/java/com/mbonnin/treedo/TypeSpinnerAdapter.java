package com.mbonnin.treedo;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by martin on 21/08/14.
 */
public class TypeSpinnerAdapter implements SpinnerAdapter {
    public static final int POSITION_ITEM = 0;
    public static final int POSITION_DIRECTORY = 1;
    private Context mContext;
    private ArrayList<String> mArray = new ArrayList<String>();
    private static TypeSpinnerAdapter sTypeSpinnerAdapter;

    public static TypeSpinnerAdapter getTypeSpinnerAdapter(Context context) {
        if (sTypeSpinnerAdapter == null) {
            sTypeSpinnerAdapter = new TypeSpinnerAdapter(context);
        }
        return sTypeSpinnerAdapter;
    }
    public TypeSpinnerAdapter(Context context) {
        mContext = context;
        mArray.add(mContext.getString(R.string.item));
        mArray.add(mContext.getString(R.string.directory));
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView view = (TextView)LayoutInflater.from(mContext).inflate(R.layout.type_spinner_dropdownview, null);
        view.setText(mArray.get(position));

        return view;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public int getCount() {
        return mArray.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView view = (TextView)LayoutInflater.from(mContext).inflate(R.layout.type_spinner_view, null);
        view.setText(mArray.get(position));

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
}
