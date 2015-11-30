package de.mm.android.longitude.network.task;

/**
 * Created by Max on 04.09.2015.
 */
public interface GenericCallback<T> {
    void onFinished(T data);
}
