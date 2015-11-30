package de.mm.android.longitude.network;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import java.util.Set;

import de.mm.android.longitude.MainActivity;
import de.mm.android.longitude.R;
import de.mm.android.longitude.common.CancelReceiver;
import de.mm.android.longitude.common.Constants;
import de.mm.android.longitude.location.UpdateLocationService;
import de.mm.android.longitude.util.PreferenceUtil;

public class GCMListenerService extends GcmListenerService {

    public enum GCMAttitude {
        ASK, ALWAYS, NEVER
    }

    public static final String GCM_ACTION = "de.mm.android.longitude.network.GCM_ACTION";
    public static final String GCM_VALUE = "de.mm.android.longitude.network.GCM_VALUE";
    private static final String TAG = GCMListenerService.class.getSimpleName();
    private static final String GCM_POKE = "poke";
    private static final String GCM_UPDATE = "update";

    @Override
    public void onMessageReceived(String from, Bundle data) {
        String message = data.getString("message","NOOOooo.");
        Log.d(TAG, "onMessageReceived, From" + from + ", Message: " + message);

        switch (message) {
            case GCM_POKE:
                GCMAttitude type = PreferenceUtil.getGCMAttitude(this);
                switch (type) {
                    case ASK:
                        showNotification();
                        break;
                    case ALWAYS:
                        startService(new Intent(this, UpdateLocationService.class));
                        break;
                    case NEVER:
                    default:
                        break;
                }
                break;

            case GCM_UPDATE:
                Intent i = new Intent(GCM_ACTION);
                i.putExtra(GCM_VALUE, message);
                LocalBroadcastManager.getInstance(this).sendBroadcast(i);
                break;

            default:
                Log.d(TAG, "unknown message " + data.toString());
                break;
        }
	}

    @Override
    public void onDeletedMessages() {
        Log.d(TAG, "onDeletedMessages");
    }

    @Override
    public void onMessageSent(String msgId) {
        Log.d(TAG, "onMessageSent: " + msgId);
    }

    @Override
    public void onSendError(String msgId, String error) {
        Log.d(TAG, "onSendError: " + msgId + " " + error);
    }

    /* Stuff */

    private void showNotification() {
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
		PendingIntent updateIntent = PendingIntent.getService(this, 0, new Intent(this, UpdateLocationService.class), 0); // TODO handle WakeLock with PendingIntent
		PendingIntent nopeIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, CancelReceiver.class), 0);
		
		Set<Integer> attitude = PreferenceUtil.getGCMNotification(this);
		int notificationAttitude = 0;
		for (int i: attitude) {
			notificationAttitude |= i;
		}

        Notification pokeNotification = new NotificationCompat.Builder(this)
			.setContentTitle(getString(R.string.app_name))
			.setContentText(getString(R.string.notification_message))
            .setPriority(Notification.PRIORITY_HIGH)
			.setAutoCancel(true)
            .setSmallIcon(R.mipmap.icon)
            .setDefaults(notificationAttitude)
            .setContentIntent(contentIntent)
            .setDeleteIntent(nopeIntent)
			.addAction(android.R.drawable.ic_menu_myplaces, getString(R.string.notification_update), updateIntent)
			.addAction(android.R.drawable.ic_menu_delete, getString(R.string.notification_ignore), nopeIntent)
            .build();

		getNotificationManager(this).notify(Constants.NOTIFICATION_ID_GCM, pokeNotification);
	}

    private NotificationManager getNotificationManager(Context context){
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

}