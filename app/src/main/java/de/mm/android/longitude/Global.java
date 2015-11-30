package de.mm.android.longitude;

import android.app.Application;

import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

import de.mm.android.longitude.network.RestService;

/**
 * Created by Max on 10.09.2015.
 */
public class Global extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        setUpPicasso();
        setUpRetrofit();
    }

    private void setUpPicasso() {
        Picasso.Builder builder = new Picasso.Builder(this);
        builder.downloader(new OkHttpDownloader(this, Integer.MAX_VALUE));
        Picasso built = builder.build();
        built.setIndicatorsEnabled(true);
        built.setLoggingEnabled(true);
        Picasso.setSingletonInstance(built);
    }

    private void setUpRetrofit() {
        RestService.Creator.create(this);
    }

}
