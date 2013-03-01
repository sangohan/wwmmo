package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import android.os.Parcel;
import android.os.Parcelable;
import au.com.codeka.warworlds.model.protobuf.Messages;

public class Colony implements Parcelable {
    private String mKey;
    private String mStarKey;
    private int mPlanetIndex;
    private float mPopulation;
    private String mEmpireKey;
    private float mFarmingFocus;
    private float mConstructionFocus;
    private float mPopulationFocus;
    private float mMiningFocus;
    private float mPopulationDelta;
    private float mGoodsDelta;
    private float mMineralsDelta;
    private float mUncollectedTaxes;
    private List<Building> mBuildings;
    private float mMaxPopulation;
    private float mDefenceBoost;
    private DateTime mCooldownTimeEnd;

    public String getKey() {
        return mKey;
    }
    public String getStarKey() {
        return mStarKey;
    }
    public int getPlanetIndex() {
        return mPlanetIndex;
    }
    public String getEmpireKey() {
        return mEmpireKey;
    }
    public float getPopulation() {
        return mPopulation;
    }
    public void setPopulation(float pop) {
        mPopulation = pop;
    }
    public float getFarmingFocus() {
        return mFarmingFocus;
    }
    public void setFarmingFocus(float focus) {
        mFarmingFocus = focus;
    }
    public float getConstructionFocus() {
        return mConstructionFocus;
    }
    public void setConstructionFocus(float focus) {
        mConstructionFocus = focus;
    }
    public float getPopulationFocus() {
        return mPopulationFocus;
    }
    public void setPopulationFocus(float focus) {
        mPopulationFocus = focus;
    }
    public float getMiningFocus() {
        return mMiningFocus;
    }
    public void setMiningFocus(float focus) {
        mMiningFocus = focus;
    }
    public float getPopulationDelta() {
        return mPopulationDelta;
    }
    public void setPopulationDelta(float dp) {
        mPopulationDelta = dp;
    }
    public float getGoodsDelta() {
        return mGoodsDelta;
    }
    public void setGoodsDelta(float d) {
        mGoodsDelta = d;
    }
    public float getMineralsDelta() {
        return mMineralsDelta;
    }
    public void setMineralsDelta(float d) {
        mMineralsDelta = d;
    }
    public float getUncollectedTaxes() {
        return mUncollectedTaxes;
    }
    public void setUncollectedTaxes(float taxes) {
        mUncollectedTaxes = taxes;
    }
    public List<Building> getBuildings() {
        return mBuildings;
    }
    public float getMaxPopulation() {
        return mMaxPopulation;
    }
    public float getDefenceBoost() {
        return mDefenceBoost;
    }
    public boolean isInCooldown() {
        if (mCooldownTimeEnd == null) {
            return false;
        }
        DateTime now = DateTime.now(DateTimeZone.UTC);
        return (now.compareTo(mCooldownTimeEnd) < 0);
    }
    public DateTime getCooldownEndTime() {
        return mCooldownTimeEnd;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mKey);
        parcel.writeString(mStarKey);
        parcel.writeInt(mPlanetIndex);
        parcel.writeFloat(mPopulation);
        parcel.writeString(mEmpireKey);
        parcel.writeFloat(mFarmingFocus);
        parcel.writeFloat(mConstructionFocus);
        parcel.writeFloat(mPopulationFocus);
        parcel.writeFloat(mMiningFocus);
        parcel.writeFloat(mPopulationDelta);
        parcel.writeFloat(mGoodsDelta);
        parcel.writeFloat(mMineralsDelta);
        parcel.writeFloat(mUncollectedTaxes);
        parcel.writeFloat(mMaxPopulation);
        parcel.writeFloat(mDefenceBoost);
        if (mCooldownTimeEnd == null) {
            parcel.writeLong(0);
        } else {
            parcel.writeLong(mCooldownTimeEnd.getMillis());
        }

        Building[] buildings = new Building[mBuildings.size()];
        parcel.writeParcelableArray(mBuildings.toArray(buildings), flags);
    }

    public static final Parcelable.Creator<Colony> CREATOR
                = new Parcelable.Creator<Colony>() {
        @Override
        public Colony createFromParcel(Parcel parcel) {
            Colony c = new Colony();
            c.mKey = parcel.readString();
            c.mStarKey = parcel.readString();
            c.mPlanetIndex = parcel.readInt();
            c.mPopulation = parcel.readFloat();
            c.mEmpireKey = parcel.readString();
            c.mFarmingFocus = parcel.readFloat();
            c.mConstructionFocus = parcel.readFloat();
            c.mPopulationFocus = parcel.readFloat();
            c.mMiningFocus = parcel.readFloat();
            c.mPopulationDelta = parcel.readFloat();
            c.mGoodsDelta = parcel.readFloat();
            c.mMineralsDelta = parcel.readFloat();
            c.mUncollectedTaxes = parcel.readFloat();
            c.mMaxPopulation = parcel.readFloat();
            c.mDefenceBoost = parcel.readFloat();
            long millis = parcel.readLong();
            if (millis > 0) {
                c.mCooldownTimeEnd = new DateTime(millis, DateTimeZone.UTC);
            }

            Parcelable[] buildings = parcel.readParcelableArray(Building.class.getClassLoader());
            c.mBuildings = new ArrayList<Building>();
            for (int i = 0; i < buildings.length; i++) {
                c.mBuildings.add((Building) buildings[i]);
            }

            return c;
        }

        @Override
        public Colony[] newArray(int size) {
            return new Colony[size];
        }
    };

    public static Colony fromProtocolBuffer(Messages.Colony pb) {
        Colony c = new Colony();
        c.mKey = pb.getKey();
        c.mStarKey = pb.getStarKey();
        c.mPlanetIndex = pb.getPlanetIndex();
        c.mPopulation = pb.getPopulation();
        if (pb.hasEmpireKey()) {
            c.mEmpireKey = pb.getEmpireKey();
        }
        c.mBuildings = new ArrayList<Building>();
        c.mFarmingFocus = pb.getFocusFarming();
        c.mConstructionFocus = pb.getFocusConstruction();
        c.mMiningFocus = pb.getFocusMining();
        c.mPopulationFocus = pb.getFocusPopulation();
        c.mPopulationDelta = pb.getDeltaPopulation();
        c.mGoodsDelta = pb.getDeltaGoods();
        c.mMineralsDelta = pb.getDeltaMinerals();
        c.mUncollectedTaxes = pb.getUncollectedTaxes();
        c.mMaxPopulation = pb.getMaxPopulation();
        c.mDefenceBoost = pb.getDefenceBonus();
        if (pb.hasCooldownEndTime()) {
            c.mCooldownTimeEnd = new DateTime(pb.getCooldownEndTime() * 1000, DateTimeZone.UTC);
        }

        return c;
    }

    public Messages.Colony toProtocolBuffer() {
        return Messages.Colony.newBuilder()
            .setKey(getKey())
            .setPlanetIndex(getPlanetIndex())
            .setStarKey(getStarKey())
            .setEmpireKey(getEmpireKey())
            .setPopulation(getPopulation())
            .setFocusPopulation(getPopulationFocus())
            .setFocusFarming(getFarmingFocus())
            .setFocusMining(getMiningFocus())
            .setFocusConstruction(getConstructionFocus())
            .build();
    }
}
