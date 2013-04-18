package au.com.codeka.warworlds.model;

import android.os.Parcel;
import android.os.Parcelable;
import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseEmpirePresence;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BasePlanet;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.protobuf.Messages;

/**
 * A \c StarSummary is a snapshot of information about a star that we can cache for a much
 * longer period of time (i.e. we cache it in the filesystem basically until the full star
 * is fetched). This is so we can do quicker look-ups of things like star names/icons without
 * having to do a full round-trip.
 */
public class StarSummary extends BaseStar implements Parcelable {
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mKey);
        parcel.writeString(mName);
        parcel.writeInt(mStarType.getIndex());
        parcel.writeInt(mSize);
        parcel.writeLong(mSectorX);
        parcel.writeLong(mSectorY);
        parcel.writeInt(mOffsetX);
        parcel.writeInt(mOffsetY);

        Planet[] planets = new Planet[mPlanets.length];
        for (int i = 0; i < mPlanets.length; i++) {
            planets[i] = (Planet) mPlanets[i];
        }
        parcel.writeParcelableArray(planets, flags);
    }

    protected void populateFromParcel(Parcel parcel) {
        mKey = parcel.readString();
        mName = parcel.readString();
        mStarType = sStarTypes[parcel.readInt()];
        mSize = parcel.readInt();
        mSectorX = parcel.readLong();
        mSectorY = parcel.readLong();
        mOffsetX = parcel.readInt();
        mOffsetY = parcel.readInt();

        Parcelable[] planets = parcel.readParcelableArray(Planet.class.getClassLoader());
        mPlanets = new Planet[planets.length];
        for (int i = 0; i < planets.length; i++) {
            mPlanets[i] = (Planet) planets[i];
        }
    }

    public static final Parcelable.Creator<StarSummary> CREATOR
                = new Parcelable.Creator<StarSummary>() {
        @Override
        public StarSummary createFromParcel(Parcel parcel) {
            StarSummary s = new StarSummary();
            s.populateFromParcel(parcel);
            return s;
        }

        @Override
        public StarSummary[] newArray(int size) {
            return new StarSummary[size];
        }
    };

    @Override
    protected BasePlanet createPlanet(Messages.Planet pb) {
        Planet p = new Planet();
        if (pb != null) {
            p.fromProtocolBuffer(this, pb);
        }
        return p;
    }

    @Override
    protected BaseColony createColony(Messages.Colony pb) {
        Colony c = new Colony();
        if (pb != null) {
            c.fromProtocolBuffer(pb);
        }
        return c;
    }

    @Override
    protected BaseBuilding createBuilding(Messages.Building pb) {
        Building b = new Building();
        if (pb != null) {
            b.fromProtocolBuffer(pb);
        }
        return b;
    }

    @Override
    protected BaseEmpirePresence createEmpirePresence(Messages.EmpirePresence pb) {
        EmpirePresence ep = new EmpirePresence();
        if (pb != null) {
            ep.fromProtocolBuffer(pb);
        }
        return ep;
    }

    @Override
    protected BaseFleet createFleet(Messages.Fleet pb) {
        Fleet f = new Fleet();
        if (pb != null) {
            f.fromProtocolBuffer(pb);
        }
        return f;
    }

    @Override
    protected BaseBuildRequest createBuildRequest(Messages.BuildRequest pb) {
        BuildRequest br = new BuildRequest();
        if (pb != null) {
            br.fromProtocolBuffer(pb);
        }
        return br;
    }

    @Override
    public BaseStar clone() {
        Parcel parcel = Parcel.obtain();
        this.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        return Star.CREATOR.createFromParcel(parcel);
    }

}
