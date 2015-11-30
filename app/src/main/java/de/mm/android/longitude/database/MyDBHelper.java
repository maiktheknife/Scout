package de.mm.android.longitude.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class MyDBHelper extends SQLiteOpenHelper {
	private static final String TAG = MyDBHelper.class.getSimpleName();
	private static final String DATABASE_NAME = "scout.db";
	private static final int DATABASE_VERSION = 3;

    private static final String T_PROCESS =
        "create table t_process ( "
            + "_id integer primary key autoincrement,"
            + "lat real,"
            + "lon real,"
            + "alt real,"
            + "acc real,"
            + "address text,"
            + "updated_on date default (datetime('now','localtime'))"
            + ");";

    private static final String T_FRIENDS =
        "create table t_friends ( "
            + "_id integer primary key autoincrement,"
            + "person_id integer not null,"
            + "email text not null,"
            + "name text not null,"
            + "plus_id text not null,"
            + "is_confirmed integer not null,"
            + "latitude real,"
            + "longitude real,"
            + "altitude real,"
            + "accuracy real,"
            + "address text,"
            + "updated_on datetime"
            + ");";

	public MyDBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate");
        db.execSQL(T_PROCESS);
		db.execSQL(T_FRIENDS);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(TAG, "onUpgrade from: " + oldVersion + " to " + newVersion);
        if (oldVersion == 1 && newVersion == 2) {
            db.execSQL("alter table t_friends add is_confirmed integer;");
        }
        if (oldVersion == 2 && newVersion == 3) {
            db.execSQL("alter table t_process add alt real;");
        }
    }
		
}