package au.com.codeka.warworlds.ctrl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import au.com.codeka.TimeInHours;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.ImageManager;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.ShipDesign;
import au.com.codeka.warworlds.model.ShipDesignManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;

/**
 * This control displays a list of fleets along with controls you can use to manage them (split
 * them, move them around, etc).
 */
public class FleetList extends FrameLayout implements StarManager.StarFetchedHandler {
    private FleetListAdapter mFleetListAdapter;
    private Fleet mSelectedFleet;
    private List<Fleet> mFleets;
    private Map<String, Star> mStars;
    private Context mContext;
    private boolean mIsInitialized;
    private OnFleetActionListener mFleetActionListener;

    public FleetList(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        View child = inflate(context, R.layout.fleet_list_ctrl, null);
        this.addView(child);
    }

    public void setOnFleetActionListener(OnFleetActionListener listener) {
        mFleetActionListener = listener;
    }

    public void refresh(List<Fleet> fleets, Map<String, Star> stars) {
        mFleets = fleets;
        mStars = stars;

        initialize();

        // if we had a fleet selected, make sure we still have the same
        // fleet selected after we refresh
        if (mSelectedFleet != null) {
            Fleet selectedFleet = mSelectedFleet;
            mSelectedFleet = null;

            for (Fleet f : mFleets) {
                if (f.getKey().equals(selectedFleet.getKey())) {
                    mSelectedFleet = f;
                    break;
                }
            }
        }

        mFleetListAdapter.setFleets(stars, fleets);
    }

