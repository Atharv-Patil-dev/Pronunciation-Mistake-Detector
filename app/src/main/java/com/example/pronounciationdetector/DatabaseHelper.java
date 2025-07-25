package com.example.pronounciationdetector;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "pronunciation.db";
    private static final int DATABASE_VERSION = 2; // Incremented database version

    public static final String TABLE_NAME = "history";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_SPOKEN_TEXT = "spoken_text";
    public static final String COLUMN_CORRECTION = "correction";
    public static final String COLUMN_IPA = "ipa";
    public static final String COLUMN_CONFIDENCE = "confidence";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_SPOKEN_TEXT + " TEXT, "
                + COLUMN_CORRECTION + " TEXT, "
                + COLUMN_IPA + " TEXT, "
                + COLUMN_CONFIDENCE + " TEXT)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Insert Data
    public void saveProgress(String spokenText, String correction, String ipa, String confidence) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SPOKEN_TEXT, spokenText);
        values.put(COLUMN_CORRECTION, correction);
        values.put(COLUMN_IPA, ipa);
        values.put(COLUMN_CONFIDENCE, confidence);
        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    // Get History Data
    public Cursor getHistory() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT _id, spoken_text, correction, ipa, confidence FROM " + TABLE_NAME + " ORDER BY _id DESC", null);
    }
}
