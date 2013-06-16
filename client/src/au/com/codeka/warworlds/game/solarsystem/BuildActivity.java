package au.com.codeka.warworlds.game.solarsystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import au.com.codeka.RomanNumeralFormatter;
import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.Design;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.TabFragmentFragment;
import au.com.codeka.warworlds.TabManager;
import au.com.codeka.warworlds.ctrl.BuildQueueList;
import au.com.codeka.warworlds.ctrl.BuildingsList;
import au.com.codeka.warworlds.game.BuildAccelerateDialog;
import au.com.codeka.warworlds.game.BuildStopConfirmDialog;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.PlanetImageManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarSummary;

/**
 * When you click "Build" shows you the list of buildings/ships that are/can be built by your
 * colony. You can swipe left/right to switch between your colonies in this star.
 */
public class BuildActivity extends BaseActivity implements StarManager.StarFetchedHandler {
    private Star mStar;
    private List<Colony> mColonies;
    private ViewPager mViewPager;
    private ColonyPagerAdapter mColonyPagerAdapter;
    private Colony mInitialColony;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.solarsystem_build);

        mColonyPagerAdapter = new ColonyPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mColonyPagerAdapter); 
    }

    @Override
    public void onResume() {
        super.onResume();
        ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
            @Override
            public void onHelloComplete(boolean success, ServerGreeting greeting) {
                Bundle extras = getIntent().getExtras();
                String starKey = extras.getString("au.com.codeka.warworlds.StarKey");
                mInitialColony = (Colony) extras.getParcelable("au.com.codeka.warworlds.Colony");

                StarManager.getInstance().requestStar(BuildActivity.this, starKey, false, BuildActivity.this);
                StarManager.getInstance().addStarUpdatedListener(starKey, BuildActivity.this);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        StarManager.getInstance().removeStarUpdatedListener(this);
    }

    /**
     * Called when our star is refreshed/updated. We want to reload the current tab.
     */
    @Override
    public void onStarFetched(Star s) {
        if (mStar == null || mStar.getKey().equals(s.getKey())) {
            boolean dataSetChanged = (mStar == null);

            mStar = s;
            mColonies = new ArrayList<Colony>();
            MyEmpire myEmpire = EmpireManager.i.getEmpire();
            for (BaseColony c : mStar.getColonies()) {
                if (c.getEmpireKey() != null && c.getEmpireKey().equals(myEmpire.getKey())) {
                    mColonies.add((Colony) c);
                }
            }
            Collections.sort(mColonies, new Comparator<Colony>() {
                @Override
                public int compare(Colony lhs, Colony rhs) {
                    return lhs.getPlanetIndex() - rhs.getPlanetIndex();
                }
            });

            if (mInitialColony != null) {
                int colonyIndex = 0;
                for (Colony colony : mColonies) {
                    if (colony.getKey().equals(mInitialColony.getKey())) {
                        break;
                    }
                    colonyIndex ++;
                }

                mViewPager.setCurrentItem(colonyIndex);
                mInitialColony = null;
            }

            if (dataSetChanged) {
                mColonyPagerAdapter.notifyDataSetChanged();
            }
        }
    }

    private void refreshColonyDetails(Colony colony) {
        ImageView planetIcon = (ImageView) findViewById(R.id.planet_icon);
        Planet planet = (Planet) mStar.getPlanets()[colony.getPlanetIndex() - 1];
        Sprite planetSprite = PlanetImageManager.getInstance().getSprite(this, planet);
        planetIcon.setImageDrawable(new SpriteDrawable(planetSprite));

        TextView planetName = (TextView) findViewById(R.id.planet_name);
        planetName.setText(String.format(Locale.ENGLISH, "%s %s",
                mStar.getName(), RomanNumeralFormatter.format(colony.getPlanetIndex())));

        TextView buildQueueDescription = (TextView) findViewById(R.id.build_queue_description);
        int buildQueueLength = 0;
        for (BaseBuildRequest br : mStar.getBuildRequests()) {
            if (br.getColonyKey().equals(colony.getKey())) {
                buildQueueLength ++;
            }
        }
        if (buildQueueLength == 0) {
            buildQueueDescription.setText("Build queue: idle");
        } else {
            buildQueueDescription.setText(String.format(Locale.ENGLISH, "Build queue: %d", buildQueueLength));
        }
    }

    public static class BuildFragment extends TabFragmentFragment {
        @Override
        protected void createTabs() {
            BuildActivity activity = (BuildActivity) getActivity();
            Bundle args = getArguments();

            getTabManager().addTab(activity, new TabInfo(this, "Buildings", BuildingsFragment.class, args));
            getTabManager().addTab(activity, new TabInfo(this, "Ships", ShipsFragment.class, args));
            getTabManager().addTab(activity, new TabInfo(this, "Queue", QueueFragment.class, args));
        }
    }

    public class ColonyPagerAdapter extends FragmentStatePagerAdapter {
        public ColonyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment = new BuildFragment();
            Bundle args = new Bundle();
            args.putString("au.com.codeka.warworlds.ColonyKey", mColonies.get(i).getKey());
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            if (mColonies == null) {
                return 0;
            }

            return mColonies.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "Colony " + (position + 1);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            refreshColonyDetails(mColonies.get(position));
        }
    }

    public static class BaseTabFragment extends Fragment {
        private String mColonyKey;

        protected Colony getColony() {
            if (mColonyKey == null) {
                Bundle args = getArguments();
                mColonyKey = args.getString("au.com.codeka.warworlds.ColonyKey");
            }

            Star star = ((BuildActivity) getActivity()).mStar;
            if (star.getColonies() == null) {
                return null;
            }

            for (BaseColony baseColony : star.getColonies()) {
                if (baseColony.getKey().equals(mColonyKey)) {
                    return (Colony) baseColony;
                }
            }

            return null;
        }
    }

    public static class BuildingsFragment extends BaseTabFragment {
        private BuildingsList mBuildingsList;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.solarsystem_build_buildings_tab, container, false);
            final Star star = ((BuildActivity) getActivity()).mStar;
            final Colony colony = getColony();

            mBuildingsList = (BuildingsList) v.findViewById(R.id.building_list);
            if (colony != null) {
                mBuildingsList.setColony(star, colony);
            }

            mBuildingsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    BuildingsList.Entry entry = mBuildingsList.getItem(position);
                    int buildQueueSize = 0;
                    BuildActivity activity = (BuildActivity) getActivity();
                    for (BaseBuildRequest br : activity.mStar.getBuildRequests()) {
                        if (br.getColonyKey().equals(colony.getKey())) {
                            buildQueueSize ++;
                        }
                    }

                    if (entry.design != null) {
                        BuildConfirmDialog dialog = new BuildConfirmDialog();
                        dialog.setup(entry.design, colony, buildQueueSize);
                        dialog.show(getActivity().getSupportFragmentManager(), "");
                    } else if (entry.building != null) {
                        BuildConfirmDialog dialog = new BuildConfirmDialog();
                        dialog.setup(entry.building, colony, buildQueueSize);
                        dialog.show(getActivity().getSupportFragmentManager(), "");
                    }
                }
            });

            return v;
        }
    }

    public static class ShipsFragment extends BaseTabFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.solarsystem_build_ships_tab, container, false);

            final Colony colony = getColony();

            final ShipDesignListAdapter adapter = new ShipDesignListAdapter();
            adapter.setDesigns(DesignManager.i.getDesigns(DesignKind.SHIP));

            ListView availableDesignsList = (ListView) v.findViewById(R.id.ship_list);
            availableDesignsList.setAdapter(adapter);
            availableDesignsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    int buildQueueSize = 0;
                    BuildActivity activity = (BuildActivity) getActivity();
                    for (BaseBuildRequest br : activity.mStar.getBuildRequests()) {
                        if (br.getColonyKey().equals(colony.getKey())) {
                            buildQueueSize ++;
                        }
                    }
                    ShipDesign design = (ShipDesign) adapter.getItem(position);
                    if (design == null) {
                        // not sure why this would ever happen?
                        return;
                    }

                    BuildConfirmDialog dialog = new BuildConfirmDialog();
                    dialog.setup(design, colony, buildQueueSize);
                    dialog.show(getActivity().getSupportFragmentManager(), "");
                }
            });

            return v;
        }

        /**
         * This adapter is used to populate the list of ship designs in our view.
         */
        private class ShipDesignListAdapter extends BaseAdapter {
            private List<ShipDesign> mDesigns;

            public void setDesigns(Map<String, Design> designs) {
                mDesigns = new ArrayList<ShipDesign>();
                for (Design d : designs.values()) {
                    mDesigns.add((ShipDesign) d);
                }
                notifyDataSetChanged();
            }

            @Override
            public int getCount() {
                if (mDesigns == null)
                    return 0;
                return mDesigns.size();
            }

            @Override
            public Object getItem(int position) {
                if (mDesigns == null)
                    return null;
                return mDesigns.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = convertView;
                if (view == null) {
                    LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService
                            (Context.LAYOUT_INFLATER_SERVICE);
                    view = inflater.inflate(R.layout.solarsystem_buildings_design, parent, false);
                }

                ImageView icon = (ImageView) view.findViewById(R.id.building_icon);
                TextView row1 = (TextView) view.findViewById(R.id.building_row1);
                TextView row2 = (TextView) view.findViewById(R.id.building_row2);
                TextView row3 = (TextView) view.findViewById(R.id.building_row3);
                view.findViewById(R.id.building_progress).setVisibility(View.GONE);
                view.findViewById(R.id.building_level).setVisibility(View.GONE);
                view.findViewById(R.id.building_level_label).setVisibility(View.GONE);

                ShipDesign design = mDesigns.get(position);

                icon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));

                row1.setText(design.getDisplayName());
                row2.setText(String.format("%.2f hours",
                        (float) design.getBuildCost().getTimeInSeconds() / 3600.0f));

                String required = design.getDependenciesList(getColony());
                row3.setText(Html.fromHtml(required));

                return view;
            }
        }
    }

    public static class QueueFragment extends BaseTabFragment implements TabManager.Reloadable {
        private BuildQueueList mBuildQueueList;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.solarsystem_build_queue_tab, container, false);

            final Star star = ((BuildActivity) getActivity()).mStar;
            final Colony colony = getColony();
            if (star == null)
                return inflater.inflate(R.layout.solarsystem_build_loading_tab, container, false);

            mBuildQueueList = (BuildQueueList) v.findViewById(R.id.build_queue);
            mBuildQueueList.setShowStars(false);
            mBuildQueueList.refresh(star, colony);

            mBuildQueueList.setBuildQueueActionListener(new BuildQueueList.BuildQueueActionListener() {
                @Override
                public void onAccelerateClick(StarSummary star, BuildRequest buildRequest) {
                    BuildAccelerateDialog dialog = new BuildAccelerateDialog();
                    dialog.setBuildRequest(star, buildRequest);
                    dialog.show(getActivity().getSupportFragmentManager(), "");
                }

                @Override
                public void onStopClick(StarSummary star, BuildRequest buildRequest) {
                    BuildStopConfirmDialog dialog = new BuildStopConfirmDialog();
                    dialog.setBuildRequest(star, buildRequest);
                    dialog.show(getActivity().getSupportFragmentManager(), "");
                }
            });

            return v;
        }

        @Override
        public void reloadTab() {
            if (mBuildQueueList != null) {
                mBuildQueueList.refreshSelection();
            }
        }
    }
}
