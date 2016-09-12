package com.mbonnin.treedo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;

/**
 * Created by martin on 9/12/16.
 */
@EViewGroup(R.layout.mainview)
public class MainView extends LinearLayout {
    @ViewById
    RecyclerView recyclerView;
    @ViewById
    Toolbar toolbar;
    @ViewById
    Toolbar editToolbar;

    MainActivity mMainActivity;
    private NodeAdapter mNodeAdapter;
    private Handler mHandler;
    private Node mParent;

    @Bean
    Clipboard clipboard;
    @Bean
    DB mDb;
    private ItemTouchHelper mItemTouchHelper;

    public MainView(Context context) {
        super(context);
    }

    @AfterViews
    void afterViews() {
        setOrientation(VERTICAL);
    }

    private View.OnClickListener mNavigationClickListener = v -> mMainActivity.onBackPressed();

    private MenuItem.OnMenuItemClickListener mOnAboutClickListener = item -> {
        mMainActivity.showAboutDialog();
        return true;
    };

    private MenuItem.OnMenuItemClickListener mOnTrashClickListener = item -> {
        ArrayList<Node> list = mNodeAdapter.cutSelectedNodes();

        for (Node node : list) {
            node.parent = mDb.getTrash();
            mDb.getTrash().childList.add(node);
        }

        setEditMode(false);
        mMainActivity.snackBar(getResources().getQuantityString(R.plurals.trash, list.size(), list.size()));

        return true;
    };

    private MenuItem.OnMenuItemClickListener mOnCutClickListener = item -> {
        ArrayList<Node> list = mNodeAdapter.cutSelectedNodes();

        clipboard.setData(list);

        setEditMode(false);
        mMainActivity.snackBar(getResources().getQuantityString(R.plurals.cut, list.size(), list.size()));
        return true;
    };

    private MenuItem.OnMenuItemClickListener mOnCopyClickListener = item -> {
        ArrayList<Node> list = mNodeAdapter.copySelectedNodes();

        clipboard.setData(list);

        setEditMode(false);
        mMainActivity.snackBar(getResources().getQuantityString(R.plurals.copy, list.size(), list.size()));
        return true;
    };

    private MenuItem.OnMenuItemClickListener mOnPasteClickListener = item -> {
        ArrayList<Node> list = clipboard.getData();
        if (list == null) {
            mMainActivity.snackBar(getResources().getString(R.string.nothingToPaste));
            return true;
        }

        clipboard.clear();
        mNodeAdapter.addData(list);

        setEditMode(false);
        mMainActivity.snackBar(getResources().getQuantityString(R.plurals.pasted, list.size(), list.size()));
        return true;
    };

    private MenuItem.OnMenuItemClickListener mOnNewFolderClickListener = item -> {
        Context context = getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.create_folder_dialog, null);
        EditText editText = (EditText) view.findViewById(R.id.editText);

