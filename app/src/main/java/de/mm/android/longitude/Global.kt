package de.mm.android.longitude

import android.app.Application

import com.squareup.picasso.OkHttpDownloader
import com.squareup.picasso.Picasso

import de.mm.android.longitude.network.RestService

/**
 * Created by Max on 10.09.2015.
 */
class Global : Application() {

    override fun onCreate() {
        super.onCreate()
        setUpPicasso()
        setUpRetrofit()
    }

    private fun setUpPicasso() {
        val builder = Picasso.Builder(this)
        builder.downloader(OkHttpDownloader(this, Integer.MAX_VALUE.toLong()))
        val built = builder.build()
        built.setIndicatorsEnabled(true)
        built.isLoggingEnabled = true
        Picasso.setSingletonInstance(built)
    }

    private fun setUpRetrofit() {
        RestService.Creator.create(this)
    }

}