    private void initialize() {
        if (mIsInitialized) {
            return;
        }
        mIsInitialized = true;

        mFleetListAdapter = new FleetListAdapter();
        final ListView fleetList = (ListView) findViewById(R.id.ship_list);
        fleetList.setAdapter(mFleetListAdapter);

        final Spinner stanceSpinner = (Spinner) findViewById(R.id.stance);
        stanceSpinner.setAdapter(new StanceAdapter());

        stanceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Fleet.Stance stance = Fleet.Stance.values()[position];
                if (mSelectedFleet == null) {
                    return;
                }

                if (mSelectedFleet.getStance() != stance && mFleetActionListener != null) {
                    mFleetActionListener.onFleetStanceModified(
                            mStars.get(mSelectedFleet.getStarKey()),
                            mSelectedFleet, stance);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        fleetList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                FleetListAdapter.ItemEntry entry =
                        (FleetListAdapter.ItemEntry) mFleetListAdapter.getItem(position);
                if (entry.type == FleetListAdapter.FLEET_ITEM_TYPE) {
                    mSelectedFleet = (Fleet) entry.value;
                    mFleetListAdapter.notifyDataSetChanged();
                }
            }
        });

        final Button splitBtn = (Button) findViewById(R.id.split_btn);
        splitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFleetActionListener != null) {
                    mFleetActionListener.onFleetSplit(mStars.get(mSelectedFleet.getStarKey()),
                                                      mSelectedFleet);
                }
            }
        });

        final Button moveBtn = (Button) findViewById(R.id.move_btn);
        moveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFleetActionListener != null) {
                    mFleetActionListener.onFleetMove(mStars.get(mSelectedFleet.getStarKey()),
                                                     mSelectedFleet);
                }
            }
        });

        final Button viewBtn = (Button) findViewById(R.id.view_btn);
        viewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFleetActionListener != null) {
                    mFleetActionListener.onFleetView(mStars.get(mSelectedFleet.getStarKey()),
                                                     mSelectedFleet);
                }
            }
        });

        StarManager.getInstance().addStarUpdatedListener(null, this);
    }

    @Override
    public void onDetachedFromWindow() {
        StarManager.getInstance().removeStarUpdatedListener(this);
    }

    /**
     * When a star is updated, we may need to refresh the list.
     */
    @Override
    public void onStarFetched(Star s) {
        for (String starKey : mStars.keySet()) {
            if (starKey.equals(s.getKey())) {
                mStars.put(s.getKey(), s);

                Iterator<Fleet> it = mFleets.iterator();
                while (it.hasNext()) {
                    Fleet f = it.next();
                    if (f.getStarKey().equals(starKey)) {
                        it.remove();
                    }
                }

                for (int j = 0; j < s.getFleets().size(); j++) {
                    mFleets.add(s.getFleets().get(j));
                }

                refresh(mFleets, mStars);
                break;
            }
        }
    }

    /**
     * Populates a solarsystem_fleet_row.xml view with details from the given fleet.
     */
    public static void populateFleetRow(final Context context, final Map<String, Star> stars, 
                                        View view, final Fleet fleet) {
        ImageView icon = (ImageView) view.findViewById(R.id.ship_icon);
        final TextView row1 = (TextView) view.findViewById(R.id.ship_row1);
        final TextView row2 = (TextView) view.findViewById(R.id.ship_row2);
        final TextView row3 = (TextView) view.findViewById(R.id.ship_row3);

        ShipDesignManager dm = ShipDesignManager.getInstance();
        ShipDesign design = dm.getDesign(fleet.getDesignID());

        icon.setImageDrawable(new SpriteDrawable(design.getSprite()));

        String text;
        if (fleet.getNumShips() == 1) {
            text = design.getDisplayName();
        } else {
            text = String.format("%s (× %d)", design.getDisplayName(), fleet.getNumShips());
        }
        row1.setText(text);

        text = String.format("%s (stance: %s)",
                             StringUtils.capitalize(fleet.getState().toString().toLowerCase()),
                             StringUtils.capitalize(fleet.getStance().toString().toLowerCase()));
        row2.setText(text);

        if (fleet.getState() == Fleet.State.MOVING) {
            row3.setVisibility(View.GONE);
            StarManager.getInstance().requestStar(fleet.getDestinationStarKey(), false,
                    new StarManager.StarFetchedHandler() {
                        @Override
                        public void onStarFetched(Star destStar) {
                            Star srcStar = null;
                            if (stars != null) {
                                srcStar = stars.get(fleet.getStarKey());
                            }
                            if (srcStar == null) {
                                srcStar = SectorManager.getInstance().findStar(fleet.getStarKey());
                            }
                            if (srcStar == null) {
                                row3.setText("→ (unknown)");
                            } else {
                                float timeRemainingInHours = fleet.getTimeToDestination(srcStar, destStar);

                                String eta = TimeInHours.format(timeRemainingInHours);
                                String html = String.format("→ <img src=\"star\" width=\"16\" height=\"16\" /> %s <b>ETA:</b> %s",
                                                            destStar.getName(), eta);
                                row3.setText(Html.fromHtml(html, 
                                                           new FleetListImageGetter(context, destStar),
                                                           null));
                            }
                            row3.setVisibility(View.VISIBLE);
                        }
                    });
        } else {
            row3.setVisibility(View.GONE);
        }
    }

    /**
     * Fetches the inline images we use to display star icons and whatnot.
     */
    private static class FleetListImageGetter implements Html.ImageGetter {
        private Context mContext;
        private Star mStar;

        public FleetListImageGetter(Context context, Star star) {
            mContext = context;
            mStar = star;
        }

        @Override
        public Drawable getDrawable(String source) {
            if (mStar != null) {
                Sprite sprite = StarImageManager.getInstance().getSprite(mContext, mStar, -1);
                Drawable d = new SpriteDrawable(sprite);
                d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                return d;
            } else {
                return null;
            }
        }
    }

    /**
     * This adapter is used to populate the list of ship fleets that the current colony has.
     */
    private class FleetListAdapter extends BaseAdapter {
        private ArrayList<Fleet> mFleets;
        private Map<String, Star> mStars;
        private ArrayList<ItemEntry> mEntries;

        private static final int STAR_ITEM_TYPE = 0;
        private static final int FLEET_ITEM_TYPE = 1;

        public FleetListAdapter() {
            // whenever a new star bitmap is generated, redraw the screen
            StarImageManager.getInstance().addBitmapGeneratedListener(
                    new ImageManager.BitmapGeneratedListener() {
                @Override
                public void onBitmapGenerated(String key, Bitmap bmp) {
                    notifyDataSetChanged();
                }
            });
        }

        /**
         * Sets the list of fleets that we'll be displaying.
         */
        public void setFleets(Map<String, Star> stars, List<Fleet> fleets) {
            mFleets = new ArrayList<Fleet>(fleets);
            mStars = stars;

            Collections.sort(mFleets, new Comparator<Fleet>() {
                @Override
                public int compare(Fleet lhs, Fleet rhs) {
                    // sort by star, then by design, then by count
                    if (!lhs.getStarKey().equals(rhs.getStarKey())) {
                        Star lhsStar = mStars.get(lhs.getStarKey());
                        Star rhsStar = mStars.get(rhs.getStarKey());
                        return lhsStar.getName().compareTo(rhsStar.getName());
                    } else if (!lhs.getDesignID().equals(rhs.getDesignID())) {
                        return lhs.getDesignID().compareTo(rhs.getDesignID());
                    } else {
                        return rhs.getNumShips() - lhs.getNumShips();
                    }
                }
            });

            mEntries = new ArrayList<ItemEntry>();
            String lastStarKey = "";
            for (Fleet f : mFleets) {
                if (!f.getStarKey().equals(lastStarKey)) {
                    mEntries.add(new ItemEntry(STAR_ITEM_TYPE, mStars.get(f.getStarKey())));
                    lastStarKey = f.getStarKey();
                }
                mEntries.add(new ItemEntry(FLEET_ITEM_TYPE, f));
            }

            notifyDataSetChanged();
        }

        /**
         * We have two types of items, the star and the actual fleet.
         */
        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            if (mEntries == null)
                return 0;

            return mEntries.get(position).type;
        }

        @Override
        public boolean isEnabled(int position) {
            if (mEntries.get(position).type == STAR_ITEM_TYPE) {
                return false;
            }

            return true;
        }

        @Override
        public int getCount() {
            if (mEntries == null)
                return 0;
            return mEntries.size();
        }

        @Override
        public Object getItem(int position) {
            if (mEntries == null)
                return null;
            return mEntries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ItemEntry entry = mEntries.get(position);
            View view = convertView;

            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService
                        (Context.LAYOUT_INFLATER_SERVICE);
                if (entry.type == STAR_ITEM_TYPE) {
                    view = inflater.inflate(R.layout.fleet_list_star_row, null);
                } else {
                    view = inflater.inflate(R.layout.fleet_list_row, null);
                }
            }

            if (entry.type == STAR_ITEM_TYPE) {
                Star star = (Star) entry.value;
                ImageView icon = (ImageView) view.findViewById(R.id.star_icon);
                TextView name = (TextView) view.findViewById(R.id.star_name);

                int imageSize = (int)(star.getSize() * star.getStarType().getImageScale() * 2);
                if (entry.drawable == null) {
                    Sprite sprite = StarImageManager.getInstance().getSprite(mContext, star, imageSize);
                    entry.drawable = new SpriteDrawable(sprite);
                }
                if (entry.drawable != null) {
                    icon.setImageDrawable(entry.drawable);
                }

                name.setText(star.getName());
            } else {
                Fleet fleet = (Fleet) entry.value;
                populateFleetRow(mContext, mStars, view, fleet);

                if (mSelectedFleet != null && mSelectedFleet.getKey().equals(fleet.getKey())) {
                    view.setBackgroundColor(0xff0c6476);
                } else {
                    view.setBackgroundColor(0xff000000);
                }
            }

            return view;
        }

        public class ItemEntry {
            public int type;
            public Object value;
            public Drawable drawable;

            public ItemEntry(int type, Object value) {
                this.type = type;
                this.value = value;
                this.drawable = null;
            }
        }
    }

    public class StanceAdapter extends BaseAdapter implements SpinnerAdapter {
        Fleet.Stance[] mValues;

        public StanceAdapter() {
            mValues = Fleet.Stance.values();
        }

        @Override
        public int getCount() {
            return mValues.length;
        }

        @Override
        public Object getItem(int position) {
            return mValues[position];
        }

        @Override
        public long getItemId(int position) {
            return mValues[position].getValue();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = getCommonView(position, convertView, parent);

            view.setTextColor(Color.WHITE);
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView view = getCommonView(position, convertView, parent);

            ViewGroup.LayoutParams lp = new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT,
                                                                     LayoutParams.MATCH_PARENT);
            lp.height = 60;
            view.setLayoutParams(lp);

            view.setTextColor(Color.BLACK);
            return view;
        }

        private TextView getCommonView(int position, View convertView, ViewGroup parent) {
            TextView view;
            if (convertView != null) {
                view = (TextView) convertView;
            } else {
                view = new TextView(mContext);
                view.setGravity(Gravity.CENTER_VERTICAL);
            }

            Fleet.Stance value = mValues[position];
            view.setText(StringUtils.capitalize(value.toString().toLowerCase()));
            return view;
        }
    }

    public interface OnFleetActionListener {
        void onFleetView(Star star, Fleet fleet);
        void onFleetSplit(Star star, Fleet fleet);
        void onFleetMove(Star star, Fleet fleet);
        void onFleetStanceModified(Star star, Fleet fleet, Fleet.Stance newStance);
    }
}
