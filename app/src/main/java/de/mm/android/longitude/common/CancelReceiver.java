package de.mm.android.longitude.common;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CancelReceiver extends BroadcastReceiver {
	private static final String TAG = CancelReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "CancelReceiver");
		getNotificationManager(context).cancel(Constants.NOTIFICATION_ID_GCM);
	}

	private static NotificationManager getNotificationManager(Context context) {
		return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	}

}