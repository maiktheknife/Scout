package de.mm.android.longitude.network;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import java.io.IOException;
import java.util.List;

import de.mm.android.longitude.util.NetworkUtil;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Created by Max on 14.08.2015.
 */
public class AddressTask {
    private static final String TAG = AddressTask.class.getSimpleName();

    public static Observable<String> getAddress(Context context, Location l) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
//                Log.d(TAG, "call");
//                // who cares, second thread
//                try {
//                    Thread.sleep(10_000);
//                } catch (InterruptedException e) {
//                    Log.e(TAG, "", e);
//                }
//                Log.d(TAG, "call 2");

                if (!NetworkUtil.isInternetAvailable(context)) {
                    subscriber.onCompleted();
                    return;
                }

                try {
                    List<Address> addresses = new Geocoder(context).getFromLocation(l.getLatitude(), l.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        subscriber.onNext(address == null ? null :address.getThoroughfare() + " " + address.getSubThoroughfare() + ", " + address.getLocality());
                        subscriber.onCompleted();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "getAddressFromLocation", e);
                    subscriber.onError(e);
                }
            }
        })
        .subscribeOn(Schedulers.newThread());
    }

    private AddressTask() {}

}