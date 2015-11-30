package de.mm.android.longitude.location;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.LocationRequest;

import java.util.Calendar;

import de.mm.android.longitude.util.PreferenceUtil;

/* http://www.javacodegeeks.com/2014/09/oop-alternative-to-utility-classes.html */
public class Tracking {
    private static final String TAG = Tracking.class.getSimpleName();
    private static final int REQUEST = 22991;

    public static final LocationRequest REQUEST_SINGLE =
            LocationRequest
                .create()
                .setInterval(1000)
                .setNumUpdates(1)
                .setPriority(LocationRequest.PRIORITY_LOW_POWER);

    public static final LocationRequest REQUEST_PERIOD =
            LocationRequest
                .create()
                .setInterval(20000)
                .setFastestInterval(20000)
                .setPriority(LocationRequest.PRIORITY_LOW_POWER);

    public void start(Context context) {
        Log.d(TAG, "start");
        if (PreferenceUtil.isTrackingEnabled(context)) {
            int interval = PreferenceUtil.getInterval(context);
            Log.d(TAG, "start at: " + interval);
            getAlarmManager(context).setInexactRepeating(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis(), interval * 60 * 1000, createPI(context));
        }
    }

    public void stop(Context context) {
        Log.d(TAG, "stop");
        getAlarmManager(context).cancel(createPI(context));
    }

    private PendingIntent createPI(Context context) {
        return PendingIntent.getService(context, REQUEST, new Intent(context, UpdateLocationService.class), 0);
    }

    private static AlarmManager getAlarmManager(Context context) {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

}