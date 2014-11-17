package com.mbonnin.treedo;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by martin on 13/11/14.
 */
public class BackupManager implements GoogleApiClient.ConnectionCallbacks {
    public static final int OK = 0;
    public static final int FAILED = 1;
    private static final String FOLDER_TITLE = "Tree-Do";
    private GoogleApiClient mGoogleApiClient;
    private boolean mConnected;
    private boolean mConnecting;
    private ArrayList<ConnectCallback> mPendingConnectCallbacks = new ArrayList<ConnectCallback>();


    public BackupManager(Context context, Handler handler, GoogleApiClient.OnConnectionFailedListener connectionFailedListener) {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(connectionFailedListener)
                .build();
    }

    public void disconnect() {
        mGoogleApiClient.disconnect();
        mConnected = false;
    }

    class GFile {
        InputStream inputStream;
        DriveContents contents;

        public GFile(InputStream inputStream, DriveContents contents) {
            this.inputStream = inputStream;
            this.contents = contents;
        }
    }

    public GFile blockingOpenFile(GDrive gDrive) {
        DriveApi.DriveContentsResult result = Drive.DriveApi.getFile(mGoogleApiClient, gDrive.driveId)
                .open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null)
                .await();
        if (!result.getStatus().isSuccess()) {
            return null;
        }

