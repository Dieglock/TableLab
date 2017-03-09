package com.dominicapps.dictionary.tablelab;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Patterns;

import com.dominicapps.dictionary.tablelab.utils.DbManager;

import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Careful: where() != where
 *
 * @NotNullabe table(): name of the table
 * @NotNullable values() Android Content Values
 * @NotNullable model() Java bean
 *
 *  Check MrBeanTable at app folder for an example
 *
 * @param <T> Any java bean
 */
public abstract class TableLab<T> {
    public static final String TAG = TableLab.class.getSimpleName();

    // TODO implement asyntask option to list select

    /**
     * If on every call made to database is logged.
     */
    public static boolean printLog = true;

    //////// ABSTRACTS ////////////////////////////////////////////////////////////////////////

    public abstract String table(); // Table name
    public abstract String where(); // Default column to order alphabetically
    public abstract String when(); // Default column to order by time
    public abstract ContentValues values(T t);
    public abstract T model(Cursor cursor);

    //////// FIELDS ///////////////////////////////////////////////////////////////////////////

    private SQLiteDatabase mDatabase = null;
    private DbManager mDbManager;

    //////// CONSTRUCTOR ///////////////////////////////////////////////////////////////////////

    public TableLab(Activity activity, SQLiteDatabase database) {
        mDatabase = database;
        mDbManager = new DbManager(activity);
    }

    //////// CREATE /////////////////////////////////////////////////////////////////////////////

    /**
     *
     * @param t TableLab<T> object
     */
    public void save(T t) {
        mDatabase.insert(table(), null, values(t));

        if (printLog) {
            Log.d(TAG, "Saving " + t.getClass().getSimpleName());
        }
    }

    /**
     *
     * @param t TableLab<T> object
     * @return sql long result of insertOrThrow(...)
     */
    public long saveWithResponse(T t) {
        if (printLog) {
            Log.d(TAG, "Saving with response" + t.getClass().getSimpleName());
        }

        return mDatabase.insertOrThrow(table(), null, values(t));
    }

    /**
     * Time saver to save T lists
     * @param labTs TableLab<T> T objects
     */
    public void saveBatch(ArrayList<T> labTs) {
        if (printLog) {
            Log.d(TAG, "Batch saving");
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
            Log.d(TAG, "Updating " + t.getClass().getSimpleName());
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
            Log.d(TAG, "Updating with response " + t.getClass().getSimpleName());
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
            Log.d(TAG, "Deleting " + String.valueOf(id));
        }

        mDatabase.execSQL("DELETE FROM " + table() + " WHERE id =?", new String[]{String.valueOf(id)});
    }

    //////// FIND ///////////////////////////////////////////////////////////////////////////////

    /**
     * select 1st ID = id
     * @param id key
     * @return select(...)
     */
    public T find(int id) {
        return select(id, null, null, null, true, null, null);
    }

    /**
     * select 1st where() LIKE query
     * @param query search arguments
     * @return select(...)
     */
    public T like(String query, String[] where) {
        return select(0, null, where, query, false, null, null);
    }

    /**
     * select 1st where() = query
     * @param query search arguments
     * @return select(...)
     */
    public T exact(String query, String[] where) {
        return select(0, null, where, query, true, null, null);
    }

    /**
     * select 1st ID = random id(entries())
     * @return select(...)
     */
    public T randomT() {
        int id = randomInt(entries());
        return select(id, null, null, null, false, null, null);
    }

    //////// FIND MASTER //////////////////////////////////////////////////////////////////////

    /**
     * Master find
     * @param id choose > 0 = find by id | id 0 is find by where(), where or when()
     * @param columns columns to retrieve.
     * @param query search arguments
     * @param isExact choose query = ? | query LIKE ?
     * @param startDate start range for when()
     * @param endDate end range for when()
     * @return TableLab<T> T object
     */

