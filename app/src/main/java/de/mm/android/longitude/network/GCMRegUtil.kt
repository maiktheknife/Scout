package de.mm.android.longitude.network

import android.content.Context
import android.util.Log

import com.google.android.gms.gcm.GoogleCloudMessaging
import com.google.android.gms.iid.InstanceID

import java.io.IOException

import de.mm.android.longitude.R
import rx.Observable
import rx.Subscriber
import rx.schedulers.Schedulers

object GCMRegUtil {
    private val TAG = GCMRegUtil::class.java.simpleName

    fun getNewGCMRegID(context: Context): Observable<String> {
        return Observable.create(Observable.OnSubscribe<kotlin.String> { subscriber ->
            try {
                val gcm = InstanceID.getInstance(context).getToken(context.resources.getString(R.string.app_id), GoogleCloudMessaging.INSTANCE_ID_SCOPE)
                subscriber.onNext(gcm)
                subscriber.onCompleted()
            } catch (e: IOException) {
                Log.e(TAG, "getNewGCMRegID", e)
                subscriber.onError(e)
            }
        }).subscribeOn(Schedulers.newThread())
    }

}