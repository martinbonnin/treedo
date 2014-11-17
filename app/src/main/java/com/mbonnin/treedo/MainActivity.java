package com.mbonnin.treedo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.mbonnin.treedo.BackupManager.GDrive;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;


public class MainActivity extends ActionBarActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static final int MENU_ID_ABOUT = 0;
    private static final int MENU_ID_ENABLE_BACKUP = 1;
    private static final int MENU_ID_DISABLE_BACKUP = 2;
    private static final int MENU_ID_IMPORT = 3;

    private static final String PREFERENCE_ENABLE_BACKUP = "enable_backup";

    public static final int REQUEST_CODE_BACKUP_RESOLVED = 1;

    private FrameLayout mFrameLayout;
    private Stack<ItemListView> listViewStack = new Stack<ItemListView>();
    private android.support.v7.app.ActionBar mActionBar;
    private BackupManager mBackupManager;
    private boolean mBackupManagerFirstSetup;
    private FrameLayout mTopLayout;
    private com.mbonnin.treedo.ProgressBar mProgressBar;
    private int mShowProgressBar;

    private void updateActionBar() {
        if (listViewStack.size() > 1) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setTitle(listViewStack.peek().getTitle());
        } else {
            mActionBar.setDisplayHomeAsUpEnabled(false);
            mActionBar.setTitle(R.string.app_name);
        }
    }

    private void pushListView(Item item, boolean animate) {
        long start = System.currentTimeMillis();
        if (!listViewStack.empty()) {
            listViewStack.peek().sync();
        }

        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        RelativeLayout relativeLayout = new RelativeLayout(this);
        relativeLayout.setGravity(Gravity.BOTTOM);

        // a dummy layout used to intercept the focus at startup;
        LinearLayout dummyLayout = new LinearLayout(this);
        dummyLayout.setFocusableInTouchMode(true);
        relativeLayout.addView(dummyLayout, 0, 0);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        relativeLayout.addView(scrollView, layoutParams);

        ItemListView listView = new ItemListView(this, item);
        listView.setOrientation(LinearLayout.VERTICAL);
        listView.setBackgroundColor(Color.WHITE);
        scrollView.addView(listView, layoutParams);

        listView.setListener(new ItemListView.Listener() {
            @Override
            public void onDirectoryClicked(Item item) {
                pushListView(item, true);
            }
        });
        Utils.log("pushListView1() took " + (System.currentTimeMillis() - start) + " ms");

        mFrameLayout.addView(relativeLayout, layoutParams);

        if (animate) {
            Display display = getWindowManager().getDefaultDisplay();
            relativeLayout.setTranslationX(display.getWidth());
            ViewPropertyAnimator animator = relativeLayout.animate();
            animator.translationX(0).setDuration(300).start();
            animator.setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mFrameLayout.clearFocus();
                }
            });
        } else {
            mFrameLayout.clearFocus();
        }

        listViewStack.push(listView);

        updateActionBar();
        Utils.log("pushListView2() took " + (System.currentTimeMillis() - start) + " ms");
    }

    public void onBackPressed() {
        popListView();
    }

    private void popListView() {
        int count = mFrameLayout.getChildCount();
        if (count == 1) {
            finish();
            return;
        }

        ItemListView oldListView = listViewStack.pop();
        oldListView.sync();
        oldListView.recycle();

        updateActionBar();

        final View relativeLayout = mFrameLayout.getChildAt(count -1);

        Display display = getWindowManager().getDefaultDisplay();
        relativeLayout.setTranslationX(0);
        ViewPropertyAnimator animator = relativeLayout.animate();
        animator.translationX(display.getWidth()).setDuration(300).start();
        animator.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mFrameLayout.removeView(relativeLayout);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.init(this, (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)));
        mBackupManager = new BackupManager(this, new Handler(), this);

        mTopLayout = new FrameLayout(this);
        mFrameLayout = new FrameLayout(this);

        mActionBar = getSupportActionBar();
        mActionBar.setLogo(R.drawable.treedo);

        mProgressBar = new com.mbonnin.treedo.ProgressBar(this);
        mProgressBar.setVisibility(View.GONE);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.topMargin = 0;
        mTopLayout.addView(mFrameLayout);
        mTopLayout.addView(mProgressBar, layoutParams);

        setContentView(mTopLayout);

        Item rootItem = Database.getRoot(this);
        pushListView(rootItem, false);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(mTopLayout);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int order = 0;
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        menu.add(Menu.NONE, MENU_ID_IMPORT, order++, getString(R.string.action_import));

        if (preferences.getBoolean(PREFERENCE_ENABLE_BACKUP, false)) {
            menu.add(Menu.NONE, MENU_ID_DISABLE_BACKUP, order++, getString(R.string.action_disable_backup));
        } else {
            menu.add(Menu.NONE, MENU_ID_ENABLE_BACKUP, order++, getString(R.string.action_enable_backup));
        }

        menu.add(Menu.NONE, MENU_ID_ABOUT, order++, getString(R.string.action_about));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id) {
            case MENU_ID_ABOUT:
                showAboutDialog();
                return true;
            case android.R.id.home:
                if (listViewStack.size() > 1) {
                    popListView();
                }
                return true;
            case MENU_ID_ENABLE_BACKUP:
                mBackupManagerFirstSetup = true;
                enableBackup();
                return true;
            case MENU_ID_DISABLE_BACKUP:
                disableBackup();
                return true;
            case MENU_ID_IMPORT:
                importBackup();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    class ImportTask extends AsyncTask<Void, Void, Integer> {
        GDrive mDrive;
        Item mItem;

        public ImportTask(GDrive drive) {
            mDrive = drive;
            showProgressBar();
        }

        @Override
        protected Integer doInBackground(Void... params) {
            BackupManager.GFile file = mBackupManager.blockingOpenFile(mDrive);

            try {
                mItem = Item.deserialize(file.inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }

            mBackupManager.blockingCloseFile(file);
            return 0;
        }

        protected void onPostExecute(Integer dummy) {
            if (mItem != null) {
                mFrameLayout.removeAllViews();
                listViewStack.clear();
                Database.setRoot(mItem);
                pushListView(mItem, false);
            }
            hideProgressBar();
        }
    }

    private void importBackup() {
        final DialogBuilder builder = new DialogBuilder(this);

        builder.setTitle(getString(R.string.select_backup));
        builder.setIcon(R.drawable.backup_import);
        builder.setView(getLayoutInflater().inflate(R.layout.progress_bar, null));
        builder.setButtonLabel(getString(R.string.cancel));
        builder.setListener(new DialogBuilder.Listener() {
            @Override
            public void onButtonClick() {

            }
        });

        final AlertDialog dialog = builder.show();

        final AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                GDrive drive = (GDrive) parent.getItemAtPosition(position);
                ImportTask task = new ImportTask(drive);

                dialog.dismiss();
                task.execute();
            }
        };

        BackupManager.DrivesCallback callback = new BackupManager.DrivesCallback() {
            @Override
            public void onDrives(ArrayList<BackupManager.GDrive> drives) {
                if (drives != null) {
                    Context context = MainActivity.this;
                    BackupAdapter adapter = new BackupAdapter(context, drives);
                    ListView listView = new ListView(context);
                    listView.setAdapter(adapter);
                    listView.setOnItemClickListener(clickListener);
                    builder.setView(listView);
                }
            }
        };

        mBackupManager.getBackups(callback);
    }

    private void disableBackup() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        preferences.edit().putBoolean(PREFERENCE_ENABLE_BACKUP, false).apply();
        invalidateOptionsMenu();

        mBackupManager.disconnect();

        showBackupDialog(R.string.backup_disabled_successfully, false);
    }

    private void showAboutDialog() {
        DialogBuilder builder = new DialogBuilder(this);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        builder.setTitle(getString(R.string.action_about));
        builder.setIcon(R.drawable.about);

        ScrollView scrollView = new ScrollView(this);
        TextView textView = new TextView(this);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        textView.setText(Html.fromHtml(getString(R.string.about)));
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        scrollView.addView(textView, layoutParams);

        builder.setView(scrollView);

        builder.show();
    }

    public void showProgressBar() {
        if (mShowProgressBar == 0) {
            mProgressBar.setVisibility(View.VISIBLE);
        }

        mShowProgressBar++;
    }

    public void hideProgressBar() {
        mShowProgressBar--;
        if (mShowProgressBar == 0) {
            mProgressBar.setVisibility(View.GONE);
        }
    }


    private void showBackupDialog(int message_id, boolean success) {
        DialogBuilder builder = new DialogBuilder(this);

        if (success) {
            builder.setIcon(R.drawable.backup_on);
        } else {
            builder.setIcon(R.drawable.backup_off);
        }
        builder.setTitle(getString(R.string.app_name));
        TextView textView = new TextView(this);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
        textView.setText(message_id);

        builder.setView(textView);

        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_BACKUP_RESOLVED) {
            if (resultCode == RESULT_OK) {
                Utils.log("Backup resolution ok, try again");
                mBackupManager.retry();
            } else {
                /*AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.cannot_enable_backup);
                builder.show();*/
                mBackupManager.connectAborted();
            }
        }
    }

    private void enableBackup() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        preferences.edit().putBoolean(PREFERENCE_ENABLE_BACKUP, true).apply();
        invalidateOptionsMenu();

        showProgressBar();

        final BackupManager.ConnectCallback callback = new BackupManager.ConnectCallback() {
            @Override
            public void onConnected(boolean success) {
                if (success) {
                    showBackupDialog(R.string.backup_enabled_successfully, true);
                }
                hideProgressBar();
            }
        };

        mBackupManager.connect(callback);
    }

    protected void onStop() {
        super.onStop();
        // save the current view
        if (!listViewStack.empty()) {
            listViewStack.peek().sync();
        }

        BackupManager.SaveCallback callback = new BackupManager.SaveCallback() {
            @Override
            public void onSave(boolean success) {
                hideProgressBar();
            }
        };

        showProgressBar();
        mBackupManager.save(Database.getRoot(this), callback);
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(MainActivity.this, REQUEST_CODE_BACKUP_RESOLVED);
            } catch (IntentSender.SendIntentException e) {
                mBackupManager.connectAborted();
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), MainActivity.this, 0).show();
            mBackupManager.connectAborted();
        }
    }
}
