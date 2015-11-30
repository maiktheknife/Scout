package de.mm.android.longitude.model;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.text.ParseException;
import java.util.Date;

import de.mm.android.longitude.common.Constants;

public class ContactData extends SimpleContactData implements Comparable<ContactData>, ClusterItem {
    public static final String TABLE = "t_friends";
    public static final String COLUMN_ID = "person_id";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PLUS_ID = "plus_id";
    public static final String COLUMN_IS_CONFIRMED = "is_confirmed";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONITUDE = "longitude";
    public static final String COLUMN_ALTITUDE = "altitude";
    public static final String COLUMN_ACCURACY = "accuracy";
    public static final String COLUMN_ADDRESS = "address";
    public static final String COLUMN_UPDATED_ON = "updated_on";
    private static final String TAG = ContactData.class.getName();

    private String plus_id;
    private boolean is_confirmed;
    // location
	private double latitude;
    private double longitude;
    private double altitude;
    private double accuracy;
    private String updated_on;
    private String address;

    public ContactData(int person_id, String email, String name, String plusID, boolean isConfirmed,
                       double latitude, double longitude, double altitude, double accuracy,
                       String address, String updated_on) {
        super(person_id, email, name);
		this.plus_id = plusID;
        this.is_confirmed = isConfirmed;
		this.latitude = latitude;
		this.longitude = longitude;
        this.altitude = altitude;
		this.accuracy = accuracy;
		this.address = address;
        this.updated_on = updated_on;
    }

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

    public double getAltitude() {
        return altitude;
    }

    public double getAccuracy() {
		return accuracy;
	}

	public String getUpdatedOn() {
		return updated_on;
	}

    public String getUpdatedOnFormatted() {
        try {
            Log.d(TAG, "1 " + updated_on);
            Date dateServer = Constants.DATEFORMAT_SERVER.parse(updated_on);
            Log.d(TAG, "2 "+dateServer);
            String xxx = Constants.DATEFORMAT_LOCAL.format(dateServer);
            Log.d(TAG, "3 " + xxx);
            return xxx;
        } catch (ParseException e) {
            Log.e(TAG, "", e);
        }
        return updated_on;
    }

	public String getAddress() {
		return address;
	}

    public double getDistanceTo(Location location) {
        if (location != null) {
            Location mee = new Location("Scout DB Location");
            mee.setLongitude(longitude);
            mee.setLatitude(latitude);
            return mee.distanceTo(location);
        } else {
            return 0;
        }
    }

    public String getPlusID() {
        return plus_id;
    }

    public boolean isConfirmed() {
        return is_confirmed;
    }

    /* Stuff */

    @Override
    public String toString() {
        return "ContactData{ person_id='" + person_id + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", plus_id='" + plus_id + '\'' +
                ", is_confirmed=" + is_confirmed +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", accuracy=" + accuracy +
                ", updated_on='" + updated_on + '\'' +
                ", address='" + address + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContactData that = (ContactData) o;
        return email.equals(that.email);
    }

    @Override
    public int hashCode() {
        return email.hashCode();
    }

	/* Parcelable */

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
		dest.writeString(plus_id);
        dest.writeInt(is_confirmed ? 1 : 0);
		dest.writeDouble(latitude);
		dest.writeDouble(longitude);
        dest.writeDouble(altitude);
        dest.writeDouble(accuracy);
		dest.writeString(address);
		dest.writeString(updated_on);
	}

	public static final Parcelable.Creator<ContactData> CREATOR = new Parcelable.Creator<ContactData>() {
		public ContactData createFromParcel(Parcel in) {
			return new ContactData(in);
		}

		public ContactData[] newArray(int size) {
			return new ContactData[size];
		}
	};

	private ContactData(Parcel in) {
        super(in);
        plus_id = in.readString();
        is_confirmed = in.readInt() == 1;
		latitude = in.readDouble();
		longitude = in.readDouble();
        altitude = in.readDouble();
		accuracy = in.readDouble();
		address = in.readString();
		updated_on = in.readString();
	}

	/* Comparable */

    /**
     * a negative integer if this instance is less than another;
     * a positive integer if this instance is greater than another;
     * 0 if this instance has the same order as another.
     */
	@Override
	public int compareTo(@NonNull ContactData another) {
        if (isConfirmed() && !another.isConfirmed()) {
            return -1;
        }else if (!isConfirmed() && another.isConfirmed()) {
            return 1;
        }
        return name.compareToIgnoreCase(another.name);
    }

    /* ClusterItem */

    @Override
    public LatLng getPosition() {
        return new LatLng(latitude, longitude);
    }

}