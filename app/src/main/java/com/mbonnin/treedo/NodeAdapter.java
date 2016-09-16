package com.mbonnin.treedo;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by martin on 1/3/15.
 */
@EBean
public class NodeAdapter extends RecyclerView.Adapter {
    private final Context mContext;
    private static final String TAG = "NodeAdapter";
    private HandleClickListener mHandleClickedListener;
    private boolean mInTrash;
    private Node mParent;
    private Node mNewItemNode;
    private boolean mEditMode;
    private int mFocusPosition;
    private int mCursorIndex;
    private boolean mSelectedItems[] = new boolean[16];
    private SelectionListener mSelectionListener;

    public void swap(int a, int b) {
        Collections.swap(mParent.childList, a, b);
        boolean tmp = mSelectedItems[a];
        mSelectedItems[a] = mSelectedItems[b];
        mSelectedItems[b] = tmp;

        populateList();
        notifyItemMoved(a, b);
    }

    public void clear() {
        mParent.childList.clear();
        populateList();
        notifyDataSetChanged();
    }

    interface SelectionListener {
        void selectionChanged(int newCount);
    }

    interface HandleClickListener {
        void onClick(RecyclerView.ViewHolder viewHolder);
    }

    @Bean
    PaperDatabase mDb;

    private ArrayList<Node> mList = new ArrayList<>();

    public NodeAdapter(Context context) {
        mContext = context;
    }

    void setOnHandleClickedListener(HandleClickListener listener) {
        mHandleClickedListener = listener;
    }

    public void setNode(Node parent) {
        mParent = parent;

        mNewItemNode = new Node();

        mInTrash = mParent.getRoot() == mDb.getTrash();

        setEditMode(false);
    }

    public void setSelectionListener(SelectionListener listener) {
        mSelectionListener = listener;
    }

    private void populateList() {
        mList.clear();
        mList.addAll(mParent.childList);
        if (!mEditMode && !mInTrash) {
            mList.add(mNewItemNode);
        }

        if (mSelectedItems.length < mList.size()) {
            boolean newArray[] = new boolean[mList.size()];
            System.arraycopy(mSelectedItems, 0, newArray, 0, mSelectedItems.length);
            mSelectedItems = newArray;
        }

        mDb.save();
    }

    public void createFolder(String text) {
        Node node = new Node();
        node.text = text;
        node.folder = true;
        int i;
        for (i = 0; i < mParent.childList.size(); i++) {
            if (!mParent.childList.get(i).folder) {
                break;
            }
        }
        mParent.childList.add(i, node);
        node.parent = mParent;
        populateList();

        mFocusPosition = 0;
        notifyDataSetChanged();
    }

    public ArrayList<Node> cutSelectedNodes() {
        ArrayList<Node> list = new ArrayList<>();
        int i = 0;
        Iterator<Node> it = mParent.childList.iterator();
        while (it.hasNext()) {
            Node node = it.next();
            if (mSelectedItems[i]) {
                node.parent = null;
                list.add(node);
                it.remove();
            }
            i++;
        }

        populateList();
        notifyDataSetChanged();
        return list;
    }

    public ArrayList<Node> copySelectedNodes() {
        ArrayList<Node> list = new ArrayList<>();
        int i = 0;
        Iterator<Node> it = mParent.childList.iterator();
        while (it.hasNext()) {
            Node node = it.next();
            if (mSelectedItems[i]) {
                Node node2 = node.deepCopy();
                list.add(node2);
            }
            i++;
        }
        return list;
    }

