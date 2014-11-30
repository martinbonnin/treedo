package com.mbonnin.treedo;

import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;

import com.google.android.gms.common.GooglePlayServicesUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;


public class MainActivity extends ActionBarActivity implements BackupManager.OAuthManager {

    private static final int MENU_ID_ABOUT = 0;
    private static final int MENU_ID_ENABLE_BACKUP = 1;
    private static final int MENU_ID_DISABLE_BACKUP = 2;
    private static final int MENU_ID_IMPORT = 3;
    private static final int MENU_ID_FLUSH_DATABASE = 4;
    private static final int MENU_ID_LOGOUT = 5;
    private static final int MENU_ID_START_REORDER = 6;
    private static final int MENU_ID_STOP_REORDER = 7;

    private static final String PREFERENCE_ENABLE_BACKUP = "enable_backup";
    private static final String PREFERENCE_OAUTH_TOKEN = "oauth_token";
    private static final String PREFERENCE_OAUTH_EMAIL = "email";

    public static final int REQUEST_CODE_CHOOSE_ACCOUNT = 1;
    private static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 2;

    private FrameLayout mFrameLayout;
    private Stack<ItemListView> listViewStack = new Stack<ItemListView>();
    private android.support.v7.app.ActionBar mActionBar;
    private BackupManager mBackupManager;
    private FrameLayout mTopLayout;
    private com.mbonnin.treedo.ProgressBar mProgressBar;
    private int mShowProgressBar;

    BackupManager.OAuthTokenCallback mOAuthCallback;
    private String mOAuthScope;
    private String mOAuthEmail;
    private String mOAuthToken;

