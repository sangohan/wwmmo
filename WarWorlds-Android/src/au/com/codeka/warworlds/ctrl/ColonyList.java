package au.com.codeka.warworlds.ctrl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.text.Html;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import au.com.codeka.Cash;
import au.com.codeka.RomanNumeralFormatter;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.ImageManager;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.PlanetImageManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;

public class ColonyList extends FrameLayout {
    private Context mContext;
    private Map<String, Star> mStars;
    private Colony mSelectedColony;
    private boolean mIsInitialized;
    private ColonyListAdapter mColonyListAdapter;
    private ColonyActionHandler mColonyActionListener;

    // collection of stars we're currently simulating
    private static TreeSet<String> sSimulatingStars = new TreeSet<String>();

    public ColonyList(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        View child = inflate(context, R.layout.colony_list_ctrl, null);
        this.addView(child);
    }

    public void refresh(final List<Colony> colonies, Map<String, Star> stars) {
        mStars = stars;

        initialize();

        // if we had a colony selected, make sure we still have the same
        // colony selected after we refresh
        if (mSelectedColony != null) {
            Colony selectedColony = mSelectedColony;
            mSelectedColony = null;

            for (Colony c : colonies) {
                if (c.getKey().equals(selectedColony.getKey())) {
                    mSelectedColony = c;
                    break;
                }
            }
        }

        refreshSelectedColony();
        mColonyListAdapter.setColonies(stars, colonies);
    }

    @Override
    public void onAttachedToWindow() {
    }

    @Override
    public void onDetachedFromWindow() {
    }

    public void setOnColonyActionListener(ColonyActionHandler listener) {
        mColonyActionListener = listener;
    }

