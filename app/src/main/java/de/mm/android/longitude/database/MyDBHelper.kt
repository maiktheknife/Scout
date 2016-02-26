package de.mm.android.longitude.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

internal class MyDBHelper(context: Context) : SQLiteOpenHelper(context, MyDBHelper.DATABASE_NAME, null, MyDBHelper.DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        Log.d(TAG, "onCreate")
        db.execSQL(T_PROCESS)
        db.execSQL(T_FRIENDS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d(TAG, "onUpgrade from: $oldVersion to $newVersion")
        if (oldVersion == 1 && newVersion == 2) {
            db.execSQL("alter table t_friends add is_confirmed integer;")
        }
        if (oldVersion == 2 && newVersion == 3) {
            db.execSQL("alter table t_process add alt real;")
        }
    }

    companion object {
        private val TAG = MyDBHelper::class.java.simpleName
        private val DATABASE_NAME = "scout.db"
        private val DATABASE_VERSION = 3

        private val T_PROCESS =
                "create table t_process ( " +
                        "_id integer primary key autoincrement," +
                        "lat real," +
                        "lon real," +
                        "alt real," +
                        "acc real," +
                        "address text," +
                        "updated_on date default (datetime('now','localtime')));"

        private val T_FRIENDS =
                "create table t_friends ( " +
                        "_id integer primary key autoincrement," +
                        "person_id integer not null," +
                        "email text not null," +
                        "name text not null," +
                        "plus_id text not null," +
                        "is_confirmed integer not null," +
                        "latitude real," +
                        "longitude real," +
                        "altitude real," +
                        "accuracy real," +
                        "address text," +
                        "updated_on datetime);"
    }

}