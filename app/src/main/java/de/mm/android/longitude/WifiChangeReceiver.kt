package de.mm.android.longitude

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.support.v4.content.WakefulBroadcastReceiver
import android.util.Log

import de.mm.android.longitude.location.UpdateLocationService

class WifiChangeReceiver : WakefulBroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive")
        val wifiInfo = getConnectivityManager(context).getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        if (wifiInfo != null && wifiInfo.isConnected) {
            Log.d(TAG, "onReceive isConnected " + firstConnect)
            if (firstConnect) {
                firstConnect = false
                // Upload Process Entries
                // startWakefulService(context, new Intent(IWebService.Actions.UPLOAD_ENTRIES.getAction()));
                // Update Location
                startWakefulService(context, Intent(context, UpdateLocationService::class.java))
            }
        }
    }

    companion object {
        val TAG = WifiChangeReceiver::class.java.simpleName

        private var firstConnect = true // --> http://stackoverflow.com/questions/15958284/broadcast-receiver-calles-twice

        private fun getConnectivityManager(context: Context): ConnectivityManager {
            return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        }
    }

}