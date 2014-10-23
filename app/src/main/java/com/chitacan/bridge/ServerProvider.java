package com.chitacan.bridge;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.HashMap;

/**
 * Created by chitacan on 2014. 9. 25..
 */
public class ServerProvider extends ContentProvider{

    static final String PROVIDER_NAME = "com.chitacan.bridge";
    static final String URL           = "content://" + PROVIDER_NAME + "/server";
    static final Uri    CONTENT_URI   = Uri.parse(URL);

    static final String _ID         = "_id";
    static final String SERVER_NAME = "name";
    static final String SERVER_Host = "host";
    static final String SERVER_PORT = "port";

    private static HashMap<String, String> SERVER_PROJECTION_MAP;

    private SQLiteDatabase mDB;

    static final int SERVERS   = 1;
    static final int SERVER_ID = 2;

    static final UriMatcher uriMatcher;
    static{
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "server",   SERVERS  );
        uriMatcher.addURI(PROVIDER_NAME, "server/#", SERVER_ID);
    }

    static final String DATABASE_NAME     = "db";
    static final String SERVER_TABLE_NAME = "servers";
    static final int DATABASE_VERSION     = 1;
    static final String CREATE_DB_TABLE =
            " CREATE TABLE " + SERVER_TABLE_NAME +
            " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            " name TEXT NOT NULL, " +
            " host TEXT NOT NULL, " +
            " port INTEGER);";

    private static class DbHelper extends SQLiteOpenHelper {

        DbHelper(Context context){
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            db.execSQL(CREATE_DB_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion,
                              int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " +  SERVER_TABLE_NAME);
            onCreate(db);
        }
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        DbHelper dbHelper = new DbHelper(context);
        mDB = dbHelper.getWritableDatabase();
        return (mDB == null)? false:true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(SERVER_TABLE_NAME);

        switch (uriMatcher.match(uri)) {
            case SERVERS:
                qb.setProjectionMap(SERVER_PROJECTION_MAP);
                break;
            case SERVER_ID:
                qb.appendWhere( _ID + "=" + uri.getPathSegments().get(1));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (sortOrder == null || sortOrder == ""){
            sortOrder = SERVER_NAME;
        }
        Cursor c = qb.query(mDB, projection, selection, selectionArgs, null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);

        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)){
            case SERVERS:
                return "vnd.android.cursor.dir/vnd.chitacan.bridge.servers";
            case SERVER_ID:
                return "vnd.android.cursor.item/vnd.chitacan.bridge.server";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long rowID = mDB.insert(SERVER_TABLE_NAME, "", values);
        if (rowID > 0) {
            Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(_uri, null);
            return _uri;
        }
        throw new SQLException("Failed to add a record into " + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;

        switch (uriMatcher.match(uri)){
            case SERVERS:
                count = mDB.delete(SERVER_TABLE_NAME, selection, selectionArgs);
                break;
            case SERVER_ID:
                String id = uri.getPathSegments().get(1);
                count = mDB.delete(SERVER_TABLE_NAME, _ID +  " = " + id +
                        (!TextUtils.isEmpty(selection) ? " AND (" +
                                selection + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;

        switch (uriMatcher.match(uri)){
            case SERVERS:
                count = mDB.update(SERVER_TABLE_NAME, values,
                        selection, selectionArgs);
                break;
            case SERVER_ID:
                count = mDB.update(SERVER_TABLE_NAME, values, _ID +
                        " = " + uri.getPathSegments().get(1) +
                        (!TextUtils.isEmpty(selection) ? " AND (" +
                                selection + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri );
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
