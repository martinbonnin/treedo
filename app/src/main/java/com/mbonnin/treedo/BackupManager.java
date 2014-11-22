package com.mbonnin.treedo;

import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Same as the standard BackupManager except it uses the raw API to have full scope
 */
public class BackupManager {
    private final RequestQueue mRequestQueue;
    String mToken;
    OAuthManager mOauthManager;
    private static String SCOPE = "oauth2:https://www.googleapis.com/auth/drive";
    private static String ENDPOINT_ABOUT = "https://www.googleapis.com/drive/v2/about";
    private static String ENDPOINT_DELETE = "https://www.googleapis.com/drive/v2/files/";
    private static String ENDPOINT_FILES = "https://www.googleapis.com/drive/v2/files";
    private static String TREEDO_FOLDER_NAME = "Tree-Do";
    private static final String ID_ROOT = "root";
    private static final String MIMETYPE_FOLDER = "application/vnd.google-apps.folder";
    private static final String MIMETYPE_TEXT = "text/plain";
    private static final String CHARSET = "utf-8";
    private static final String CONTENT_TYPE_JSON = String.format("application/json; charset=%s", CHARSET);

    int mAuthorizationErrors;
    Map<String, String> mHeaders = new HashMap<String, String>();


    public class Drive {
        public String downloadUrl;
        public String title;
        public String id;

        Drive(String title, String id) {
            this.title = title;
            this.id = id;
        }

        Drive(JSONObject object) throws JSONException {
            id = object.getString("id");
            title = object.getString("title");
            if (object.has("downloadUrl")) {
                downloadUrl = object.getString("downloadUrl");
            }
        }

    }

    public interface SaveCallback {
        void onSave(boolean success);
    }

    public interface DrivesCallback {
        void onDrives(ArrayList<Drive> drives);
    }

    public interface DriveCallback {
        void onDrive(Drive drive);
    }

    public interface UploadCallback {
        void onUpload(boolean success);
    }

    public interface ConnectCallback {
        void onConnect(boolean success);
    }

    interface OAuthTokenCallback {
        void onOAuthToken(String token);
    }

    public interface OAuthManager {
        void getNewOAuthToken(OAuthTokenCallback callback, String scope);
    }

    private interface ApiRequestCallback {
        public void onRequestDone(JSONObject object);
    }

    private interface ApiTextRequestCallback {
        public void onRequestDone(String text);
    }

    public BackupManager(Context context, OAuthManager OAuthManger, String lastOAuthToken) {
        setToken(lastOAuthToken);
        mOauthManager = OAuthManger;
        mRequestQueue = Volley.newRequestQueue(context);
    }

    public void connect(final ConnectCallback callback) {

        ApiRequestCallback apiCallback = new ApiRequestCallback() {

            @Override
            public void onRequestDone(JSONObject object) {
                if (object != null) {
                    callback.onConnect(true);
                } else {
                    callback.onConnect(false);
                }
            }
        };

        sendJsonApiRequest(Request.Method.GET, ENDPOINT_ABOUT, null, apiCallback);
    }

    class ApiRequest extends Request<byte[]> {
        private final Response.Listener<byte[]> mListener;
        private byte[] mBody;
        private String mBodyContentType;

        public ApiRequest(int method, String url, byte[] body, String bodyContentType, Response.Listener<byte[]> listener, Response.ErrorListener errorListener) {
            super(method, url, errorListener);
            mListener = listener;
            mBody = body;
            mBodyContentType = bodyContentType;
        }

        public Map<String, String> getHeaders() throws AuthFailureError {
            return mHeaders;
        }

        @Override
        protected Response<byte[]> parseNetworkResponse(NetworkResponse response) {
            return Response.success(response.data, HttpHeaderParser.parseCacheHeaders(response));
        }

        @Override
        protected void deliverResponse(byte[] response) {
            mListener.onResponse(response);
        }

        @Override
        public String getBodyContentType() {
            return mBodyContentType;
        }

        @Override
        public byte[] getBody() {
            return mBody;
        }

    }

    private void sendJsonApiRequest(int method, final String url, final JSONObject postData, final ApiRequestCallback callback) {
        sendApiRequest(method, url, postData != null ? postData.toString().getBytes():null, CONTENT_TYPE_JSON, callback, null);
    }

