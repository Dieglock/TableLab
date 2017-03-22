package com.dominicapps.tablelab.lab.labs;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import com.dominicapps.tablelab.lab.db.DbManager;

import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

/**
 * @param <T> Any java bean
 * returns crud and meta data.
 */
public abstract class TableLab<T> {
    public static final String TAG = TableLab.class.getSimpleName();

     /**
     * If on, every call made to database is logged.
     */
    public static boolean printLog = false;

    //////// ABSTRACTS ////////////////////////////////////////////////////////////////////////

    public abstract String table(); // Table name
    public abstract String[] where(); // Default columns to search
    public abstract String when(); // Default column to order by time
    public abstract String orderBy(); // Default column to order alphabetically
    public abstract String groupBy(); // Default column to order alphabetically
    public abstract String having(); // Default column to order alphabetically
    public abstract ContentValues values(T t); // Used at save and update
    public abstract T model(Cursor cursor); // Any java bean to be rebuild from database

    //////// FIELDS ///////////////////////////////////////////////////////////////////////////

    private SQLiteDatabase mDatabase = null;
    private DbManager mDbManager;
    private Activity mActivity;

    //////// CONSTRUCTOR ///////////////////////////////////////////////////////////////////////

    public TableLab(Activity activity, SQLiteDatabase database) {
        mActivity = activity;
        mDatabase = database;
        mDbManager = new DbManager(activity);
    }

    //////// CREATE_TABLE /////////////////////////////////////////////////////////////////////////////

    /**
     *
     * @param t TableLab<T> object
     */
    public void save(T t) {
        if (printLog) {
            Log.v(TAG, SAVING + t.getClass().getSimpleName());
        }

        mDatabase.insert(table(), null, values(t));
    }

    /**
     *
     * @param t TableLab<T> object
     * @return sql long result of insertOrThrow(...)
     */
    public long saveWithResponse(T t) {
        if (printLog) {
            Log.v(TAG, SAVING_WITH_RESPONSE + t.getClass().getSimpleName());
        }

        return mDatabase.insertOrThrow(table(), null, values(t));
    }

    /**
     * Time saver to save T lists
     * @param labTs TableLab<T> T objects
     */
    public void saveBatch(ArrayList<T> labTs) {
        if (printLog) {
            Log.v(TAG, BATCH_SAVING);
        }

        for (int i=0;i<labTs.size();i++) {
            save(labTs.get(i));
        }
    }

    //////// UPDATE /////////////////////////////////////////////////////////////////////////////

    /**
     *
     * @param t TableLab<T> object
     * @param id key
     */
    public void update(T t, int id) {
        if (printLog) {
            Log.v(TAG, UPDATING + t.getClass().getSimpleName());
        }
        // TODO get id from t
        mDatabase.update(table(), values(t), "id=?", new String[]{String.valueOf(id)});
    }

    /**
     *
     * @param t TableLab<T> object
     * @param id key
     * @return SQL response long to update()
     */
    public long updateWithResponse(T t, int id) {
        if (printLog) {
            Log.v(TAG, UPDATING_WITH_RESPONSE + t.getClass().getSimpleName());
        }
        // TODO get id from t
        return mDatabase.update(table(), values(t), "id=?", new String[]{String.valueOf(id)});
    }

    //////// DELETE /////////////////////////////////////////////////////////////////////////////

    /**
     *
     * @param id key
     */
    public void delete(int id) {
        if (printLog) {
            Log.v(TAG, DELETING + String.valueOf(id));
        }

        mDatabase.execSQL("DELETE FROM " + table() + " WHERE id =?", new String[]{String.valueOf(id)});
    }

    /**
     *
     * @param id
     * @param where
     */
    public void delete(int id, String where) {
        if (printLog) {
            Log.v(TAG, DELETING + where + "=" + String.valueOf(id));
        }

        mDatabase.execSQL("DELETE FROM " + table() + " WHERE " + where + "=?", new String[]{String.valueOf(id)});
    }

    /**
     *
     * @param what
     * @param where
     */
    public void delete(String what, String where) {
        if (printLog) {
            Log.v(TAG, DELETING + what);
        }

        mDatabase.execSQL("DELETE FROM " + table() + " WHERE " + where + "=?", new String[]{what});
    }

    //////// FIND ///////////////////////////////////////////////////////////////////////////////

    /**
     * select 1st ID = id
     * @param id key
     * @return t
     */
    public T find(int id) {
        return select(id, null, null, null, true, null, null);
    }

