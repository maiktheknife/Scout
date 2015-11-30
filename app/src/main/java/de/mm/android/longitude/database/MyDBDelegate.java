package de.mm.android.longitude.database;

import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import de.mm.android.longitude.model.ContactData;
import de.mm.android.longitude.model.ProcessEntry;

/**
 * Created by Max on 07.04.2015.
 */
public class MyDBDelegate {

    private MyDBDelegate() {
        throw new AssertionError("No Instances");
    }

    /* Insert + Update */

    public static void insertProcessEntry(Context c, ProcessEntry p){
        MyDB db = new MyDB(c);
        db.open();
        db.insertProcessEntry(p);
        db.close();
    }

    public static void mergeFriends(Context c, List<ContactData> data){
        MyDB db = new MyDB(c);
        db.open();
        db.mergeFriends(data);
        db.close();
    }

    /* Select's */

    public static ArrayList<ContactData> selectFriends(Context c) {
        MyDB db = new MyDB(c);
        db.open();
        ArrayList<ContactData> data = db.selectFriends();
        db.close();
        return data;
    }

    public static List<ProcessEntry> selectProcess(Context c, Calendar min, Calendar max){
        MyDB db = new MyDB(c);
        db.open();
        List<ProcessEntry> data = db.selectProcess(min, max);
        db.close();
        return data;
    }

    /* Delete's */

    public static void deleteProcessEntries(Context c, Calendar calendar) {
        MyDB db = new MyDB(c);
        db.open();
        db.deleteProcessEntries(calendar);
        db.close();
    }

}
