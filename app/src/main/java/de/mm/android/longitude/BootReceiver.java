package de.mm.android.longitude;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.mm.android.longitude.location.Tracking;
import de.mm.android.longitude.util.PreferenceUtil;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = BootReceiver.class.getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        if (PreferenceUtil.isUsingApp(context) && PreferenceUtil.isTrackingEnabled(context)) { // restart background tracking
            new Tracking().start(context);
        }
    }
}