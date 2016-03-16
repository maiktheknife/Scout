package de.mm.android.longitude.network

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.util.Log

import java.io.IOException

import de.mm.android.longitude.util.NetworkUtil
import rx.Observable
import rx.Subscriber
import rx.schedulers.Schedulers

/**
 * Created by Max on 14.08.2015.
 */
object AddressTask {
    private val TAG = AddressTask::class.java.simpleName

    fun getAddress(context: Context, l: Location): Observable<String> {
        return Observable.create(Observable.OnSubscribe<kotlin.String> { subscriber ->

            if (!NetworkUtil.isInternetAvailable(context)) {
                subscriber.onCompleted()
                return@OnSubscribe
            }

            try {
                val addresses = Geocoder(context).getFromLocation(l.latitude, l.longitude, 1)
                if (addresses != null && !addresses.isEmpty()) {
                    val address = addresses[0]
                    subscriber.onNext(if (address == null) null else address.thoroughfare + " " + address.subThoroughfare + ", " + address.locality)
                    subscriber.onCompleted()
                }
            } catch (e: IOException) {
                Log.e(TAG, "getAddressFromLocation", e)
                subscriber.onError(e)
            }
        }).subscribeOn(Schedulers.newThread())
    }

}