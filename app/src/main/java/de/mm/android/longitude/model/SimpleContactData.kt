package de.mm.android.longitude.model

import android.os.Parcel
import android.os.Parcelable

/**
 * Created by Max on 03.05.2015.
 */
open class SimpleContactData(val person_id: Int, val email: String, val name: String) : Parcelable {

    override fun toString(): String {
        return "SimpleContactData{person_id=$person_id, email='$email', name='$name'}"
    }

    /* Parcelable */

    protected constructor(p: Parcel) : this(p.readInt(), p.readString(), p.readString())

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(person_id)
        dest.writeString(email)
        dest.writeString(name)
    }

    companion object {

        val CREATOR: Parcelable.Creator<SimpleContactData> = object : Parcelable.Creator<SimpleContactData> {
            override fun createFromParcel(p: Parcel): SimpleContactData {
                return SimpleContactData(p)
            }

            override fun newArray(size: Int): Array<SimpleContactData?> {
                return arrayOfNulls(size)
            }
        }
    }

}