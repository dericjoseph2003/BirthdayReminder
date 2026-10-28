
package com.example.birthdayreminder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "Birthdays.db";
    private static final int DB_VERSION = 1;
    public static final String TABLE = "birthdays";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "birth_date TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db,
                          int oldVersion, int newVersion) {
        // Add database migration logic if the schema changes.
    }

    public long addBirthday(String name, String date) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("birth_date", date);

        return getWritableDatabase().insert(TABLE, null, values);
    }

    public Cursor getAllBirthdays() {
        return getReadableDatabase().rawQuery(
                "SELECT id, name, birth_date FROM " + TABLE,
                null
        );
    }

    public int updateBirthday(int id, String name, String date) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("birth_date", date);

        return getWritableDatabase().update(
                TABLE, values, "id = ?",
                new String[]{String.valueOf(id)}
        );
    }

    public int deleteBirthday(int id) {
        return getWritableDatabase().delete(
                TABLE, "id = ?",
                new String[]{String.valueOf(id)}
        );
    }
}
