package de.mm.android.longitude.network.task;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Created by Max on 31.03.2015.
 * call: execute(url)
 */
public class ImageLoaderTask extends AsyncTask<String, Void, Bitmap> {
    private static final String TAG = ImageLoaderTask.class.getSimpleName();

    private GenericCallback<Bitmap> callback;

    public ImageLoaderTask(@NonNull GenericCallback<Bitmap> callback) {
        this.callback = callback;
    }

    protected Bitmap doInBackground(String... urls) {
        Bitmap mIcon = null;
        try {
            InputStream in = new URL(urls[0]).openStream();
            mIcon = BitmapFactory.decodeStream(in);
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
        }
        return mIcon;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        callback.onFinished(result);
    }

}