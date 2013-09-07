package au.com.codeka.warworlds.game;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.model.BuildRequest;
import au.com.codeka.common.model.Star;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.StarManager;

public class BuildStopConfirmDialog extends DialogFragment {
    private BuildRequest mBuildRequest;
    private Star mStar;

    public void setBuildRequest(Star star, BuildRequest buildRequest) {
        mBuildRequest = buildRequest;
        mStar = star;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String msg =
                "Are you <em>sure</em> you want to stop this build? Stopping the " +
                "build will not return any resources to you, though it will free " +
                "up your population to work on other constructions.";

           return new StyledDialog.Builder(getActivity())
                                  .setTitle("Stop Build")
                                  .setMessage(Html.fromHtml(msg))
                                  .setPositiveButton("Stop Build", new DialogInterface.OnClickListener() {
                                      @Override
                                      public void onClick(DialogInterface dialog, int which) {
                                          stopBuild();
                                      }
                                  })
                                  .setNegativeButton("Cancel", null)
                                  .create();
    }

    private void stopBuild() {
        final StyledDialog dialog = ((StyledDialog) getDialog());
        dialog.setCloseable(false);

        new BackgroundRunner<Boolean>() {
            @Override
            protected Boolean doInBackground() {
                String url = "stars/"+mStar.key+"/build/"+mBuildRequest.key+"/stop";

                try {
                    ApiClient.postProtoBuf(url, null);
                    return true;
                } catch (ApiException e) {
                    //log.error("Error issuing build request", e);
                    return false;
                }
            }
            @Override
            protected void onComplete(Boolean success) {
                EmpireManager.i.refreshEmpire();
                StarManager.i.refreshStar(mStar.key);
                dismiss();
            }
        }.execute();
    }
}
