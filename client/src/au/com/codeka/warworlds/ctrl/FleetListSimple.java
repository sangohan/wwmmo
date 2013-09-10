package au.com.codeka.warworlds.ctrl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.Design;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.Star;

public class FleetListSimple extends LinearLayout {
    private Context mContext;
    private Star mStar;
    private List<Fleet> mFleets;
    private FleetSelectedHandler mFleetSelectedHandler;
    private View.OnClickListener mOnClickListener;

    public FleetListSimple(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public FleetListSimple(Context context) {
        super(context);
        mContext = context;
    }

    public void setFleetSelectedHandler(FleetSelectedHandler handler) {
        mFleetSelectedHandler = handler;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public void setStar(Star s) {
        mStar = s;
        refresh();
    }

    private void refresh() {
        if (mOnClickListener == null) {
            mOnClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Fleet fleet = (Fleet) v.getTag();
                    if (mFleetSelectedHandler != null) {
                        mFleetSelectedHandler.onFleetSelected(fleet);
                    }
                }
            };
        }

        mFleets = new ArrayList<Fleet>();
        if (mStar.getFleets() != null) {
            for (BaseFleet f : mStar.getFleets()) {
                if (!f.getState().equals(Fleet.State.MOVING)) {
                    mFleets.add((Fleet) f);
                }
            }
        }

        removeAllViews();
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        for (Fleet fleet : mFleets) {
            View rowView = getRowView(inflater, fleet);
            addView(rowView);
        }
    }

    private View getRowView(LayoutInflater inflater, Fleet fleet) {
        View view = (ViewGroup) inflater.inflate(R.layout.starfield_planet, null);

        final ImageView icon = (ImageView) view.findViewById(R.id.starfield_planet_icon);
        Design design = DesignManager.i.getDesign(DesignKind.SHIP, fleet.getDesignID());

        icon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));

        TextView shipKindTextView = (TextView) view.findViewById(R.id.starfield_planet_type);
        shipKindTextView.setText(String.format("%d × %s",
                (int) Math.ceil(fleet.getNumShips()), design.getDisplayName(fleet.getNumShips() > 1)));

        final TextView shipCountTextView = (TextView) view.findViewById(R.id.starfield_planet_colony);
        shipCountTextView.setText(String.format("%s",
                StringUtils.capitalize(fleet.getStance().toString().toLowerCase(Locale.ENGLISH))));

        view.setOnClickListener(mOnClickListener);
        view.setTag(fleet);
        return view;
    }

    public interface FleetSelectedHandler {
        void onFleetSelected(Fleet fleet);
    }
}
