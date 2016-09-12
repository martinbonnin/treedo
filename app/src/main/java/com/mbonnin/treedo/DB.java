package com.mbonnin.treedo;

import android.os.Handler;
import android.util.Log;


import org.androidannotations.annotations.EBean;

import io.paperdb.Paper;


@EBean(scope = EBean.Scope.Singleton)
public class DB {
    private static final String TAG = "DB";

    private static final java.lang.String KEY_DATA = "data";
    private final Handler mHandler;

    private long lastTime;

    public void setData(Data data) {
        if (data == null) {
            data = new Data();
        }

        if (data.root == null) {
            data.root = new Node();
        }

        data.root.folder = true;
        data.root.text = "Treedo";

        if (data.trash == null) {
            data.trash = new Node();
        }

        data.trash.text = "Trash";
        data.trash.trash = true;
        data.trash.folder = true;


        walk(data.root);
        walk(data.trash);

        this.mData = data;
    }

    public static class Data {
        public Node root;
        public Node trash;
    }

    Data mData;

    public DB() {
        mHandler = new Handler();

        mData = Paper.book().read(KEY_DATA);

        setData(mData);
    }

    private void walk(Node node) {
        if (node.text == null) {
            node.text = "";
        }
        for (Node child : node.childList) {
            child.parent = node;
            walk(child);
        }
    }

    public Node getRoot() {
        return mData.root;
    }

    public Node getTrash() {
        return mData.trash;
    }

    public void save() {
        if (System.currentTimeMillis() - lastTime < 5000) {
            mHandler.postDelayed(() -> save(), 5000);
        } else {
            forceSave();
        }
    }


    public void forceSave() {
        Log.d(TAG, "saving...");
        Paper.book().write(KEY_DATA, mData);
        lastTime = System.currentTimeMillis();
    }

    public Data getData() {
        return mData;
    }
}
