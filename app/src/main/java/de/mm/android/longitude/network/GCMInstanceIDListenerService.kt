package de.mm.android.longitude.network

import android.util.Log

import com.google.android.gms.iid.InstanceIDListenerService

import retrofit.RetrofitError

/**
 * Created by Max on 20.08.2015.
 */
class GCMInstanceIDListenerService : InstanceIDListenerService() {

    /**
     * Called if InstanceID token is updated.
     * This may occur if the security of the previous token had been compromised.
     * This call is initiated by the InstanceID provider.
     */
    override fun onTokenRefresh() {
        Log.d(TAG, "onTokenRefresh")
        GCMRegUtil
                .getNewGCMRegID(this)
                .flatMap<RestService.NetworkResponse>({ gcm ->
                    RestService.Creator.create(this).addGCMRegID(gcm) })
                .subscribe(
                    { networkResponse ->
                        Log.d(TAG, "onHandleIntent.onNext: " + networkResponse) }
                    { throwable ->
                        val error = throwable as RetrofitError
                        Log.d(TAG, "onHandleIntent.onError: " + error)
                    }
    }

    companion object {
        private val TAG = GCMInstanceIDListenerService::class.java.simpleName
    }

}