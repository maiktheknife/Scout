package de.mm.android.longitude.common

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class CancelReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "CancelReceiver")
        getNotificationManager(context).cancel(Constants.NOTIFICATION_ID_GCM)
    }

    companion object {
        private val TAG = CancelReceiver::class.java.simpleName

        private fun getNotificationManager(context: Context): NotificationManager {
            return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
    }

}