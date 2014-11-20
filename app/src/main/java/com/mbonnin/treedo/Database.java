package com.mbonnin.treedo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by martin on 14/08/14.
 */
public class Database {
    private static final String TABLE_NAME = "items";

    private static final String COLUMN_NAME_ID = "id";
    private static final String COLUMN_NAME_CHECKED = "checked";
    private static final String COLUMN_NAME_ORDER = "user_order";
    private static final String COLUMN_NAME_PARENT = "parent";
    private static final String COLUMN_NAME_IS_A_DIRECTORY = "is_a_directory";
    private static final String COLUMN_NAME_TEXT = "text";

    private static Item sRoot;
    private static SQLiteDatabase sDatabase;
    // the biggest unique id
    private static int sID;
    private static WorkerThread sThread;

    private static final int MESSAGE_SAVE = 1;
    private static final int MESSAGE_SYNC = 2;

    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_NAME_ID + " INTEGER PRIMARY KEY," +
                    COLUMN_NAME_CHECKED + " INTEGER," +
                    COLUMN_NAME_ORDER + " INTEGER," +
                    COLUMN_NAME_PARENT + " INTEGER," +
                    COLUMN_NAME_IS_A_DIRECTORY + " INTEGER," +
                    COLUMN_NAME_TEXT + " TEXT)";

    public static void setRoot(Item mItem) {
        sRoot = mItem;

        sID = 0;
        findBiggestID(mItem);
        sID++;
    }

    private static void findBiggestID(Item mItem) {
        if (mItem.id > sID) {
            sID = mItem.id;
        }

        for (Item child:mItem.children) {
            findBiggestID(child);
        }
    }

    public static void sync() {
        if (sThread != null) {
            sendMessage(MESSAGE_SYNC, null);
            boolean continueWaiting = true;
            synchronized (sDatabase) {
                while (continueWaiting) {
                    try {
                        sDatabase.wait();
                        continueWaiting = false;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            Utils.log("Database thread synced");
        }
    }

    static class ItemDatabaseOpenHelper extends SQLiteOpenHelper {
        // If you change the database schema, you must increment the database version.
        public static final int DATABASE_VERSION = 1;
        public static final String DATABASE_NAME = "list.db";
        private Context mContext;

        public ItemDatabaseOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL(SQL_CREATE_TABLE);

            InputStream inputStream = mContext.getResources().openRawResource(R.raw.tutorial);
            try {
                Item root = Item.deserialize(inputStream);
                save(sqLiteDatabase, root);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int fromVersion, int toVersion) {
            Utils.log("database upgrade not handled");
        }
    }

    static Item getRoot(Context context) {
        if (sDatabase == null) {
            ItemDatabaseOpenHelper helper = new ItemDatabaseOpenHelper(context);
            sDatabase = helper.getWritableDatabase();

            String[] projection = {
                    COLUMN_NAME_ID,
                    COLUMN_NAME_CHECKED,
                    COLUMN_NAME_ORDER,
                    COLUMN_NAME_PARENT,
                    COLUMN_NAME_IS_A_DIRECTORY,
                    COLUMN_NAME_TEXT
            };

            final String sortOrder = "parent";

            Cursor cursor;
            cursor = sDatabase.query(TABLE_NAME, projection, null, null, null, null, sortOrder);

            ArrayList<Item> itemArrayList = new ArrayList<Item>(cursor.getCount());

            while (cursor.moveToNext()) {
                Item item = new Item(cursor.getInt(0));
                item.checked = cursor.getInt(1) > 0 ? true : false;
                item.order = cursor.getInt(2);
                item.parent = cursor.getInt(3);
                item.isADirectory = cursor.getInt(4) > 0 ? true : false;
                // XXX: needed ?
                item.text = new String(cursor.getString(5));
                itemArrayList.add(item);

                Utils.log(item.toString());
                if (item.id > sID) {
                    sID = item.id;
                }
            }
            sID++;

            if (itemArrayList.size() == 0) {
                // might happen if we really mess the database;
                sRoot = new Item(0);
            } else {
                sRoot = itemArrayList.get(0);
                Item currentParent = null;
                for (int i = 1; i < itemArrayList.size(); i++) {
                    Item item = itemArrayList.get(i);
                    if (currentParent != null && item.parent != currentParent.id) {
                        currentParent = null;
                    }

                    if (currentParent == null) {
                        // XXX sort + binary search
                        for (Item item2 : itemArrayList) {
                            if (item2.id == item.parent) {
                                currentParent = item2;
                                break;
                            }
                        }
                        if (currentParent == null) {
                            Utils.log("parent not found for item " + item.id + ", parent " + item.parent);
                        }
                    }

                    if (currentParent != null) {
                        currentParent.children.add(item);
                    }
                }
            }
        }

        return sRoot;
    }

    static private void insertOrUpdateItem(SQLiteDatabase sqLiteDatabase, Item item, boolean insert) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_ID, item.id);
        values.put(COLUMN_NAME_CHECKED, item.checked ? 1 : 0);
        values.put(COLUMN_NAME_ORDER, item.order);
        values.put(COLUMN_NAME_PARENT, item.parent);
        values.put(COLUMN_NAME_IS_A_DIRECTORY, item.isADirectory ? 1 : 0);
        values.put(COLUMN_NAME_TEXT, item.text);

        if (insert) {
            sqLiteDatabase.insert(TABLE_NAME, null, values);
        } else {
            sqLiteDatabase.update(TABLE_NAME, values, COLUMN_NAME_ID + "=" + item.id, null);
        }
    }

    static Item createItem() {
        Item item = new Item(sID++);
        return item;
    }

    static void insertItem(SQLiteDatabase database, Item item) {
        Utils.log("inserting item " + item.id);
        insertOrUpdateItem(database, item, true);
    }

    static class WorkerThread extends HandlerThread {
        public Handler mHandler;

        public WorkerThread(String name) {
            super(name);
        }

        public synchronized void waitUntilReady() {
            mHandler = new Handler(getLooper(), new Handler.Callback() {
                public boolean handleMessage(Message msg) {
                    switch (msg.what) {
                        case MESSAGE_SAVE:
                            save(sDatabase, (Item) msg.obj);
                            break;
                        case MESSAGE_SYNC:
                            synchronized (sDatabase) {
                                sDatabase.notifyAll();
                            }
                            break;
                    }

                    return true;
                }
            });
        }
    }

    private static void sendMessage(int what, Object param) {
        if (sThread == null) {
            sThread = new WorkerThread("database worker");
            sThread.start();
            // wait for the thread to start;
            sThread.waitUntilReady();
        }

        Message message = sThread.mHandler.obtainMessage(what, param);
        sThread.mHandler.sendMessage(message);
    }

    static void saveAsync(Item item) {
        // we do a copy first as item are accessed from 2 threads
        Item clone = item.deepCopy(Integer.MAX_VALUE);

        sendMessage(MESSAGE_SAVE, clone);
    }

    private static void add(SQLiteDatabase database, Item item) {
        insertItem(database, item);
        for (Item item2:item.children) {
            add(database, item2);
        }
    }

    private static void save(SQLiteDatabase database, Item item) {
        database.beginTransaction();
        database.delete(TABLE_NAME, null, null);
        add(database, item);
        database.setTransactionSuccessful();
        database.endTransaction();
    }
}