    /**
     * select 1st where() LIKE query
     * @param query search arguments
     * @return t
     */
    public T like(String query) {
        return select(0, null, where(), query, false, null, null);
    }

    /**
     * select 1st where() = query
     * @param query search arguments
     * @return t
     */
    public T exact(String query) {
        return select(0, null, where(), query, true, null, null);
    }

    /**
     * select 1st ID = random id(entries())
     * @return t
     */
    public T random() {
        int id = randomInt(entries());
        return select(id, null, null, null, false, null, null);
    }

    //////// FIND MASTER //////////////////////////////////////////////////////////////////////

    /**
     * 
     * @param id id if 0, searches by query
     * @param columns columns to retrieve
     * @param where columns to search
     * @param query search criteria
     * @param isExact choose query = ? | query like = ?
     * @param startDate start date for range when()
     * @param endDate end date for range()
     * @return
     */
    public T select(int id,
                    String[] columns,
                    String[] where,
                    @Nullable String query,
                    boolean isExact,
                    @Nullable DateTime startDate,
                    @Nullable DateTime endDate) {
        T t = null;
        Cursor cursor = null;
        String queryArgs;
        String[] whereArgs;
        StringBuilder queryBuilder = new StringBuilder();
        List<String> params = new ArrayList<>();

        if (id > 0) {
            queryBuilder.append(ID).append(" =?");
            params.add(String.valueOf(id));

        } else if (!isEmpty(query)) {
            String[] theWhere;

            if (null != where) {
                theWhere = where;
            } else {
                theWhere = where();
            }

            if (null != theWhere) {
                for (int i = 0; i < theWhere.length; i++) {

                    if (isExact) {
                        if (queryBuilder.length() > 0) {
                            queryBuilder.append(" AND ");
                        }

                        queryBuilder.append(theWhere[i]).append(" =?");
                        params.add(query);

                    } else {
                        if (queryBuilder.length() > 0) {
                            queryBuilder.append(" OR ");
                        }

                        queryBuilder.append(theWhere[i]).append(" LIKE ?");
                        params.add(likeParams(query.toLowerCase()));
                    }
                }
            }

            if (null != startDate && null != endDate) {
                if (queryBuilder.length() > 0) {
                    queryBuilder.append(" AND ");
                }

                queryBuilder.append(when()).append(" > ?");
                params.add(String.valueOf(startDate.getMillis()));

                queryBuilder.append(" AND ");
                queryBuilder.append(when()).append(" < ?");
                params.add(String.valueOf(endDate.getMillis()));
            }
        }

        queryArgs = queryBuilder.toString(); // Column = ? | LIKE ?
        whereArgs = new String[params.size()]; // {"string", "date", "integer", "etc"}
        params.toArray(whereArgs);

        try {
            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables(table());
            String sql = builder.buildQuery(columns, queryArgs, null, null, null, "1");

            cursor = mDatabase.rawQuery(sql, whereArgs);

            if (printLog) {
                Log.v(TAG, sql + logSelect(query));
            }

            if (cursor != null && cursor.moveToFirst()) {
                t = model(cursor);
            }

        } finally {
            mDbManager.close(cursor);
        }

        return t;

    }

    //////// LIST //////////////////////////////////////////////////////////////////////////////

    /**
     *
     * @param orderByTime choose when() | orderBy()
     * @param ascOrder choose ASC | DESC
     * @return T object list
     */
    public ArrayList<T> list(boolean orderByTime, boolean ascOrder) {
        return select(null, null, null, null, null, false, null, null, null, orderByTime, ascOrder, null, null, false);
    }

    /**
     *
     * @param query search criteria
     * @param orderByTime choose when() | orderBy()
     * @param ascOrder choose ASC | DESC
     * @return T object list
     */
    public ArrayList<T> like(String query, boolean orderByTime, boolean ascOrder) {
        return select(null, null, null, orderBy(), query, false, null, null, null, orderByTime, ascOrder, null, null, false);
    }

    /**
     *
     * @param query search criteria
     * @param orderByTime choose when() | orderBy()
     * @param ascOrder choose ASC | DESC
     * @return T object list
     */
    public ArrayList<T> exact(String query, boolean orderByTime, boolean ascOrder) {
        return select(null, null, null, orderBy(), query, true, null, null, null, orderByTime, ascOrder, null, null, false);
    }

