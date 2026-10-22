package com.ripti.cluerush.data.db;

import android.net.Uri;
import android.provider.BaseColumns;

public final class MatchHistoryContract {

    public static final String AUTHORITY = "com.ripti.cluerush.provider";
    public static final String PATH = "matches";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH);

    public static final String MIME_DIR = "vnd.android.cursor.dir/vnd.com.ripti.cluerush.match";
    public static final String MIME_ITEM = "vnd.android.cursor.item/vnd.com.ripti.cluerush.match";

    public static final String TABLE = "match_history";
    public static final String COL_ID = BaseColumns._ID;
    public static final String COL_UID = "uid";
    public static final String COL_ARENA_ID = "arena_id";
    public static final String COL_ARENA_NAME = "arena_name";
    public static final String COL_DIFFICULTY = "difficulty";
    public static final String COL_MY_SCORE = "my_score";
    public static final String COL_OPP_SCORE = "opp_score";
    public static final String COL_OPPONENT = "opponent";
    public static final String COL_OUTCOME = "outcome";
    public static final String COL_DECIDER = "decider";
    public static final String COL_PLAYED_AT = "played_at";

    /** Per user, only the newest N matches are kept. */
    public static final int MAX_ROWS_PER_USER = 100;

    private MatchHistoryContract() {
    }
}