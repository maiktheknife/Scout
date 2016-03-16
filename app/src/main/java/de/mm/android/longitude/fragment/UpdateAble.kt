package de.mm.android.longitude.fragment

import de.mm.android.longitude.model.ContactData

/**
 * Created by Max on 23.09.2015.
 */
interface UpdateAble {
    fun onConnectivityUpdate(isConnected: Boolean)
    fun onDataReceived(contacts: List<ContactData>?)
}
