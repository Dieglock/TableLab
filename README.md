# TableLab
Database handler for Android TableLab&lt;T>

Solves a common scenario: new app in your hands, new models. Why to write crud again? 

It is better to copy the lab folder extend and enjoy...

# Usage

Lets say the bean in question is Word(id, name, usage_example, created, updated);

1. Extend TableLab with your object: "WordTable extends TableLab<Word>".
2. Declare life cycle ad DbHelper: "Wordtable.create(db)" and "WordTable.drop(db)"
3. Give your self a nice method at DbManager, like: 

          public WordTable openWordLab() {
          return new WordTable(mActivity, writable())
     }
     
4. You can instantiate like this:

DbManager manager = new DbManager(activity);
WordTable wordLab = manger.openWordLab();

wordLab can SAVE, UPDATE, DELETE, LIST, LIST LIKE, LIST EXACT, PRINT DATABASE SCHEMA... and more.

manager.closeLab(wordLab); // do nor forget


# Entension example

public class WordTable extends TableLab<Word> {

    public static final String TABLE = "words";
    public static final String USAGE_EXAMPLE = "usage_example";

    public WordTable(Activity activity, SQLiteDatabase database) {
        super(activity, database);
    }

    public static void create(SQLiteDatabase database) {
        database.execSQL(CREATE + TABLE + " (" +
                ID + " " + PRIMARY_KEY + ", " +
                NAME + " " + TEXT_NOT_NULL + ", " +
                USAGE_EXAMPLE + " " + TEXT_NOT_NULL + ", " +
                CREATED + " " + INTEGER_NOT_NULL + ", " +
                UPDATED + " " + INTEGER_NOT_NULL + ")");
    }

    public static void drop(SQLiteDatabase database) {
        database.execSQL(DROP + TABLE);
    }

    @Override
    public String table() {
        return TABLE; // table name
    }

    @Override
    public String where() {
        return NAME; // default column to order alphabetically
    }
    
    @Override
    public String when() {
        return UPDATED; // default column to oder by time
    }

    @Override
    public ContentValues values(Word word) { // default method to save and upate
        ContentValues values = new ContentValues();
        values.put(NAME, word.getName());
        values.put(USAGE_EXAMPLE, word.getExtra());
        values.put(CREATED, word.getCreated().getMillis());
        values.put(UPDATED, word.getCreated().getMillis());
        return values;
    }

    @Override
    public Word model(Cursor cursor) { // default method to recreate with cursor
        return new Word(
        cursor.getInt(cursor.getColumnIndex(ID)),
        cursor.getString(cursor.getColumnIndex(NAME)),
        cursor.getString(cursor.getColumnIndex(USAGE_EXAMPLE)),
        TableLab.date(cursor.getColumnIndex(CREATED)),
        TableLab.date(cursor.getColumnIndex(UPDATED))
        );
    }
}



