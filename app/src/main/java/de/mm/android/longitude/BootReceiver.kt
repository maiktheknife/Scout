package de.mm.android.longitude

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

import de.mm.android.longitude.location.Tracking
import de.mm.android.longitude.util.PreferenceUtil

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive")
        if (PreferenceUtil.isUsingApp(context) && PreferenceUtil.isTrackingEnabled(context)) {
            Tracking().start(context) // restart background tracking
        }
    }

    companion object {
        private val TAG = BootReceiver::class.java.simpleName
    }
}