    public void addData(ArrayList<Node> list) {
        for (Node node : list) {
            node.parent = mParent;
            mParent.childList.add(node);
        }

        mFocusPosition = mParent.childList.size() - 1;
        populateList();
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, MyEditText.Listener, CompoundButton.OnCheckedChangeListener {
        private Node mNode;

        public ViewHolder(View itemView) {
            super(itemView);
        }

        public void setNode(Node node) {
            NodeView nodeView = (NodeView) itemView;
            boolean isTrash = node.trash;
            boolean isNew = node == mNewItemNode;

            nodeView.editText.setListener(null);

            mNode = node;

            nodeView.setOnClickListener(null);

            if (mEditMode && mSelectedItems[getAdapterPosition()]) {
                nodeView.setBackgroundColor(nodeView.getContext().getResources().getColor(R.color.vibrant_50));
            } else {
                nodeView.setBackgroundColor(Color.TRANSPARENT);
            }

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

            boolean editable = !node.folder && !mEditMode;
            nodeView.editText.setEditable(editable);
            nodeView.editText.setFocusable(editable);
            nodeView.editText.setFocusableInTouchMode(editable);
            nodeView.editText.setHint(isNew ? nodeView.getResources().getString(R.string.new_item) : "");
            nodeView.checkbox.setEnabled(editable);

            nodeView.arrow.setVisibility((node.folder || mEditMode) ? VISIBLE : GONE);
            nodeView.arrow.setImageResource(mEditMode ? R.drawable.handle : R.drawable.arrow);

            nodeView.setOnClickListener(mEditMode || node.folder ? this : null);

            nodeView.setOnTouchListener((v, event) -> {
                if (mNode.folder) {
                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        v.setBackgroundColor(Color.LTGRAY);
                    } else if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                        nodeView.setBackgroundColor(Color.TRANSPARENT);
                    }
                }
                return false;
            });

            nodeView.checkbox.setOnCheckedChangeListener(this);
            nodeView.arrow.setOnTouchListener((v, event) -> {
                if (mEditMode && mHandleClickedListener != null) {
                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        mHandleClickedListener.onClick(this);
                    }
                }
                return false;
            });

            if (mFocusPosition == getAdapterPosition()) {
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
            int position = getAdapterPosition();

            for (int i = start; i < start + lengthAfter; i++) {
                char c = str.charAt(i);
                if (c == '\n') {
                    String part1 = str.substring(0, i);
                    String part2 = str.substring(i + 1, str.length());
                    Node nodeToAdd = new Node();
                    nodeToAdd.parent = mParent;

                    if (mNode == mNewItemNode) {
                        nodeToAdd.text = part1;
                        mNode.text = part2;
                        mParent.childList.add(position, nodeToAdd);
                    } else {
                        mNode.text = part1;
                        nodeToAdd.text = part2;
                        mParent.childList.add(position + 1, nodeToAdd);
                    }

                    mFocusPosition = position + 1;
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
            NodeView nodeView = (NodeView) itemView;
            String remainingText = nodeView.editText.getText().toString();
            if (mNode.parent != null) {
                mNode.parent.childList.remove(mNode);
                mNode.parent = null;
                populateList();
            } else if (mNode == mNewItemNode) {
                mNode.text = "";
            }

            mFocusPosition = getAdapterPosition() - 1;
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
            int position = getAdapterPosition();
            if (mEditMode) {
                mSelectedItems[position] = !mSelectedItems[position];
                notifyItemChanged(position);
                if (mSelectionListener != null) {
                    int count = 0;
                    for (int i = 0; i < mSelectedItems.length; i++) {
                        if (mSelectedItems[i]) {
                            count++;
                        }
                    }
                    mSelectionListener.selectionChanged(count);
                }
            } else {
                MainActivity.pushNodeG(mNode);
            }
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
        ((ViewHolder) holder).setNode(mList.get(position));
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public void setEditMode(boolean editMode) {
        mEditMode = editMode;

        populateList();

        mFocusPosition = -1;
        if (editMode) {
            for (int i = 0; i < mSelectedItems.length; i++) {
                mSelectedItems[i] = false;
            }
        }

        mCursorIndex = -1;

        notifyDataSetChanged();
    }

    public int getFocusPosition() {
        return mFocusPosition;
    }
}
