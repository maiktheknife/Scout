package de.mm.android.longitude.network;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

import de.mm.android.longitude.R;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

public class GCMRegUtil {
    private static final String TAG = GCMRegUtil.class.getSimpleName();

    public static Observable<String> getNewGCMRegID(Context context) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                try {
                    String gcm = InstanceID.getInstance(context).getToken(context.getResources().getString(R.string.app_id), GoogleCloudMessaging.INSTANCE_ID_SCOPE);
                    subscriber.onNext(gcm);
                    subscriber.onCompleted();
                } catch (IOException e) {
                    Log.e(TAG, "getNewGCMRegID", e);
                    subscriber.onError(e);
                }
            }
        })
        .subscribeOn(Schedulers.newThread());
    }

    private GCMRegUtil() {}

}