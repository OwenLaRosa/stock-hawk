package com.sam_chordas.android.stockhawk.info;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Owen LaRosa on 9/8/16.
 */

public class Article implements Parcelable {

    public String title;
    public String link;

    public Article(String title, String link) {
        this.title = title;
        this.link = link;
    }

    // Parcelable implementation referenced from: http://stackoverflow.com/questions/7181526/how-can-i-make-my-custom-objects-be-parcelable

    Article(Parcel in) {
        String[] data = new String[2];
        in.readStringArray(data);
        this.title = data[0];
        this.link = data[1];
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[] {
                this.title,
                this.link
        });

    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public Article createFromParcel(Parcel source) {
            return new Article(source);
        }

        @Override
        public Article[] newArray(int size) {
            return new Article[size];
        }
    };

}
