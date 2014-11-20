package com.mbonnin.treedo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Stack;

/**
 * Created by martin on 14/08/14.
 */
public class Item implements Cloneable {
    public int order;
    public String text;
    public boolean checked;
    public ArrayList<Item> children;
    int parent;
    int id;
    public boolean isADirectory;

    public Item(int id) {
        children = new ArrayList<Item>();
        text = "";
        this.id = id;
    }

    public Item shallowCopy() {
        Item clone = new Item(id);
        clone.order = order;
        clone.text = text;
        clone.checked = checked;
        clone.parent = parent;
        clone.id = id;
        clone.isADirectory = isADirectory;

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

    @Override
    public String toString() {
        return String.format("%3d. %50s - %3d", id, text, parent);
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

        Collections.sort(children, new Comparator<Item>() {
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
        Stack<Integer> orderStack = new Stack<Integer>();

        int id = 0;
        int order = 0;

        Item root = new Item(id++);
        root.isADirectory = true;
        root.parent = -1;
        itemStack.push(root);
        orderStack.push(0);
        Item item;
        Item lastItem = root;
        int depth = 0;
        while ((line = r.readLine()) != null) {
            item = new Item(id++);
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
                    orderStack.pop();
                    order = orderStack.peek();
                }
                if (depth != spaceCount) {
                    Utils.log("bad number of spaces: " + line);
                }
                item.order = order;
                addChild(itemStack, item);
            } else if (spaceCount == depth) {
                item.order = order;
                addChild(itemStack, item);
            } else if (spaceCount == depth + 4) {
                itemStack.push(lastItem);
                orderStack.push(order);

                order = 0;

                item.order = order;
                addChild(itemStack, item);
            } else {
                Utils.log("bad number of spaces: " + line);
                continue;
            }

            depth = spaceCount;
            lastItem = item;
            order++;
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
        item.parent = parent.id;
    }
}
