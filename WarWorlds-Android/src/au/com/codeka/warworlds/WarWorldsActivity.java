
package au.com.codeka.warworlds;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.ctrl.TransparentWebView;
import au.com.codeka.warworlds.game.starfield.StarfieldActivity;
import au.com.codeka.warworlds.model.ChatManager;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarSummary;
import au.com.codeka.warworlds.model.protobuf.Messages;

import com.google.android.gcm.GCMRegistrar;

/**
 * Main activity. Displays the message of the day and lets you select "Start Game", "Options", etc.
 */
public class WarWorldsActivity extends BaseActivity {
    private static Logger log = LoggerFactory.getLogger(WarWorldsActivity.class);
    private Context mContext = this;
    private Button mStartGameButton;
    private TextView mConnectionStatus;
    private Handler mHandler;
    private boolean mNeedHello;
    private List<Colony> mColonies;
    private String mStarKey;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        log.info("WarWorlds activity starting...");

        super.onCreate(savedInstanceState);

        Util.loadProperties(mContext);
        Authenticator.configure(mContext);
        PreferenceManager.setDefaultValues(this, R.xml.global_options, false);

        GCMRegistrar.checkDevice(mContext);
        GCMRegistrar.checkManifest(mContext);

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
        if (prefs.getBoolean("WarmWelcome",  false) == false) {
            // if we've never done the warm-welcome, do it now
            log.info("Starting Warm Welcome");
            mNeedHello = true;
            startActivity(new Intent(this, WarmWelcomeActivity.class));
            return;
        }

        if (prefs.getString("AccountName", null) == null) {
            log.info("No accountName saved, switching to AccountsActivity");
            mNeedHello = true;
            startActivity(new Intent(this, AccountsActivity.class));
            return;
        }

        if (mNeedHello) {
            sayHello(0);
            mNeedHello = false;
        }
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
            mNeedHello = true;
            startActivity(new Intent(this, AccountsActivity.class));
            return;
        }

        new AsyncTask<Void, Void, String>() {
            private boolean mNeedsEmpireSetup;
            private boolean mErrorOccured;
            private boolean mNoAutoRetry;
            private boolean mNeedsReAuthenticate;

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
                        mNeedsReAuthenticate = true;
                        mErrorOccured = true;
                        message = "<p>Re-authentication needed...</p>";
                        return message;
                    }
                    String url = "hello/"+deviceRegistrationKey;
                    Messages.Hello hello = ApiClient.putProtoBuf(url, null, Messages.Hello.class);
                    if (hello.hasEmpire()) {
                        mNeedsEmpireSetup = false;
                        EmpireManager.getInstance().setup(
                                MyEmpire.fromProtocolBuffer(hello.getEmpire()));
                    } else {
                        mNeedsEmpireSetup = true;
                    }

                    if (hello.hasRequireGcmRegister() && hello.getRequireGcmRegister()) {
                        log.info("Re-registering for GCM...");
                        GCMIntentService.register(WarWorldsActivity.this);
                        // we can keep going, though...
                    }

                    ChatManager.getInstance().setup();

                    mColonies = new ArrayList<Colony>();
                    for (Messages.Colony c : hello.getColoniesList()) {
                        mColonies.add(Colony.fromProtocolBuffer(c));
                    }

                    message = hello.getMotd().getMessage();
                    mErrorOccured = false;
                } catch(ApiException e) {
                    log.error("Error occurred in 'hello'", e);

                    if (e.getHttpStatusLine() == null) {
                        // if there's no status line, it likely means we were unable to connect
                        // (i.e. a network error) just keep retrying until it works.
                        message = "<p class=\"error\">An error occured talking to the server, check " +
                                "data connection.</p>";
                        mErrorOccured = true;
                        mNeedsReAuthenticate = false;
                    } else {
                        // an HTTP error is likely because our credentials are out of date, we'll
                        // want to re-authenticate ourselves.
                        message = "<p class=\"error\">Authentication failed.</p>";
                        mErrorOccured = true;
                        mNeedsReAuthenticate = true;
                    }
                }

                return message;
            }

            @Override
            protected void onPostExecute(String result) {
                final TransparentWebView motdView = (TransparentWebView) findViewById(R.id.motd);

                mConnectionStatus.setText("Connected");
                if (mNeedsEmpireSetup) {
                    mNeedHello = true;
                    startActivity(new Intent(mContext, EmpireSetupActivity.class));
                    return;
                } else if (!mErrorOccured) {
                    Util.setup(mContext);

                    // make sure we're correctly registered as online.
                    BackgroundDetector.getInstance().onBackgroundStatusChange(WarWorldsActivity.this);

                    motdView.loadHtml("html/skeleton.html", result);
                    findColony();
                } else /* mErrorOccured */ {
                    mConnectionStatus.setText("Connection Failed");
                    mStartGameButton.setEnabled(false);
                    motdView.loadHtml("html/skeleton.html", result);

                    if (mNeedsReAuthenticate) {
                        // if we need to re-authenticate, first forget the current credentials
                        // the switch to the AccountsActivity.
                        final SharedPreferences prefs = Util.getSharedPreferences(mContext);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.remove("AccountName");
                        editor.commit();

                        // we'll need to say hello the next time we come back to this activity
                        mNeedHello = true;

                        startActivity(new Intent(mContext, AccountsActivity.class));
                    } else if (!mNoAutoRetry) {
                        // otherwise, just try again
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                sayHello(retries+1);
                            }
                        }, 3000);
                    }
                }
            }
        }.execute();
    }

    private void findColony() {
        // we'll want to start off near one of your stars. If you
        // only have one, that's easy -- but if you've got lots
        // what then?
        mStarKey = null;
        for (Colony c : mColonies) {
            mStarKey = c.getStarKey();
        }

        if (mStarKey != null) {
            mStartGameButton.setEnabled(false);
            StarManager.getInstance().requestStarSummary(mContext, mStarKey,
                    new StarManager.StarSummaryFetchedHandler() {
                @Override
                public void onStarSummaryFetched(StarSummary s) {
                    mStartGameButton.setEnabled(true);

                    // we don't do anything with the star, we just want
                    // to make sure it's in the cache before we start
                    // the activity. Now the start button is ready to go!
                    mStartGameButton.setEnabled(true);

                    boolean showSituationReport = getIntent().getBooleanExtra("au.com.codeka.warworlds.ShowSituationReport", false);
                    if (showSituationReport) {
                        Intent intent = new Intent(mContext, StarfieldActivity.class);
                        intent.putExtra("au.com.codeka.warworlds.ShowSituationReport", true);
                        startActivity(intent);
                    }
                }
            });
        } else {
            mStartGameButton.setEnabled(true);
        }
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
                mNeedHello = true;
                startActivity(new Intent(mContext, AccountsActivity.class));
            }
        });

        optionsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContext, GlobalOptionsActivity.class));
            }
        });

        mStartGameButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                final Intent intent = new Intent(mContext, StarfieldActivity.class);
                intent.putExtra("au.com.codeka.warworlds.StarKey", mStarKey);
                startActivity(intent);
            }
        });
    }
}
