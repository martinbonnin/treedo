package com.mbonnin.treedo;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.support.design.widget.Snackbar;

import com.example.android.trivialdrivesample.util.IabHelper;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.RuntimeExecutionException;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.gson.Gson;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Stack;
import java.util.UUID;

import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.util.async.Async;


@EActivity
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final String PREFERENCE_ACCOUNT_NAME = "account_name";
    private static final String PREFERENCE_UUID = "uuid";
    public static final String PREFERENCE_FILE_ID = "file_id";

    private static final int ACTIVITY_RESULT_ACCOUNT_CHOSEN = 1;
    private static final int ACTIVITY_RESULT_GOOGLE_PLAY_SERVICES = 2;
    private static final int ACTIVITY_RESULT_OAUTH_GRANTED = 3;
    private static final int ACTIVITY_RESULT_BACKUP_SELECTED = 4;

    private static final int PERMISSION_RESULT_GET_ACCOUNTS = 1;

    private static MainActivity sActivity;

    private AnimatedFrameLayout<MainView> mAnimatedFrameLayout;

    private static final String[] SCOPES = {DriveScopes.DRIVE_FILE, DriveScopes.DRIVE};

    /**
     * it is tempting to remove this stack and recreate the view each time but if we do this, we lose the scroll offset
     */
    private Stack<MainView> mStack = new Stack();

    private boolean mIsDebuggable;

    @Bean
    DB mDb;
    private IabHelper mHelper;
    private GoogleAccountCredential mCredential;
    private Drive mService;
    private String mFileName;
    private String mFileId;
    private Subscription mSubscription;
    Gson gson;

    private Observer<? super Boolean> mSubscriber = new Observer<Boolean>() {
        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            mSubscription = null;
            handleDriveException(e);
            e.printStackTrace();
        }

        @Override
        public void onNext(Boolean aBoolean) {
            mSubscription = null;
            Log.d(TAG, "backup success");
        }
    };

    private void handleDriveException(Throwable e) {
        if (e instanceof RuntimeException) {
            if (e.getCause() instanceof UserRecoverableAuthIOException) {
                startActivityForResult(((UserRecoverableAuthIOException) e.getCause()).getIntent(),
                        ACTIVITY_RESULT_OAUTH_GRANTED);
            } else if (e.getCause() instanceof GooglePlayServicesAvailabilityIOException) {
                snackBar(getString(R.string.need_google_play_services));
            }
        }
    }

    private void setView(MainView view, int animate) {
        long start = System.currentTimeMillis();

        mDb.save();
        backup();

        mAnimatedFrameLayout.setView(view, animate);

        closeSoftInput();

        Utils.log("setView took " + (System.currentTimeMillis() - start) + " ms");
    }

    public void onBackPressed() {
        popMainView();
    }

    private void popMainView() {
        if (mStack.size() <= 1) {
            moveTaskToBack(true);
            return;
        }
        mStack.pop();

        mStack.peek().refreshTitle();
        setView(mStack.peek(), AnimatedFrameLayout.ANIMATE_EXIT);
    }

    private void closeSoftInput() {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mAnimatedFrameLayout.getWindowToken(), 0);
    }

    public void openSoftInput(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, 0);
    }

    public String getPreference(String key, String defaultValue) {
        return getPreferences(MODE_PRIVATE).getString(key, defaultValue);
    }

    private void putPreference(String key, String value) {
        getPreferences(MODE_PRIVATE).edit().putString(key, value).apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gson = new Gson();

        sActivity = this;
        mIsDebuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;

        Utils.init(getApplicationContext(), mIsDebuggable);

        mAnimatedFrameLayout = new AnimatedFrameLayout<>(this);

        setContentView(mAnimatedFrameLayout);

        String uuid = getPreference(PREFERENCE_UUID, null);
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
            putPreference(PREFERENCE_UUID, uuid);
        }
        mFileName = "TreeDo_" + uuid;

        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjOQf9scJlfaEtp79eWVyQdLw6bqFiIXHvAjXvY+YdKpzvQFwSC0N72KgbAX9u6K5B2btAEziO3PPouubgN3426Ay00DWdjNUiGrpBhQ1Wzq4/+ZkwhPM3LKp6aH3y8B6oalnY9BeKWIBvzkPOxzuqoqghm2WSO/Al3K7bpcGQZkcHQO8g66e0PmTYOR3v5z7hIk16fqD77Raac8DhfY+0903e4ePsjok09OjJOJ7v3vgjCYpwnwn/apcCysEhSZk9fRw05R5du0VDWLFEHWYyCeTnV5c5Zx5JgWddbLnsb3Mnwlki14bpdUzYV2WgxcUvySJIFtNMF9m8t5nCzdoOwIDAQAB";

        mHelper = new IabHelper(this, base64EncodedPublicKey);
        mHelper.startSetup(result -> {
            if (!result.isSuccess()) {
                // Oh no, there was a problem.
                Log.d(TAG, "Problem setting up In-app Billing: " + result);
            } else {
                Log.d(TAG, "Hooray, IAB is fully set up ");
            }
        });

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        String accountName = getPreference(PREFERENCE_ACCOUNT_NAME, null);
        if (accountName != null) {
            createService(accountName);
        }
        mFileId = getPreference(PREFERENCE_FILE_ID, null);

        pushNode(mDb.getRoot(), AnimatedFrameLayout.ANIMATE_NONE);
    }

    private void setRootItem() {
        setView(null, AnimatedFrameLayout.ANIMATE_NONE);
    }

    private void flushDatabase() {
        setRootItem();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHelper != null) {
            try {
                mHelper.dispose();
            } catch (IabHelper.IabAsyncInProgressException e) {
                e.printStackTrace();
            }
        }
        mHelper = null;
        sActivity = null;
    }

    public void showAboutDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.about_dialog, null);

        TextView textView = ((TextView) view.findViewById(R.id.about));
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

    public static void pushNodeG(Node node) {
        if (sActivity != null) {
            sActivity.pushNode(node, AnimatedFrameLayout.ANIMATE_ENTER);
        }
    }

    public void pushNode(Node node, int animate) {
        MainView view = MainView_.build(this);
        view.setNode(this, node);
        mStack.push(view);

        setView(view, animate);
    }

    public void snackBar(String string) {
        Snackbar.make(mAnimatedFrameLayout, string, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case ACTIVITY_RESULT_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    snackBar(getString(R.string.need_google_play_services));
                } else {
                    enableBackup();
                }
                break;
            case ACTIVITY_RESULT_ACCOUNT_CHOSEN:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        putPreference(PREFERENCE_ACCOUNT_NAME, accountName);

                        createService(accountName);
                        enableBackup();
                    }
                }
                break;
            case ACTIVITY_RESULT_OAUTH_GRANTED:
                if (resultCode == RESULT_OK) {
                    enableBackup();
                }
                break;
            case ACTIVITY_RESULT_BACKUP_SELECTED:
                openBackup(data.getData());
                break;

        }
    }

    public String readFullyAsString(InputStream inputStream, String encoding)
            throws IOException {
        return readFully(inputStream).toString(encoding);
    }

    public byte[] readFullyAsBytes(InputStream inputStream)
            throws IOException {
        return readFully(inputStream).toByteArray();
    }

    private ByteArrayOutputStream readFully(InputStream inputStream)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos;
    }

    private DB.Data openLegacyBackup(Uri uri) {
        InputStream inputStream = null;
        try {
            inputStream = getContentResolver().openInputStream(uri);
            Item item = Item.deserialize(inputStream);
            item.findOrCreateTrash(this);

            Node root = toNode(item);
            Node trash = null;
            Iterator<Node> iterator = root.childList.iterator();
            while (iterator.hasNext()) {
                Node child = iterator.next();
                if (child.trash) {
                    trash = child;
                    iterator.remove();
                    break;
                }
            }
            if (trash != null) {
                DB.Data data = new DB.Data();
                data.root = root;
                data.trash = trash;
                return data;
            }
        } catch (Exception e) {
            e.printStackTrace();
            snackBar(getString(R.string.cannot_open_backup));
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    static private Node toNode(Item item) {
        Node node = new Node();
        node.folder = item.isAFolder;
        node.checked = item.checked;
        node.text = item.text;
        node.trash = item.isTrash;

        for (Item childItem: item.children) {
            node.childList.add(toNode(childItem));
        }
        return node;
    }

    private DB.Data openJsonBackup(Uri uri) {
        InputStream inputStream = null;
        String s;
        try {
            inputStream = getContentResolver().openInputStream(uri);
            s = readFullyAsString(inputStream, "UTF-8");
            DB.Data data = gson.fromJson(s, DB.Data.class);
            if (data != null && data.root != null && data.trash != null) {
                return data;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void openBackup(Uri uri) {
        DB.Data data = openJsonBackup(uri);
        if (data == null) {
            data = openLegacyBackup(uri);
        }

        if (data == null || data.root == null || data.trash == null) {
            snackBar(getString(R.string.cannot_open_backup));
            return;
        }

        DB.Data finalData = data;
        Dialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.backup_found))
                .setMessage(R.string.do_you_want_to_replace)
                .setPositiveButton(getString(R.string.ok), (dialog1, which) -> {
                    mDb.setData(finalData);
                    mStack.clear();
                    pushNode(mDb.getRoot(), AnimatedFrameLayout.ANIMATE_NONE);
                }).setNegativeButton(getString(R.string.cancel), (dialog1, which) -> {
                    dialog1.dismiss();
                })
                .create();

        dialog.show();
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

    private void chooseAccount() {
        String accountName = getPreference(PREFERENCE_ACCOUNT_NAME, null);
        if (accountName != null) {
            createService(accountName);
            enableBackup();
        } else {
            // Start a dialog from which the user can choose an account
            startActivityForResult(
                    mCredential.newChooseAccountIntent(),
                    ACTIVITY_RESULT_ACCOUNT_CHOSEN);
        }
    }

    private void createService(String accountName) {
        mCredential.setSelectedAccountName(accountName);
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.drive.Drive.Builder(
                transport, jsonFactory, mCredential)
                .setApplicationName(getString(R.string.app_name))
                .build();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_RESULT_GET_ACCOUNTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableBackup();
            } else {
                snackBar(getString(R.string.permission_needed));
            }
        }
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                ACTIVITY_RESULT_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    Observer<? super String> mEnableBackupSubscriber = new Observer<String>() {


        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            handleDriveException(e);
            e.printStackTrace();
        }

        @Override
        public void onNext(String s) {
            putPreference(PREFERENCE_FILE_ID, s);
            mFileId = s;
            snackBar(getString(R.string.backup_enabled));
        }
    };

    public void enableBackup() {
        if (!isConnected()) {
            snackBar(getString(R.string.connection_needed));
        } else if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (Build.VERSION.SDK_INT >= 23 && !(checkCallingOrSelfPermission(Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{Manifest.permission.GET_ACCOUNTS}, PERMISSION_RESULT_GET_ACCOUNTS);
        } else if (mService == null) {
            chooseAccount();
        } else if (mFileId == null) {
            Async.start(() -> {
                try {
                    File file = new File();
                    file.setName(mFileName);
                    file.setMimeType("application/json");
                    File result = mService.files().create(file).setFields("id").execute();
                    return result.getId();
                } catch (IOException e) {
                    throw new RuntimeExecutionException(e);
                }
            }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(mEnableBackupSubscriber);
        }
    }

    public void importBackup() {
        Intent mediaIntent = new Intent(Intent.ACTION_GET_CONTENT);
        mediaIntent.addCategory(Intent.CATEGORY_OPENABLE);
        mediaIntent.setType("*/*");
        startActivityForResult(mediaIntent, ACTIVITY_RESULT_BACKUP_SELECTED);
    }

    static class FileContent extends AbstractInputStreamContent {
        String mJson;

        public FileContent(String type, String json) {
            super(type);
            mJson = json;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(mJson.getBytes());
        }

        @Override
        public long getLength() throws IOException {
            return mJson.getBytes().length;
        }

        @Override
        public boolean retrySupported() {
            return false;
        }
    }

    protected void onStop() {
        super.onStop();
        mDb.forceSave();

        backup();
    }

    private void backup() {
        if (mFileId != null) {
            File file = new File();
            String fileId = mFileId;
            FileContent content = new FileContent("application/json", gson.toJson(mDb.getData()));

            if (mSubscription == null) {
                mSubscription = Async.start(() -> {
                    try {
                        mService.files().update(fileId, file, content).execute();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return true;
                }).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(mSubscriber);
            }
        }
    }

    public void disableBackup() {
        putPreference(PREFERENCE_FILE_ID, null);
        mFileId = null;
    }
}
