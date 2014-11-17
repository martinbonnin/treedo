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

    private static final int MESSAGE_SAVE = 0;
    private static final int MESSAGE_QUIT = 1;

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

        saveAsyncDeep(mItem);

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

        return sRoot;
    }

    static private void insertOrUpdateItem(Item item, boolean insert) {
        insertOrUpdateItem(sDatabase, item, insert);
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
            sqLiteDatabase.update(TABLE_NAME, values, COLUMN_NAME_ID + "=" + Integer.toString(item.id), null);
        }
    }

    static Item createItem() {
        Item item = new Item(sID++);
        return item;
    }

    static void insertItem(SQLiteDatabase database, Item item) {
        insertOrUpdateItem(database, item, true);
    }

    static void insertItem(Item item) {
        insertOrUpdateItem(sDatabase, item, true);
    }

    static void updateItem(Item item) {
        insertOrUpdateItem(item, false);
    }

    static void deleteItem(Item item) {
        sDatabase.delete(TABLE_NAME, COLUMN_NAME_ID + "=" + Integer.toString(item.id), null);
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
                            save((Item) msg.obj);
                            break;
                    }

                    return true;
                }
            });
        }
    }

    static void saveAsyncShallow(Item item) {
        // we do a copy first as item are accessed from 2 threads
        Item clone = item.deepCopy(1);

        saveAsync(clone);
    }

    static void saveAsyncDeep(Item item) {
        // we do a copy first as item are accessed from 2 threads
        Item clone = item.deepCopy(Integer.MAX_VALUE);

        saveAsync(clone);
    }

    static void saveAsync(Item item) {

        if (sThread == null) {
            sThread = new WorkerThread("database worker");
            sThread.start();
            // wait for the thread to start;
            sThread.waitUntilReady();
        }

        Message message = sThread.mHandler.obtainMessage(MESSAGE_SAVE, item);
        sThread.mHandler.sendMessage(message);
    }

    static void save(SQLiteDatabase database, Item item) {
        database.delete(TABLE_NAME, COLUMN_NAME_ID + "=" + Integer.toString(item.id), null);
        insertItem(database, item);
        // delete everything just in case
        database.delete(TABLE_NAME, COLUMN_NAME_PARENT + "=" + Integer.toString(item.id), null);
        for (Item item2:item.children) {
            save(database, item2);
        }
    }

    static void save(Item item) {
        save(sDatabase, item);
    }
}
