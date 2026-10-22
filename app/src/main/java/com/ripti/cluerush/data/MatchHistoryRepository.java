package com.ripti.cluerush.data;

import static com.ripti.cluerush.data.db.MatchHistoryContract.*;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ripti.cluerush.model.MatchHistoryItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MatchHistoryRepository {

    public interface Callback<T> {
        void onResult(T value);
    }

    private static final String TAG = "MatchHistoryRepo";
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final ContentResolver resolver;

    public MatchHistoryRepository(Context context) {
        this.resolver = context.getApplicationContext().getContentResolver();
    }

    public void insert(MatchHistoryItem item) {
        IO.execute(() -> {
            try {
                ContentValues values = new ContentValues();
                values.put(COL_UID, item.getUid());
                values.put(COL_ARENA_ID, item.getArenaId());
                values.put(COL_ARENA_NAME, item.getArenaName());
                values.put(COL_DIFFICULTY, item.getDifficulty());
                values.put(COL_MY_SCORE, item.getMyScore());
                if (item.hasOpponentScore()) {
                    values.put(COL_OPP_SCORE, item.getOpponentScore());
                } else {
                    values.putNull(COL_OPP_SCORE);
                }
                values.put(COL_OPPONENT, item.getOpponent());
                values.put(COL_OUTCOME, item.getOutcome());
                values.put(COL_DECIDER, item.getDecider());
                values.put(COL_PLAYED_AT, item.getPlayedAt());
                resolver.insert(CONTENT_URI, values);
            } catch (RuntimeException e) {
                Log.e(TAG, "insert failed", e);
            }
        });
    }

    public void loadAll(String uid, Callback<List<MatchHistoryItem>> callback) {
        IO.execute(() -> {
            List<MatchHistoryItem> list = new ArrayList<>();
            try (Cursor c = resolver.query(CONTENT_URI, null, COL_UID + "=?",
                    new String[]{uid}, COL_PLAYED_AT + " DESC, " + COL_ID + " DESC")) {
                if (c != null) {
                    while (c.moveToNext()) {
                        list.add(fromCursor(c));
                    }
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "query failed", e);
            }
            MAIN.post(() -> callback.onResult(list));
        });
    }

    public void delete(long id) {
        IO.execute(() -> {
            try {
                resolver.delete(ContentUris.withAppendedId(CONTENT_URI, id), null, null);
            } catch (RuntimeException e) {
                Log.e(TAG, "delete failed", e);
            }
        });
    }

    public void clear(String uid) {
        IO.execute(() -> {
            try {
                resolver.delete(CONTENT_URI, COL_UID + "=?", new String[]{uid});
            } catch (RuntimeException e) {
                Log.e(TAG, "clear failed", e);
            }
        });
    }

    private static MatchHistoryItem fromCursor(Cursor c) {
        int oppIndex = c.getColumnIndexOrThrow(COL_OPP_SCORE);
        return new MatchHistoryItem(
                c.getLong(c.getColumnIndexOrThrow(COL_ID)),
                c.getString(c.getColumnIndexOrThrow(COL_UID)),
                c.getString(c.getColumnIndexOrThrow(COL_ARENA_ID)),
                c.getString(c.getColumnIndexOrThrow(COL_ARENA_NAME)),
                c.getString(c.getColumnIndexOrThrow(COL_DIFFICULTY)),
                c.getInt(c.getColumnIndexOrThrow(COL_MY_SCORE)),
                c.isNull(oppIndex) ? null : c.getInt(oppIndex),
                c.getString(c.getColumnIndexOrThrow(COL_OPPONENT)),
                c.getString(c.getColumnIndexOrThrow(COL_OUTCOME)),
                c.getString(c.getColumnIndexOrThrow(COL_DECIDER)),
                c.getLong(c.getColumnIndexOrThrow(COL_PLAYED_AT)));
    }
}