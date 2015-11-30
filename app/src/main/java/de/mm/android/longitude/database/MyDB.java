package de.mm.android.longitude.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import de.mm.android.longitude.common.Constants;
import de.mm.android.longitude.model.ContactData;
import de.mm.android.longitude.model.ProcessEntry;

class MyDB {
	public static final String TAG = MyDB.class.getSimpleName();
	
	private SQLiteDatabase database;
	private MyDBHelper dbHelper;

	MyDB(Context context) {
		dbHelper = new MyDBHelper(context);
	}

	void open() {
		database = dbHelper.getWritableDatabase();
	}

	void close() {
		dbHelper.close();
	}

	/* Insert + Update */

	void insertProcessEntry(ProcessEntry p) {
        Log.d(TAG, "insertProcessEntry: " + p);

        ContentValues values = new ContentValues();
			values.put(ProcessEntry.COLUMN_LAT, p.getLat());
			values.put(ProcessEntry.COLUMN_LON, p.getLon());
			values.put(ProcessEntry.COLUMN_ALT, p.getAlt());
			values.put(ProcessEntry.COLUMN_ACC, p.getAccuracy());
            values.put(ProcessEntry.COLUMN_ADDRESS, p.getAddress());
			// id und updated_on Felder werden automatisch gesetzt
		database.insert(ProcessEntry.TABLE, null, values);
	}

	void mergeFriends(List<ContactData> data) {
        Log.d(TAG, "mergeFriends: " + data.size());

        database.delete(ContactData.TABLE, null, null);
		for (ContactData contactData : data) {
			ContentValues values = new ContentValues();
            values.put(ContactData.COLUMN_ID, contactData.getPerson_id());
            values.put(ContactData.COLUMN_EMAIL, contactData.getEmail());
            values.put(ContactData.COLUMN_NAME, contactData.getName());
            values.put(ContactData.COLUMN_PLUS_ID, contactData.getPlusID());
			values.put(ContactData.COLUMN_IS_CONFIRMED, contactData.isConfirmed() ? 1 : 0);
            values.put(ContactData.COLUMN_LATITUDE, contactData.getLatitude());
            values.put(ContactData.COLUMN_LONITUDE, contactData.getLongitude());
            values.put(ContactData.COLUMN_ALTITUDE, contactData.getAltitude());
            values.put(ContactData.COLUMN_ACCURACY, contactData.getAccuracy());
            values.put(ContactData.COLUMN_ADDRESS, contactData.getAddress());
            values.put(ContactData.COLUMN_UPDATED_ON, contactData.getUpdatedOn());
			database.insert(ContactData.TABLE, null, values);
		}
	}

	/* Select's */

	ArrayList<ContactData> selectFriends() {
        Log.d(TAG, "selectFriends");
        ArrayList<ContactData> list = new ArrayList<>();

		Cursor c = database.query(ContactData.TABLE, new String[] {
                ContactData.COLUMN_ID,
                ContactData.COLUMN_EMAIL,
                ContactData.COLUMN_NAME,
                ContactData.COLUMN_PLUS_ID,
				ContactData.COLUMN_IS_CONFIRMED,
                ContactData.COLUMN_LATITUDE,
                ContactData.COLUMN_LONITUDE,
                ContactData.COLUMN_ALTITUDE,
                ContactData.COLUMN_ACCURACY,
				ContactData.COLUMN_ADDRESS,
                ContactData.COLUMN_UPDATED_ON} , null, null, null, null, null);

		if (!c.moveToFirst()) {
			return list;
		}

		for (int i = 0; i < c.getCount(); i++) {
			ContactData d = new ContactData(
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
					c.getString(10));
			list.add(d);
			c.moveToNext();
		}
		c.close();
        return list;
	}

    private static void clearCalender(Calendar c){
        if (c != null) {
            c.clear(Calendar.HOUR_OF_DAY);
            c.clear(Calendar.HOUR);
            c.clear(Calendar.MINUTE);
            c.clear(Calendar.SECOND);
        }
    }

	List<ProcessEntry> selectProcess(Calendar min, Calendar max) {
        Log.d(TAG, "selectProcess: " + (min == null ? "<min Null>" : min.getTime().toString()) + ", " + (max == null ? "<max Null>" : max.getTime().toString()));
        clearCalender(min);
        clearCalender(max);

        String where;
        if (min == null && max == null) {
            where = "";
        } else if (min != null && max == null) {
            where = ProcessEntry.COLUMN_UPDATED_ON + " > '" + Constants.DATEFORMAT_SERVER.format(min.getTime()) + "'";
        }else if (min != null) {
            where = ProcessEntry.COLUMN_UPDATED_ON + " > '" + Constants.DATEFORMAT_SERVER.format(min.getTime()) + "' and " + ProcessEntry.COLUMN_UPDATED_ON + " < '" + Constants.DATEFORMAT_SERVER.format(max.getTime()) + "'";
        } else {
            throw new IllegalArgumentException("min == null && max != null isn't supported");
        }

        Log.d(TAG, "selectProcess " + where);
		Cursor c = database.query(ProcessEntry.TABLE, new String[] {ProcessEntry.COLUMN_ID, ProcessEntry.COLUMN_LAT, ProcessEntry.COLUMN_LON,
                ProcessEntry.COLUMN_ALT, ProcessEntry.COLUMN_ACC, ProcessEntry.COLUMN_UPDATED_ON, ProcessEntry.COLUMN_ADDRESS},
                where, null, null, null, null);
        List<ProcessEntry> resultList = new ArrayList<>();

        if (!c.moveToFirst()) {
			return resultList;
		}

		for (int i = 0; i < c.getCount(); i++) {
			ProcessEntry p = new ProcessEntry(c.getLong(0), c.getDouble(1), c.getDouble(2), c.getDouble(3), c.getDouble(4), c.getString(5), c.getString(5));
			resultList.add(p);
			c.moveToNext();
		}
		c.close();
		return resultList;
	}

	/* Delete's */
	
	void deleteProcessEntries(Calendar c) {
        Log.d(TAG, "deleteProcessEntries: " + c.getTime().toString());
        database.delete(ProcessEntry.TABLE, ProcessEntry.COLUMN_UPDATED_ON + " <= " + Constants.DATEFORMAT_SERVER.format(c.getTime()), null);
	}

}