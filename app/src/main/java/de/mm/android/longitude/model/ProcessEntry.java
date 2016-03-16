package de.mm.android.longitude.model;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class ProcessEntry {
    public static final String TAG = ProcessEntry.class.getSimpleName();
    public static final String TABLE = "t_process";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_LAT = "lat";
    public static final String COLUMN_LON = "lon";
    public static final String COLUMN_ALT = "alt";
    public static final String COLUMN_ACC = "acc";
    public static final String COLUMN_UPDATED_ON = "updated_on";
    public static final String COLUMN_ADDRESS = "address";

    private long id;
    private double lat;
    private double lon;
    private double alt;
    private double acc;
    private String updated_on;
    private String address;

    public ProcessEntry(double lat, double lon, double alt, double acc, String address) {
        this(-1, lat, lon, alt, acc, "", address);
    }

    public ProcessEntry(long id, double lat, double lon, double alt, double acc, String updated_on, String address) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        this.alt = alt;
        this.acc = acc;
        this.updated_on = updated_on;
        this.address = address;
    }

    public long getId() {
        return id;
    }

    public double getLon() {
        return lon;
    }

    public double getLat() {
        return lat;
    }

    public double getAlt() {
        return alt;
    }

    public double getAccuracy() {
        return acc;
    }

    public String getUpdated_on() {
        return updated_on;
    }

    public String getAddress() {
        return address;
    }

    public double getAcc() {
        return acc;
    }

    public JSONObject toJSON() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("lat", lat);
            obj.put("lon", lon);
            obj.put("alt", alt);
            obj.put("acc", acc);
            obj.put("updated_on", updated_on);
            return obj;
        } catch (JSONException e) {
            Log.e(TAG, "JSONException", e);
            return null;
        }
    }

	/* Stuff */

    @Override
    public String toString() {
        return "ProcessEntry{" + "id=" + id + ", lat=" + lat + ", lon=" + lon + ", alt=" + alt +
                ", acc=" + acc + ", updated_on='" + updated_on + '\'' + ", address='" + address + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProcessEntry that = (ProcessEntry) o;

        return id == that.id && Double.compare(that.lat, lat) == 0 && Double.compare(that.lon, lon) == 0 &&
                Double.compare(that.alt, alt) == 0 && Double.compare(that.acc, acc) == 0 &&
                updated_on.equals(that.updated_on) && !(address != null ?
                !address.equals(that.address) :
                that.address != null);

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = (int) (id ^ (id >>> 32));
        temp = Double.doubleToLongBits(lat);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lon);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(alt);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(acc);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + updated_on.hashCode();
        result = 31 * result + (address != null ? address.hashCode() : 0);
        return result;
    }
}