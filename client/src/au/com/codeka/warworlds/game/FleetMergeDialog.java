package au.com.codeka.warworlds.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.model.Fleet;
import au.com.codeka.common.model.FleetOrder;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.ctrl.FleetList;
import au.com.codeka.warworlds.model.StarManager;

public class FleetMergeDialog extends DialogFragment {
    private Fleet mFleet;
    private List<Fleet> mPotentialMergeTargets;
    private View mView;
    private Fleet mSelectedFleet;

    public FleetMergeDialog() {
    }

    public void setup(Fleet fleet, List<Fleet> potentialFleets) {
        mPotentialMergeTargets = potentialFleets;
        mFleet = fleet;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.fleet_merge_dlg, null);

        final ListView fleetList = (ListView) mView.findViewById(R.id.ship_list);
        final TextView note = (TextView) mView.findViewById(R.id.note);
        boolean isError = false;

        final FleetListAdapter adapter = new FleetListAdapter();
        fleetList.setAdapter(adapter);

        if (!mFleet.state.equals(Fleet.FLEET_STATE.IDLE)) {
            note.setText("You cannot merge a fleet unless it is Idle.");
            isError = true;
        } else {
            ArrayList<Fleet> otherFleets = new ArrayList<Fleet>();
            for (Fleet f : mPotentialMergeTargets) {
                if (f.key.equals(mFleet.key)) {
                    continue;
                }
                if (!f.star_key.equals(mFleet.star_key)) {
                    continue;
                }
                if (!f.empire_key.equals(mFleet.empire_key)) {
                    continue;
                }
                if (!f.design_id.equals(mFleet.design_id)) {
                    continue;
                }
                if (!f.state.equals(Fleet.FLEET_STATE.IDLE)) {
                    continue;
                }
                otherFleets.add(f);
            }

            if (otherFleets.isEmpty()) {
                note.setText("No other fleet is suitable for merging.");
                isError = true;
            } else {
                adapter.setFleets(otherFleets);
            }
        }

        StyledDialog.Builder b = new StyledDialog.Builder(getActivity());
        b.setView(mView);
        b.setTitle("Merge Fleet");

        if (!isError) {
            b.setPositiveButton("Merge", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onMergeClick();
                }
            });
        }

        b.setNegativeButton("Cancel", null);

        final StyledDialog dialog = b.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                dialog.getPositiveButton().setEnabled(false);
            }
        });

        fleetList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Fleet f = (Fleet) adapter.getItem(position);
                mSelectedFleet = f;
                adapter.notifyDataSetChanged();
                dialog.getPositiveButton().setEnabled(true);
            }
        });

        return dialog;
    }

    private void onMergeClick() {
        if (mSelectedFleet == null) {
            return;
        }

        final StyledDialog dialog = ((StyledDialog) getDialog());
        dialog.setCloseable(false);
        dismiss();

        new BackgroundRunner<Boolean>() {
            @Override
            protected Boolean doInBackground() {
                String url = String.format("stars/%s/fleets/%s/orders",
                                           mFleet.star_key,
                                           mFleet.key);
                FleetOrder fleetOrder = new FleetOrder.Builder()
                               .order(FleetOrder.FLEET_ORDER.MERGE)
                               .merge_fleet_key(mSelectedFleet.key)
                               .build();

                try {
                    return ApiClient.postProtoBuf(url, fleetOrder);
                } catch (ApiException e) {
                    // TODO: do something..?
                    return false;
                }
            }

            @Override
            protected void onComplete(Boolean success) {
                // the star this fleet is attached to needs to be refreshed...
                StarManager.i.refreshStar(mFleet.star_key);
            }

        }.execute();
    }

    private class FleetListAdapter extends BaseAdapter {
        private ArrayList<Fleet> mFleets;
        private Context mContext;

        public FleetListAdapter() {
            mContext = getActivity();
        }

        public void setFleets(List<Fleet> fleets) {
            mFleets = new ArrayList<Fleet>(fleets);

            Collections.sort(mFleets, new Comparator<Fleet>() {
                @Override
                public int compare(Fleet lhs, Fleet rhs) {
                    // by definition, they'll all be the same design so just
                    // sort based on number of ships
                    return (int)(rhs.num_ships - lhs.num_ships);
                }
            });

            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (mFleets == null)
                return 0;
            return mFleets.size();
        }

        @Override
        public Object getItem(int position) {
            if (mFleets == null)
                return null;
            return mFleets.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Fleet fleet = mFleets.get(position);
            View view = convertView;

            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService
                        (Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.fleet_list_row, null);
            }

            FleetList.populateFleetRow(mContext, null, view, fleet);

            if (mSelectedFleet != null && mSelectedFleet.key.equals(fleet.key)) {
                view.setBackgroundColor(0xff0c6476);
            } else {
                view.setBackgroundColor(0xff000000);
            }

            return view;
        }
    }
}
