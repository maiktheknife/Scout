package de.mm.android.longitude.fragment;

import android.support.annotation.Nullable;

import java.util.List;

import de.mm.android.longitude.model.ContactData;

/**
 * Created by Max on 23.09.2015.
 */
public interface UpdateAble {
    void update(@Nullable final List<ContactData> contacts);
}
