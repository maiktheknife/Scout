package de.mm.android.longitude.model

import android.util.Log

import org.json.JSONException
import org.json.JSONObject

class ProcessEntry(val id: Long, val lat: Double, val lon: Double, val alt: Double, val accuracy: Double, val updated_on: String, val address: String?) {

    constructor(lat: Double, lon: Double, alt: Double, acc: Double, address: String) : this(-1, lat, lon, alt, acc, "", address)

    fun toJSON(): JSONObject? {
        try {
            val obj = JSONObject()
            obj.put("id", id)
            obj.put("lat", lat)
            obj.put("lon", lon)
            obj.put("alt", alt)
            obj.put("acc", accuracy)
            obj.put("updated_on", updated_on)
            return obj
        } catch (e: JSONException) {
            Log.e(TAG, "JSONException", e)
            return null
        }

    }

    /* Stuff */

    override fun toString(): String {
        return "ProcessEntry{id=$id, lat=$lat, lon=$lon, alt=$alt, acc=$accuracy, updated_on='$updated_on\', address='$address\'}"
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as ProcessEntry?

        if (id != that.id) return false
        if (java.lang.Double.compare(that.lat, lat) != 0) return false
        if (java.lang.Double.compare(that.lon, lon) != 0) return false
        if (java.lang.Double.compare(that.alt, alt) != 0) return false
        if (java.lang.Double.compare(that.accuracy, accuracy) != 0) return false
        if (updated_on != that.updated_on) return false
        return !if (address != null) address != that.address else that.address != null

    }

    override fun hashCode(): Int {
        var result: Int
        var temp: Long
        result = (id xor id.ushr(32)).toInt()
        temp = java.lang.Double.doubleToLongBits(lat)
        result = 31 * result + (temp xor temp.ushr(32)).toInt()
        temp = java.lang.Double.doubleToLongBits(lon)
        result = 31 * result + (temp xor temp.ushr(32)).toInt()
        temp = java.lang.Double.doubleToLongBits(alt)
        result = 31 * result + (temp xor temp.ushr(32)).toInt()
        temp = java.lang.Double.doubleToLongBits(accuracy)
        result = 31 * result + (temp xor temp.ushr(32)).toInt()
        result = 31 * result + updated_on.hashCode()
        result = 31 * result + if (address != null) address.hashCode() else 0
        return result
    }

    companion object {
        val TAG = ProcessEntry::class.java!!.getSimpleName()
        val TABLE = "t_process"
        val COLUMN_ID = "_id"
        val COLUMN_LAT = "lat"
        val COLUMN_LON = "lon"
        val COLUMN_ALT = "alt"
        val COLUMN_ACC = "acc"
        val COLUMN_UPDATED_ON = "updated_on"
        val COLUMN_ADDRESS = "address"
    }
}