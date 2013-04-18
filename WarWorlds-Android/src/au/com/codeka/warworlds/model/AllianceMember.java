package au.com.codeka.warworlds.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import android.os.Parcel;
import android.os.Parcelable;
import au.com.codeka.common.model.BaseAllianceMember;

public class AllianceMember extends BaseAllianceMember implements Parcelable {
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mKey);
        parcel.writeString(mAllianceKey);
        parcel.writeString(mEmpireKey);
        parcel.writeLong(mTimeJoined.getMillis());
    }

    protected void readFromParcel(Parcel parcel) {
        mKey = parcel.readString();
        mAllianceKey = parcel.readString();
        mEmpireKey = parcel.readString();
        mTimeJoined = new DateTime(parcel.readLong(), DateTimeZone.UTC);
    }

    public static final Parcelable.Creator<AllianceMember> CREATOR
                = new Parcelable.Creator<AllianceMember>() {
        @Override
        public AllianceMember createFromParcel(Parcel parcel) {
            AllianceMember am = new AllianceMember();
            am.readFromParcel(parcel);
            return am;
        }

        @Override
        public AllianceMember[] newArray(int size) {
            return new AllianceMember[size];
        }
    };
}