    /**
     *
     * @param id
     * @param columns
     * @param where
     * @param query
     * @param isExact
     * @param startDate
     * @param endDate
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
        String queryArgs = null;
        String[] whereArgs = null;
        StringBuilder queryBuilder = new StringBuilder();
        List<String> params = new ArrayList<>();

        if (id > 0) {
            queryBuilder.append(ID).append("=?");
            params.add(String.valueOf(id));

        } else if (!isEmpty(query)) {

            if (null != where) {
                for (int i = 0; i < where.length; i++) {

                    if (queryBuilder.length() > 0) {
                        queryBuilder.append(" AND ");
                    }

                    if (isExact) {
                        queryBuilder.append(where[i]).append(" =?");
                        params.add(query);

                    } else {
                        queryBuilder.append(where[i]).append(" LIKE ?");
                        params.add(prepareLikeParams(query.toLowerCase()));
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

            cursor = mDatabase.query(table(), columns, queryArgs, whereArgs, null, null, null, "1");

            if (printLog) {
                Log.d(TAG, "FIND 1 | SELECT " + logFor(cols(columns))  + " FROM " + table() + " WHERE " + logWhere(whereArgs) + "=" + queryArgs);
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

    public ArrayList<T> list(boolean orderByTime, boolean descOrder) {
        return select(null, null, null, null, false, null, null, null, orderByTime, descOrder, null, null);
    }

    public ArrayList<T> list(String query, boolean orderByTime, boolean descOrder) {
        return select(null, new String[]{where()}, where(), query, false, null, null, null, orderByTime, descOrder, null, null);
    }

    public ArrayList<T> list(String[] where, String query, boolean orderByTime, boolean descOrder) {
        return select(null, where, where(), query, false, null, null, null, orderByTime, descOrder, null, null);
    }

    public ArrayList<T> list(String[] where, String orderByColumn, String query, boolean orderByTime, boolean descOrder) {
        return select(null, where, orderByColumn, query, false, null, null, null, orderByTime, descOrder, null, null);
    }

    public ArrayList<T> like(String query, boolean orderByTime, boolean descOrder) {
        return select(null, new String[]{where()}, where(), query, false, null, null, null, orderByTime, descOrder, null, null);
    }

    public ArrayList<T> like(String[] where, String query, boolean orderByTime, boolean descOrder) {
        return select(null, where, where(), query, false, null, null, null, orderByTime, descOrder, null, null);
    }

    public ArrayList<T> like(String[] where, String orderByColumn, String query, boolean orderByTime, boolean descOrder) {
        return select(null, where, orderByColumn, query, false, null, null, null, orderByTime, descOrder, null, null);
    }

    public ArrayList<T> exact(String query, boolean orderByTime, boolean descOrder) {
        return select(null, new String[]{where()}, where(), query, true, null, null, null, orderByTime, descOrder, null, null);
    }

    public ArrayList<T> exact(String[] where, String query, boolean orderByTime, boolean descOrder) {
        return select(null, where, where(), query, true, null, null, null, orderByTime, descOrder, null, null);
    }

    public ArrayList<T> exact(String[] where, String orderByColumn, String query, boolean orderByTime, boolean descOrder) {
        return select(null, where, orderByColumn, query, true, null, null, null, orderByTime, descOrder, null, null);
    }

    //////// LIST MASTER //////////////////////////////////////////////////////////////////////

    // columns, query, isExact, groupBy, having, limit,
    // orderByColumn, orderByTime, descOrder, startDate, endDate

    /**
     *
     * @param columns
     * @param where
     * @param orderByColumn
     * @param query
     * @param isExact
     * @param groupBy
     * @param having
     * @param limit
     * @param orderByTime
     * @param descOrder
     * @param start
     * @param end
     * @return
     */
    public ArrayList<T> select(@Nullable String[] columns,
                               @Nullable String[] where,
                               String orderByColumn,
                               @Nullable String query,
                               boolean isExact,
                               String groupBy,
                               String having,
                               String limit,
                               boolean orderByTime,
                               boolean descOrder,
                               @Nullable DateTime start,
                               @Nullable DateTime end) {

        ArrayList<T> objects = new ArrayList<>();
        Cursor cursor = null;
        String queryArgs = null;
        String[] whereArgs = null;
        String orderBy = null;
        StringBuilder queryBuilder = new StringBuilder();
        List<String> params = new ArrayList<>();


        if (!isEmpty(query)) {

            for (int i = 0; i < where.length; i++) {

                if (queryBuilder.length() > 0) {
                    queryBuilder.append(" AND ");
                }

                if (isExact) {
                    queryBuilder.append(where[i]).append(" =?");
                    params.add(query);

                } else {
                    queryBuilder.append(where[i]).append(" LIKE ?");
                    params.add(prepareLikeParams(query.toLowerCase()));
                }
            }

            if (orderByTime) {
                if (descOrder) {
                    orderBy = when() + " DESC ";

                } else {
                    orderBy = when() + " ASC ";
                }

            } else {
                String theWhere = null;
                if (null == orderByColumn) {
                    theWhere = where();
                } else {
                    theWhere = orderByColumn;
                }

                if (descOrder) {
                    orderBy = "lower(" + theWhere + ") DESC ";
                } else {
                    orderBy = "lower(" + theWhere + ") ASC ";
                }
            }
        }

        if (null != start && null != end) {
            if (queryBuilder.length() > 0) {
                queryBuilder.append(" AND ");
            }

            queryBuilder.append(when()).append(" > ?");
            params.add(String.valueOf(start.getMillis()));

            queryBuilder.append(" AND ");
            queryBuilder.append(when()).append(" < ?");
            params.add(String.valueOf(end.getMillis()));
        }

        if (queryBuilder.length() > 0) { // safety check
            queryArgs = queryBuilder.toString(); // Column = ? // LIKE ?
            whereArgs = new String[params.size()]; // {"string", "date", "integer", "etc"}
            params.toArray(whereArgs);
        }

        try {

            if (printLog) {
                Log.d(TAG, "SELECT " + logFor(cols(columns)) + " FROM " + table() + logWhere(whereArgs)
                        + logSelect(queryArgs) + logByGroup(groupBy) + logHaving(having) + " ORDER BY " + orderBy + logLimit(limit));
            }

            cursor = mDatabase.query(table(), columns, queryArgs, whereArgs, groupBy, having, orderBy, limit);

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

    //////// COMMON QUERIES ///////////////////////////////////////////////////////////////////////

    public T theMode(String table, String value) {
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
                Log.d(TAG, selection);
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

//    public int theMean(String table, String value) {
//        T t = null;
//        Cursor cursor = null;
//        String times = "times";
//        String selectedTable = null;
//
//        String selection = SELECT + "`" + value + "`," + COUNT + "(*) " + AS + "`" + times + "`" +
//                FROM + table;
//        // + LIMIT + "1";
//        //+ GROUP_BY + "`" + value +"`" + ORDER_BY + "`" + times + "`" +
//
//
//        try {
//            if (printLog) {
//                Log.d(TAG, selection);
//            }
//
//            cursor = mDatabase.rawQuery(selection, null);
//
//            if (cursor != null && cursor.moveToFirst()) {
//                t = model(cursor);
//            }
//        } finally {
//            mDbManager.close(cursor);
//        }
//        return t;
//    }

    //////// META QUERIES ///////////////////////////////////////////////////////////////////////

    public boolean hasData() {
        String select = SELECT_COUNT_ALL_FROM + table();

        if (printLog) {
            Log.d(TAG, select);
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
            Log.d(TAG, select);
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
            Log.d(TAG, select);
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
            Log.d(TAG, "Table " + table() + " has " + String.valueOf(count) + " entries");
        }
        return  count;
    }

    public Cursor data(String[] columns) {
        String query = SELECT_ALL_FROM + table();
        if (printLog) {
            Log.d(TAG, query + " WHERE " + logWhere(columns));
        }
        return mDatabase.rawQuery(SELECT_ALL_FROM + table(), columns);
    }

    public Cursor data() {
        String query = SELECT_ALL_FROM + table();
        if (printLog) {
            Log.d(TAG, query);
        }

        return mDatabase.rawQuery(query, null);
    }

    public String schema() {

        ArrayList<String> schema = new ArrayList<>();
        int max = tables().size();

        for (int i = 0; i < max; i++) {
            StringBuilder builder = new StringBuilder();
            String table = tables().get(i);
            String columns = columns(table).toString();
            builder.append(table).append(": ");
            builder.append(columns);
            builder.append("\n");
            schema.add(builder.toString());
        }

        String result = schema.toString();

        if (printLog) {
            Log.d(TAG, "Database schema: " + result);
        }

        return result;


    }

    public void logTable() {
        Log.d(TAG, table() + " table:");
        Log.d(TAG, "Columns: " );
        for (String s : columns(table())) {
            Log.d(TAG, s);
        }
        Log.d(TAG, "Does it have data? " + hasData());
        Log.d(TAG, "Number of entries: " + String.valueOf(entries()));
    }

    //////// UTILS /////////////////////////////////////////////////////////////////////////////

    public static DateTime now() {
        return DateTime.now();
    }

    public static DateTime date(int given) {
        return new DateTime(given);
    }

//    public static String[] whereFromString(String query) {
//        return new String[] {query};
//    }

    public static String prepareLikeParams(String paramsToCompose) {
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

    public static String logFor(String[] cols) {
        return Arrays.toString(cols);
    }

    public static String[] cols(String[] columns) {
        if (null == columns || columns.length < 1) {
            return new String[]{"*"};
        } else {
            return columns;
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

    public static String logSelect(String selection) {
        if (isEmpty(selection)) {
            return "";
        } else {
            return " = " + selection;
        }
    }

    public static String logLimit(String limit) {
        if (isEmpty(limit)) {
            return "";
        } else {
            return " LIMIT " + limit;
        }
    }

    public static String logByGroup(String s) {
        if (isEmpty(s)) {
            return "";
        } else {
            return " GROUP BY "  + s;
        }
    }

    public static String logHaving(String s) {
        if (isEmpty(s)) {
            return "";
        } else {
            return " HAVING " + s;
        }
    }

    public static void toLog(String action, Class c) {
        Log.d(TAG, action + c.getSimpleName());
    }

//    public void logSchema() {
//        Log.d(TAG, schema());
//    }

//    public Activity labContext() {
//        return mActivity;
//    }
//
//    public SQLiteDatabase database() {
//        return mDatabase;
//    }



    //////// VALIDATION //////////////////////////////////////////////////////////////////////////

    public static boolean isValidEmail(String email) {
        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    private boolean isValidPassword(@NonNull String password) {
        if (password.length() > 7) {
            return true;
        } else {
            Pattern pattern = Pattern.compile(ALPHANUMERIC_EXTENDED_PATTERN);
            Matcher matcher = pattern.matcher(password);
            return matcher.matches();
        }
    }

    private boolean isValidToughPassword(@NonNull String password) {
        if (password.length() > 7) {
            return true;
        } else {
            Pattern pattern = Pattern.compile(PASSWORD_PATTERN);
            Matcher matcher = pattern.matcher(password);
            return matcher.matches();
        }
    }

    public static boolean isValidUrl(String url) {
        return Patterns.WEB_URL.matcher(url).matches();
    }

    public static boolean isValidDomain(String domain) {
        return Patterns.WEB_URL.matcher(domain).matches();
    }

    public boolean isValidCardNumber(String cardNumber) {
        try {
            return validateCardNumber(cardNumber);
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());

            return false;
        }
    }

    public static boolean validateCardNumber(String cardNumber) throws NumberFormatException {
        int sum = 0, digit, addend = 0;
        boolean doubled = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            digit = Integer.parseInt(cardNumber.substring(i, i + 1));
            if (doubled) {
                addend = digit * 2;
                if (addend > 9) {
                    addend -= 9;
                }
            } else {
                addend = digit;
            }
            sum += addend;
            doubled = !doubled;
        }
        return (sum % 10) == 0;
    }

    //////// LOCALE /////////////////////////////////////////////////////////////////////////

    public static Locale currentLocale(Activity activity) {
        Locale locale = activity.getResources().getConfiguration().locale;
        return locale;
    }

    public static String monthDayYear(DateTime dateTime, Locale locale) {
        SimpleDateFormat monthDayYearFormatter = new SimpleDateFormat(MONTH_DAY_YEAR_DISPLAY, locale);
        return monthDayYearFormatter.format(dateTime.toDate());
    }

    public static String fullDate(DateTime dateTime, Locale locale) {
        SimpleDateFormat monthDayYearFormatter = new SimpleDateFormat(MONTH_DAY_YEAR_DISPLAY, locale);
        return monthDayYearFormatter.format(dateTime.toDate());
    }

    public static String currency(BigDecimal amount, Locale locale) {
        if (amount == null) {
            amount = new BigDecimal(0);
        }

        DecimalFormat fmt = (DecimalFormat) NumberFormat.getInstance();
        String symbol = Currency.getInstance(locale).getSymbol(locale);
        fmt.setGroupingUsed(true);
        fmt.setPositivePrefix(symbol + " ");
        fmt.setNegativePrefix("-" + symbol + " ");
        fmt.setMinimumFractionDigits(2);
        fmt.setMaximumFractionDigits(2);

        return fmt.format(amount);
    }

    //////// CONSTANTS //////////////////////////////////////////////////////////////////////////

    public static final String ID = "id";
    public static final String PARENT_ID = "parent_id";

    // Numbers
    public static final String IS_PRIMARY = "name";

    public static final String COLOR = "color";
    public static final String ALPHA = "alpha";
    public static final String BLUE = "blue";
    public static final String GREEN = "green";
    public static final String RED = "red";

    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";

    public static final String PRICE = "price";
    public static final String AMOUNT = "amount";
    public static final String DISCOUNT = "discount";
    public static final String QUANTITY = "quantity";
    public static final String SUBTOTAL = "subtotal";
    public static final String TAX = "tax";
    public static final String TOTAL = "total";

    // Strings
    public static final String NAME = "name";
    public static final String MIDDLE_NAME = "middle_name";
    public static final String LAST_NAME = "last_name";
    public static final String SECOND_LAST_NAME = "second_last_name";

    public static final String TITLE = "title";
    public static final String TYPE = "type";
    public static final String COMMENT = "comment";
    public static final String CONTENT = "content";
    public static final String LABEL = "label";
    public static final String DESCRIPTION = "description";
    public static final String EXTRA = "extra";
    public static final String ZOOM = "zoom";

    // SQL
    public static final String AS = " AS ";
    public static final String ASC = " ASC ";
    public static final String COUNT = " COUNT ";
    public static final String CREATE = "CREATE TABLE ";
    public static final String DESC = " DESC ";
    public static final String DROP = "DROP TABLE IF EXISTS ";
    public static final String FROM = " FROM ";
    public static final String GROUP_BY = " GROUP BY ";
    public static final String INNER = " INNER ";
    public static final String INTEGER = " INTEGER";
    public static final String INTEGER_NOT_NULL = " INTEGER NOT NULL";
    public static final String JOIN = " JOIN ";
    public static final String LIMIT = " LIMIT ";
    public static final String SELECT = "SELECT ";
    public static final String SELECT_ALL_FROM = "SELECT * FROM ";
    public static final String SELECT_COUNT_ALL_FROM = "SELECT COUNT(*) FROM ";
    public static final String PRIMARY_KEY = " INTEGER PRIMARY KEY";
    public static final String ON = " ON ";
    public static final String ORDER_BY = " ORDER BY ";
    public static final String OUTER = " OUTER ";
    public static final String TEXT = " TEXT";
    public static final String TEXT_NOT_NULL = " TEXT NOT NULL";
    public static final String WHERE = " WHERE ";
    public static final String UNIQUE = " UNIQUE";

    // Patterns
    public static final String ALPHANUMERIC_PATTERN = "^[a-zA-Z0-9]*$";
    public static final String ALPHANUMERIC_EXTENDED_PATTERN = "^[a-zA-Z0-9_@*^%?#+]*$";
    public static final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    public static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{4,}$";

    // Dates
    public static final String END_DATE = "end_date";
    public static final String CREATED = "created";
    public static final String START_DATE = "start_date";
    public static final String UPDATED = "updated";

    // Date patterns
    public static final String MONTH_DAY_YEAR_DISPLAY = "MMM dd, yyyy";
    public static final String DATE_TIME_DISPLAY = "MMM dd, yyyy. HH:mm";
    public static final String DATE_DISPLAY_FORMAT = "EEE,  MMM d  yyyy ' at ' HH:mm";
    public static final String DATE_TO_DATABASE = "yyyy MM dd HH:mm:ss";
    public static final String DATE_CREATE_FORMAT = "yyyy.MM.dd 'at' HH:mm";
    public static final String DATE_SEMI_FULL_FORMAT = "yyyy.MM.dd 'at' HH:mm:ss z";
    public static final String DATE_FULL_TEXT = "yyyy.MM.dd 'dayYear: ' D ', weekYear: ' w ', hour: ' HH:mm:ss ', time zone: ' Z";
    public static final String DATE_LOCAL_TIME = "EEE, d MMM yyyy HH:mm:ss Z";

    public static final String YEAR_MONTH_DAY = "yyyy-MM-dd";
    public static final String MONTH_DAY_MONTH_SHORT = "MMM dd";
    public static final String MONTH_DAY_MONTH_LONG = "MMMM dd";
    public static final String DAY_WEEK_DAY_MONTH_SHORT = "EEE dd";
    public static final String DAY_WEEK_DAY_MONTH_LONG = "EEEE dd";
    public static final String DAY_MONTH_YEAR = "d MMMM, yyyy";

    public static final String HOUR_AM_PM_FORMAT = "h:mm a";
    public static final String HOUR_TWO_FOUR_FORMAT = "HH:mm";

    // CRUD
    public static final String CLOSING = "Closing ";
    public static final String DELETING = "Deleting ";
    public static final String OPENING = "Opening ";

    //////// COPY PASTE ///////////////////////////////////////////////////////////////////////////

//    public static void create(SQLiteDatabase database) {
//        database.execSQL(CREATE + TABLE + " (" +
//                ID + " " + PRIMARY_KEY + ", " +
//                NAME + " " + TEXT_NOT_NULL + ", " +
//                CREATED + " " + INTEGER_NOT_NULL + ", " +
//                UPDATED + " " + INTEGER_NOT_NULL + ")");
//    }
//
//    public static void drop(SQLiteDatabase database) {
//        database.execSQL(DROP + TABLE);
//    }
//
//        ContentValues values = new ContentValues();
//        values.put(NAME, .getName());
//        values.put(CREATED, .getCreated().getMillis());
//        values.put(UPDATED, .getCreated().getMillis());
//        return values;
//
//        cursor.getInt(cursor.getColumnIndex(ID)),
//        cursor.getString(cursor.getColumnIndex(NAME)),
//        TableLab.date(cursor.getColumnIndex(CREATED)),
//        TableLab.date(cursor.getColumnIndex(UPDATED))

}
