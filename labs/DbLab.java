package com.dominicapps.tablelab.lab.labs;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


/**
 * Empty class helpful to query the database meta data info.
 */

public class DbLab extends TableLab {

    public DbLab(Activity activity, SQLiteDatabase database) {
        super(activity, database);
    }

        @Override
    public String table() {
        return null;
    }

    @Override
    public String[] where() {
        return new String[0];
    }

    @Override
    public String when() {
        return null;
    }

    @Override
    public String orderBy() {
        return null;
    }

    @Override
    public String groupBy() {
        return null;
    }

    @Override
    public String having() {
        return null;
    }

    @Override
    public ContentValues values(Object o) {
        return null;
    }

    @Override
    public Object model(Cursor cursor) {
        return null;
    }
}
