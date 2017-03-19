// package com.dominicapps.dictionary.tablelab.utils;
package com.dominicapps.tablelab.lab.db;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.dominicapps.tablelab.lab.labs.TableLab;

import java.io.File;

/**
 *  Regular database helper with couple of utils methods. Use it do declare the tables
 *  to the database.
 */

public class DbHelper extends SQLiteOpenHelper {
    public static final String TAG = DbHelper.class.getSimpleName();


    public static String DATABASE_NAME ="your_database.db";
    public static int DATABASE_VERSION = 1;

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {


        if (TableLab.printLog) {
            Log.d(TAG, DATABASE_NAME + " has been created with 4 tables");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {



        if(TableLab.printLog) {
            Log.d(TAG, "Upgrading " + DATABASE_NAME
                    + " version from: v." + oldVersion + ", to: v." + newVersion);
        }
    }

    //////// UTILS ////////////////////////////////////////////////////////////////////////////

    public static long getDbSize(Activity activity) {
        if (TableLab.printLog) {
            Log.d(TAG, "Getting database size");
        }
        return getDbFile(activity).length();
    }

    public static File getDbFile(Activity context) {
        if (TableLab.printLog) {
            Log.d(TAG, "Getting database file");
        }
        return new File(context.getDatabasePath(DATABASE_NAME).getPath());
    }
}
