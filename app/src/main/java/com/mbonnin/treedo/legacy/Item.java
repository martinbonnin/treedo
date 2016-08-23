package com.mbonnin.treedo.legacy;

import android.content.Context;

import com.mbonnin.treedo.Node;
import com.mbonnin.treedo.PaperDatabase;
import com.mbonnin.treedo.R;
import com.mbonnin.treedo.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

/**
 * Created by martin on 14/08/14.
 */
public class Item {
    public static final int TYPE_FOLDER = 0;
    public static final int TYPE_ITEM = 1;
    public String text;
    public boolean checked;
    public ArrayList<Item> children;
    public boolean isAFolder;
    public boolean isTrash;
    public boolean isRoot;

    public Item() {
        children = new ArrayList<Item>();
        text = "";
    }

    public static Node toNode(Item item) {
        Node node = new Node();
        node.folder = item.isAFolder;
        node.checked = item.checked;
        node.text = item.text;
        node.trash = item.isTrash;

        for (Item childItem: item.children) {
            node.childList.add(toNode(childItem));
        }
        return node;
    }

    public PaperDatabase.Data toData(Context context) {
        findOrCreateTrash(context);

        Node root = toNode(this);
        Node trash = null;
        Iterator<Node> iterator = root.childList.iterator();
        while (iterator.hasNext()) {
            Node child = iterator.next();
            if (child.trash) {
                trash = child;
                iterator.remove();
                break;
            }
        }
        if (trash != null) {
            PaperDatabase.Data data = new PaperDatabase.Data();
            data.root = root;
            data.trash = trash;
            return data;
        }

        return null;
    }

    public Item findOrCreateTrash(Context context) {
        Item trash = null;

        for (Item child:children) {
            if (child.isTrash) {
                trash = child;
                break;
            }
        }
        if (trash == null) {
            for (Item child:children) {
                if (child.text.equals(context.getString(R.string.trash))) {
                    child.isTrash = true;
                    trash = child;
                    break;
                }
            }
        }

        if (trash == null) {
            trash = new Item();
            trash.isAFolder = true;
            trash.isTrash = true;
            trash.text = context.getString(R.string.trash);
            children.add(0, trash);
        }

        return trash;
    }

    public static Item createRoot() {
        Item item = new Item();
        item.isAFolder = true;
        item.text = "root";
        item.isRoot = true;

        return item;
    }
}
