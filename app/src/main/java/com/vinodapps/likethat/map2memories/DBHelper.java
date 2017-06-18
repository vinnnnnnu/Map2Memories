package com.vinodapps.likethat.map2memories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "Memories.db";
    private static final String TABLE_NAME = "memories_table";

    private static final String ID = "id";
    public static final String LOCATION_LAT_LAN = "latlan";
    public static final String IMAGE_PATH = "imgpath";
    private static int VERSION = 1;
    private SQLiteDatabase mDB;
    public String CREATE_QUERY = "CREATE TABLE " + TABLE_NAME + "(" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + LOCATION_LAT_LAN + " TEXT," + IMAGE_PATH + " TEXT );";


    public DBHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
        Log.d("db operations", "db created");


    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_QUERY);
        Log.d("db operations", "table created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        mDB.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(mDB);
    }

    public void insertData(ContentValues contentValues) {
        this.mDB = getWritableDatabase();
        long rowID = mDB.insert(TABLE_NAME, null, contentValues);
        Log.d("db operations", "one row inserted");
    }

    public Cursor getData() {
        this.mDB = getReadableDatabase();
        String[] colums = {LOCATION_LAT_LAN, IMAGE_PATH};
        Cursor CR = mDB.query(TABLE_NAME, colums, null, null, null, null, null);
        return CR;
    }

}
