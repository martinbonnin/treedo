package com.mbonnin.treedo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import java.util.ArrayList;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by martin on 1/3/15.
 */
public class NodeAdapter extends RecyclerView.Adapter {
    private final Context mContext;
    private Node mParent;
    private Node mTrash;
    private Node mNewItemNode;
    private boolean mGrabable;
    private int mFocusPosition;
    private int mCursorIndex;

    private ArrayList<Node> mList = new ArrayList<>();

    public NodeAdapter(Context context, Node parent, Node trash) {
        mParent = parent;
        mTrash = trash;
        mNewItemNode = new Node();
        mContext = context;

        setGrabable(false);
    }

    private void populateList() {
        mList.clear();
        if (mTrash != null) {
            mList.add(mTrash);
        }
        mList.addAll(mParent.childList);
        if (!mGrabable) {
            mList.add(mNewItemNode);
        }
    }

    private int getPositionInParent(int position) {
        if (mTrash != null) {
            return position - 1;
        } else {
            return position;
        }
    }

    public void createFolder(String text) {
        Node node = new Node();
        node.text = text;
        node.folder = true;
        mParent.childList.add(0, node);
        node.parent = mParent;
        populateList();

        mFocusPosition = getPositionInAdapter(0);
        notifyDataSetChanged();
    }

    private int getPositionInAdapter(int positionInParent) {
        if (mTrash != null) {
            return positionInParent + 1;
        } else {
            return positionInParent;
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, ItemEditText.Listener, CompoundButton.OnCheckedChangeListener {
        private Node mNode;
        private int mPosition;

        public ViewHolder(View itemView) {
            super(itemView);
        }

        public void setNode(Node node, int position) {
            NodeView nodeView = (NodeView)itemView;
            boolean isTrash = node.trash;
            boolean isNew = node == mNewItemNode;

            nodeView.editText.setListener(null);

            mNode = node;
            mPosition = position;

            nodeView.setOnClickListener(null);

            nodeView.checkbox.setVisibility(node.folder || isNew ? GONE : VISIBLE);
            nodeView.checkbox.setChecked(node.checked);
            nodeView.folder.setVisibility(node.folder || isNew ? VISIBLE : GONE);
            if (isTrash) {
                nodeView.folder.setImageResource(R.drawable.trash);
            } else if (isNew) {
                nodeView.folder.setImageResource(R.drawable.plus_gray);
            } else {
                nodeView.folder.setImageResource(R.drawable.folder);
            }
            /**
             * be paranoid about carriage return, we don't want setText() to trigger the listener
             */
            nodeView.editText.setText(node.text.replaceAll("\n", ""));
            boolean editable = !node.folder && !mGrabable;
            nodeView.editText.setEditable(editable);
            nodeView.editText.setFocusable(editable);
            nodeView.editText.setFocusableInTouchMode(editable);
            nodeView.editText.setHint(isNew ? nodeView.getResources().getString(R.string.new_item) : "");
            nodeView.arrow.setVisibility((node.folder || mGrabable) ? VISIBLE : GONE);
            nodeView.arrow.setImageResource(mGrabable ? R.drawable.handle : R.drawable.arrow);

            nodeView.setOnClickListener(node.folder ? this: null);

            nodeView.checkbox.setOnCheckedChangeListener(this);

            if (mFocusPosition == position) {
                nodeView.editText.requestFocus();
                int selection = mCursorIndex;
                int textLength = nodeView.editText.getText().length();
                if (selection < 0) {
                    selection = textLength;
                } else if (selection > textLength) {
                    selection = textLength;
                }
                nodeView.editText.setSelection(selection);
            }

            nodeView.editText.setListener(this);
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int lengthBefore, int lengthAfter) {
            String str = s.toString();
            for (int i = start; i < start + lengthAfter; i++) {
                char c = str.charAt(i);
                if (c == '\n') {
                    String part1 = str.substring(0, i);
                    String part2 = str.substring(i + 1, str.length());
                    Node nodeToAdd = new Node();
                    nodeToAdd.parent = mParent;
                    int positionInParent = getPositionInParent(mPosition);

                    if (mNode == mNewItemNode) {
                        nodeToAdd.text = part1;
                        mNode.text = part2;
                        mParent.childList.add(positionInParent, nodeToAdd);
                    } else {
                        mNode.text = part1;
                        nodeToAdd.text = part2;
                        mParent.childList.add(positionInParent + 1, nodeToAdd);
                    }

                    mFocusPosition = mPosition + 1;
                    mCursorIndex = 0;

                    populateList();
                    notifyDataSetChanged();
                    return;
                }
            }

            mNode.text = str;
        }

        @Override
        public void onDeleteItem() {
            NodeView nodeView = (NodeView)itemView;
            String remainingText = nodeView.editText.getText().toString();
            if (mNode.parent != null) {
                mNode.parent.childList.remove(mNode);
                mNode.parent = null;
                populateList();
            }

            mFocusPosition = mPosition - 1;
            if (mFocusPosition >= 0) {
                Node focusNode = mList.get(mFocusPosition);
                if (!focusNode.trash) {
                    mCursorIndex = focusNode.text.length();
                    focusNode.text += remainingText;
                }
            }

            notifyDataSetChanged();
        }

        @Override
        public void onClick(View v) {
            MainActivity.pushNode(mNode);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mNode.checked = isChecked;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        NodeView nodeView = NodeView_.build(parent.getContext());

        RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nodeView.setLayoutParams(layoutParams);
        return new ViewHolder(nodeView);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        ((ViewHolder)holder).setNode(mList.get(position), position);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public void setGrabable(boolean grabable) {
        mGrabable = grabable;

        populateList();

        if (grabable) {
            mFocusPosition = -1;
        } else {
            /**
             * focus on the last item by default
             */
            mFocusPosition = getItemCount() - 1;
        }
        mCursorIndex = -1;

        notifyDataSetChanged();
    }

    public int getFocusPosition() {
        return mFocusPosition;
    }
}
