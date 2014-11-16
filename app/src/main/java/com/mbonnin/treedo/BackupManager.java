package com.mbonnin.treedo;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Calendar;

/**
 * Created by martin on 13/11/14.
 */
public class BackupManager implements GoogleApiClient.ConnectionCallbacks {
    public static final int OK = 0;
    public static final int FAILED = 1;
    private static final String FOLDER_TITLE = "Tree-Do";
    private GoogleApiClient mGoogleApiClient;
    private Handler mHandler;
    Listener mListener;
    private boolean mConnected;

    @Override
    public void onConnected(Bundle bundle) {
        mConnected = true;

        mListener.onConnected();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    interface Listener {
        void onConnected();
        void onSaveDone(int ret);
    }

    public BackupManager(Context context, Handler handler, GoogleApiClient.OnConnectionFailedListener connectionFailedListener) {
        mHandler = handler;

        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(connectionFailedListener)
                .build();
    }


    public void connect() {
        mGoogleApiClient.connect();
    }

    private void postSaveResult(final int ret) {
        mHandler.post( new Runnable() {
            @Override
            public void run() {
                mListener.onSaveDone(ret);
            }
        });

    }

    public void disconnect() {
        mGoogleApiClient.disconnect();
        mConnected = false;
    }

    class DriveFileResultCallback implements ResultCallback<DriveFolder.DriveFileResult> {

        @Override
        public void onResult(DriveFolder.DriveFileResult driveFileResult) {
            if (!driveFileResult.getStatus().isSuccess()) {
                Utils.log("Error while trying to create the file");
                postSaveResult(FAILED);
                return;
            } else {
                postSaveResult(OK);
            }
        }
    }

    public void save(Item item) {
        final Item clone = item.deepCopy(Integer.MAX_VALUE);

        if (mConnected == false) {
            return;
        }

        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, FOLDER_TITLE))
                .addFilter(Filters.eq(SearchableField.MIME_TYPE, "application/vnd.google-apps.folder"))
                .build();

        Calendar rightNow = Calendar.getInstance();
        String title = rightNow.get(Calendar.YEAR) + "_W" + rightNow.get(Calendar.WEEK_OF_YEAR) + ".txt";

