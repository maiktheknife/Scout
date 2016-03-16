package de.mm.android.longitude.database

import android.content.Context

import java.util.ArrayList
import java.util.Calendar

import de.mm.android.longitude.model.ContactData
import de.mm.android.longitude.model.ProcessEntry

/**
 * Created by Max on 07.04.2015.
 */
class MyDBDelegate private constructor() {

    companion object {

        /* Insert + Update */

        fun insertProcessEntry(c: Context, p: ProcessEntry) {
            val db = MyDB(c)
            db.open()
            db.insertProcessEntry(p)
            db.close()
        }

        fun mergeFriends(c: Context, data: List<ContactData>) {
            val db = MyDB(c)
            db.open()
            db.mergeFriends(data)
            db.close()
        }

        /* Select's */

        fun selectFriends(c: Context): ArrayList<ContactData> {
            val db = MyDB(c)
            db.open()
            val data = db.selectFriends()
            db.close()
            return data
        }

        fun selectProcess(c: Context, min: Calendar?, max: Calendar?): List<ProcessEntry> {
            val db = MyDB(c)
            db.open()
            val data = db.selectProcess(min, max)
            db.close()
            return data
        }

        /* Delete's */

        fun deleteProcessEntries(c: Context, calendar: Calendar) {
            val db = MyDB(c)
            db.open()
            db.deleteProcessEntries(calendar)
            db.close()
        }
    }

}
