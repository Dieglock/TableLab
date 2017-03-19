//package com.dominicapps.dictionary.tablelab.labs;
package com.dominicapps.tablelab.lab.labs;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.dominicapps.tablelab.lab.utils.Pair;

/**
 * Lab to fast create tables with common id, left, right.
 * Check DatingTable for implementation
 */

public abstract class ManyLab extends TableLab<Pair> {
    public static final String TAG = ManyLab.class.getSimpleName();

    public ManyLab(Activity activity, SQLiteDatabase database) {
        super(activity, database);
    }

    public abstract String left();
    public abstract String right();

    @Override
    public String when() {
        return UPDATED;
    }

    @Override
    public String where() {
        return null;
    }

    @Override
    public ContentValues values(Pair pair) {
        ContentValues values = new ContentValues();
        values.put(left(), pair.getLeft());
        values.put(right(), pair.getRight());
        values.put(CREATED, pair.getCreated().getMillis());
        values.put(UPDATED, pair.getUpdated().getMillis());
        return values;
    }

    @Override
    public Pair model(Cursor cursor) {
        Pair pair = new Pair();
        pair.setId(cursor.getInt(cursor.getColumnIndex(ID)));
        pair.setLeft(cursor.getInt(cursor.getColumnIndex(left())));
        pair.setRight(cursor.getInt(cursor.getColumnIndex(right())));
        pair.setCreated(TableLab.date(cursor.getInt(cursor.getColumnIndex(CREATED))));
        pair.setUpdated(TableLab.date(cursor.getInt(cursor.getColumnIndex(UPDATED))));
        return pair;
    }

//    public ArrayList<Pair> pairs(int id, int side, boolean orderByTime, boolean descOrder) {
//        return select(null, String.valueOf(id), true, null, null, null, side, orderByTime, descOrder, null, null);
//    }
//
//    public ArrayList<Pair> pairs(int id, int side, boolean orderByTime, boolean descOrder, DateTime start, DateTime end) {
//        return select(null, String.valueOf(id), true, null, null, null, side, orderByTime, descOrder, start, end);
//    }
}
