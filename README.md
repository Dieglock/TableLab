# TableLab

Solves a common scenario: new app in your hands, new models. Why to write crud again? 

It is better to copy the lab folder extend and enjoy...

# Usage

Lets say the bean in question is a trilingual dictionary entry like Word(id, nombre, name, nomine, description, created, updated);

1. Extend TableLab with your object: 

          WordTable extends TableLab<Word> {...}
          
2. Declare life cycle at DbHelper: 

          Under onCreate()
          Wordtable.create(db) 
          
          under onUpgrade()
          WordTable.drop(db)

3. Give your self a nice method at DbManager, like: 

          public WordTable openWordLab() {
               return new WordTable(mActivity, writable())
          }


4. You can open/close it like this:

          DbManager manager = new DbManager(activity);
          WordTable wordLab = manager.openWordLab();
          wordLad.save(myBean); // ETC*
          manager.closeLab(); // do NOT forget

* Save, find, update, list normally, list asynchronously, log tables and database schema, query for statistics, seed from .txt, etc...

# Entension example

    public class WordTable extends TableLab<Word> {
      public static final String TAG = WordTable.class.getSimpleName();

      public static final String TABLE = "words";
      public static final String LETTER_ID = "letter_id";
      public static final String IMAGE = "image";
      public static final String NOMBRE = "nombre";
      public static final String NOMINE = "nomine";

      public WordTable(Activity activity, SQLiteDatabase database) {
          super(activity, database);
      }

      public static void create(SQLiteDatabase database) { // easy to debug with statics
          database.execSQL(CREATE_TABLE + TABLE + " (" +
                  ID + PRIMARY_KEY + ", " +
                  LETTER_ID + INTEGER_NOT_NULL + ", " +
                  NOMBRE + TEXT_NOT_NULL + UNIQUE + ", " +
                  NAME + TEXT_NOT_NULL + UNIQUE + ", " +
                  NOMINE + TEXT_NOT_NULL + UNIQUE + ", " +
                  DEFINITION + TEXT_NOT_NULL + ", " +
                  IMAGE + TEXT_NOT_NULL + ", " +
                  CREATED + INTEGER_NOT_NULL + ", " +
                  UPDATED + INTEGER_NOT_NULL + ")");
      }

      public static void drop(SQLiteDatabase database) {
          database.execSQL(DROP_TABLE_IF_EXISTS + TABLE);
      }

      @Override
      public String table() { // table name
          return TABLE;
      }

      @Override
      public String[] where() { // default columns to search queries
          return new String[]{ NOMBRE, NAME, NOMINE, DEFINITION, IMAGE };
      }

      @Override
      public String when() { // default column to sort in time
          return UPDATED;
      }

      @Override
      public String orderBy() { // default column to sort alphabetically
          return NOMBRE;
      }

          @Override
      public String groupBy() { // default value to groupBy
          return null; // not in use now example
      }

      @Override
      public String having() { // default value for having
          return null;
      }

      @Override
      public ContentValues values(Word word) {
          ContentValues values = new ContentValues();
          values.put(LETTER_ID, word.getLetterId());
          values.put(NOMBRE, word.getNombre());
          values.put(NAME, word.getName());
          values.put(NOMINE, word.getNomine());
          values.put(DEFINITION, word.getDefinition());
          values.put(IMAGE, word.getImage());
          values.put(CREATED, word.getCreated().getMillis());
          values.put(UPDATED, word.getCreated().getMillis());
          return values;
      }

      @Override
      public Word model(Cursor cursor) {
          return new Word(
                  cursor.getInt(cursor.getColumnIndex(ID)),
                  cursor.getInt(cursor.getColumnIndex(LETTER_ID)),
                  cursor.getString(cursor.getColumnIndex(NOMBRE)),
                  cursor.getString(cursor.getColumnIndex(NAME)),
                  cursor.getString(cursor.getColumnIndex(NOMINE)),
                  cursor.getString(cursor.getColumnIndex(DEFINITION)),
                  cursor.getString(cursor.getColumnIndex(IMAGE)),
                  TableLab.date(cursor.getColumnIndex(CREATED)),
                  TableLab.date(cursor.getColumnIndex(UPDATED)));
      }

      //////// SEED ///////////////////////////////////////////////////////////////////////////////

      public void seedFromTxt(Activity activity) { // example seed from assets folder .txt file.
          BufferedReader reader = null;
          try {
              reader = TableLab.txtReader(activity, "words.txt");

              String line;

              while ((line = reader.readLine()) != null) {
                  String[] lineArray = line.split("\\|", -1);
                  Word word = new Word();
                  word.setLetterId(Integer.valueOf(lineArray[0]));
                  word.setNombre(lineArray[1]);
                  word.setName(lineArray[2]);
                  word.setNomine(lineArray[3]);
                  word.setImage(lineArray[4]);
                  word.setImage(image);
                  word.setDefinition(lineArray[5]);
                  word.setCreated(TableLab.now());
                  word.setUpdated(TableLab.now());
                  save(word);
              }

          } catch (IOException e) {
              Log.e(TAG, e.getMessage(), e);

          } finally {
              if (reader != null) {
                  try {
                      reader.close();
                  } catch (IOException e) {
                      Log.e(TAG, e.getMessage(), e);
                  }
              }
          }
      }
    }



