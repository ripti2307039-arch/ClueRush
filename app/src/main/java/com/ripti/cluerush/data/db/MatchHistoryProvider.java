package com.ripti.cluerush.data.db;

import static com.ripti.cluerush.data.db.MatchHistoryContract.*;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MatchHistoryProvider extends ContentProvider {

    private static final int MATCHES = 1;
    private static final int MATCH_ID = 2;

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_MATCHER.addURI(AUTHORITY, PATH, MATCHES);
        URI_MATCHER.addURI(AUTHORITY, PATH + "/#", MATCH_ID);
    }

    private HistoryDbHelper helper;

    @Override
    public boolean onCreate() {
        helper = new HistoryDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
                        @Nullable String selection, @Nullable String[] selectionArgs,
                        @Nullable String sortOrder) {
        String where = selection;
        String[] args = selectionArgs;
        switch (URI_MATCHER.match(uri)) {
            case MATCHES:
                break;
            case MATCH_ID:
                where = DatabaseUtils.concatenateWhere(selection, COL_ID + "=?");
                args = DatabaseUtils.appendSelectionArgs(selectionArgs,
                        new String[]{uri.getLastPathSegment()});
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        String order = TextUtils.isEmpty(sortOrder)
                ? COL_PLAYED_AT + " DESC, " + COL_ID + " DESC" : sortOrder;
        Cursor cursor = helper.getReadableDatabase()
                .query(TABLE, projection, where, args, null, null, order);
        Context context = getContext();
        if (context != null) {
            cursor.setNotificationUri(context.getContentResolver(), CONTENT_URI);
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case MATCHES:
                return MIME_DIR;
            case MATCH_ID:
                return MIME_ITEM;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (URI_MATCHER.match(uri) != MATCHES || values == null) {
            throw new IllegalArgumentException("Cannot insert into " + uri);
        }
        String uid = values.getAsString(COL_UID);
        if (TextUtils.isEmpty(uid)) {
            throw new IllegalArgumentException("uid is required");
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        long id;
        db.beginTransaction();
        try {
            id = db.insertOrThrow(TABLE, null, values);
            // Keep only the newest MAX_ROWS_PER_USER rows for this user.
            db.execSQL("DELETE FROM " + TABLE + " WHERE " + COL_UID + "=? AND "
                            + COL_ID + " NOT IN (SELECT " + COL_ID + " FROM " + TABLE
                            + " WHERE " + COL_UID + "=? ORDER BY " + COL_PLAYED_AT + " DESC, "
                            + COL_ID + " DESC LIMIT " + MAX_ROWS_PER_USER + ")",
                    new Object[]{uid, uid});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        notifyChange();
        return ContentUris.withAppendedId(CONTENT_URI, id);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        String where = selection;
        String[] args = selectionArgs;
        switch (URI_MATCHER.match(uri)) {
            case MATCHES:
                break;
            case MATCH_ID:
                where = DatabaseUtils.concatenateWhere(selection, COL_ID + "=?");
                args = DatabaseUtils.appendSelectionArgs(selectionArgs,
                        new String[]{uri.getLastPathSegment()});
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        int count = helper.getWritableDatabase().delete(TABLE, where, args);
        if (count > 0) {
            notifyChange();
        }
        return count;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
                      @Nullable String selection, @Nullable String[] selectionArgs) {
        if (values == null) {
            return 0;
        }
        String where = selection;
        String[] args = selectionArgs;
        switch (URI_MATCHER.match(uri)) {
            case MATCHES:
                break;
            case MATCH_ID:
                where = DatabaseUtils.concatenateWhere(selection, COL_ID + "=?");
                args = DatabaseUtils.appendSelectionArgs(selectionArgs,
                        new String[]{uri.getLastPathSegment()});
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        int count = helper.getWritableDatabase().update(TABLE, values, where, args);
        if (count > 0) {
            notifyChange();
        }
        return count;
    }

    private void notifyChange() {
        Context context = getContext();
        if (context != null) {
            context.getContentResolver().notifyChange(CONTENT_URI, null);
        }
    }
}