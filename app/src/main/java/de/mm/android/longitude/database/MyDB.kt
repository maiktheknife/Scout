package de.mm.android.longitude.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log

import java.util.ArrayList
import java.util.Calendar

import de.mm.android.longitude.common.Constants
import de.mm.android.longitude.model.ContactData
import de.mm.android.longitude.model.ProcessEntry

internal class MyDB(context: Context) {

    private lateinit var database: SQLiteDatabase
    private val dbHelper: MyDBHelper

    init {
        dbHelper = MyDBHelper(context)
    }

    fun open() {
        database = dbHelper.writableDatabase
    }

    fun close() {
        dbHelper.close()
    }

    /* Insert + Update */

    fun insertProcessEntry(p: ProcessEntry) {
        Log.d(TAG, "insertProcessEntry: " + p)

        val values = ContentValues(5)
        values.put(ProcessEntry.COLUMN_LAT, p.lat)
        values.put(ProcessEntry.COLUMN_LON, p.lon)
        values.put(ProcessEntry.COLUMN_ALT, p.alt)
        values.put(ProcessEntry.COLUMN_ACC, p.accuracy)
        values.put(ProcessEntry.COLUMN_ADDRESS, p.address)
        // id und updated_on Felder werden automatisch gesetzt
        database.insert(ProcessEntry.TABLE, null, values)
    }

    fun mergeFriends(data: List<ContactData>) {
        Log.d(TAG, "mergeFriends: " + data.size)

        database.delete(ContactData.TABLE, null, null)
        for (contactData in data) {
            Log.d(TAG, "mergeFriend: " + contactData)
            val values = ContentValues(11)
            values.put(ContactData.COLUMN_ID, contactData.person_id)
            values.put(ContactData.COLUMN_EMAIL, contactData.email)
            values.put(ContactData.COLUMN_NAME, contactData.name)
            values.put(ContactData.COLUMN_PLUS_ID, contactData.plus_id)
            values.put(ContactData.COLUMN_IS_CONFIRMED, if (contactData.is_confirmed) 1 else 0)
            values.put(ContactData.COLUMN_LATITUDE, contactData.latitude)
            values.put(ContactData.COLUMN_LONITUDE, contactData.longitude)
            values.put(ContactData.COLUMN_ALTITUDE, contactData.altitude)
            values.put(ContactData.COLUMN_ACCURACY, contactData.accuracy)
            values.put(ContactData.COLUMN_ADDRESS, contactData.address)
            values.put(ContactData.COLUMN_UPDATED_ON, contactData.updated_on)
            database.insert(ContactData.TABLE, null, values)
        }
    }

    /* Select's */

    fun selectFriends(): ArrayList<ContactData> {
        Log.d(TAG, "selectFriends")
        val list = ArrayList<ContactData>()

        val c = database.query(ContactData.TABLE,
                arrayOf(ContactData.COLUMN_ID,
                        ContactData.COLUMN_EMAIL,
                        ContactData.COLUMN_NAME,
                        ContactData.COLUMN_PLUS_ID,
                        ContactData.COLUMN_IS_CONFIRMED,
                        ContactData.COLUMN_LATITUDE,
                        ContactData.COLUMN_LONITUDE,
                        ContactData.COLUMN_ALTITUDE,
                        ContactData.COLUMN_ACCURACY,
                        ContactData.COLUMN_ADDRESS,
                        ContactData.COLUMN_UPDATED_ON), null, null, null, null, null)

        if (!c.moveToFirst()) {
            return list
        }

        for (i in 0..c.count - 1) {
            val d = ContactData(
                    c.getInt(0),
                    c.getString(1),
                    c.getString(2),
                    c.getString(3),
                    c.getInt(4) == 1,
                    c.getDouble(5),
                    c.getDouble(6),
                    c.getDouble(7),
                    c.getDouble(8),
                    c.getString(9),
                    c.getString(10))
            list.add(d)
            c.moveToNext()
        }
        c.close()
        return list
    }

    fun selectProcess(min: Calendar?, max: Calendar?): List<ProcessEntry> {
        Log.d(TAG, "selectProcess: " + (if (min == null) "<min Null>" else min.time.toString()) + ", " + if (max == null) "<max Null>" else max.time.toString())
        clearCalender(min)
        clearCalender(max)

        val where: String
        if (min == null && max == null) {
            where = ""
        } else if (min != null && max == null) {
            where = ProcessEntry.COLUMN_UPDATED_ON + " > '" + Constants.DATEFORMAT_SERVER.format(min.time) + "'"
        } else if (min != null) {
            where = ProcessEntry.COLUMN_UPDATED_ON + " > '" + Constants.DATEFORMAT_SERVER.format(min.time) + "' and " + ProcessEntry.COLUMN_UPDATED_ON + " < '" + Constants.DATEFORMAT_SERVER.format(max!!.time) + "'"
        } else {
            throw IllegalArgumentException("min == null && max != null isn't supported")
        }

        Log.d(TAG, "selectProcess " + where)
        val c = database.query(ProcessEntry.TABLE, arrayOf(ProcessEntry.COLUMN_ID, ProcessEntry.COLUMN_LAT, ProcessEntry.COLUMN_LON, ProcessEntry.COLUMN_ALT, ProcessEntry.COLUMN_ACC, ProcessEntry.COLUMN_UPDATED_ON, ProcessEntry.COLUMN_ADDRESS),
                where, null, null, null, null)
        val resultList = ArrayList<ProcessEntry>()

        if (!c.moveToFirst()) {
            return resultList
        }

        for (i in 0..c.count - 1) {
            val p = ProcessEntry(c.getLong(0), c.getDouble(1), c.getDouble(2), c.getDouble(3), c.getDouble(4), c.getString(5), c.getString(5))
            resultList.add(p)
            c.moveToNext()
        }
        c.close()
        return resultList
    }

    /* Delete's */

    fun deleteProcessEntries(c: Calendar) {
        Log.d(TAG, "deleteProcessEntries: " + c.time.toString())
        database.delete(ProcessEntry.TABLE, ProcessEntry.COLUMN_UPDATED_ON + " <= " + Constants.DATEFORMAT_SERVER.format(c.time), null)
    }

    companion object {
        val TAG = MyDB::class.java.simpleName

        private fun clearCalender(c: Calendar?) {
            if (c != null) {
                c.clear(Calendar.HOUR_OF_DAY)
                c.clear(Calendar.HOUR)
                c.clear(Calendar.MINUTE)
                c.clear(Calendar.SECOND)
            }
        }
    }

}