        Dialog dialog = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.new_folder))
                .setView(view)
                .setPositiveButton(context.getString(R.string.ok), (dialog1, which) -> {
                    mNodeAdapter.createFolder(editText.getText().toString());
                    dialog1.dismiss();
                    mMainActivity.openSoftInput(editText);
                }).setNegativeButton(context.getString(R.string.cancel), (dialog1, which) -> {
                    dialog1.dismiss();
                })
                .create();

        dialog.show();

        editText.requestFocus();
        mMainActivity.openSoftInput(editText);

        return true;
    };

    private MenuItem.OnMenuItemClickListener mRenameFolderClickListener = item -> {
        Context context = getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.create_folder_dialog, null);
        EditText editText = (EditText) view.findViewById(R.id.editText);
        editText.setText(mParent.text);

        Dialog dialog = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.rename_folder))
                .setView(view)
                .setPositiveButton(context.getString(R.string.ok), (dialog1, which) -> {
                    mParent.text = editText.getText().toString();
                    toolbar.setTitle(mParent.text);
                    dialog1.dismiss();
                    mMainActivity.openSoftInput(editText);
                }).setNegativeButton(context.getString(R.string.cancel), (dialog1, which) -> {
                    dialog1.dismiss();
                })
                .create();

        dialog.show();

        editText.requestFocus();
        mMainActivity.openSoftInput(editText);

        return true;
    };

    private MenuItem.OnMenuItemClickListener mOnEditClickListener = item -> {
        setEditMode(true);

        return true;
    };

    private void setEditMode(boolean editMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mMainActivity.getWindow().setStatusBarColor(editMode ? Color.BLACK : mMainActivity.getResources().getColor(R.color.muted_700));
        }
        editToolbar.setVisibility(editMode ? VISIBLE : GONE);
        editToolbar.setTitle("");
        mNodeAdapter.setEditMode(editMode);
        mDb.save();
    }

    Runnable mEnsureFocusVisibleRunnable = new Runnable() {

        @Override
        public void run() {
            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            int focusPosition = mNodeAdapter.getFocusPosition();
            if (focusPosition >= 0) {
                if (focusPosition < layoutManager.findFirstVisibleItemPosition()
                        || focusPosition > layoutManager.findLastVisibleItemPosition()) {
                    recyclerView.smoothScrollToPosition(focusPosition);
                }
            }
            mHandler.removeCallbacks(this);
        }
    };

    public void setNode(MainActivity activity, Node parent) {
        mParent = parent;
        mMainActivity = activity;

        mHandler = new Handler();
        mNodeAdapter = NodeAdapter_.getInstance_(activity);
        mNodeAdapter.setSelectionListener(count -> editToolbar.setTitle(count + ""));
        mNodeAdapter.setNode(parent);

        mNodeAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                mHandler.post(mEnsureFocusVisibleRunnable);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(mNodeAdapter);
        recyclerView.setHasFixedSize(true);

        mItemTouchHelper = new ItemTouchHelper(new MyItemTouchHelperCallback(mNodeAdapter));
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        mNodeAdapter.setOnHandleClickedListener(viewHolder -> mItemTouchHelper.startDrag(viewHolder));


        setEditMode(false);

        setupToolbar();
        setupEditToolbar();
    }

    private void setupToolbar() {
        Menu menu = toolbar.getMenu();
        int order = 0;

        if (mParent == mDb.getRoot()) {
            toolbar.setTitle(R.string.app_name);
            toolbar.setNavigationIcon(null);
            toolbar.setNavigationOnClickListener(null);
        } else {
            Drawable d = getResources().getDrawable(R.drawable.ic_arrow_back_black_24dp);
            DrawableCompat.setTint(d.mutate(), Color.WHITE);
            toolbar.setNavigationIcon(d);
            toolbar.setNavigationOnClickListener(mNavigationClickListener);
            toolbar.setTitle((mParent == mDb.getTrash()) ? getContext().getString(R.string.trash) : mParent.text);
        }

        menu.clear();
        MenuItem menuItem;

        Drawable creatFolderDrawable = getResources().getDrawable(R.drawable.ic_create_new_folder_black_24dp);
        DrawableCompat.setTint(creatFolderDrawable, Color.WHITE);
        Drawable editDrawable = getResources().getDrawable(R.drawable.ic_create_black_24dp);
        DrawableCompat.setTint(editDrawable, Color.WHITE);
        Drawable beerDrawable = getResources().getDrawable(R.drawable.ic_beer);
        DrawableCompat.setTint(beerDrawable, Color.WHITE);

        menuItem = menu.add(Menu.NONE, Menu.NONE, order++, getContext().getString(R.string.beer));
        menuItem.setIcon(beerDrawable);
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuItem.setOnMenuItemClickListener(item -> true);

        if (mParent.getRoot() != mDb.getTrash()) {
            menuItem = menu.add(Menu.NONE, Menu.NONE, order++, getContext().getString(R.string.new_folder));
            menuItem.setIcon(creatFolderDrawable);
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menuItem.setOnMenuItemClickListener(mOnNewFolderClickListener);
        }

        menuItem = menu.add(Menu.NONE, Menu.NONE, order++, getContext().getString(R.string.edit));
        menuItem.setIcon(editDrawable);
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuItem.setOnMenuItemClickListener(mOnEditClickListener);

        if (mParent == mDb.getTrash()) {
            menuItem = menu.add(Menu.NONE, Menu.NONE, order++, getContext().getString(R.string.empty_trash));
            menuItem.setOnMenuItemClickListener(item -> {
                mNodeAdapter.clear();
                return true;
            });
        } else {
            menuItem = menu.add(Menu.NONE, Menu.NONE, order++, getContext().getString(R.string.open_trash));
            menuItem.setOnMenuItemClickListener(item -> {
                MainActivity.pushNodeG(mDb.getTrash());
                return true;
            });
        }

        if (mMainActivity.getPreference(MainActivity.PREFERENCE_FILE_ID, null) == null) {
            menuItem = menu.add(Menu.NONE, Menu.NONE, order++, getContext().getString(R.string.action_enable_backup));
            menuItem.setOnMenuItemClickListener(item -> {
                mMainActivity.enableBackup();
                return true;
            });
        } else {
            menuItem = menu.add(Menu.NONE, Menu.NONE, order++, getContext().getString(R.string.action_disable_backup));
            menuItem.setOnMenuItemClickListener(item -> {
                mMainActivity.disableBackup();
                return true;
            });

        }

        if (mParent.parent != null) {
            menuItem = menu.add(Menu.NONE, Menu.NONE, order++, getContext().getString(R.string.rename_folder));
            menuItem.setOnMenuItemClickListener(mRenameFolderClickListener);
        } else {
            menuItem = menu.add(Menu.NONE, Menu.NONE, order++, getContext().getString(R.string.action_import));
            menuItem.setOnMenuItemClickListener(item -> {
                mMainActivity.importBackup();
                return true;
            });
        }

        menuItem = menu.add(Menu.NONE, Menu.NONE, order++, getContext().getString(R.string.action_about));
        menuItem.setOnMenuItemClickListener(mOnAboutClickListener);
    }

    private void setupEditToolbar() {
        Menu menu = editToolbar.getMenu();
        int order = 0;

        Drawable d = getResources().getDrawable(R.drawable.ic_arrow_back_black_24dp);
        DrawableCompat.setTint(d.mutate(), Color.WHITE);
        editToolbar.setNavigationIcon(d);
        editToolbar.setNavigationOnClickListener(item -> setEditMode(false));

        menu.clear();
        MenuItem menuItem;

        Drawable trashDrawable = getResources().getDrawable(R.drawable.ic_delete_black_24dp);
        DrawableCompat.setTint(trashDrawable, Color.WHITE);
        Drawable cutDrawable = getResources().getDrawable(R.drawable.ic_content_cut_black_24dp);
        DrawableCompat.setTint(cutDrawable, Color.WHITE);
        Drawable copyDrawable = getResources().getDrawable(R.drawable.ic_content_copy_black_24dp);
        DrawableCompat.setTint(copyDrawable, Color.WHITE);
        Drawable pasteDrawable = getResources().getDrawable(R.drawable.ic_content_paste_black_24dp);
        DrawableCompat.setTint(pasteDrawable, Color.WHITE);

        if (mParent.getRoot() != mDb.getTrash()) {
            menuItem = menu.add(Menu.NONE, Menu.NONE, order++, getContext().getString(R.string.trash));
            menuItem.setIcon(trashDrawable);
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menuItem.setOnMenuItemClickListener(mOnTrashClickListener);
        }

        menuItem = menu.add(Menu.NONE, Menu.NONE, order++, getContext().getString(R.string.cut));
        menuItem.setIcon(cutDrawable);
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuItem.setOnMenuItemClickListener(mOnCutClickListener);

        menuItem = menu.add(Menu.NONE, Menu.NONE, order++, getContext().getString(R.string.copy));
        menuItem.setIcon(copyDrawable);
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuItem.setOnMenuItemClickListener(mOnCopyClickListener);

        menuItem = menu.add(Menu.NONE, Menu.NONE, order++, getContext().getString(R.string.paste));
        menuItem.setIcon(pasteDrawable);
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuItem.setOnMenuItemClickListener(mOnPasteClickListener);
    }

    public void refreshTitle() {
        toolbar.setTitle(mParent.text);
    }
}
