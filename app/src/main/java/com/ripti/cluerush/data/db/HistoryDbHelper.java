package com.ripti.cluerush.data.db;

import static com.ripti.cluerush.data.db.MatchHistoryContract.*;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HistoryDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "cluerush_history.db";
    private static final int DB_VERSION = 1;

    public HistoryDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_UID + " TEXT NOT NULL, "
                + COL_ARENA_ID + " TEXT NOT NULL, "
                + COL_ARENA_NAME + " TEXT NOT NULL, "
                + COL_DIFFICULTY + " TEXT NOT NULL, "
                + COL_MY_SCORE + " INTEGER NOT NULL, "
                + COL_OPP_SCORE + " INTEGER, "
                + COL_OPPONENT + " TEXT NOT NULL, "
                + COL_OUTCOME + " TEXT NOT NULL, "
                + COL_DECIDER + " TEXT NOT NULL DEFAULT '', "
                + COL_PLAYED_AT + " INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX idx_history_uid_time ON " + TABLE
                + " (" + COL_UID + ", " + COL_PLAYED_AT + " DESC)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // History is only a local cache, so a rebuild is acceptable.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }
}