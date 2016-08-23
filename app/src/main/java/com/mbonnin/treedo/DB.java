package com.mbonnin.treedo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.androidannotations.annotations.EBean;
import org.json.JSONObject;

import java.lang.reflect.Type;

import io.paperdb.Paper;


@EBean(scope = EBean.Scope.Singleton)
public class DB {

    private static final java.lang.String KEY_ROOT = "root";
    private static final java.lang.String KEY_TRASH = "trash";

    Gson gson;
    public Node root;
    public Node trash;

    public DB() {
        gson = new Gson();

        String str = Paper.book().read(KEY_ROOT);
        if (str != null) {
            root = gson.fromJson(str, Node.class);
        }
        if (root == null) {
            root = new Node();
        }

        if (!root.folder) {
            root.folder = true;
        }

        str = Paper.book().read(KEY_TRASH);
        if (str != null) {
            trash = gson.fromJson(str, Node.class);
        }
        if (trash == null) {
            trash = new Node();
        }

        trash.text = "Trash";

        if (!trash.folder) {
            trash.folder = true;
        }

        walk(root);
        walk(trash);
        trash.trash = true;
    }

    private void walk(Node node) {
        if (node.text == null) {
            node.text = "";
        }
        for (Node child: node.childList) {
            child.parent = node;
            walk(child);
        }
    }

    public Node getRoot() {
        return root;
    }

    public Node getTrash() {
        return trash;
    }

    public void save() {
        Paper.book().write(KEY_ROOT, gson.toJson(root));
        Paper.book().write(KEY_TRASH, gson.toJson(trash));
    }


}
