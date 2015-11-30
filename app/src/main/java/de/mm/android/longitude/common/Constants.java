package de.mm.android.longitude.common;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import de.mm.android.longitude.R;

public interface Constants {
    /** insert into location values (21,1,12,34,56,78,'address', '2015-08-27 04:05:06 -8:00'); */
	DateFormat DATEFORMAT_SERVER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
	DateFormat DATEFORMAT_LOCAL = SimpleDateFormat.getDateTimeInstance();

	String PREFS_NAME_SETTINGS = "scout_settings";
    String PREFS_NAME_STUFF = "scout_app";
    String PREFS_NAME_GOOGLE = "scout_google";

	int REQUEST_PICK_CONTACT = 9123;
	int REQUEST_PLAY_SERVICES = 9124;
    int REQUEST_PICK_GOOGLE_ACCOUNT = 9125;
	
	int NOTIFICATION_ID_GCM = R.layout.f_contacts;
	int NOTIFICATION_ID_SYNC = R.layout.f_gmap;

}