        Drive.DriveApi.getRootFolder(mGoogleApiClient).queryChildren(mGoogleApiClient, query).setResultCallback(new FolderSearchResultCallback(title, item));
    }

    private class FileSearchResultCallback implements ResultCallback<DriveApi.MetadataBufferResult> {
        String mTitle;
        Item mItem;
        DriveId mFolderId;

        public FileSearchResultCallback(String title, Item item, DriveId driveId) {
            mTitle = title;
            mItem = item;
            mFolderId = driveId;
        }

        @Override
        public void onResult(DriveApi.MetadataBufferResult metadataBufferResult) {
            if (!metadataBufferResult.getStatus().isSuccess()) {
                Utils.log("File does not exist should I create it ?");
                return;
            }

            for (Metadata metadata:metadataBufferResult.getMetadataBuffer()) {
                if (metadata.getTitle().equals(mTitle)) {
                    Utils.log("File found: " + metadata.getDriveId().encodeToString());
                    saveInFile(metadata.getDriveId(), mItem);
                    metadataBufferResult.getMetadataBuffer().release();
                    return;
                }
            }

            metadataBufferResult.getMetadataBuffer().release();

            Utils.log("File not found, create it");
            Drive.DriveApi.newDriveContents(mGoogleApiClient)
                    .setResultCallback(new FileContentsResultCallback(mTitle, mItem, mFolderId));
        }
    }

    private void saveInFile(final DriveId driveId, final Item item) {
        // Perform I/O off the UI thread.
        new Thread() {
            @Override
            public void run() {
                // write content to DriveContents
                DriveApi.DriveContentsResult driveContentsResult = Drive.DriveApi.getFile(mGoogleApiClient, driveId).open(mGoogleApiClient, DriveFile.MODE_WRITE_ONLY, null).await();

                if (!driveContentsResult.getStatus().isSuccess()) {
                    Utils.log("Cannot open File");
                    return;
                }
                final DriveContents driveContents = driveContentsResult.getDriveContents();

                try {
                    writeItem(driveContents, item);

                    com.google.android.gms.common.api.Status status =
                            driveContents.commit(mGoogleApiClient, null).await();
                    if (status.getStatus().isSuccess()) {
                        postSaveResult(OK);
                    } else {
                        postSaveResult(FAILED);
                    }

                } catch (IOException e) {
                    Utils.log(e.getMessage());
                    postSaveResult(FAILED);
                }
            }
        }.start();
    }

    private class FileContentsResultCallback implements ResultCallback<DriveApi.DriveContentsResult> {
        String mTitle;
        Item mItem;
        DriveId mFolderId;

        public FileContentsResultCallback(String title, Item item, DriveId folderId) {
            mTitle = title;
            mItem = item;
            mFolderId = folderId;
        }

        @Override
        public void onResult(DriveApi.DriveContentsResult driveContentsResult) {
            if (!driveContentsResult.getStatus().isSuccess()) {
                Utils.log("Cannot create new driveContents");
                return;
            }

            final DriveContents driveContents = driveContentsResult.getDriveContents();

            // Perform I/O off the UI thread.
            new Thread() {
                @Override
                public void run() {
                    // write content to DriveContents
                    try {
                        writeItem(driveContents, mItem);
                    } catch (IOException e) {
                        e.printStackTrace();
                        postSaveResult(FAILED);
                    }

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle(mTitle)
                            .setMimeType("text/plain")
                            .build();

                    Drive.DriveApi.getFolder(mGoogleApiClient, mFolderId)
                            .createFile(mGoogleApiClient, changeSet, driveContents)
                            .setResultCallback(new FileCreateResultCallback(mTitle, mItem));
                }
            }.start();
        }
    }

    private void writeItem(DriveContents driveContents, Item item) throws IOException {
        OutputStream outputStream = driveContents.getOutputStream();
        // XXX: skip the root item...
        for (Item child:item.children) {
            child.serialize(outputStream);
        }
        outputStream.close();

    }

    private class FileCreateResultCallback implements ResultCallback<DriveFolder.DriveFileResult> {
        Item mItem;
        public FileCreateResultCallback(String title, Item item) {
            mItem = item;
        }

        @Override
        public void onResult(DriveFolder.DriveFileResult driveFileResult) {
            if (!driveFileResult.getStatus().isSuccess()) {
                Utils.log("Cannot create new file");
                return;
            }

            Utils.log("File created: " + driveFileResult.getDriveFile().getDriveId().encodeToString());

            postSaveResult(OK);
        }
    }

    private class FolderSearchResultCallback implements ResultCallback<DriveApi.MetadataBufferResult> {
        String mTitle;
        Item mItem;
        public FolderSearchResultCallback(String title, Item item) {
            mTitle = title;
            mItem = item;
        }

        @Override
        public void onResult(DriveApi.MetadataBufferResult metadataBufferResult) {
            if (!metadataBufferResult.getStatus().isSuccess()) {
                Utils.log("Cannot search folder");
                return;
            }

            for (Metadata metadata:metadataBufferResult.getMetadataBuffer()) {
                Utils.log("Folder found: " + metadata.getDriveId().encodeToString());
                String title = metadata.getTitle();
                String mimeType = metadata.getMimeType();
                boolean isTrashed = metadata.isTrashed();
                boolean isFolder = metadata.isFolder();

                searchInFolder(metadata.getDriveId(), mTitle, mItem);
                metadataBufferResult.getMetadataBuffer().release();
                return;
            }

            Utils.log("Folder not found, create it");

            metadataBufferResult.getMetadataBuffer().release();

            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle(FOLDER_TITLE).build();
            Drive.DriveApi.getRootFolder(mGoogleApiClient).createFolder(
                    mGoogleApiClient, changeSet).setResultCallback(new FolderCreateResultCallback(mTitle, mItem));
        }
    }

    private void searchInFolder(DriveId driveId, String title, Item item) {
        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, title))
                .build();

        DriveFolder folder = Drive.DriveApi.getFolder(mGoogleApiClient, driveId);
        folder.queryChildren(mGoogleApiClient, query).setResultCallback(new FileSearchResultCallback(title, item, driveId));

    }

    private class FolderCreateResultCallback implements ResultCallback<DriveFolder.DriveFolderResult> {
        String mTitle;
        Item mItem;

        public FolderCreateResultCallback(String title, Item item) {
            mTitle = title;
            mItem = item;
        }

        @Override
        public void onResult(DriveFolder.DriveFolderResult driveFolderResult) {
            if (!driveFolderResult.getStatus().isSuccess()) {
                Utils.log("Cannot create Folder folder");
                return;
            }

            Utils.log("Folder created: " + driveFolderResult.getDriveFolder().getDriveId().encodeToString());

            Query query = new Query.Builder()
                    .addFilter(Filters.eq(SearchableField.TITLE, mTitle))
                    .build();

            DriveId driveId = driveFolderResult.getDriveFolder().getDriveId();
            DriveFolder folder = Drive.DriveApi.getFolder(mGoogleApiClient, driveId);
            folder.queryChildren(mGoogleApiClient, query).setResultCallback(new FileSearchResultCallback(mTitle, mItem, driveId));

        }
    }
}
