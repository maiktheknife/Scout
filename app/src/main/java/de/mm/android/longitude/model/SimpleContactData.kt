package de.mm.android.longitude.model

import android.os.Parcel
import android.os.Parcelable

/**
 * Created by Max on 03.05.2015.
 */
open class SimpleContactData : Parcelable {
    var person_id: Int = 0
        protected set
    var email: String
        protected set
    var name: String
        protected set

    constructor(person_id: Int, email: String, name: String) {
        this.person_id = person_id
        this.email = email
        this.name = name
    }

    override fun toString(): String {
        return "SimpleContactData{person_id=$person_id, email='$email', name='$name'}"
    }

    /* Parcelable */

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(person_id)
        dest.writeString(email)
        dest.writeString(name)
    }

    protected constructor(p: Parcel) {
        person_id = p.readInt()
        email = p.readString()
        name = p.readString()
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