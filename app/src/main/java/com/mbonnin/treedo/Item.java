package com.mbonnin.treedo;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Stack;

/**
 * Created by martin on 14/08/14.
 */
public class Item {
    public String text;
    public boolean checked;
    public ArrayList<Item> children;
    public boolean isADirectory;
    public boolean isTrash;
    public boolean isRoot;

    public Item() {
        children = new ArrayList<Item>();
        text = "";
    }

    public Item shallowCopy() {
        Item clone = new Item();
        clone.text = text;
        clone.checked = checked;
        clone.isADirectory = isADirectory;
        clone.isRoot = isRoot;
        clone.isTrash = isTrash;

        return clone;
    }

    public Item deepCopy(int depth) {
        Item clone = shallowCopy();
        if (depth > 0) {
            for (Item child:children) {
                clone.children.add(child.deepCopy(depth - 1));
            }
        }
        return clone;
    }


    public void serialize(OutputStream outputStream, int depth) throws IOException {
        String line = "";
        if (checked) {
            line += "X";
        } else {
            line += " ";
        }
        for (int i = 0; i < depth; i++) {
            line += "    ";
        }
        line += Utils.encode(text);
        line += "\n";
        outputStream.write(line.getBytes());

        for (Item child:children){
            child.serialize(outputStream, depth + 1);
        }
    }

    public void serialize(OutputStream outputStream) throws IOException {
        // We do not want the
        for (Item child: children) {
            child.serialize(outputStream, 0);
        }
    }

    static public Item deserialize(InputStream inputStream) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        Stack<Item> itemStack = new Stack<Item>();

        Item root = Item.createRoot();
        root.isADirectory = true;
        itemStack.push(root);
        Item item;
        Item lastItem = root;
        int depth = 0;
        while ((line = r.readLine()) != null) {
            item = new Item();
            if (line.startsWith("X")) {
                item.checked = true;
            } else if (line.startsWith(" ")) {
                item.checked = false;
            } else {
                Utils.log("line should start by X or space: " + line);
                continue;
            }

            int spaceCount = 0;
            int i = 1;
            while (i < line.length() && line.charAt(i++) == ' ') {
                spaceCount++;
            }

            if (i == line.length()) {
                // empty line, ignore
                continue;
            }

            item.text = Utils.decode(line.substring(i - 1));

            if (spaceCount < depth) {
                while (depth > spaceCount) {
                    depth -= 4;
                    itemStack.pop();
                }
                if (depth != spaceCount) {
                    Utils.log("bad number of spaces: " + line);
                }
                addChild(itemStack, item);
            } else if (spaceCount == depth) {
                addChild(itemStack, item);
            } else if (spaceCount == depth + 4) {
                itemStack.push(lastItem);
                addChild(itemStack, item);
            } else {
                Utils.log("bad number of spaces: " + line);
                continue;
            }

            depth = spaceCount;
            lastItem = item;
        }

        detectDirectories(root);
        return root;
    }

    private static void detectDirectories(Item item) {
        boolean containsADirectory = false;
        for (Item child:item.children) {
            detectDirectories(child);
            if (child.isADirectory) {
                containsADirectory = true;
            }
        }

        if (containsADirectory) {
            for (Item child:item.children) {
                child.isADirectory = true;
            }
        }
    }

    private static void addChild(Stack<Item> itemStack, Item item) {
        Item parent = itemStack.peek();
        parent.isADirectory = true;
        parent.children.add(item);
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
                    trash = child;
                    break;
                }
            }
        }

        if (trash == null) {
            trash = new Item();
            trash.isADirectory = true;
            trash.isTrash = true;
            trash.text = context.getString(R.string.trash);
            children.add(0, trash);
        }

        return trash;
    }

    public static Item createRoot() {
        Item item = new Item();
        item.isADirectory = true;
        item.text = "root";
        item.isRoot = true;

        return item;
    }
}
