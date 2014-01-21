
package au.com.codeka.warworlds;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.warworlds.ctrl.TransparentWebView;
import au.com.codeka.warworlds.game.starfield.StarfieldActivity;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.MyEmpire;

/**
 * Main activity. Displays the message of the day and lets you select "Start Game", "Options", etc.
 */
public class WarWorldsActivity extends BaseActivity implements EmpireShieldManager.EmpireShieldUpdatedHandler {
    private static Logger log = LoggerFactory.getLogger(WarWorldsActivity.class);
    private Context mContext = this;
    private Button mStartGameButton;
    private TextView mConnectionStatus;
    private HelloWatcher mHelloWatcher;
    private TextView mRealmName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log.info("WarWorlds activity starting...");
        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar

        setContentView(R.layout.welcome);
        Util.setup(mContext);

        View rootView = findViewById(android.R.id.content);
        ActivityBackgroundGenerator.setBackground(rootView);

        mStartGameButton = (Button) findViewById(R.id.start_game_btn);
        mConnectionStatus = (TextView) findViewById(R.id.connection_status);
        mRealmName = (TextView) findViewById(R.id.realm_name);
        final Button realmSelectButton = (Button) findViewById(R.id.realm_select_btn);
        final Button optionsButton = (Button) findViewById(R.id.options_btn);

        refreshWelcomeMessage();

        realmSelectButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(mContext, RealmSelectActivity.class));
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
                startActivity(intent);
            }
        });

        ((Button) findViewById(R.id.help_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("http://www.war-worlds.com/doc/getting-started"));
                startActivity(i);
            }
        });

        ((Button) findViewById(R.id.website_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("http://www.war-worlds.com/"));
                startActivity(i);
            }
        });

        ((Button) findViewById(R.id.reauth_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(mContext, AccountsActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        log.debug("WarWorldsActivity.onResume...");

        SharedPreferences prefs = Util.getSharedPreferences();
        if (prefs.getBoolean("WarmWelcome",  false) == false) {
            // if we've never done the warm-welcome, do it now
            log.info("Starting Warm Welcome");
            startActivity(new Intent(this, WarmWelcomeActivity.class));
            return;
        }

        if (prefs.getString("AccountName", null) == null) {
            log.info("No accountName saved, switching to AccountsActivity");
            startActivity(new Intent(this, AccountsActivity.class));
            return;
        }

        if (RealmContext.i.getCurrentRealm() == null) {
            log.info("No realm selected, switching to RealmSelectActivity");
            startActivity(new Intent(this, RealmSelectActivity.class));
            return;
        }

        mStartGameButton.setEnabled(false);
        mRealmName.setText(String.format(Locale.ENGLISH, "Realm: %s", RealmContext.i.getCurrentRealm().getDisplayName()));

        final TextView empireName = (TextView) findViewById(R.id.empire_name);
        final ImageView empireIcon = (ImageView) findViewById(R.id.empire_icon);
        empireName.setText("");;
        empireIcon.setImageBitmap(null);

        mHelloWatcher = new HelloWatcher();
        ServerGreeter.addHelloWatcher(mHelloWatcher);

        ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
            @Override
            public void onHelloComplete(boolean success, ServerGreeter.ServerGreeting greeting) {
                if (success) {

                    // we'll display a bit of debugging info along with the 'connected' message
                    long maxMemoryBytes = Runtime.getRuntime().maxMemory();
                    int memoryClass = ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).getMemoryClass();
                    PackageInfo packageInfo = null;
                    try {
                        packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                    } catch (NameNotFoundException e) {
                    }

                    DecimalFormat formatter = new DecimalFormat("#,##0");
                    String msg = String.format(Locale.ENGLISH,
                                               "Connected\r\nMemory Class: %d - Max bytes: %s\r\nVersion: %s%s",
                                               memoryClass,
                                               formatter.format(maxMemoryBytes),
                                               packageInfo == null ? "Unknown" : packageInfo.versionName,
                                               Util.isDebug() ? " (debug)" : " (rel)");
                    mConnectionStatus.setText(msg);
                    mStartGameButton.setEnabled(true);

                    MyEmpire empire = EmpireManager.i.getEmpire();
                    if (empire != null) {
                        empireName.setText(empire.getDisplayName());
                        empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(mContext, empire));
                    }
                }
            }
        });
    }

    private void refreshWelcomeMessage() {
        new BackgroundRunner<Document>() {
            @Override
            protected Document doInBackground() {
                String url = (String) Util.getProperties().get("welcome.rss");
                try {
                    // we have to use the built-in one because our special version assume all requests go to the
                    // game server...
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpResponse response = httpClient.execute(new HttpGet(url));
                    if (response.getStatusLine().getStatusCode() == 200) {
                        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                        builderFactory.setValidating(false);

                        DocumentBuilder builder = builderFactory.newDocumentBuilder();
                        return builder.parse(response.getEntity().getContent());
                    }
                } catch (Exception e) {
                    log.error("Error fetching MOTD.", e);
                }

                return null;
            }

            @Override
            protected void onComplete(Document rss) {
                StringBuilder motd = new StringBuilder();
                if (rss != null) {
                    NodeList itemNodes = rss.getElementsByTagName("item");
                    for (int i = 0; i < itemNodes.getLength(); i++) {
                        Element itemElem = (Element) itemNodes.item(i);
                        String title = itemElem.getElementsByTagName("title").item(0).getTextContent();
                        String content = itemElem.getElementsByTagName("description").item(0).getTextContent();
                        String pubDate = itemElem.getElementsByTagName("pubDate").item(0).getTextContent();
                        String link = itemElem.getElementsByTagName("link").item(0).getTextContent();

                        try {
                            DateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
                            Date date = formatter.parse(pubDate);
                            formatter = new SimpleDateFormat("dd MMM yyyy h:mm a");
                            motd.append("<h1>");
                            motd.append(formatter.format(date));
                            motd.append("</h1>");
                        } catch (ParseException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                        motd.append("<h2>"); motd.append(title); motd.append("</h2>");
                        motd.append(content);
                        motd.append("<div style=\"text-align: right; border-bottom: dashed 1px #fff; padding-bottom: 4px;\">");
                        motd.append("<a href=\""); motd.append(link); motd.append("\">");
                        motd.append("View forum post");
                        motd.append("</a></div>");
                    }
                }

                TransparentWebView motdView = (TransparentWebView) findViewById(R.id.motd);
                motdView.loadHtml("html/skeleton.html", motd.toString());

            }
        }.execute();
    }

    @Override
    public void onEmpireShieldUpdated(int empireID) {
        // if it's the same as our empire, we'll need to update the icon we're currently showing.
        MyEmpire empire = EmpireManager.i.getEmpire();
        if (empireID == Integer.parseInt(empire.getKey())) {
            ImageView empireIcon = (ImageView) findViewById(R.id.empire_icon);
            empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(mContext, empire));

        }
    }

    @Override
    public void onPause() {
        super.onPause();
        ServerGreeter.removeHelloWatcher(mHelloWatcher);
    }

    private class HelloWatcher implements ServerGreeter.HelloWatcher {
        private int mNumRetries = 0;

        @Override
        public void onRetry(final int retries) {
            mNumRetries = retries + 1;
            mConnectionStatus.setText(String.format("Retrying (#%d)...", mNumRetries));
        }

        @Override
        public void onAuthenticating() {
            if (mNumRetries > 0) {
                return;
            }
            mConnectionStatus.setText("Authenticating...");
        }

        @Override
        public void onConnecting() {
            if (mNumRetries > 0) {
                return;
            }
            mConnectionStatus.setText("Connecting...");
        }
    }
}