    private void sendApiRequest(final int method, final String url, final byte[] body, final String bodyContentType, final ApiRequestCallback callback, final ApiTextRequestCallback textCallback) {
        final OAuthTokenCallback tokenCallback = new OAuthTokenCallback() {
            @Override
            public void onOAuthToken(String token) {
                if (token != null && mAuthorizationErrors < 2) {
                    setToken(token);
                    sendApiRequest(method, url, body, bodyContentType, callback, textCallback);
                } else {
                    callback.onRequestDone(null);
                }
            }
        };

        Response.Listener<byte[]> responseListener = new Response.Listener<byte[]>() {
            @Override
            public void onResponse(byte[] response) {
                if (callback != null) {
                    try {
                        String jsonString =
                                new String(response, CHARSET);
                        JSONObject object = new JSONObject(jsonString);
                        callback.onRequestDone(object);
                    } catch (UnsupportedEncodingException e) {
                        callback.onRequestDone(null);
                    } catch (JSONException je) {
                        callback.onRequestDone(null);
                    }
                } else {
                    try {
                        String string =
                                new String(response, CHARSET);
                        textCallback.onRequestDone(string);
                    } catch (UnsupportedEncodingException e) {
                        textCallback.onRequestDone(null);
                    }
                }
                mAuthorizationErrors = 0;
            }
        };

        Response.ErrorListener errorListenerWrapper = new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
                    mOauthManager.getNewOAuthToken(tokenCallback, SCOPE);
                    mAuthorizationErrors++;
                } else {
                    callback.onRequestDone(null);
                }
            }
        };

        ApiRequest request = new ApiRequest(method, url, body, bodyContentType, responseListener, errorListenerWrapper);

        mRequestQueue.add(request);

    }

    public void clearToken() {
        setToken("");
    }

    private void setToken(String token) {
        mToken = token;
        mHeaders.put("Authorization", "Bearer " + token);
    }

    private void listDrives(String folder, String searchString, final DrivesCallback callback) {
        final ApiRequestCallback findCallback = new ApiRequestCallback() {
            @Override
            public void onRequestDone(JSONObject object) {
                try {
                    JSONArray items = object.getJSONArray("items");
                    ArrayList<Drive> drives = new ArrayList<Drive>();
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        drives.add(new Drive(item));
                    }
                    callback.onDrives(drives);
                } catch (Exception e) {
                    callback.onDrives(null);
                }
            }
        };

        String url = String.format(ENDPOINT_FILES, folder);
        if (!searchString.equals("")) {
            searchString += " and ";
        }
        searchString += "trashed = false";
        searchString += " and '" + folder + "' in parents";
        searchString = URLEncoder.encode(searchString);
        url += "?q=" + searchString;

        sendJsonApiRequest(Request.Method.GET, url, null, findCallback);
    }

    private void findOrCreateDrive(final String parent, final String title, final String mimeType, final DriveCallback callback) {

        final DriveCallback createCallback = new DriveCallback() {
            @Override
            public void onDrive(Drive drive) {
                callback.onDrive(drive);
            }
        };

        final DrivesCallback findCallback = new DrivesCallback() {
            @Override
            public void onDrives(ArrayList<Drive> drives) {
                if (drives == null || drives.size() == 0) {
                    createDrive(parent, title, mimeType, createCallback);
                } else {
                    if (drives.size() > 1) {
                        Utils.log("More than 1 drive matching " + title + " (" + mimeType + ")");
                    }
                    callback.onDrive(drives.get(0));
                }
            }
        };

        String searchString = "title = '" + title + "'";
        searchString += " and mimeType = '" + mimeType + "'";
        listDrives(parent, searchString, findCallback);
    }

    public void putBackup(final Item item, final SaveCallback callback) {
        Calendar rightNow = Calendar.getInstance();
        final String title = "Backup_" + rightNow.get(Calendar.YEAR) + "_W" + rightNow.get(Calendar.WEEK_OF_YEAR) + ".txt";

        final UploadCallback uploadCallback = new UploadCallback() {
            @Override
            public void onUpload(boolean success) {
                callback.onSave(success);
            }
        };

        final DriveCallback folderCallback = new DriveCallback() {
            @Override
            public void onDrive(Drive drive) {
                if (drive == null) {
                    callback.onSave(false);
                } else {
                    final String parentId = drive.id;

                    final DrivesCallback filesCallback = new DrivesCallback() {
                        @Override
                        public void onDrives(ArrayList<Drive> drives) {
                            if (drives == null) {
                                callback.onSave(false);
                                return;
                            }

                            for (int i = 1; i < drives.size(); i++) {
                                Utils.log("delete extra backup");
                                deleteDrive(drives.get(i).id);
                            }

                            String fileId = null;
                            if (drives.size() > 0) {
                                fileId = drives.get(0).id;
                            }
                            ByteArrayOutputStream ostream = new ByteArrayOutputStream();
                            try {
                                item.serialize(ostream);
                                uploadTextContent(parentId, fileId, title, new String(ostream.toByteArray()), uploadCallback);
                            } catch (IOException e) {
                                e.printStackTrace();
                                callback.onSave(false);
                            }
                        }
                    };

                    String searchString = "title = '" + title + "'";
                    listDrives(drive.id, searchString, filesCallback);
                }
            }
        };


        findOrCreateDrive(ID_ROOT, TREEDO_FOLDER_NAME, MIMETYPE_FOLDER, folderCallback);
    }

    private void deleteDrive(final String id) {
        ApiRequestCallback apiCallback = new ApiRequestCallback() {
            @Override
            public void onRequestDone(JSONObject object) {
                Utils.log(id + " deleted");
            }
        };

        String url = ENDPOINT_DELETE + id;
        sendJsonApiRequest(Request.Method.DELETE, url, null, apiCallback);
    }

    private void uploadTextContent(String parentId, String id, String title, String text, final UploadCallback callback) {

        final ApiRequestCallback apiCallback = new ApiRequestCallback() {
            @Override
            public void onRequestDone(JSONObject object) {
                callback.onUpload(object != null);
            }
        };

        String boundary = "foo_bar_baz";
        String contentType = "multipart/related; boundary=\"" + boundary + "\"";

        String body = "--" + boundary + "\n";
        body += "Content-Type: " + CONTENT_TYPE_JSON + "\n\n";

        JSONObject root = new JSONObject();
        try {
            root.put("title", title);
            JSONArray parents = new JSONArray();
            JSONObject parent = new JSONObject();
            parent.put("id", parentId);
            parents.put(parent);
            root.put("parents", parents);
        } catch (JSONException e) {
            e.printStackTrace();
            callback.onUpload(false);
        }

        body += root.toString();

        body += "\n" + "--" + boundary + "\n";
        body += "Content-Type: " + MIMETYPE_TEXT + "\n\n";

        body += text;

        body += "\n" + "--" + boundary + "--\n";

        int method;
        String url = null;

        if (id != null) {
            url = "https://www.googleapis.com/upload/drive/v2/files/" + id;
            method = Request.Method.PUT;
        } else {
            url = "https://www.googleapis.com/upload/drive/v2/files";
            method = Request.Method.POST;
        }
        url += "?uploadType=multipart";
        sendApiRequest(method, url, body.getBytes(), contentType, apiCallback, null);
    }

    private void createDrive(String parent, String title, String mimeType, final DriveCallback callback) {
        String url = ENDPOINT_FILES;
        JSONObject postData = new JSONObject();
        try {
            JSONArray parentArray = new JSONArray();
            JSONObject parentObject = new JSONObject();
            parentObject.put("id", parent);
            parentArray.put(parentObject);

            postData.put("mimeType", mimeType);
            postData.put("title", title);
            postData.put("parents", parentArray);
        } catch (JSONException e) {
            e.printStackTrace();
            callback.onDrive(null);
            return;
        }

        ApiRequestCallback apiCallback = new ApiRequestCallback() {
            @Override
            public void onRequestDone(JSONObject object) {
                Drive drive = null;
                try {
                    drive = new Drive(object);
                } catch (Exception e) {
                }

                callback.onDrive(drive);
            }
        };

        sendJsonApiRequest(Request.Method.POST, url, postData, apiCallback);
    }

    interface BackupCallback {
        public void onBackupDone(Item backup);
    }

    public void getBackup(Drive drive, final BackupCallback callback) {
        String url = drive.downloadUrl;
        if (url == null) {
            callback.onBackupDone(null);
            return;
        }

        final ApiTextRequestCallback textCallback = new ApiTextRequestCallback() {
            @Override
            public void onRequestDone(String text) {
                InputStream istream = new ByteArrayInputStream(text.getBytes());
                Item item = null;
                try {
                    item = Item.deserialize(istream);
                } catch (IOException e) {
                }

                callback.onBackupDone(item);
            }
        };

        sendApiRequest(Request.Method.GET, url, null, null, null, textCallback);
    }

    public void getBackupList(final DrivesCallback callback) {

        DriveCallback folderCallback = new DriveCallback() {
            @Override
            public void onDrive(Drive drive) {
                if (drive == null) {
                    callback.onDrives(null);
                } else {
                    listDrives(drive.id, "", callback);

                }
            }
        };
        findOrCreateDrive(ID_ROOT, TREEDO_FOLDER_NAME, MIMETYPE_FOLDER, folderCallback);
    }

}
