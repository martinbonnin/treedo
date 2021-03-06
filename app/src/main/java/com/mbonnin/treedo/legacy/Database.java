package com.mbonnin.treedo.legacy;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.mbonnin.treedo.Utils;

import java.io.File;
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

    private static final int ID_NONE = -1;
    public static final int ID_ROOT = 0;
    public static final int ID_TRASH = 1;
    public static final int ID_FIRST_USABLE = 2;

    public static final String DATABASE_NAME = "list.db";

    private static Item sRoot;
    private static SQLiteDatabase sDatabase;
    private static int sID;

    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_NAME_ID + " INTEGER PRIMARY KEY," +
                    COLUMN_NAME_CHECKED + " INTEGER," +
                    COLUMN_NAME_ORDER + " INTEGER," +
                    COLUMN_NAME_PARENT + " INTEGER," +
                    COLUMN_NAME_IS_A_DIRECTORY + " INTEGER," +
                    COLUMN_NAME_TEXT + " TEXT)";

    private static class DatabaseItem {
        int id;
        boolean checked;
        String text;
        int order;
        int parent;
        boolean isADirectory;
        @Override
        public String toString() {
            return String.format("%3d. %50s - %3d", id, text, parent);
        }
    }


    static class ItemDatabaseOpenHelper extends SQLiteOpenHelper {
        // If you change the database schema, you must increment the database version.
        public static final int DATABASE_VERSION = 1;
        private Context mContext;

        public ItemDatabaseOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL(SQL_CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int fromVersion, int toVersion) {
            Utils.log("database upgrade not handled");
        }
    }

    static public Item getRoot(Context context) {
        if (sDatabase == null) {
            File dbFile = context.getDatabasePath(DATABASE_NAME);
            if (!dbFile.exists()) {
                return null;
            }

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

            ArrayList<DatabaseItem> dbItemArrayList = new ArrayList<DatabaseItem>(cursor.getCount());

            while (cursor.moveToNext()) {
                DatabaseItem dbItem = new DatabaseItem();
                dbItem.id = cursor.getInt(0);
                dbItem.checked = cursor.getInt(1) > 0 ? true : false;
                dbItem.order = cursor.getInt(2);
                dbItem.parent = cursor.getInt(3);
                dbItem.isADirectory = cursor.getInt(4) > 0 ? true : false;
                dbItem.text = cursor.getString(5);
                dbItemArrayList.add(dbItem);

                Utils.log(dbItem.toString());
            }


            ArrayList<Item> itemArrayList = new ArrayList<Item>();
            for (int i = 0; i < dbItemArrayList.size(); i++) {
                Item item = new Item();
                DatabaseItem dbItem = dbItemArrayList.get(i);
                item.isAFolder = dbItem.isADirectory;
                item.text = dbItem.text;
                item.checked = dbItem.checked;
                if (dbItem.id == ID_ROOT) {
                    sRoot = item;
                    item.isRoot = true;
                } else if(dbItem.id == ID_TRASH) {
                    item.isTrash = true;
                }
                itemArrayList.add(item);
            }

            DatabaseItem currentDbParent = null;
            Item currentParent = null;
            for (int i = 0; i < dbItemArrayList.size(); i++) {
                DatabaseItem dbItem = dbItemArrayList.get(i);
                if (currentDbParent != null && dbItem.parent != currentDbParent.id) {
                    currentDbParent = null;
                }

                if (currentDbParent == null) {
                    for (int j = 0; j < dbItemArrayList.size(); j++) {
                        DatabaseItem dbItem2 = dbItemArrayList.get(j);
                        if (dbItem2.id == dbItem.parent) {
                            currentDbParent = dbItem2;
                            currentParent = itemArrayList.get(j);
                            break;
                        }
                    }
                    if (currentDbParent == null) {
                        Utils.log("parent not found for item " + dbItem.id + ", parent " + dbItem.parent);
                    }
                }

                if (currentParent != null) {
                    currentParent.children.add(itemArrayList.get(i));
                }
            }

            if (sRoot == null) {
                Utils.log("Oopps, no root found");
                sRoot = Item.createRoot();
            } else {
                if (!sRoot.isAFolder) {
                    sRoot.isAFolder = true;
                    Utils.log("Oopps, root item is not a directory");
                }
            }
        }

        return sRoot;
    }

    static int insertItem(SQLiteDatabase database, Item item, int parentId, int order) {
        ContentValues values = new ContentValues();
        int id;
        if (item.isRoot) {
            id = ID_ROOT;
        } else if (item.isTrash) {
            id = ID_TRASH;
        } else {
            id = sID++;
        }
        values.put(COLUMN_NAME_ID, id);
        values.put(COLUMN_NAME_CHECKED, item.checked ? 1 : 0);
        values.put(COLUMN_NAME_ORDER, order);
        values.put(COLUMN_NAME_PARENT, parentId);
        values.put(COLUMN_NAME_IS_A_DIRECTORY, item.isAFolder ? 1 : 0);
        values.put(COLUMN_NAME_TEXT, item.text);

        database.insert(TABLE_NAME, null, values);

        return id;
    }
}
