package de.mm.android.longitude.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Max on 03.05.2015.
 */
public class SimpleContactData implements Parcelable {
    protected int person_id;
    protected String email;
    protected String name;

    public SimpleContactData(int person_id, String email, String name) {
        this.person_id = person_id;
        this.email = email;
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public int getPerson_id() {
        return person_id;
    }

    @Override
    public String toString() {
        return "SimpleContactData{" +
                "person_id=" + person_id +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    /* Parcelable */

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(person_id);
        dest.writeString(email);
        dest.writeString(name);
    }

    public static final Parcelable.Creator<SimpleContactData> CREATOR = new Parcelable.Creator<SimpleContactData>() {
        public SimpleContactData createFromParcel(Parcel in) {
            return new SimpleContactData(in);
        }

        public SimpleContactData[] newArray(int size) {
            return new SimpleContactData[size];
        }
    };

    protected SimpleContactData(Parcel in) {
        person_id = in.readInt();
        email = in.readString();
        name = in.readString();
    }

}