    private boolean mIsDebuggable;
    private long mLastSaveTime;
    private boolean mReordering;
    private ImageView mFab;

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
            listViewStack.peek().updateItemsTextAndChecked();
        }

        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        RelativeLayout relativeLayout = new RelativeLayout(this);
        relativeLayout.setGravity(Gravity.BOTTOM);

        // a dummy layout used to intercept the focus at startup;
        LinearLayout dummyLayout = new LinearLayout(this);
        dummyLayout.setFocusableInTouchMode(true);
        relativeLayout.addView(dummyLayout, 0, 0);

        ItemListView listView = new ItemListView(this, item);
        relativeLayout.addView(listView, layoutParams);

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

        mFab.animate().translationY(0).setDuration(300).start();
        listViewStack.peek().mScrollView.setOnScrollChangedListener(new ObservableScrollView.OnScrollChangedListener() {
            private int mLastScrollOffset;
            @Override
            public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
                if (Math.abs(t - mLastScrollOffset) > Utils.toPixels(20)) {
                    if (t > mLastScrollOffset) {
                        mFab.animate().translationY(Utils.toPixels(80)).setDuration(300).start();
                    } else {
                        //mFab.animate().translationY(0).setDuration(300).start();
                    }
                }
                mLastScrollOffset = t;
            }
        });
        saveData(false);
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
        oldListView.updateItemsTextAndChecked();
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

        saveData(false);

        mFab.animate().translationY(0).setDuration(300).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIsDebuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;

        Utils.init(getApplicationContext(), mIsDebuggable);


        mOAuthToken = getPreferences(MODE_PRIVATE).getString(PREFERENCE_OAUTH_TOKEN, "");
        mOAuthEmail = getPreferences(MODE_PRIVATE).getString(PREFERENCE_OAUTH_EMAIL, "");
        mBackupManager = new BackupManager(this, this, mOAuthToken);

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

        mFab = new ImageView(this);
        layoutParams.width = Utils.toPixels(60);
        layoutParams.height = Utils.toPixels(60);
        layoutParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        layoutParams.bottomMargin = Utils.toPixels(10);
        layoutParams.rightMargin = Utils.toPixels(8);
        mFab.setImageResource(R.drawable.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFab.animate().translationY(0).setDuration(300).start();
                listViewStack.peek().focusLastItem();
            }
        });
        mTopLayout.addView(mFab, layoutParams);

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
        if (mReordering) {
            MenuItem menuItem = menu.add(Menu.NONE, MENU_ID_STOP_REORDER, order++, getString(R.string.stop_reorder));
            menuItem.setIcon(R.drawable.check);
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        } else {
            SharedPreferences preferences = getPreferences(MODE_PRIVATE);

            MenuItem menuItem = menu.add(Menu.NONE, MENU_ID_START_REORDER, order++, getString(R.string.reorder));
            menuItem.setIcon(R.drawable.swap);
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

            menu.add(Menu.NONE, MENU_ID_IMPORT, order++, getString(R.string.action_import));

            if (preferences.getBoolean(PREFERENCE_ENABLE_BACKUP, false)) {
                menu.add(Menu.NONE, MENU_ID_DISABLE_BACKUP, order++, getString(R.string.action_disable_backup));
            } else {
                menu.add(Menu.NONE, MENU_ID_ENABLE_BACKUP, order++, getString(R.string.action_enable_backup));
            }

            if (mIsDebuggable) {
                menu.add(Menu.NONE, MENU_ID_FLUSH_DATABASE, order++, getString(R.string.flush_database));
            }
            menu.add(Menu.NONE, MENU_ID_ABOUT, order++, getString(R.string.action_about));

            if (!mOAuthEmail.equals("") || !mOAuthToken.equals("")) {
                menu.add(Menu.NONE, MENU_ID_LOGOUT, order++, getString(R.string.action_logout));
            }
        }
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
                enableBackup();
                return true;
            case MENU_ID_DISABLE_BACKUP:
                disableBackup();
                return true;
            case MENU_ID_FLUSH_DATABASE:
                flushDatabase();
                return true;
            case MENU_ID_IMPORT:
                importBackup();
                return true;
            case MENU_ID_LOGOUT:
                logout();
                return true;
            case MENU_ID_START_REORDER:
                startReorder();
                return true;
            case MENU_ID_STOP_REORDER:
                stopReorder();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startReorder() {
        listViewStack.peek().startReorder();

        mActionBar.setTitle("");
        mActionBar.setDisplayHomeAsUpEnabled(false);
        mReordering = true;
        invalidateOptionsMenu();
    }

    private void stopReorder() {
        listViewStack.peek().stopReorder();

        mActionBar.setCustomView(null);
        updateActionBar();

        mReordering = false;
        invalidateOptionsMenu();
    }

    private void logout() {
        mOAuthEmail = "";
        mOAuthToken = "";
        getPreferences(MODE_PRIVATE).edit().putString(PREFERENCE_OAUTH_EMAIL,"").apply();
        getPreferences(MODE_PRIVATE).edit().putString(PREFERENCE_OAUTH_TOKEN, "").apply();

        ClearTokenTask task = new ClearTokenTask(mOAuthToken) ;
        task.execute();
        mBackupManager.clearToken();
        disableBackup();
        invalidateOptionsMenu();
    }

    private void setRootItem(Item root) {
        mFrameLayout.removeAllViews();
        listViewStack.clear();
        Database.setRoot(root, this);
        Database.saveAsync(root);
        pushListView(root, false);
    }

    private void flushDatabase() {
        Item item = Item.createRoot();
        setRootItem(item);
    }

    @Override
    public void getNewOAuthToken(BackupManager.OAuthTokenCallback callback, String scope) {
        if (mOAuthCallback != null) {
            Utils.log("We cannot request 2 tokens at the same time");
            return;
        }

        mOAuthCallback = callback;
        mOAuthScope = scope;

        if (mOAuthEmail.equals("")) {
            String[] accountTypes = new String[]{"com.google"};
            Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                    accountTypes, false, null, null, null, null);

            startActivityForResult(intent, REQUEST_CODE_CHOOSE_ACCOUNT);
        } else {
            GetTokenTask task = new GetTokenTask(mOAuthEmail, scope);
            task.execute();
        }
    }

    private void importBackup() {
        final DialogBuilder builder = new DialogBuilder(this);

        if (!isConnected()) {
            showBackupDialog(R.string.connection_needed, false);
            return;
        }

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

        final BackupManager.BackupCallback backupCallback = new BackupManager.BackupCallback() {
            public void onBackupDone(Item backup) {
                if (backup != null) {
                    setRootItem(backup);
                } else {
                    showBackupDialog(R.string.get_backup_failed, false);
                }
                hideProgressBar();
            }
        };

        final AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BackupManager.Drive drive = (BackupManager.Drive) parent.getItemAtPosition(position);
                showProgressBar();

                mBackupManager.getBackup(drive, backupCallback);

                dialog.dismiss();
            }
        };

        BackupManager.DrivesCallback callback = new BackupManager.DrivesCallback() {
            @Override
            public void onDrives(ArrayList<BackupManager.Drive> drives) {
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

        mBackupManager.getBackupList(callback);
    }

    private void setBackupEnabled(boolean enabled) {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        preferences.edit().putBoolean(PREFERENCE_ENABLE_BACKUP, enabled).apply();
        invalidateOptionsMenu();
    }

    private boolean hasBackupEnabled() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        return preferences.getBoolean(PREFERENCE_ENABLE_BACKUP, false);
    }

    private void disableBackup() {
        setBackupEnabled(false);
        showBackupDialog(R.string.backup_disabled_successfully, false);
    }

    private void showAboutDialog() {
        DialogBuilder builder = new DialogBuilder(this);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        builder.setTitle(getString(R.string.app_name));
        builder.setIcon(R.drawable.treedo_blue);

        ScrollView scrollView = new ScrollView(this);
        TextView textView = new TextView(this);
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
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
        textView.setText(message_id);

        builder.setView(textView);

        builder.show();
    }

    public class ClearTokenTask extends AsyncTask <Void, Void, Void> {
        String mToken;
        Exception mException;

        ClearTokenTask(String token) {
            this.mToken = token;
        }

        /**
         * Executes the asynchronous job. This runs when you call execute()
         * on the AsyncTask instance.
         */
        @Override
        protected Void doInBackground(Void... params) {
            try {
                GoogleAuthUtil.clearToken(MainActivity.this, mToken);
            } catch (GoogleAuthException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    public class GetTokenTask extends AsyncTask <Void, Void, Integer> {
        String mEmail;
        String mScope;
        String mToken;
        Exception mException;

        GetTokenTask(String email, String scope) {
            this.mScope = scope;
            this.mEmail = email;
        }

        /**
         * Executes the asynchronous job. This runs when you call execute()
         * on the AsyncTask instance.
         */
        @Override
        protected Integer doInBackground(Void... params) {
            try {
                mToken = GoogleAuthUtil.getToken(MainActivity.this, mEmail, mScope);
            } catch (Exception e) {
                mException = e;
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            if (mException != null) {
                if (mException instanceof GooglePlayServicesAvailabilityException) {
                    // The Google Play services APK is old, disabled, or not present.
                    // Show a dialog created by Google Play services that allows
                    // the user to update the APK
                    int statusCode = ((GooglePlayServicesAvailabilityException)mException)
                            .getConnectionStatusCode();
                    Dialog dialog = GooglePlayServicesUtil.getErrorDialog(statusCode,
                            MainActivity.this,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                    dialog.show();
                } else if (mException instanceof UserRecoverableAuthException) {
                    // Unable to authenticate, such as when the user has not yet granted
                    // the app access to the account, but the user can fix this.
                    // Forward the user to an activity in Google Play services.
                    Intent intent = ((UserRecoverableAuthException)mException).getIntent();
                    startActivityForResult(intent,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                } else {
                    // IO exception or other
                    mOAuthCallback.onOAuthToken(mToken);
                    mOAuthCallback = null;
                }
            } else {
                mOAuthToken = mToken;
                getPreferences(MODE_PRIVATE).edit().putString(PREFERENCE_OAUTH_TOKEN, mToken).apply();
                mOAuthCallback.onOAuthToken(mToken);
                mOAuthCallback = null;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CHOOSE_ACCOUNT) {
            if (mOAuthCallback == null) {
                Utils.log("onActivityResult with mOAuthCallback null");
                return;
            }

            if (resultCode == RESULT_OK) {
                Utils.log("User account ok");
                mOAuthEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                getPreferences(MODE_PRIVATE).edit().putString(PREFERENCE_OAUTH_EMAIL, mOAuthEmail).apply();
                GetTokenTask task = new GetTokenTask(mOAuthEmail, mOAuthScope);
                task.execute();

                invalidateOptionsMenu();
            } else {
                mOAuthCallback.onOAuthToken(null);
                mOAuthCallback = null;
            }
        } else if (requestCode == REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR) {
            if (resultCode == RESULT_OK) {
                GetTokenTask task = new GetTokenTask(mOAuthEmail, mOAuthScope);
                task.execute();
            } else {
                mOAuthCallback.onOAuthToken(null);
                mOAuthCallback = null;
            }
        }
    }

    private boolean isConnected() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    private void putBackup() {
        BackupManager.SaveCallback callback = new BackupManager.SaveCallback() {
            @Override
            public void onSave(boolean success) {
            }
        };
        mBackupManager.putBackup(Database.getRoot(MainActivity.this), callback);
    }
    private void enableBackup() {
        if (!isConnected()) {
            showBackupDialog(R.string.connection_needed, false);
            return;
        }

        showProgressBar();

        final BackupManager.ConnectCallback callback = new BackupManager.ConnectCallback() {
            @Override
            public void onConnect(boolean success) {
                if (success) {
                    setBackupEnabled(true);
                    showBackupDialog(R.string.backup_enabled_successfully, true);
                    putBackup();
                } else {
                    showBackupDialog(R.string.backup_enabling_failed, false);
                }
                hideProgressBar();
            }
        };

        mBackupManager.connect(callback);
    }

    protected void onStop() {
        super.onStop();
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (!listViewStack.empty()) {
            ItemListView listView = listViewStack.peek();
            listView.updateItemsTextAndChecked();
        }

        saveData(true);
    }

    private void saveData(boolean force) {
        if (!force && System.currentTimeMillis() - mLastSaveTime < 5000) {
            return;
        }

        if (hasBackupEnabled()) {
            putBackup();
        }

        mLastSaveTime = System.currentTimeMillis();
        Database.saveAsync(Database.getRoot(this));
        Database.sync();
    }
}
