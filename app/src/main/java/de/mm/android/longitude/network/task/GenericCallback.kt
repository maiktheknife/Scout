package de.mm.android.longitude.network.task

/**
 * Created by Max on 04.09.2015.
 */
interface GenericCallback<T> {
    fun onFinished(data: T)
}
