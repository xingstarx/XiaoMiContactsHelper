package com.star.contacts.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Contact implements Parcelable {
    public String displayName;
    public String phoneNumber;
    public String contactId;
    public String rawContactId;
    public String dataId;

    public Contact(String displayName, String phoneNumber, String contactId, String dataId, String rawContactId) {
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
        this.contactId = contactId;
        this.dataId = dataId;
        this.rawContactId = rawContactId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.displayName);
        dest.writeString(this.phoneNumber);
        dest.writeString(this.contactId);
        dest.writeString(this.dataId);
        dest.writeString(this.rawContactId);
    }

    protected Contact(Parcel in) {
        this.displayName = in.readString();
        this.phoneNumber = in.readString();
        this.contactId = in.readString();
        this.dataId = in.readString();
        this.rawContactId = in.readString();
    }

    public static final Parcelable.Creator<Contact> CREATOR = new Parcelable.Creator<Contact>() {
        @Override
        public Contact createFromParcel(Parcel source) {
            return new Contact(source);
        }

        @Override
        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };
}