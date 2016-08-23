package com.mbonnin.treedo;

import org.androidannotations.annotations.EBean;

import java.util.ArrayList;

/**
 * Created by martin on 9/12/16.
 */
@EBean(scope = EBean.Scope.Singleton)
public class Clipboard {

    private ArrayList<Node> mData;

    public void setData(ArrayList<Node> list) {
        mData = list;
    }

    public ArrayList<Node> getData() {
        return mData;
    }

    public void clear() {
        mData = null;
    }
}
