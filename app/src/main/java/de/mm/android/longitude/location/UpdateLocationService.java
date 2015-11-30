package de.mm.android.longitude.location;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.plus.Plus;

import java.util.Date;

import de.mm.android.longitude.WifiChangeReceiver;
import de.mm.android.longitude.common.Constants;
import de.mm.android.longitude.database.MyDBDelegate;
import de.mm.android.longitude.model.ProcessEntry;
import de.mm.android.longitude.network.AddressTask;
import de.mm.android.longitude.network.RestService;
import de.mm.android.longitude.util.GameUtil;
import de.mm.android.longitude.util.PreferenceUtil;
import rx.android.schedulers.AndroidSchedulers;

/**
 * <ul>
 * <li> aktualisiert den Standort </li>
 * <li> speichert ihn in der lokalen DB </li>
 * <li> speichert ihn in der Remote DB </li>
 * <li> aktualisiert die Achiviements </li>
 * <li> versendet eine local Broadcast, falls jemand intessiert ist </li>
 * </ul>
 */
public class UpdateLocationService extends IntentService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public static final String LOCATION_ACTION = "de.mm.android.longitude.network.LOCATION_ACTION";
    public static final String LOCATION_VALUE = "de.mm.android.longitude.network.LOCATION_VALUE";
    private static final String TAG = UpdateLocationService.class.getSimpleName();

    private Intent intent;
    private GoogleApiClient googleApiClient;
    private RestService restService;

    public UpdateLocationService() {
        super(TAG);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new IllegalStateException("Do not bind this service, start it!");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "onHandleIntent " + Thread.currentThread().getId() + " " + Thread.currentThread().getName());
        this.intent = intent;
        this.restService = RestService.Creator.create(this);
        this.googleApiClient = new GoogleApiClient.Builder(this)
            .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
            .addApi(Games.API).addScope(Games.SCOPE_GAMES)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build();
        this.googleApiClient.connect();
        getNotificationManager().cancel(Constants.NOTIFICATION_ID_GCM);
    }

    /* Util */

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private void stop() {
        Log.i(TAG, "stop");
        googleApiClient.disconnect();
        WifiChangeReceiver.completeWakefulIntent(intent);
    }

    /* ConnectionCallbacks, OnConnectionFailedListener */

    @Override
    public void onConnected(Bundle bundle) {
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, Tracking.REQUEST_SINGLE, l -> {
//            Log.i(TAG, "Location got");
            if (l == null) {
                return;
            }
            AddressTask
                .getAddress(this, l)
                .flatMap(address -> {
                    GameUtil.incrementDistance(this, googleApiClient, l);
                    GameUtil.submitDistanceScore(this, googleApiClient, (long) PreferenceUtil.getTraveledDistance(this));
                    PreferenceUtil.setLatestLocation(this, l);
                    MyDBDelegate.insertProcessEntry(this, new ProcessEntry(l.getLatitude(), l.getLongitude(), l.getAltitude(), l.getAccuracy(), address));
                    Intent i = new Intent(LOCATION_ACTION);
                    i.putExtra(LOCATION_VALUE, l);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(i);
                    return restService.addLocation(l.getLatitude(), l.getLongitude(), l.getAltitude(), l.getAccuracy(), address, Constants.DATEFORMAT_SERVER.format(new Date().getTime()));
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(networkResponse -> {
                    Log.d(TAG, "subscribe.onNext " + networkResponse + " " + Thread.currentThread().getId() + " " + Thread.currentThread().getName());
                    stop();
                }, t -> {
                    Log.e(TAG, "subscribe.onError " + Thread.currentThread().getId() + " " + Thread.currentThread().getName(), t);
                    stop();
                }, () -> {
                    Log.d(TAG, "subscribe.onComplete " + Thread.currentThread().getId() + " " + Thread.currentThread().getName());
                    stop();
                });
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "onConnectionSuspended: " + i);
        stop();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "onConnectionFailed: " + connectionResult.getErrorCode());
        stop();
    }

}