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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;

import com.google.android.gms.common.GooglePlayServicesUtil;

import java.util.ArrayList;
import java.util.Stack;


public class MainActivity extends ActionBarActivity implements RESTBackupManager.OAuthManager {

    private static final int MENU_ID_ABOUT = 0;
    private static final int MENU_ID_ENABLE_BACKUP = 1;
    private static final int MENU_ID_DISABLE_BACKUP = 2;

    private static final String PREFERENCE_ENABLE_BACKUP = "enable_backup";
    private static final String PREFERENCE_OAUTH_TOKEN = "oauth_token";

    public static final int REQUEST_CODE_CHOOSE_ACCOUNT = 1;
    private static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 2;
    private static final int MENU_ID_IMPORT = 3;
    private static final int MENU_ID_FLUSH_DATABASE = 4;

    private FrameLayout mFrameLayout;
    private Stack<ItemListView> listViewStack = new Stack<ItemListView>();
    private android.support.v7.app.ActionBar mActionBar;
    private RESTBackupManager mBackupManager;
    private FrameLayout mTopLayout;
    private com.mbonnin.treedo.ProgressBar mProgressBar;
    private int mShowProgressBar;

    RESTBackupManager.OAuthTokenCallback mOAuthCallback;
    private String mOAuthScope;
    private String mOAuthEmail;
    private boolean mIsDebuggable;

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

        saveData();
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

        saveData();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIsDebuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;

        Utils.init(this, mIsDebuggable);

        String lastOAuthToken = getPreferences(MODE_PRIVATE).getString(PREFERENCE_OAUTH_TOKEN, "");
        if (lastOAuthToken.equals("")) {
            lastOAuthToken = null;
        }
        mBackupManager = new RESTBackupManager(this, this, lastOAuthToken);

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

        if (mIsDebuggable) {
            menu.add(Menu.NONE, MENU_ID_FLUSH_DATABASE, order++, getString(R.string.flush_database));
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
        }
        return super.onOptionsItemSelected(item);
    }

    private void setRootItem(Item item) {
        mFrameLayout.removeAllViews();
        listViewStack.clear();
        Database.setRoot(item);
        Database.saveAsync(item);
        pushListView(item, false);
    }

    private void flushDatabase() {
        Item item = new Item(0);
        item.parent = -1;

        setRootItem(item);
    }

    @Override
    public void getNewOAuthToken(RESTBackupManager.OAuthTokenCallback callback, String scope) {
        if (mOAuthCallback != null) {
            Utils.log("We cannot request 2 tokens at the same time");
            return;
        }
        String[] accountTypes = new String[]{"com.google"};
        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                accountTypes, false, null, null, null, null);
        mOAuthCallback = callback;
        mOAuthScope = scope;

        startActivityForResult(intent, REQUEST_CODE_CHOOSE_ACCOUNT);
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

        final RESTBackupManager.BackupCallback backupCallback = new RESTBackupManager.BackupCallback() {
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
                RESTBackupManager.Drive drive = (RESTBackupManager.Drive) parent.getItemAtPosition(position);
                showProgressBar();

                mBackupManager.getBackup(drive, backupCallback);

                dialog.dismiss();
            }
        };

        RESTBackupManager.DrivesCallback callback = new RESTBackupManager.DrivesCallback() {
            @Override
            public void onDrives(ArrayList<RESTBackupManager.Drive> drives) {
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

    public class TokenTask extends AsyncTask <Void, Void, Integer> {
        String mEmail;
        String mScope;
        String mToken;
        Exception mException;

        TokenTask(String email, String scope) {
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
                if (mToken != null) {
                    getPreferences(MODE_PRIVATE).edit().putString(PREFERENCE_OAUTH_TOKEN, mToken).apply();
                }
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

                TokenTask task = new TokenTask(mOAuthEmail, mOAuthScope);
                task.execute();
            } else {
                mOAuthCallback.onOAuthToken(null);
                mOAuthCallback = null;
            }
        } else if (requestCode == REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR) {
            TokenTask task = new TokenTask(mOAuthEmail, mOAuthScope);
            task.execute();
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

    private void doBackup() {
        showProgressBar();
        RESTBackupManager.SaveCallback callback = new RESTBackupManager.SaveCallback() {
            @Override
            public void onSave(boolean success) {
                hideProgressBar();
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

        final RESTBackupManager.ConnectCallback callback = new RESTBackupManager.ConnectCallback() {
            @Override
            public void onConnect(boolean success) {
                if (success) {
                    setBackupEnabled(true);
                    showBackupDialog(R.string.backup_enabled_successfully, true);
                } else {
                    showBackupDialog(R.string.backup_enabling_failed, false);
                }
                doBackup();
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
            listView.sync();
        }

        saveData();
    }

    private void saveData() {
        if (hasBackupEnabled()) {
            RESTBackupManager.SaveCallback callback = new RESTBackupManager.SaveCallback() {
                @Override
                public void onSave(boolean success) {
                    //hideProgressBar();
                }
            };

            //showProgressBar();
            mBackupManager.putBackup(Database.getRoot(this), callback);
        }

        Database.saveAsync(Database.getRoot(this));
        Database.sync();
    }
}
