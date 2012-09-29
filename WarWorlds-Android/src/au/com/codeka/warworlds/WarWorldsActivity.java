
package au.com.codeka.warworlds;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import warworlds.Warworlds.Hello;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.ctrl.TransparentWebView;
import au.com.codeka.warworlds.game.starfield.StarfieldActivity;
import au.com.codeka.warworlds.model.BuildQueueManager;
import au.com.codeka.warworlds.model.BuildingDesignManager;
import au.com.codeka.warworlds.model.ChatManager;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.ShipDesignManager;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

/**
 * Main activity. Displays the message of the day and lets you select "Start Game", "Options", etc.
 */
public class WarWorldsActivity extends Activity {
    private static Logger log = LoggerFactory.getLogger(WarWorldsActivity.class);
    private Context mContext = this;
    private Button mStartGameButton;
    private TextView mConnectionStatus;
    private Handler mHandler;
    private boolean mNeedHello;
    private List<Colony> mColonies;

    private static final int OPTIONS_DIALOG = 1000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        log.info("WarWorlds activity starting...");

        super.onCreate(savedInstanceState);

        Util.loadProperties(mContext);
        Authenticator.configure(mContext);

        if (Util.isDebug()) {
            enableStrictMode();
        }

        mHandler = new Handler();

        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar
        setHomeScreenContent();
        mNeedHello = true;
    }

    @SuppressLint({ "NewApi" }) // StrictMode doesn't work on < 3.0
    private void enableStrictMode() {
        try {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                      .detectDiskReads()
                      .detectDiskWrites()
                      .detectNetwork()
                      .penaltyLog()
                      .build());
        } catch(Exception e) {
            // ignore errors
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        log.debug("WarWorldsActivity.onResume...");

        SharedPreferences prefs = Util.getSharedPreferences(mContext);
        if (prefs.getString("AccountName", null) == null) {
            log.info("No accountName saved, switching to AccountsActivity");
            startActivity(new Intent(this, AccountsActivity.class));
            return;
        }

        if (mNeedHello) {
            sayHello(0);
            mNeedHello = false;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == OPTIONS_DIALOG) {
            GlobalOptionsDialog dialog = new GlobalOptionsDialog(mContext);
            return dialog;
        }

        return super.onCreateDialog(id);
    }

    /**
     * Says "hello" to the server. Lets it know who we are, fetches the MOTD and if there's
     * no empire registered yet, switches over to the \c EmpireSetupActivity.
     * 
     * @param motd The \c WebView we'll install the MOTD to.
     */
    private void sayHello(final int retries) {
        log.debug("Saying 'hello'...");

        mStartGameButton.setEnabled(false);
        if (retries == 0) {
            mConnectionStatus.setText("Connecting...");
        } else {
            mConnectionStatus.setText(String.format("Retrying (#%d)...", retries+1));
        }

        // if we've saved off the authentication cookie, cool!
        SharedPreferences prefs = Util.getSharedPreferences(mContext);
        final String accountName = prefs.getString("AccountName", null);
        if (accountName == null) {
            // You're not logged in... how did we get this far anyway?
            startActivity(new Intent(this, AccountsActivity.class));
            return;
        }

        new AsyncTask<Void, Void, String>() {
            private boolean mNeedsEmpireSetup;
            private boolean mErrorOccured;

            @Override
            protected String doInBackground(Void... arg0) {
                // re-authenticate and get a new cookie
                String authCookie = Authenticator.authenticate(WarWorldsActivity.this, accountName);
                ApiClient.getCookies().clear();
                ApiClient.getCookies().add(authCookie);
                log.debug("Got auth cookie: "+authCookie);

                // say hello to the server
                String message;
                try {
                    String deviceRegistrationKey = DeviceRegistrar.getDeviceRegistrationKey(mContext);
                    if (deviceRegistrationKey.length() == 0) {
                        mErrorOccured = true;
                        message = "<p>Your device was not registered... for some reason.</p>";
                        return message;
                    }
                    String url = "hello/"+deviceRegistrationKey;
                    Hello hello = ApiClient.putProtoBuf(url, null, Hello.class);
                    if (hello == null) {
                        // Usually this happens on the dev server when we've just cleared the
                        // data store. Not good :-)
                        throw new ApiException("Server Error");
                    }

                    if (hello.hasEmpire()) {
                        mNeedsEmpireSetup = false;
                        EmpireManager.getInstance().setup(
                                MyEmpire.fromProtocolBuffer(hello.getEmpire()));
                    } else {
                        mNeedsEmpireSetup = true;
                    }

                    if (hello.hasRequireC2DmRegister() && hello.getRequireC2DmRegister()) {
                        log.info("TODO: re-register for C2DM...");
                    }

                    ChatManager.getInstance().setup(hello.getChannelToken());

                    mColonies = new ArrayList<Colony>();
                    for (warworlds.Warworlds.Colony c : hello.getColoniesList()) {
                        mColonies.add(Colony.fromProtocolBuffer(c));
                    }

                    message = hello.getMotd().getMessage();
                    mErrorOccured = false;
                } catch(ApiException e) {
                    message = "<p class=\"error\">An error occured talking to the server, check " +
                              "data connection.</p>";
                    mErrorOccured = true;

                    log.error("Error occurred in 'hello'", e);
                }

                return message;
            }

            @Override
            protected void onPostExecute(String result) {
                final TransparentWebView motdView = (TransparentWebView) findViewById(R.id.motd);

                mConnectionStatus.setText("Connected");
                mStartGameButton.setEnabled(true);
                if (mNeedsEmpireSetup) {
                    startActivity(new Intent(mContext, EmpireSetupActivity.class));
                    return;
                } else if (!mErrorOccured) {
                    SpriteManager.getInstance().setup(mContext);
                    BuildingDesignManager.getInstance().setup(mContext);
                    ShipDesignManager.getInstance().setup(mContext);
                    BuildQueueManager.getInstance().setup();

                    motdView.loadHtml("html/motd-template.html", result);
                } else /* mErrorOccured */ {
                    mConnectionStatus.setText("Connection Failed");
                    mStartGameButton.setEnabled(false);

                    // if there's an error, try again in a few seconds
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sayHello(retries+1);
                        }
                    }, 3000);
                }
            }
        }.execute();
    }

    private void setHomeScreenContent() {
        setContentView(R.layout.home);

        View rootView = findViewById(android.R.id.content);
        ActivityBackgroundGenerator.setBackground(rootView);

        mStartGameButton = (Button) findViewById(R.id.start_game_btn);
        mConnectionStatus = (TextView) findViewById(R.id.connection_status);
        final Button logOutButton = (Button) findViewById(R.id.log_out_btn);
        final Button optionsButton = (Button) findViewById(R.id.options_btn);

        logOutButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(mContext, AccountsActivity.class));
            }
        });

        optionsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(OPTIONS_DIALOG);
            }
        });

        mStartGameButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // we'll want to start off near one of your stars. If you only have one, that's
                // easy -- but if you've got lots what then? (TODO)
                String starKey = null;
                for (Colony c : mColonies) {
                    starKey = c.getStarKey();
                }

                final Intent intent = new Intent(mContext, StarfieldActivity.class);
                intent.putExtra("au.com.codeka.warworlds.StarKey", starKey);

                if (starKey != null) {
                    mStartGameButton.setEnabled(false);
                    StarManager.getInstance().requestStar(starKey, false,
                            new StarManager.StarFetchedHandler() {
                        @Override
                        public void onStarFetched(Star s) {
                            mStartGameButton.setEnabled(true);

                            // we don't do anything with the star, we just want to make sure it's
                            // in the cache before we start the activity.
                            startActivity(intent);
                        }
                    });
                } else {
                    startActivity(intent);
                }
            }
        });
    }
}
