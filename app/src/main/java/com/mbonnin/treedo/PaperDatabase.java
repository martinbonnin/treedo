package com.mbonnin.treedo;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.google.gson.Gson;
import com.mbonnin.treedo.legacy.Database;
import com.mbonnin.treedo.legacy.Item;

import org.androidannotations.annotations.EBean;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Stack;

import io.paperdb.Paper;


@EBean(scope = EBean.Scope.Singleton)
public class PaperDatabase {
    private static final String TAG = "PaperDatabase";

    private static final java.lang.String KEY_DATA = "data";
    private static final String TRASH_NAME = "Trash";
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

        data.trash.text = TRASH_NAME;
        data.trash.trash = true;
        data.trash.folder = true;


        walk(data.root);
        walk(data.trash);

        this.mData = data;
    }

    private static void addChild(Stack<Node> nodeStack, Node node) {
        Node parent = nodeStack.peek();
        parent.folder = true;
        parent.childList.add(node);
    }

    public void serialize(Node node, OutputStream outputStream, int depth) throws IOException {
        String line = "";
        if (node.checked) {
            line += "X";
        } else {
            line += " ";
        }
        for (int i = 0; i < depth; i++) {
            line += "    ";
        }
        line += Utils.encode(node.text);
        line += "\n";
        outputStream.write(line.getBytes());

        for (Node child:node.childList){
            serialize(child, outputStream, depth + 1);
        }
    }

    public void toHumanFormat(Data data, OutputStream outputStream) throws IOException {
        for (Node child: data.root.childList) {
            serialize(child, outputStream, 0);
        }
        serialize(data.trash, outputStream, 0);
    }

    public static String readFullyAsString(InputStream inputStream, String encoding)
            throws IOException {
        return readFully(inputStream).toString(encoding);
    }

    private static ByteArrayOutputStream readFully(InputStream inputStream)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos;
    }

    public static Data fromJson(InputStream inputStream) throws IOException {
        String s = readFullyAsString(inputStream, "UTF-8");
        return (new Gson()).fromJson(s, Data.class);
    }

    public static String toJson(Data data) {
        return (new Gson()).toJson(data);
    }

    public static Data fromHumanFormat(InputStream inputStream) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        Stack<Node> nodeStack = new Stack<>();

        Node fakeNode = new Node();
        fakeNode.folder = true;

        nodeStack.push(fakeNode);
        Node node;
        Node lastNode = fakeNode;
        int depth = 0;
        while ((line = r.readLine()) != null) {
            node = new Node();
            if (line.startsWith("X")) {
                node.checked = true;
            } else if (line.startsWith("-")) {
                node.folder = true;
            } else if (line.startsWith(" ")) {
                node.checked = false;
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

            node.text = Utils.decode(line.substring(i - 1));

            if (spaceCount < depth) {
                while (depth > spaceCount) {
                    depth -= 4;
                    nodeStack.pop();
                }
                if (depth != spaceCount) {
                    Utils.log("bad number of spaces: " + line);
                }
                addChild(nodeStack, node);
            } else if (spaceCount == depth) {
                addChild(nodeStack, node);
            } else if (spaceCount == depth + 4) {
                nodeStack.push(lastNode);
                addChild(nodeStack, node);
            } else {
                Utils.log("bad number of spaces: " + line);
                continue;
            }

            depth = spaceCount;
            lastNode = node;
        }

        Node trash = null;
        Iterator<Node> iterator = fakeNode.childList.iterator();
        while (iterator.hasNext()) {
            Node child = iterator.next();
            if (child.text.equals(TRASH_NAME)) {
                trash = child;
                child.parent = null;
                iterator.remove();
                break;
            }
        }

        if (trash == null) {
            trash = new Node();
            trash.trash = true;
            trash.folder = true;
        }

        PaperDatabase.Data data = new PaperDatabase.Data();
        data.root = fakeNode;
        data.trash = trash;
        return data;
    }


    public static class Data {
        public Node root;
        public Node trash;
    }

    Data mData;

    public PaperDatabase(Context context) {
        mHandler = new Handler();

        mData = Paper.book().read(KEY_DATA);

        if (mData == null) {
            /**
             * try to use the previous DB
             */
            Item item = Database.getRoot(context);
            if (item != null && item.children.size() > 1) {
                mData = item.toData(context);
            }
        }
        if (mData == null) {
            /**
             * Load the tutorial
             */
            try {
                mData = fromHumanFormat(context.getResources().openRawResource(R.raw.tutorial));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
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
