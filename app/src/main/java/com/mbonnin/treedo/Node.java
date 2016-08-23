package com.mbonnin.treedo;

import java.util.ArrayList;

/**
 * Created by martin on 8/23/16.
 */

public class Node {
    public String text = "";
    public boolean checked;
    public boolean folder;

    public transient boolean trash;

    /**
     * do not serialize parent, it creates cycles. Instead, we will rebuild the parent link at startup
     */
    public transient Node parent;

    public ArrayList<Node> childList = new ArrayList<>();

    public Node getRoot() {
        Node node = this;
        while(node.parent != null) {
            node = node.parent;
        }

        return node;
    }
}
