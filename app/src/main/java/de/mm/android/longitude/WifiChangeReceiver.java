package de.mm.android.longitude;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import de.mm.android.longitude.location.UpdateLocationService;

public class WifiChangeReceiver extends WakefulBroadcastReceiver {
    public static final String TAG = WifiChangeReceiver.class.getSimpleName();

    private static boolean firstConnect = true; // --> http://stackoverflow.com/questions/15958284/broadcast-receiver-calles-twice

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        NetworkInfo wifiInfo = getConnectivityManager(context).getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiInfo != null && wifiInfo.isConnected()) {
            Log.d(TAG, "onReceive isConnected " + firstConnect);
            if (firstConnect) {
                firstConnect = false;
                // Upload Process Entries
//                startWakefulService(context, new Intent(IWebService.Actions.UPLOAD_ENTRIES.getAction()));
                // Update Location
                startWakefulService(context, new Intent(context, UpdateLocationService.class));
            }
        }
    }

    private static ConnectivityManager getConnectivityManager(Context context) {
        return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

}