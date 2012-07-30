package au.com.codeka.warworlds.game.solarsystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.joda.time.Duration;
import org.joda.time.Period;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import au.com.codeka.warworlds.DialogManager;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.TabFragmentActivity;
import au.com.codeka.warworlds.ctrl.HorizontalSeparator;
import au.com.codeka.warworlds.model.BuildQueueManager;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.Building;
import au.com.codeka.warworlds.model.BuildingDesign;
import au.com.codeka.warworlds.model.BuildingDesignManager;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Design;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.ShipDesign;
import au.com.codeka.warworlds.model.ShipDesignManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

/**
 * When you click "Build" shows you the list of buildings/ships that are/can be built by your
 * colony.
 */
public class BuildActivity extends TabFragmentActivity implements StarManager.StarFetchedHandler {
    private Star mStar;
    private Colony mColony;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addTab("Buildings", BuildingsFragment.class, null);
        addTab("Ships", ShipsFragment.class, null);
        addTab("Queue", QueueFragment.class, null);
    }

    @Override
    public void onResume() {
        Bundle extras = this.getIntent().getExtras();
        String starKey = extras.getString("au.com.codeka.warworlds.StarKey");
        mColony = (Colony) extras.getParcelable("au.com.codeka.warworlds.Colony");

        StarManager.getInstance().requestStar(starKey, false, this);
        StarManager.getInstance().addStarUpdatedListener(starKey, this);

        

        super.onResume();
    }

    @Override
    public void onPause() {
        StarManager.getInstance().removeStarUpdatedListener(this);

        super.onPause();
    }

    /**
     * Called when our star is refreshed/updated. We want to reload the current tab.
     */
    @Override
    public void onStarFetched(Star s) {
        mStar = s;
        reloadTab();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog d = DialogManager.getInstance().onCreateDialog(this, id);
        if (d == null) {
            d = super.onCreateDialog(id);
        }

        return d;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog d, Bundle args) {
        DialogManager.getInstance().onPrepareDialog(this, id, d, args);
        super.onPrepareDialog(id, d, args);
    }

    public static class BuildingsFragment extends Fragment {
        private BuildingListAdapter mBuildingListAdapter;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.solarsystem_build_buildings_tab, null);

            final Colony colony = ((BuildActivity) getActivity()).mColony;

            mBuildingListAdapter = new BuildingListAdapter();
            if (colony != null) {
                mBuildingListAdapter.setBuildings(colony.getBuildings());
                mBuildingListAdapter.setDesigns(BuildingDesignManager.getInstance().getDesigns());
            }

            ListView buildingsList = (ListView) v.findViewById(R.id.building_list);
            buildingsList.setAdapter(mBuildingListAdapter);

            // make sure we're aware of any changes to the designs
            BuildingDesignManager.getInstance().addDesignsChangedListener(new DesignManager.DesignsChangedListener() {
                @Override
                public void onDesignsChanged() {
                    mBuildingListAdapter.setDesigns(BuildingDesignManager.getInstance().getDesigns());
                    if (colony != null) {
                        mBuildingListAdapter.setBuildings(colony.getBuildings());
                    }
                }
            });

            buildingsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Bundle args = new Bundle();

                    Object o = mBuildingListAdapter.getItem(position);
                    if (o instanceof BuildingDesign) {
                        BuildingDesign design = (BuildingDesign) o;
                        args.putString("au.com.codeka.warworlds.DesignID", design.getID());
                        args.putInt("au.com.codeka.warworlds.DesignKind", design.getDesignKind().getValue());
                        args.putParcelable("au.com.codeka.warworlds.Colony", colony);

                        DialogManager.getInstance().show(getActivity(),
                                                         BuildConfirmDialog.class,
                                                         args);
                    } else if (o instanceof Building) {
                        // TODO: upgrade building
                    }
                }
            });

            return v;
        }

        /**
         * This adapter is used to populate a list of buildings in a list view.
         */
        private class BuildingListAdapter extends BaseAdapter {
            private List<Building> mBuildings;
            private List<BuildingDesign> mDesigns;

            private static final int HEADING_TYPE = 0;
            private static final int EXISTING_BUILDING_TYPE = 1;
            private static final int NEW_BUILDING_TYPE = 2;

            public void setBuildings(List<Building> buildings) {
                if (buildings == null) {
                    buildings = new ArrayList<Building>();
                }

                mBuildings = buildings;
                notifyDataSetChanged();
            }

            public void setDesigns(Map<String, Design> designs) {
                mDesigns = new ArrayList<BuildingDesign>();
                for (Design d : designs.values()) {
                    mDesigns.add((BuildingDesign) d);
                }
                notifyDataSetChanged();
            }

            /**
             * We have three types of items, the "headings", the list of existing buildings
             * and the list of building designs.
             */
            @Override
            public int getViewTypeCount() {
                return 3;
            }

            @Override
            public int getItemViewType(int position) {
                if (mBuildings == null || mDesigns == null)
                    return 0;

                if (position == 0 || position == (mBuildings.size() + 1)) {
                    return HEADING_TYPE;
                } else if (position <= mBuildings.size()) {
                    return EXISTING_BUILDING_TYPE;
                } else {
                    return NEW_BUILDING_TYPE;
                }
            }

            @Override
            public boolean isEnabled(int position) {
                if (getItemViewType(position) == HEADING_TYPE) {
                    return false;
                }

                return true;
            }

            @Override
            public int getCount() {
                if (mBuildings == null || mDesigns == null)
                    return 0;

                return mBuildings.size() + mDesigns.size() + 2;
            }

            @Override
            public Object getItem(int position) {
                if (mBuildings == null)
                    return null;

                if (position == 0 || position == (mBuildings.size() + 1)) {
                    return null;
                } else if (position <= mBuildings.size()) {
                    return mBuildings.get(position - 1);
                } else {
                    return mDesigns.get(position - mBuildings.size() - 2);
                }
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

                    int viewType = getItemViewType(position);
                    if (viewType == HEADING_TYPE) {
                        view = new HorizontalSeparator(getActivity());
                    } else {
                        view = inflater.inflate(R.layout.solarsystem_buildings_design, null);
                    }
                }

                if (position == 0) {
                    HorizontalSeparator hs = (HorizontalSeparator) view;
                    hs.setText("Existing Buildings");
                } else if (position <= mBuildings.size()) {
                    ImageView icon = (ImageView) view.findViewById(R.id.building_icon);
                    TextView row1 = (TextView) view.findViewById(R.id.building_row1);
                    TextView row2 = (TextView) view.findViewById(R.id.building_row2);
                    TextView row3 = (TextView) view.findViewById(R.id.building_row3);
                    ProgressBar progress = (ProgressBar) view.findViewById(R.id.building_progress);

                    Building building = mBuildings.get(position - 1);
                    BuildingDesign design = building.getDesign();

                    Bitmap bm = BuildingDesignManager.getInstance().getDesignIcon(design);
                    if (bm != null) {
                        icon.setImageBitmap(bm);
                    } else {
                        icon.setImageBitmap(null);
                    }

                    row1.setText(design.getName());
                    row2.setText("Level 1");

                    row3.setVisibility(View.VISIBLE);
                    progress.setVisibility(View.GONE);
                    row3.setText(String.format("Upgrade: $ %d, %.2f hours", design.getBuildCost(),
                            (float) design.getBuildTimeSeconds() / 3600.0f));
                } else if (position == mBuildings.size() + 1) {
                    HorizontalSeparator hs = (HorizontalSeparator) view;
                    hs.setText("New Buildings");
                } else {
                    ImageView icon = (ImageView) view.findViewById(R.id.building_icon);
                    TextView row1 = (TextView) view.findViewById(R.id.building_row1);
                    TextView row2 = (TextView) view.findViewById(R.id.building_row2);
                    TextView row3 = (TextView) view.findViewById(R.id.building_row3);
                    ProgressBar progress = (ProgressBar) view.findViewById(R.id.building_progress);
                    progress.setVisibility(View.GONE);

                    BuildingDesign design = mDesigns.get(position - mBuildings.size() - 2);

                    Bitmap bm = BuildingDesignManager.getInstance().getDesignIcon(design);
                    if (bm != null) {
                        icon.setImageBitmap(bm);
                    } else {
                        icon.setImageBitmap(null);
                    }

                    row1.setText(design.getName());
                    row2.setText(String.format("$ %d - %.2f hours", design.getBuildCost(),
                            (float) design.getBuildTimeSeconds() / 3600.0f));
                    row3.setText("Required: none");
                }

                return view;
            }
        }
    }

    public static class ShipsFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.solarsystem_build_ships_tab, null);

            final Colony colony = ((BuildActivity) getActivity()).mColony;

            final ShipDesignListAdapter adapter = new ShipDesignListAdapter();
            adapter.setDesigns(ShipDesignManager.getInstance().getDesigns());

            ListView availableDesignsList = (ListView) v.findViewById(R.id.ship_list);
            availableDesignsList.setAdapter(adapter);
            availableDesignsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Bundle args = new Bundle();
                    ShipDesign design = (ShipDesign) adapter.getItem(position);
                    args.putString("au.com.codeka.warworlds.DesignID", design.getID());
                    args.putInt("au.com.codeka.warworlds.DesignKind", design.getDesignKind().getValue());
                    args.putParcelable("au.com.codeka.warworlds.Colony", colony);

                    DialogManager.getInstance().show(getActivity(), BuildConfirmDialog.class, args);
                }
            });

            // make sure we're aware of any changes to the designs
            ShipDesignManager.getInstance().addDesignsChangedListener(new DesignManager.DesignsChangedListener() {
                @Override
                public void onDesignsChanged() {
                    adapter.setDesigns(ShipDesignManager.getInstance().getDesigns());
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
                    view = inflater.inflate(R.layout.solarsystem_buildings_design, null);
                }

                ImageView icon = (ImageView) view.findViewById(R.id.building_icon);
                TextView row1 = (TextView) view.findViewById(R.id.building_row1);
                TextView row2 = (TextView) view.findViewById(R.id.building_row2);
                TextView row3 = (TextView) view.findViewById(R.id.building_row3);
                ProgressBar progress = (ProgressBar) view.findViewById(R.id.building_progress);
                progress.setVisibility(View.GONE);

                ShipDesign design = mDesigns.get(position);

                Bitmap bm = ShipDesignManager.getInstance().getDesignIcon(design);
                if (bm != null) {
                    icon.setImageBitmap(bm);
                } else {
                    icon.setImageBitmap(null);
                }

                row1.setText(design.getName());
                row2.setText(String.format("$ %d - %.2f hours", design.getBuildCost(),
                        (float) design.getBuildTimeSeconds() / 3600.0f));
                row3.setText("Required: none");

                return view;
            }
        }
    }

    public static class QueueFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.solarsystem_build_queue_tab, null);

            final Star star = ((BuildActivity) getActivity()).mStar;
            final Colony colony = ((BuildActivity) getActivity()).mColony;
            if (star == null)
                return inflater.inflate(R.layout.solarsystem_build_loading_tab, null);

            final BuildQueueListAdapter adapter = new BuildQueueListAdapter();
            adapter.setBuildQueue(star, colony);

            ListView buildQueueList = (ListView) v.findViewById(R.id.build_queue);
            buildQueueList.setAdapter(adapter);

            // make sure we're aware of any changes to the designs
            BuildingDesignManager.getInstance().addDesignsChangedListener(new BuildingDesignManager.DesignsChangedListener() {
                @Override
                public void onDesignsChanged() {
                    adapter.setBuildQueue(star, colony);
                }
            });
            ShipDesignManager.getInstance().addDesignsChangedListener(new ShipDesignManager.DesignsChangedListener() {
                @Override
                public void onDesignsChanged() {
                    adapter.setBuildQueue(star, colony);
                }
            });

            // make sure we're aware of changes to the build queue
            BuildQueueManager.getInstance().addBuildQueueUpdatedListener(new BuildQueueManager.BuildQueueUpdatedListener() {
                @Override
                public void onBuildQueueUpdated(List<BuildRequest> queue) {
                    // TODO: this will be out-of-date...
                    adapter.setBuildQueue(star, colony);
                }
            });

            return v;
        }

        /**
         * This adapter is used to populate the list of buildings that are currently in progress.
         */
        private class BuildQueueListAdapter extends BaseAdapter {
            private List<BuildRequest> mQueue;

            public void setBuildQueue(Star star, Colony colony) {
                mQueue = new ArrayList<BuildRequest>();
                for (BuildRequest buildRequest : star.getBuildRequests()) {
                    if (buildRequest.getColonyKey().equals(colony.getKey())) {
                        mQueue.add(buildRequest);
                    }
                }

                notifyDataSetChanged();
            }

            @Override
            public int getCount() {
                if (mQueue == null)
                    return 0;
                return mQueue.size();
            }

            @Override
            public Object getItem(int position) {
                if (mQueue == null)
                    return null;
                return mQueue.get(position);
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
                    view = inflater.inflate(R.layout.solarsystem_buildings_design, null);
                }

                ImageView icon = (ImageView) view.findViewById(R.id.building_icon);
                TextView row1 = (TextView) view.findViewById(R.id.building_row1);
                TextView row2 = (TextView) view.findViewById(R.id.building_row2);
                TextView row3 = (TextView) view.findViewById(R.id.building_row3);
                ProgressBar progress = (ProgressBar) view.findViewById(R.id.building_progress);

                BuildRequest request = mQueue.get(position);
                DesignManager dm = DesignManager.getInstance(request.getBuildKind());
                Design design = dm.getDesign(request.getDesignName());

                Bitmap bm = dm.getDesignIcon(design);
                if (bm != null) {
                    icon.setImageBitmap(bm);
                } else {
                    icon.setImageBitmap(null);
                }

                row1.setText(design.getName());
                Duration remainingDuration = request.getRemainingTime();
                if (remainingDuration.equals(Duration.ZERO)) {
                    row2.setText(String.format("%d %%, not enough resources to complete.",
                                 (int) request.getPercentComplete()));
                } else {
                    Period remainingPeriod = remainingDuration.toPeriod();
                    row2.setText(String.format("%d %%, %d:%02d left",
                                 (int) request.getPercentComplete(),
                                 remainingPeriod.getHours(), remainingPeriod.getMinutes()));
                }

                row3.setVisibility(View.GONE);
                progress.setVisibility(View.VISIBLE);
                progress.setProgress((int) request.getPercentComplete());

                return view;
            }
        }
    }
}