    /**
     *
     * @param parentId parentId
     * @param where column with parent keys
     * @param orderByTime choose when() | orderBy()
     * @param ascOrder choose ASC | DESC
     * @param asyncTask T object list
     * @return
     */
    public ArrayList<T> children(int parentId, String where, boolean orderByTime, boolean ascOrder, boolean asyncTask) {
        return select(null, null, new String[]{where}, null, String.valueOf(parentId), true, null, null, null, orderByTime, ascOrder, null, null, asyncTask);
    }

    /**
     *
     * @param query
     * @param isExact
     * @param orderByTime
     * @param ascOrder
     * @return
     */
    public ArrayList<T> async(String query, boolean isExact, boolean orderByTime, boolean ascOrder) {
        return select(null, null, null, orderBy(), query, isExact, null, null, null, orderByTime, ascOrder, null, null, true);
    }

    /**
     *
     * @param limit
     * @return
     */
    public ArrayList<T> random(int limit, boolean orderByTime, boolean ascOrder) {
        return select(null, null, null, null, null, false, null, null, String.valueOf(limit), orderByTime, ascOrder, null, null, false);
    }

    //////// LIST MASTER //////////////////////////////////////////////////////////////////////

    /**
     * Master search. Uses given or default values for each field.
     * @param table table to search. By default extended one
     * @param columns columns to retrieve. If null all
     * @param where columns to search. If null where()
     * @param orderBy column to order. If null orderBy()
     * @param query search criteria. If null searches by id
     * @param isExact choose query = ? | query like = ?
     * @param groupBy choose different groups from groupBy()
     * @param having choose different group having from having()
     * @param limit list limit
     * @param orderByTime choose when() | orderBy()
     * @param ascOrder choose ASC | DESC
     * @param start start date for range when()
     * @param end end date for range when()
     * @param asyncTask choose true for heavy load
     * @return T object list
     */
    public ArrayList<T> select(String table,
                               @Nullable String[] columns,
                               @Nullable String[] where,
                               String orderBy,
                               @Nullable String query,
                               boolean isExact,
                               String groupBy,
                               String having,
                               String limit,
                               boolean orderByTime,
                               boolean ascOrder,
                               @Nullable DateTime start,
                               @Nullable DateTime end,
                               boolean asyncTask) {

        ArrayList<T> objects = new ArrayList<>();
        Cursor cursor = null;
        String queryArgs;
        String[] whereArgs;
        String sortOrder;
        StringBuilder queryBuilder = new StringBuilder();
        StringBuilder orderBuilder = new StringBuilder();
        List<String> params = new ArrayList<>();

        String theTable;
        if (null != table) {
            theTable = table;
        } else {
            theTable = table();
        }

        if (!isEmpty(query)) {
            String[] theWhere;

            if (where != null) {
                theWhere = where;
            } else {
                theWhere = where();
            }

            if (theWhere != null) {
                for (int i = 0; i < theWhere.length; i++) {
                    if (isExact) {
                        if (queryBuilder.length() > 0) {
                            queryBuilder.append(" AND ");
                        }

                        queryBuilder.append(theWhere[i]).append(" = ?");
                        params.add(query);

                    } else {
                        if (queryBuilder.length() > 0) {
                            queryBuilder.append(" OR ");
                        }

                        queryBuilder.append(theWhere[i]).append(" LIKE ?");
                        params.add(likeParams(query.toLowerCase()));
                    }
                }
            }
        }

        if (orderByTime) {
            orderBuilder.append(when());

            if (ascOrder) {
                orderBuilder.append(" ASC ");
            } else {
                orderBuilder.append(" DESC ");
            }

        } else {
            orderBuilder.append("lower(");
            if (orderBy == null) {

                orderBuilder.append(orderBy());
            } else {
                orderBuilder.append(orderBy);
            }

            orderBuilder.append(")");

            if (ascOrder) {
                orderBuilder.append(" ASC ");
            } else {
                orderBuilder.append(" DESC ");
            }
        }

        if (null != start && null != end) {
            if (queryBuilder.length() > 0) {
                queryBuilder.append(" AND ");
            }

            queryBuilder.append(when()).append(">?");
            params.add(String.valueOf(start.getMillis()));

            queryBuilder.append(" AND ");
            queryBuilder.append(when()).append("<?");
            params.add(String.valueOf(end.getMillis()));
        }

        String theGroupBy;
        String theHaving;

        if (groupBy != null) {
            theGroupBy = groupBy;
        } else {
            theGroupBy = groupBy();
        }

        if (having != null) {
            theHaving = having;
        } else {
            theHaving = having();
        }

        queryArgs = queryBuilder.toString(); // Column = ? // LIKE ?
        whereArgs = new String[params.size()]; // {"string", "date", "integer", "etc"}
        params.toArray(whereArgs);

        sortOrder = orderBuilder.toString();

        try {

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables(theTable);
            String sql = builder.buildQuery(columns, queryArgs, theGroupBy, theHaving, sortOrder, limit);

            if (printLog) {
                Log.v(TAG, sql + logSelect(query));
            }


            if (asyncTask) {
                HeavyTask heavyTask = new HeavyTask();
                heavyTask.execute(new String[]{sql}, whereArgs);

                try {

                    cursor = heavyTask.get();

                } catch (InterruptedException e) {
                    Log.e(TAG, e.getMessage());
                } catch (ExecutionException e) {
                    Log.e(TAG, e.getMessage(), e.fillInStackTrace());
                }


            } else  {
                cursor = mDatabase.rawQuery(sql, whereArgs);
            }

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    T t = model(cursor);
                    objects.add(t);
                }
            }

        } finally {
            mDbManager.close(cursor);
        }

        return objects;
    }

    //////// ASYNC TASK ///////////////////////////////////////////////////////////////////////////

    public class HeavyTask extends AsyncTask<String[], Integer, Cursor> {

        @Override
        protected Cursor doInBackground(String[]... params) {
            if (printLog) {
                Log.v(TAG, "Starting asyncTask");
            }
                return mDatabase.rawQuery(params[0][0], params[1]);
            }

            @Override
            protected void onPostExecute(Cursor cursor) {
                super.onPostExecute(cursor);
            if (printLog) {
                Log.v(TAG, "Asynctask complete");
            }
        }
    }

    //////// COMMON QUERIES ///////////////////////////////////////////////////////////////////////

    /**
     *
     * @param table
     * @param value
     * @return
     */
    public T mode(String table, String value) {
        // todo test
        T t = null;
        Cursor cursor = null;
        String times = "times";
        String selectedTable = null;

        String selection = SELECT + "`" + value + "`," + COUNT + "(*) " + AS + "`" + times + "`" +
                            FROM + table;
                // + LIMIT + "1";
                //+ GROUP_BY + "`" + value +"`" + ORDER_BY + "`" + times + "`" +


        try {
            if (printLog) {
                Log.v(TAG, selection);
            }

            cursor = mDatabase.rawQuery(selection, null);

            if (cursor != null && cursor.moveToFirst()) {
                t = model(cursor);
            }
        } finally {
            mDbManager.close(cursor);
        }
        return t;
    }

    /**
     *
     * @param table
     * @param value
     * @return
     */
    public T mean(String table, String value) {
        // todo test
        T t = null;
        Cursor cursor = null;
        String times = "times";
        String selectedTable = null;

        String selection = SELECT + "`" + value + "`," + COUNT + "(*) " + AS + "`" + times + "`" +
                FROM + table;
        // + LIMIT + "1";
        //+ GROUP_BY + "`" + value +"`" + ORDER_BY + "`" + times + "`" +


        try {
            if (printLog) {
                Log.v(TAG, selection);
            }

            cursor = mDatabase.rawQuery(selection, null);

            if (cursor != null && cursor.moveToFirst()) {
                t = model(cursor);
            }
        } finally {
            mDbManager.close(cursor);
        }
        return t;
    }

    public void standardDeviation() {
        // TODO
    }

    //////// META QUERIES ///////////////////////////////////////////////////////////////////////

    public boolean hasData() {
        String select = SELECT_COUNT_ALL_FROM + table();

        if (printLog) {
            // Log.v(TAG, select);
        }

        int count = -1;
        Cursor cursor = null;
        try {
            cursor = mDatabase.rawQuery(select, null);
            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } finally {
            mDbManager.close(cursor);
        }

        if (count > 0) {
            return true;
        }

        return false;
    }

    public ArrayList<String> tables() {
        String select = "select name from sqlite_master where type='table' order by name";
        if (printLog) {
            // Log.v(TAG, select);
        }
        ArrayList<String> tables = new ArrayList<>();
        Cursor cursor = mDatabase.rawQuery(select, null);
        while (cursor.moveToNext()) {
            tables.add(cursor.getString(0));
        }
        cursor.close();
        return tables;
    }

    public ArrayList<String> views() {
        String select = "select name from sqlite_master where type='view' order by name";
        if (printLog) {
            // Log.v(TAG, select);
        }
        ArrayList<String> tables = new ArrayList<>();
        Cursor cursor = mDatabase.rawQuery(select, null);
        while (cursor.moveToNext()) {
            tables.add(cursor.getString(0));
        }
        cursor.close();
        return tables;
    }

    public ArrayList<String> columns(String table) {
        String select = "PRAGMA table_info(" + table + ")";
        if (printLog) {
            // Log.v(TAG, select);
        }

        ArrayList<String> columns = new ArrayList<>();
        Cursor cursor = mDatabase.rawQuery(select, null);
        while (cursor.moveToNext()) {
            columns.add(cursor.getString(1));
        }
        cursor.close();
        return columns;
    }

    public int entries() {
        int count = (int) DatabaseUtils.queryNumEntries(mDatabase, table());
        if (printLog) {
            Log.v(TAG, "Table " + table() + " has " + String.valueOf(count) + " entries");
        }
        return  count;
    }

    public Cursor data(String[] columns) {
        String query = SELECT_ALL_FROM + table();
        if (printLog) {
            // Log.v(TAG, query + " WHERE " + logWhere(columns));
        }
        return mDatabase.rawQuery(SELECT_ALL_FROM + table(), columns);
    }

    public Cursor data() {
        String query = SELECT_ALL_FROM + table();
        if (printLog) {
            // Log.v(TAG, query);
        }

        return mDatabase.rawQuery(query, null);
    }

    public String schema() {

        ArrayList<String> schema = new ArrayList<>();
        int max = tables().size();
        schema.add("TABLES\n");

        for (int i = 0; i < max; i++) {
            StringBuilder builder = new StringBuilder();
            String table = tables().get(i);
            String columns = columns(table).toString();
            builder.append(table).append(": ");
            builder.append(columns);
            builder.append("\n");
            schema.add(builder.toString());
        }

        schema.add("\nVIEWS\n");
        int remax = views().size();
        for (int i = 0; i < remax; i++) {
            StringBuilder builder2 = new StringBuilder();
            String view = views().get(i);
            String columns = columns(view).toString();
            builder2.append(view).append(": ");
            builder2.append(columns);
            builder2.append("\n");
            schema.add(builder2.toString());
        }

        String result = schema.toString();

        Log.v(TAG, "DATABASE SCHEMA: \n" + result);

        return result;
    }

    public void log(boolean includeData) {
        Log.v(TAG, "---table\n");
        Log.v(TAG, table());
        for (String s : columns(table())) {
            Log.v(TAG, s);
        }
        Log.v(TAG, ":");
        Log.v(TAG, "data? " + hasData());
        Log.v(TAG, "entries: " + String.valueOf(entries()));

        // Log.v(TAG, "-*\n");
        if (includeData) {
            Log.v(TAG,"");
            for (int i = 0; i > entries(); i++) {
                T t = find(i);
                Log.v(TAG, t.toString());
            }
        }
    }

    //////// UTILS /////////////////////////////////////////////////////////////////////////////

    public static DateTime now() {
        return DateTime.now();
    }

    public static DateTime date(int given) {
        return new DateTime(given);
    }

    public static String likeParams(String paramsToCompose) {
        String composed = null;
        if (paramsToCompose == null) {
            throw new NullPointerException("paramsToCompose can not be null");
        }
        composed = "%" + paramsToCompose + "%";
        return composed;
    }

    public static BigDecimal decimal(int amount) {
        return new BigDecimal(amount);
    }

    public static int randomInt(int range) {
        Random random = new Random();
        return random.nextInt(range);
    }

    public static boolean isEmpty(String string) {
        return string == null || string.length() == 0;
    }

    public static String snake(String camelCase) {
        String name = "";
        String regex = "([a-z])([A-Z]+)";
        String replacement = "$1_$2";
        return name.replaceAll(regex, replacement).toLowerCase();
    }

    public static String logSelect(String selection) {
        if (isEmpty(selection)) {
            return "";
        } else {
            return " Selection: " + selection;
        }
    }

    public static String logWhere(String[] array) {
        if (array == null) {
            return  "";
        } else if (array.length < 1) {
            return  "";
        } else {
            return " WHERE " + Arrays.toString(array);
        }
    }

    public static void classToLog(String action, Class c) {
        if (printLog) {
            Log.v(TAG, action + c.getSimpleName());
        }
    }

    public Activity context() {
        return mActivity;
    }

    public SQLiteDatabase database() {
        return mDatabase;
    }

    //////// SEED ///////////////////////////////////////////////////////////////////////////////

    public static BufferedReader txtReader(Activity activity, String uri) throws IOException {
        return new BufferedReader(new InputStreamReader(activity.getAssets().open(uri)));
    }

    //////// CONSTANTS easy to i18 ////////////////////////////////////////////////////////////////

    protected static final String ID = "id";
    protected static final String PARENT_ID = "parent_id";

    // Numbers
    protected static final String IS_PRIMARY = "name";

    protected static final String COLOR = "color";
    protected static final String ALPHA = "alpha";
    protected static final String BLUE = "blue";
    protected static final String GREEN = "green";
    protected static final String RED = "red";

    protected static final String LATITUDE = "latitude";
    protected static final String LONGITUDE = "longitude";

    protected static final String PRICE = "price";
    protected static final String AMOUNT = "amount";
    protected static final String DISCOUNT = "discount";
    protected static final String QUANTITY = "quantity";
    protected static final String SUBTOTAL = "subtotal";
    protected static final String TAX = "tax";
    protected static final String TOTAL = "total";

    // Strings
    protected static final String NAME = "name";
    protected static final String MIDDLE_NAME = "middle_name";
    protected static final String LAST_NAME = "last_name";
    protected static final String SECOND_LAST_NAME = "second_last_name";

    protected static final String TITLE = "title";
    protected static final String TYPE = "type";
    protected static final String DEFINITION = "definition";
    protected static final String COMMENT = "comment";
    protected static final String CONTENT = "content";
    protected static final String LABEL = "label";
    protected static final String DESCRIPTION = "description";
    protected static final String EXTRA = "extra";
    protected static final String ZOOM = "zoom";

    // SQL
    protected static final String AS = " AS ";
    protected static final String ASC = " ASC ";
    protected static final String COUNT = " COUNT ";
    protected static final String CREATE_TABLE_IF_NOT_EXISTS = "CREATE TABLE IF NOT EXISTS ";
    protected static final String CREATE_TABLE = "CREATE TABLE ";
    protected static final String CREATE_VIEW = "CREATE VIEW ";
    protected static final String CREATE_VIEW_IF_NOT_EXISTS = "CREATE VIEW IF NOT EXISTS ";
    protected static final String COMMA = ",";
    protected static final String DESC = " DESC ";
    protected static final String DROP_TABLE_IF_EXISTS = "DROP TABLE IF EXISTS ";
    protected static final String DROP_VIEW_IF_EXISTS = "DROP VIEW IF EXISTS ";
    protected static final String FROM = " FROM ";
    protected static final String GROUP_BY = " GROUP BY ";
    protected static final String INNER = " INNER ";
    protected static final String INTEGER = " INTEGER";
    protected static final String INTEGER_NOT_NULL = " INTEGER NOT NULL";
    protected static final String JOIN = " JOIN ";
    protected static final String LIMIT = " LIMIT ";
    protected static final String SELECT = "SELECT ";
    protected static final String SELECT_ALL_FROM = "SELECT * FROM ";
    protected static final String SELECT_COUNT_ALL_FROM = "SELECT COUNT(*) FROM ";
    protected static final String PRIMARY_KEY = " INTEGER PRIMARY KEY";
    protected static final String ON = " ON ";
    protected static final String ORDER_BY = " ORDER BY ";
    protected static final String OUTER = " OUTER ";
    protected static final String TEXT = " TEXT";
    protected static final String TEXT_NOT_NULL = " TEXT NOT NULL";
    protected static final String WHERE = " WHERE ";
    protected static final String UNIQUE = " UNIQUE ";

    // Dates
    protected static final String END_DATE = "end_date";
    protected static final String CREATED = "created";
    protected static final String START_DATE = "start_date";
    protected static final String UPDATED = "updated";

    // CRUD
    protected static final String CLOSING = "Closing: ";
    protected static final String DELETING = "Deleting: ";
    protected static final String OPENING = "Opening: ";
    protected static final String BATCH_SAVING = "Batch saving: ";
    protected static final String SAVING = "Saving: ";
    protected static final String SAVING_WITH_RESPONSE = "Saving with response: ";
    protected static final String UPDATING = "Updating: ";
    protected static final String UPDATING_WITH_RESPONSE = "Updating with response: ";
}
