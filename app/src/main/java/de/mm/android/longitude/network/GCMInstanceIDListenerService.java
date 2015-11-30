package de.mm.android.longitude.network;

import android.util.Log;

import com.google.android.gms.iid.InstanceIDListenerService;

import retrofit.RetrofitError;

/**
 * Created by Max on 20.08.2015.
 */
public class GCMInstanceIDListenerService extends InstanceIDListenerService {
    private static final String TAG = GCMInstanceIDListenerService.class.getSimpleName();

    /**
     * Called if InstanceID token is updated. This may occur if the security of the previous token had been compromised.
     * This call is initiated by the InstanceID provider.
     */
    @Override
    public void onTokenRefresh() {
        Log.d(TAG, "onTokenRefresh");
        GCMRegUtil
            .getNewGCMRegID(this)
            .flatMap(gcm -> RestService.Creator.create(this).addGCMRegID(gcm))
            .subscribe(networkResponse -> Log.d(TAG, "onHandleIntent.onNext: " + networkResponse), throwable -> {
                RetrofitError error = (RetrofitError) throwable;
                Log.d(TAG, "onHandleIntent.onError: " + error);
            });
    }

}