    private void initialize() {
        if (mIsInitialized) {
            return;
        }
        mIsInitialized = true;

        mColonyListAdapter = new ColonyListAdapter();
        final ListView colonyList = (ListView) findViewById(R.id.colonies);
        colonyList.setAdapter(mColonyListAdapter);

        colonyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                mSelectedColony = mColonyListAdapter.getColonyAtPosition(position);
                mColonyListAdapter.notifyDataSetChanged();
                refreshSelectedColony();
            }
        });

        final Button viewBtn = (Button) findViewById(R.id.view_btn);
        viewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedColony != null && mColonyActionListener != null) {
                    Star star = mStars.get(mSelectedColony.getStarKey());
                    mColonyActionListener.onViewColony(star, mSelectedColony);
                }
            }
        });

        final Button collectBtn = (Button) findViewById(R.id.collect_btn);
        collectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mColonyActionListener.onCollectTaxes();
            }
        });
    }

    private void refreshSelectedColony() {
        final TextView colonyInfo = (TextView) findViewById(R.id.colony_info);

        if (mSelectedColony == null) {
            colonyInfo.setText("");
        } else {
            // the colony might've changed so update it first
            for(Colony c : mColonyListAdapter.getColonies()) {
                if (c.getKey().equals(mSelectedColony.getKey())) {
                    mSelectedColony = c;
                }
            }

            String fmt = mContext.getString(R.string.colony_overview_format);
            String html = String.format(fmt,
                    (int) mSelectedColony.getPopulation(),
                    mSelectedColony.getFarmingFocus(),
                    mSelectedColony.getMiningFocus(),
                    mSelectedColony.getConstructionFocus()
                );
            colonyInfo.setText(Html.fromHtml(html));
        }
    }

    public static void populateColonyListRow(Context context, View view, Colony colony, Star star) {
        Planet planet = (Planet) star.getPlanets()[colony.getPlanetIndex() - 1];

        ImageView starIcon = (ImageView) view.findViewById(R.id.star_icon);
        ImageView planetIcon = (ImageView) view.findViewById(R.id.planet_icon);
        TextView colonyName = (TextView) view.findViewById(R.id.colony_name);
        TextView colonySummary = (TextView) view.findViewById(R.id.colony_summary);
        TextView uncollectedTaxes = (TextView) view.findViewById(R.id.colony_taxes);

        int imageSize = (int)(star.getSize() * star.getStarType().getImageScale() * 2);
        Sprite sprite = StarImageManager.getInstance().getSprite(context, star, imageSize);
        starIcon.setImageDrawable(new SpriteDrawable(sprite));

        sprite = PlanetImageManager.getInstance().getSprite(context, planet);
        planetIcon.setImageDrawable(new SpriteDrawable(sprite));

        colonyName.setText(String.format("%s %s", star.getName(), RomanNumeralFormatter.format(planet.getIndex())));

        if (star.getLastSimulation().compareTo(DateTime.now(DateTimeZone.UTC).minusMinutes(5)) < 0) {
            // if the star hasn't been simulated for > 5 minutes, schedule a simulation now and
            // just display ??? for the various parameters
            colonySummary.setText("Pop: ?");
            uncollectedTaxes.setText("Taxes: ?");
            scheduleSimulate(star);
        } else {
            colonySummary.setText(String.format("Pop: %d", (int) colony.getPopulation()));
            uncollectedTaxes.setText(String.format("Taxes: %s", Cash.format(colony.getUncollectedTaxes())));
        }
    }

    private static void scheduleSimulate(final Star star) {
        synchronized(sSimulatingStars) {
            if (sSimulatingStars.contains(star.getKey())) {
                return;
            }
            sSimulatingStars.add(star.getKey());
        }

        new AsyncTask<Void, Void, Star>() {
            @Override
            protected Star doInBackground(Void... params) {
                Simulation sim = new Simulation();
                sim.simulate(star);
                return star;
            }

            @Override
            protected void onPostExecute(Star star) {
                StarManager.getInstance().fireStarUpdated(star);
                sSimulatingStars.remove(star.getKey());
            }
        }.execute();
    }

    /**
     * This adapter is used to populate the list of colonies that we're looking at.
     */
    private class ColonyListAdapter extends BaseAdapter {
        private ArrayList<Colony> mColonies;
        private Map<String, Star> mStars;

        public ColonyListAdapter() {
            // whenever a new star/planet bitmap is generated, redraw the list
            StarImageManager.getInstance().addBitmapGeneratedListener(
                    new ImageManager.BitmapGeneratedListener() {
                @Override
                public void onBitmapGenerated(String key, Bitmap bmp) {
                    notifyDataSetChanged();
                }
            });
            PlanetImageManager.getInstance().addBitmapGeneratedListener(
                    new ImageManager.BitmapGeneratedListener() {
                @Override
                public void onBitmapGenerated(String key, Bitmap bmp) {
                    notifyDataSetChanged();
                }
            });
            StarManager.getInstance().addStarUpdatedListener(null, new StarManager.StarFetchedHandler() {
                @Override
                public void onStarFetched(Star s) {
                    // if a star is updated, we'll want to refresh our colony list because the
                    // colony inside it might've changed too...
                    for (BaseColony starColony : s.getColonies()) {
                        for (int i = 0; i < mColonies.size(); i++) {
                            if (mColonies.get(i).getKey().equals(starColony.getKey())) {
                                mColonies.set(i, (Colony) starColony);
                                break;
                            }
                        }
                    }

                    notifyDataSetChanged();
                    refreshSelectedColony();
                }
            });
        }

        /**
         * Sets the list of fleets that we'll be displaying.
         */
        public void setColonies(Map<String, Star> stars, List<Colony> colonies) {
            mColonies = new ArrayList<Colony>(colonies);
            mStars = stars;

            Collections.sort(mColonies, new Comparator<Colony>() {
                @Override
                public int compare(Colony lhs, Colony rhs) {
                    // sort by star, then by planet index
                    if (!lhs.getStarKey().equals(rhs.getStarKey())) {
                        Star lhsStar = mStars.get(lhs.getStarKey());
                        Star rhsStar = mStars.get(rhs.getStarKey());
                        return lhsStar.getName().compareTo(rhsStar.getName());
                    } else {
                        return lhs.getPlanetIndex() - rhs.getPlanetIndex();
                    }
                }
            });

            notifyDataSetChanged();
        }

        public List<Colony> getColonies() {
            return mColonies;
        }

        public Colony getColonyAtPosition(int position) {
            if (mColonies == null)
                return null;
            return mColonies.get(position);
        }

        @Override
        public int getCount() {
            if (mColonies == null)
                return 0;
            return mColonies.size();
        }

        @Override
        public Object getItem(int position) {
            return getColonyAtPosition(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService
                        (Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.colony_list_row, null);
            }

            Colony colony = mColonies.get(position);
            Star star = mStars.get(colony.getStarKey());
            populateColonyListRow(mContext, view, colony, star);

            if (mSelectedColony != null && mSelectedColony.getKey().equals(colony.getKey())) {
                view.setBackgroundColor(0xff0c6476);
            } else {
                view.setBackgroundColor(0xff000000);
            }

            return view;
        }
    }

    public interface ColonyActionHandler {
        void onViewColony(Star star, Colony colony);
        void onCollectTaxes();
    }
}