        DriveContents contents = result.getDriveContents();
        return new GFile(contents.getInputStream(), contents);
    }

    public void blockingCloseFile(GFile file) {
        file.contents.discard(mGoogleApiClient);
    }
    private void saveInFile(final DriveId driveId, final Item item, final SaveCallback callback) {
        final Handler handler = new Handler();

        // Perform I/O off the UI thread.
        new Thread() {
            @Override
            public void run() {
                // write content to DriveContents
                DriveApi.DriveContentsResult driveContentsResult = com.google.android.gms.drive.Drive.DriveApi.getFile(mGoogleApiClient, driveId).open(mGoogleApiClient, DriveFile.MODE_WRITE_ONLY, null).await();

                if (!driveContentsResult.getStatus().isSuccess()) {
                    Utils.log("Cannot open File");
                    return;
                }
                final DriveContents driveContents = driveContentsResult.getDriveContents();

                boolean success = false;
                try {
                    writeItem(driveContents, item);

                    com.google.android.gms.common.api.Status status =
                            driveContents.commit(mGoogleApiClient, null).await();
                    if (status.getStatus().isSuccess()) {
                        success = true;
                    }
                } catch (IOException e) {
                    Utils.log(e.getMessage());
                }

                final boolean s = success;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSave(s);
                    }
                });
            }
        }.start();
    }


    private void writeItem(DriveContents driveContents, Item item) throws IOException {
        OutputStream outputStream = driveContents.getOutputStream();
        // XXX: skip the root item...
        for (Item child:item.children) {
            child.serialize(outputStream);
        }
        outputStream.close();

    }

    private void findOrCreateFile(final DriveId driveId, final String title, final DriveCallback callback) {
        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, title))
                .build();

        final ResultCallback<DriveFolder.DriveFileResult> createResultCallback = new ResultCallback<DriveFolder.DriveFileResult>() {
            @Override
            public void onResult(DriveFolder.DriveFileResult driveFileResult) {
                if (!driveFileResult.getStatus().isSuccess()) {
                    Utils.log("Cannot create new file");
                    callback.onDrive(null);
                    return;
                }

                DriveId fileDriveId = driveFileResult.getDriveFile().getDriveId();
                Utils.log("File created: " + fileDriveId.encodeToString());
                callback.onDrive(new GDrive(title, fileDriveId));
            }
        };


        final ResultCallback<DriveApi.DriveContentsResult> contentResultCallback = new ResultCallback<DriveApi.DriveContentsResult>() {
            @Override
            public void onResult(DriveApi.DriveContentsResult driveContentsResult) {
                if (!driveContentsResult.getStatus().isSuccess()) {
                    Utils.log("Cannot create new driveContents");
                    callback.onDrive(null);
                    return;
                }

                final DriveContents driveContents = driveContentsResult.getDriveContents();

                // Perform I/O off the UI thread.
                new Thread() {
                    @Override
                    public void run() {
                        // write content to DriveContents
                        try {
                            driveContents.getOutputStream().write("".getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                .setTitle(title)
                                .setMimeType("text/plain")
                                .build();

                        Drive.DriveApi.getFolder(mGoogleApiClient, driveId)
                                .createFile(mGoogleApiClient, changeSet, driveContents)
                                .setResultCallback(createResultCallback);
                    }
                }.start();
            }
        };

        final ResultCallback<DriveApi.MetadataBufferResult> findResultCallback = new ResultCallback<DriveApi.MetadataBufferResult>() {
            @Override
            public void onResult(DriveApi.MetadataBufferResult metadataBufferResult) {
                if (!metadataBufferResult.getStatus().isSuccess()) {
                    Utils.log("File does not exist should I create it ?");
                    callback.onDrive(null);
                    return;
                }

                MetadataBuffer buffer = metadataBufferResult.getMetadataBuffer();
                for (Metadata metadata:buffer) {
                    if (metadata.getTitle().equals(title)) {
                        Utils.log("File found: " + metadata.getDriveId().encodeToString());
                        callback.onDrive(new GDrive(metadata.getTitle(), metadata.getDriveId()));
                        buffer.release();
                        return;
                    }
                }

                buffer.release();

                Utils.log("File not found, create it");
                Drive.DriveApi.newDriveContents(mGoogleApiClient)
                        .setResultCallback(contentResultCallback);
            }
        };

        DriveFolder folder = Drive.DriveApi.getFolder(mGoogleApiClient, driveId);
        folder.queryChildren(mGoogleApiClient, query).setResultCallback(findResultCallback);

    }

    void findOrCreateTreedoFolder(final DriveCallback callback) {
        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, FOLDER_TITLE))
                .addFilter(Filters.eq(SearchableField.MIME_TYPE, "application/vnd.google-apps.folder"))
                .build();

        final ResultCallback<DriveFolder.DriveFolderResult> createResultCallback = new ResultCallback<DriveFolder.DriveFolderResult>() {
            @Override
            public void onResult(DriveFolder.DriveFolderResult driveFolderResult) {
                if (!driveFolderResult.getStatus().isSuccess()) {
                    Utils.log("Cannot create Folder folder");
                    callback.onDrive(null);
                    return;
                }

                Utils.log("Folder created: " + driveFolderResult.getDriveFolder().getDriveId().encodeToString());

                DriveId driveId = driveFolderResult.getDriveFolder().getDriveId();

                callback.onDrive(new GDrive(FOLDER_TITLE, driveId));

            }
        };

        final ResultCallback<DriveApi.MetadataBufferResult> searchResultCallback = new ResultCallback<DriveApi.MetadataBufferResult>() {
            @Override
            public void onResult(DriveApi.MetadataBufferResult metadataBufferResult) {
                if (!metadataBufferResult.getStatus().isSuccess()) {
                    Utils.log("Cannot search folder");
                    callback.onDrive(null);
                    return;
                }

                MetadataBuffer buffer = metadataBufferResult.getMetadataBuffer();
                for (Metadata metadata:buffer) {
                    if (!metadata.isTrashed()) {
                        Utils.log("Folder found: " + metadata.getDriveId().encodeToString());
                        String title = metadata.getTitle();
                        String mimeType = metadata.getMimeType();
                        boolean isTrashed = metadata.isTrashed();
                        boolean isFolder = metadata.isFolder();

                        callback.onDrive(new GDrive(metadata.getTitle(), metadata.getDriveId()));
                        buffer.release();
                        return;
                    }
                }

                Utils.log("Folder not found, create it");

                buffer.release();

                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                        .setTitle(FOLDER_TITLE).build();
                Drive.DriveApi.getRootFolder(mGoogleApiClient).createFolder(
                        mGoogleApiClient, changeSet).setResultCallback(createResultCallback);

            }
        };

        Drive.DriveApi.getRootFolder(mGoogleApiClient).queryChildren(mGoogleApiClient, query).setResultCallback(searchResultCallback);
    }

    public class GDrive {
        String title;
        DriveId driveId;

        GDrive(String title, DriveId driveId) {
            this.title = title;
            this.driveId = driveId;
        }
    }

    public interface SaveCallback {
        void onSave(boolean success);
    }

    public interface ConnectCallback {
        void onConnected(boolean success);
    }

    public interface DrivesCallback {
        void onDrives(ArrayList<GDrive> gDriveList);
    }

    public interface DriveCallback {
        void onDrive(GDrive gDrive);
    }

    public void connect(ConnectCallback callback) {
        if (mConnected) {
            callback.onConnected(true);
            return;
        } else {
            mPendingConnectCallbacks.add(callback);
            if (mConnecting == false) {
                mConnecting = true;
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        mConnected = true;
        mConnecting = false;
        for (ConnectCallback callback: mPendingConnectCallbacks) {
            callback.onConnected(true);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    public void retry() {
        mGoogleApiClient.connect();
    }

    public void connectAborted() {
        mConnected = false;
        mConnecting = false;
        for (ConnectCallback callback: mPendingConnectCallbacks) {
            callback.onConnected(false);
        }
    }

    public void save(Item item, final SaveCallback callback) {
        final Item clone = item.deepCopy(Integer.MAX_VALUE);

        if (mConnected == false) {
            return;
        }

        Calendar rightNow = Calendar.getInstance();
        final String title = rightNow.get(Calendar.YEAR) + "_W" + rightNow.get(Calendar.WEEK_OF_YEAR) + ".txt";

        final DriveCallback fileCallback = new DriveCallback() {
            @Override
            public void onDrive(GDrive drive) {
                saveInFile(drive.driveId, clone, callback);
            }
        };

        final DriveCallback folderCallback = new DriveCallback() {
            @Override
            public void onDrive(GDrive drive) {
                findOrCreateFile(drive.driveId, title, fileCallback);
            }
        };

        findOrCreateTreedoFolder(folderCallback);
    }

    void getBackups(final DrivesCallback callback) {

        final ResultCallback<DriveApi.MetadataBufferResult> listChildrenResultCallback = new ResultCallback<DriveApi.MetadataBufferResult>() {
            @Override
            public void onResult(DriveApi.MetadataBufferResult metadataBufferResult) {
                if (!metadataBufferResult.getStatus().isSuccess()) {
                    Utils.log("Cannot list children");
                    callback.onDrives(null);
                    return;
                }

                final ArrayList<GDrive> list = new ArrayList<GDrive>();
                MetadataBuffer buffer = metadataBufferResult.getMetadataBuffer();
                for (Metadata metadata:buffer) {
                    if (!metadata.isTrashed() && !metadata.isFolder()) {
                        list.add(new GDrive(metadata.getTitle(), metadata.getDriveId()));
                    }
                }
                buffer.release();

                callback.onDrives(list);
            }
        };

        final DriveCallback treedoCallback = new DriveCallback() {
            @Override
            public void onDrive(GDrive drive) {
                Drive.DriveApi.getFolder(mGoogleApiClient, drive.driveId).listChildren(mGoogleApiClient).setResultCallback(listChildrenResultCallback);

            }
        };

        final ConnectCallback connectCallback = new ConnectCallback() {
            @Override
            public void onConnected(boolean success) {
                if (!success) {
                    callback.onDrives(null);
                    return;
                }

                findOrCreateTreedoFolder(treedoCallback);
            }
        };

        connect(connectCallback);

    }
}
