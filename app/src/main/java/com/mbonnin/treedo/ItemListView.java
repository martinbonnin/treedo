package com.mbonnin.treedo;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by martin on 21/08/14.
 */
public class ItemListView extends LinearLayout {
    private static final String TAG = "ItemListView";
    private Listener mListener;
    private View mEmptyView;
    private int mType;
    private ItemAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private Item mItem;
    private EditText mEditText;

    private void init() {
        setOrientation(VERTICAL);
        setBackgroundColor(Color.WHITE);
    }

    public ItemListView(Context context) {
        super(context);
        init();
    }

    public ItemListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ItemListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void onAddItem() {
        if (mEditText.getText().equals("")) {
            mEditText.clearFocus();

            InputMethodManager inputManager =
                    (InputMethodManager) getContext().
                            getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(
                    mEditText.getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);

            return;
        }

        Item item = new Item();
        item.text = mEditText.getText().toString();
        item.checked = false;
        item.isAFolder = mType == Item.TYPE_FOLDER;

        mAdapter.add(item);
        mEditText.setText("");
    }

    private void createListView(int type, boolean focus) {
        if (mEmptyView != null) {
            removeView(mEmptyView);
            mEmptyView = null;
        }

        mType = type;

        final View footer = LayoutInflater.from(getContext()).inflate(R.layout.listview_footer, null);
        mEditText = (EditText)footer.findViewById(R.id.edit_text);
        mEditText.setHint(mType == Item.TYPE_FOLDER ? R.string.new_folder : R.string.new_item);
        mEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mEditText.setSingleLine(true);
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    onAddItem();
                }
                return true;
            }
        });
        final Button button = (Button)footer.findViewById(R.id.add_button);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddItem();
            }
        });

        LinearLayout.LayoutParams layoutParams;

        layoutParams = new LinearLayout.LayoutParams(0,0);
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.weight = 1;

        mAdapter.setOnFolderClickedListener(new ItemAdapter.OnFolderClickedListener() {
            @Override
            public void onFolderClicked(Item item) {
                mListener.onFolderClicked(item);
            }
        });

        mRecyclerView = new RecyclerView(getContext());
        mRecyclerView.setAdapter(mAdapter);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.addItemDecoration(new ItemDecorator());
        mAdapter.setOnFolderClickedListener(new ItemAdapter.OnFolderClickedListener() {
            @Override
            public void onFolderClicked(Item item) {
                mListener.onFolderClicked(item);
            }
        });
        addView(mRecyclerView, layoutParams);

        layoutParams = new LinearLayout.LayoutParams(0,0);
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        layoutParams.weight = 0;
        addView(footer, layoutParams);

        if (focus) {
            mEditText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(mEditText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    public void setItem(final Item item) {
        mItem = item;
        mAdapter = new ItemAdapter(getContext(), item);

        if (item.children.size() == 0) {
            mEmptyView = LayoutInflater.from(getContext()).inflate(R.layout.listview_empty, null);
            Button subFoldersButton = (Button) mEmptyView.findViewById(R.id.subfolders_button);
            Button itemButton = (Button) mEmptyView.findViewById(R.id.items_button);

            subFoldersButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    createListView(Item.TYPE_FOLDER, true);
                }
            });

            itemButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    createListView(Item.TYPE_ITEM, true);
                }
            });

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            layoutParams.weight = 1;

            addView(mEmptyView, layoutParams);
        } else {
            int type = item.children.get(0).isAFolder ? Item.TYPE_FOLDER : Item.TYPE_ITEM;
            createListView(type, false);
        }
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public String getTitle() {
        return mItem.text;
    }

    public void setEditMode(boolean b) {
        Log.d(TAG, "setEditMode not implemented");
    }

    public static abstract class Listener {
        public abstract void onFolderClicked(Item item);
    }

    public boolean onTouchEvent(MotionEvent event) {
        // always eat the events so that we do not click the view below
        return true;
    }
}
