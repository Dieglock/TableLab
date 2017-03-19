//package com.dominicapps.dictionary.tablelab.utils;
package com.dominicapps.tablelab.lab.db;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.dominicapps.tablelab.lab.labs.DbLab;
import com.dominicapps.tablelab.lab.labs.TableLab;

/**
 * Handles database open close cycle. Allows you to open a close SQL database / TableLab
 */

public class DbManager {
    public static final String TAG = DbManager.class.getSimpleName();

    private DbHelper mDbHelper;
    private Activity mActivity;
    private SQLiteDatabase mDatabase;

    public DbManager() {
    }

    public DbManager(Activity activity) {
        this.mDbHelper = new DbHelper(activity);
        mActivity = activity;
    }

    public DbLab openDbLab() {
        if (TableLab.printLog) {
            TableLab.toLog("Opening ", DbLab.class);
        }
        return new DbLab(mActivity, readable());
    }

    // new tables here:

    private SQLiteDatabase writable() {
        return mDatabase = mDbHelper.getWritableDatabase();
    }

    private SQLiteDatabase readable() {
        return mDatabase = mDbHelper.getReadableDatabase();
    }

    public void closeLab() {
        close(mDatabase);
    }

    public void close(SQLiteDatabase database) {
        if (null != database) {
            if (TableLab.printLog) {
                TableLab.toLog("Closing ", database.getClass());
            }
            database.close();
        }
    }

    public void close(Cursor cursor) {
        if (null != cursor) {
            if (TableLab.printLog) {
                TableLab.toLog("Closing ", cursor.getClass());
            }
            cursor.close();
        }
    }

}
