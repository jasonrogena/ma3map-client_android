package ke.co.ma3map.android.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by jason on 21/09/14.
 */
public class Database extends SQLiteOpenHelper {
    private static final String TAG = "ma3map.Database";

    private static final String DB_NAME = "ma3map";
    private static final int DB_VERSION = 1;

    public static final String TABLE_ROUTE = "route";
    public static final String TABLE_LINE = "line";
    public static final String TABLE_STOP = "stop";
    public static final String TABLE_POINT = "point";
    public static final String TABLE_STOP_LINE = "stop_line";

    public Database(Context context){
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE "+TABLE_ROUTE+"(route_id text, route_short_name text, route_long_name text, route_desc text, route_type int, route_url text, route_color text, route_text_color text)");
        sqLiteDatabase.execSQL("CREATE TABLE "+TABLE_STOP+"(stop_id text, stop_name text, stop_code text, stop_desc text, stop_lat text, stop_lon text, location_type int, parent_station text)");
        sqLiteDatabase.execSQL("CREATE TABLE "+TABLE_LINE+"(line_id text, route_id text, direction_id int)");
        sqLiteDatabase.execSQL("CREATE TABLE "+TABLE_POINT+"(line_id text, point_lat text, point_lon text, point_sequence int, dist_traveled int)");
        sqLiteDatabase.execSQL("CREATE TABLE "+TABLE_STOP_LINE+"(line_id text, stop_id text)");

        Log.i(TAG, DB_NAME+" database with version"+DB_VERSION+" created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS "+TABLE_ROUTE);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS "+TABLE_STOP_LINE);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS "+TABLE_STOP);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS "+TABLE_LINE);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS "+TABLE_POINT);

        Log.w(TAG, "Deleted all tables in "+DB_NAME+" database in preparation for the upgrade from version "+oldVersion+" to "+newVersion);
        onCreate(sqLiteDatabase);
    }

    /**
     * This method is use dto run select queries to the database
     *
     * @param db    The readable database
     * @param table The name of the table where the select query is to be run
     * @param columns   An array of column names to be fetched in the query
     * @param selection The selection criteria in the form column=value
     * @param selectionArgs You may include ?s in selection, which will be replaced by the values from selectionArgs, in order that they appear in the selection. The values will be bound as Strings.
     * @param groupBy   A filter declaring how to gr
     *                  oup rows, formatted as an SQL GROUP BY clause (excluding the GROUP BY itself). Passing null will cause the rows to not be grouped.
     * @param having    A filter declare which row groups to include in the cursor, if row grouping is being used, formatted as an SQL HAVING clause (excluding the HAVING itself). Passing null will cause all row groups to be included, and is required when row grouping is not being used.
     * @param orderBy   How to order the rows, formatted as an SQL ORDER BY clause (excluding the ORDER BY itself). Passing null will use the default sort order, which may be unordered.
     * @param limit Limits the number of rows returned by the query, formatted as LIMIT clause. Passing null denotes no LIMIT clause.
     *
     * @return  A multidimensional array in the form array[selected_rows][selected_columns]
     */
    public String[][] runSelectQuery(SQLiteDatabase db, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {

        //Log.d(TAG, "About to run select query on " + table + " table");
        Cursor cursor=db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
        if(cursor.getCount()!=-1) {
            String[][] result=new String[cursor.getCount()][columns.length];
            //Log.d(TAG, "number of rows " + String.valueOf(cursor.getCount()));
            int c1=0;
            cursor.moveToFirst();
            while(c1<cursor.getCount()) {
                int c2=0;
                while(c2<columns.length) {
                    String currResult = cursor.getString(c2);
                    if(currResult == null || currResult.equals("null"))
                        currResult = "";//nulls from server not handled well by json. Set 'null' and null to empty string

                    result[c1][c2] = currResult;
                    c2++;
                }
                if(c1!=cursor.getCount()-1) {//is not the last row
                    cursor.moveToNext();
                }
                c1++;
            }
            cursor.close();

            return result;
        }
        else {
            return null;
        }
    }

    /**
     * This method deletes rows form a table
     *
     * @param db    The writable database
     * @param table The table from which rows are to be deleted
     * @param referenceColumn   Column to be used as a reference for the delete
     * @param columnValues   The values of the reference column. All rows with these values will be deleted
     */
    public void runDeleteQuery(SQLiteDatabase db, String table, String referenceColumn, String[] columnValues) {
        //Log.d(TAG, "About to run delete query on "+table+" table");

        db.delete(table, referenceColumn+"=?", columnValues);
    }

    /**
     * This method Runs an insert query (duh)
     *
     * @param table The table where you want to insert the data
     * @param columns   An array of the columns to be inserted
     * @param values    An array of the column values. Should correspond to the array of column names
     * @param uniqueColumnIndex Index of the unique key (primary key). Set this to -1 if none
     * @param db    The writable database
     */
    public void runInsertQuery(String table,String[] columns,String[] values, int uniqueColumnIndex,SQLiteDatabase db) {
        //Log.d(TAG, "About to run insert query on "+table+" table");
        if(columns.length==values.length) {
            ContentValues cv=new ContentValues();
            int count=0;
            while(count<columns.length) {
                cv.put(columns[count], values[count]);
                count++;
            }

            //delete row with same unique key
            if(uniqueColumnIndex != -1){
                //Log.w(TAG, "About to delete any row with "+columns[uniqueColumnIndex]+" = "+values[uniqueColumnIndex]);
                runDeleteQuery(db, table, columns[uniqueColumnIndex], new String[]{values[uniqueColumnIndex]});
            }

            db.insert(table, null, cv);

            cv.clear();
        }
    }

    /**
     * This method deletes all data in a table. Please be careful, this method will delete all the data in that table
     *
     * @param db    The writable database
     * @param table The table to truncate
     */
    public void runTruncateQuery(SQLiteDatabase db, String table){
        Log.w(TAG, "About to truncate table "+table);
        String query = "DELETE FROM "+table;
        runQuery(db, query);
    }

    /**
     * This method runs a generic query in the database.
     * If you want to run:
     *      select queries, please use runSelectQuery()
     *      insert queries, please use runInsertQuery()
     *      delete queries, please use runDeleteQuery()
     *
     * @param db    The readable/writable database to use depending on whether you need to write into the database
     * @param query The query that you want to run. Please use SQLite friendly queries
     */
    public void runQuery(SQLiteDatabase db, String query) {//non return queries
        Log.d(TAG, "about to run generic query on the database");
        db.execSQL(query);
    }
}
