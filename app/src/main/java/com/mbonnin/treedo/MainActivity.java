package com.mbonnin.treedo;

import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;

import com.google.android.gms.common.GooglePlayServicesUtil;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;

import java.io.IOException;


@EActivity
public class MainActivity extends AppCompatActivity implements BackupManager.OAuthManager {

    private static final String PREFERENCE_OAUTH_TOKEN = "oauth_token";
    private static final String PREFERENCE_OAUTH_EMAIL = "email";

    public static final int REQUEST_CODE_CHOOSE_ACCOUNT = 1;
    private static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 2;
    private static MainActivity sActivity;

    private ViewStack<NodeRecyclerView> mViewStack;
    private Toolbar mToolbar;
    private BackupManager mBackupManager;
    private LinearLayout mTopLayout;

    BackupManager.OAuthTokenCallback mOAuthCallback;
    private String mOAuthScope;
    private String mOAuthEmail;
    private String mOAuthToken;

    private boolean mIsDebuggable;
    private long mLastSaveTime;
    private Drawable createFolderDrawable;

    @Bean
    DB mDB;

    private View.OnClickListener mNavigationClickListener = v -> onBackPressed();
    private MenuItem.OnMenuItemClickListener mOnAboutClickListener = item -> {
        showAboutDialog();
        return true;
    };
    private MenuItem.OnMenuItemClickListener mOnNewFolderClickListener = item -> {
        View view = LayoutInflater.from(this).inflate(R.layout.create_folder_dialog, null);
        EditText editText = (EditText)view.findViewById(R.id.editText);

        Dialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.new_folder))
                .setView(view)
                .setPositiveButton(getString(R.string.ok), (dialog1, which) -> {
                    mViewStack.peek().createFolder(editText.getText().toString());
                    dialog1.dismiss();
                    openSoftInput(editText);
                }).setNegativeButton(getString(R.string.cancel), (dialog1, which) -> {
                    dialog1.dismiss();
                })
                .create();

        dialog.show();

        return true;
    };

    private void updateToolbar() {
        if (mViewStack.size() > 1) {
            Drawable d = getResources().getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            mToolbar.setNavigationIcon(d);
            mToolbar.setNavigationOnClickListener(mNavigationClickListener);
            mToolbar.setTitle(mViewStack.peek().getTitle());
        } else {
            mToolbar.setTitle(R.string.app_name);
            mToolbar.setNavigationIcon(null);
            mToolbar.setNavigationOnClickListener(null);
        }

        mToolbar.setTitleTextColor(Color.WHITE);
        Menu menu = mToolbar.getMenu();
        int order = 0;

        menu.clear();
        MenuItem menuItem;

        menuItem = menu.add(Menu.NONE, Menu.NONE, order++, getString(R.string.new_folder));
        menuItem.setIcon(createFolderDrawable);
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuItem.setOnMenuItemClickListener(mOnNewFolderClickListener);

        menuItem = menu.add(Menu.NONE, Menu.NONE, order++, getString(R.string.action_about));
        menuItem.setOnMenuItemClickListener(mOnAboutClickListener);
    }

    private void pushView(Node node) {
        long start = System.currentTimeMillis();

        mDB.save();

        NodeRecyclerView listView = new NodeRecyclerView(this, node);
        listView.setHasFixedSize(true);

        mViewStack.pushView(listView);

        closeSoftInput();

        updateToolbar();
        Utils.log("pushListView2() took " + (System.currentTimeMillis() - start) + " ms");

        saveData(false);
    }

    public void onBackPressed() {
        popListView();
    }

    private void popListView() {
        if (mViewStack.size() == 1) {
            finish();
            return;
        }

        mViewStack.popView();

        closeSoftInput();

        updateToolbar();

        saveData(false);
    }

    private void closeSoftInput() {
        InputMethodManager imm = (InputMethodManager)getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mViewStack.peek().getWindowToken(), 0);
    }

    private void openSoftInput(View view) {
        InputMethodManager imm = (InputMethodManager)getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, 0);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sActivity = this;
        mIsDebuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;

        Utils.init(getApplicationContext(), mIsDebuggable);

        createFolderDrawable = getResources().getDrawable(R.drawable.ic_create_new_folder_black_24dp);
        DrawableCompat.setTint(createFolderDrawable, Color.WHITE);

        mOAuthToken = getPreferences(MODE_PRIVATE).getString(PREFERENCE_OAUTH_TOKEN, "");
        mOAuthEmail = getPreferences(MODE_PRIVATE).getString(PREFERENCE_OAUTH_EMAIL, "");
        mBackupManager = new BackupManager(this, this, mOAuthToken);

        mTopLayout = new LinearLayout(this);
        mTopLayout.setOrientation(LinearLayout.VERTICAL);

        mToolbar = new Toolbar(new ContextThemeWrapper(this, R.style.MyToolbar));
        mToolbar.setBackgroundColor(getResources().getColor(R.color.muted_500));
        mTopLayout.addView(mToolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mViewStack = new ViewStack<>(this);
        mTopLayout.addView(mViewStack, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mTopLayout.setFitsSystemWindows(true);
        setContentView(mTopLayout);

        pushView(null);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(mTopLayout);
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

    private void setRootItem() {
        mViewStack.clear();
        pushView(null);
    }

    private void flushDatabase() {
        setRootItem();
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
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (!isConnected()) {
            showBackupDialog(R.string.connection_needed, false);
            return;
        }

        builder.setTitle(getString(R.string.select_backup));
        builder.setIcon(R.drawable.backup_import);
        builder.setView(getLayoutInflater().inflate(R.layout.progress_bar, null));

        final AlertDialog dialog = builder.show();

        final BackupManager.BackupCallback backupCallback = backup -> {
            if (backup != null) {
                setRootItem();
            } else {
                showBackupDialog(R.string.get_backup_failed, false);
            }
        };

        final AdapterView.OnItemClickListener clickListener = (parent, view, position, id) -> {
            BackupManager.Drive drive = (BackupManager.Drive) parent.getItemAtPosition(position);

            mBackupManager.getBackup(drive, backupCallback);

            dialog.dismiss();
        };

        BackupManager.DrivesCallback callback = drives -> {
            if (drives != null) {
                Context context = MainActivity.this;
                BackupAdapter adapter = new BackupAdapter(context, drives);
                ListView listView = new ListView(context);
                listView.setAdapter(adapter);
                listView.setOnItemClickListener(clickListener);
                builder.setView(listView);
            }
        };

        mBackupManager.getBackupList(callback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sActivity = null;
    }

    private void disableBackup() {
        showBackupDialog(R.string.backup_disabled_successfully, false);
    }

    private void showAboutDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.about_dialog, null);

        TextView textView = ((TextView)view.findViewById(R.id.about));
        textView.setText(Html.fromHtml(getString(R.string.about)));
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        Dialog dialog = new AlertDialog.Builder(this)
                .setIcon(getResources().getDrawable(R.drawable.treedo_blue))
                .setTitle(getString(R.string.app_name))
                .setView(view).setPositiveButton(getString(R.string.ok), (dialog1, which) -> {
                    dialog1.dismiss();
                })
                .create();

        dialog.show();
    }

    private void showBackupDialog(int message_id, boolean success) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

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

    public static void pushNode(Node node) {
        if (sActivity != null) {
            sActivity.pushView(node);
        }
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
                    // Show a dialog_header created by Google Play services that allows
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


        final BackupManager.ConnectCallback callback = new BackupManager.ConnectCallback() {
            @Override
            public void onConnect(boolean success) {
                if (success) {
                    showBackupDialog(R.string.backup_enabled_successfully, true);
                    putBackup();
                } else {
                    showBackupDialog(R.string.backup_enabling_failed, false);
                }
            }
        };

        mBackupManager.connect(callback);
    }

    protected void onStop() {
        super.onStop();
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        saveData(true);
    }

    private void saveData(boolean force) {
        if (!force && System.currentTimeMillis() - mLastSaveTime < 5000) {
            return;
        }

        mLastSaveTime = System.currentTimeMillis();
        Database.saveAsync(Database.getRoot(this));
        Database.sync();
    }
}
