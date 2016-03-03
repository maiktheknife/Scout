package de.mm.android.longitude.model

import android.location.Location
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import de.mm.android.longitude.common.Constants
import java.text.ParseException

class ContactData: SimpleContactData, Comparable<ContactData>, ClusterItem {
    val plus_id: String
    val is_confirmed: Boolean
    val latitude: Double
    val longitude: Double
    val altitude: Double
    val accuracy: Double
    val updated_on: String
    val address: String

    constructor(person_id: Int, email: String, name: String, plusID: String, isConfirmed: Boolean,
                latitude: Double, longitude: Double, altitude: Double, accuracy: Double,
                address: String, updated_on: String) : super(person_id, email, name) {
        this.plus_id = plusID
        this.is_confirmed = isConfirmed
        this.latitude = latitude
        this.longitude = longitude
        this.altitude = altitude
        this.accuracy = accuracy
        this.address = address
        this.updated_on = updated_on
    }

    val updatedOnFormatted: String
        get() {
            try {
                Log.d(TAG, "1 " + updated_on)
                val dateServer = Constants.DATEFORMAT_SERVER.parse(updated_on)
                Log.d(TAG, "2 " + dateServer)
                val xxx = Constants.DATEFORMAT_LOCAL.format(dateServer)
                Log.d(TAG, "3 " + xxx)
                return xxx
            } catch (e: ParseException) {
                Log.e(TAG, "e", e)
            }
            return updated_on
        }

    fun getDistanceTo(location: Location?): Double {
        if (location != null) {
            val mee = Location("Scout DB Location")
            mee.longitude = longitude
            mee.latitude = latitude
            return mee.distanceTo(location).toDouble()
        } else {
            return 0.0
        }
    }

    /* Stuff */

    override fun toString(): String {
        return "ContactData{ person_id='$person_id', email='$email', name='$name', plus_id='$plus_id', is_confirmed=$is_confirmed, latitude=$latitude, longitude=$longitude, altitude=$altitude, accuracy=$accuracy, updated_on='$updated_on', address='$address'}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as ContactData?
        return email == that!!.email
    }

    override fun hashCode(): Int {
        return email.hashCode()
    }

    /* Parcelable */

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeString(plus_id)
        dest.writeInt(if (is_confirmed) 1 else 0)
        dest.writeDouble(latitude)
        dest.writeDouble(longitude)
        dest.writeDouble(altitude)
        dest.writeDouble(accuracy)
        dest.writeString(address)
        dest.writeString(updated_on)
    }

    private constructor(`in`: Parcel) : super(`in`) {
        plus_id = `in`.readString()
        is_confirmed = `in`.readInt() == 1
        latitude = `in`.readDouble()
        longitude = `in`.readDouble()
        altitude = `in`.readDouble()
        accuracy = `in`.readDouble()
        address = `in`.readString()
        updated_on = `in`.readString()
    }

    /* Comparable */

    /**
     * a negative integer if this instance is less than another;
     * a positive integer if this instance is greater than another;
     * 0 if this instance has the same order as another.
     */
    override fun compareTo(another: ContactData): Int {
        if (is_confirmed && !another.is_confirmed) {
            return -1
        } else if (!is_confirmed && another.is_confirmed) {
            return 1
        }
        return name.compareTo(another.name, ignoreCase = true)
    }

    /* ClusterItem */

    override fun getPosition(): LatLng {
        return LatLng(latitude, longitude)
    }

    companion object {
        val TABLE = "t_friends"
        val COLUMN_ID = "person_id"
        val COLUMN_EMAIL = "email"
        val COLUMN_NAME = "name"
        val COLUMN_PLUS_ID = "plus_id"
        val COLUMN_IS_CONFIRMED = "is_confirmed"
        val COLUMN_LATITUDE = "latitude"
        val COLUMN_LONITUDE = "longitude"
        val COLUMN_ALTITUDE = "altitude"
        val COLUMN_ACCURACY = "accuracy"
        val COLUMN_ADDRESS = "address"
        val COLUMN_UPDATED_ON = "updated_on"
        private val TAG = ContactData::class.java.simpleName

        val CREATOR: Parcelable.Creator<ContactData> = object : Parcelable.Creator<ContactData> {
            override fun createFromParcel(`in`: Parcel): ContactData {
                return ContactData(`in`)
            }

            override fun newArray(size: Int): Array<ContactData?> {
                return arrayOfNulls(size)
            }
        }
    }

}
