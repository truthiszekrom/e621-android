package de.e621.rebane.activities;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import de.e621.rebane.FilterManager;
import de.e621.rebane.LoginManager;
import de.e621.rebane.MiscStatics;
import de.e621.rebane.SQLite.SQLiteDB;
import de.e621.rebane.a621.R;
import de.e621.rebane.components.WebImageView;
import de.e621.rebane.service.DMailService;

public class DrawerWrapper extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    FilterManager blacklist = null;
    LoginManager login;
    public static SQLiteDB database = null;
    public static String baseURL;
    public static Class<? extends DrawerWrapper> openActivity;

    final static String LIMITEREXTRA = "RequestAmmountLimiterTimestampArray";
    void handleIntent(Intent intent) {
        long[] values = intent.getLongArrayExtra(LIMITEREXTRA);
        if (values != null) for (long l : values) MiscStatics.addRequestTO(l);
        MiscStatics.canRequest(this);//too clear the list
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        List<Long> rhist = MiscStatics.getRequestHistory();
        if (rhist.size()>0) {
            long[] vals = new long[rhist.size()];
            for (int i = 0; i < rhist.size(); i++) vals[i] = (long) rhist.get(i);
            outState.putLongArray(LIMITEREXTRA, vals);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        openDB();
        //blacklist = new FilterManager(this, database.getStringArray(SettingsActivity.SETTINGBLACKLIST));
        baseURL = database.getValue(SettingsActivity.SETTINGBASEURL);
        //eventually get login and other data
        login = LoginManager.getInstance(this, database);
    }

    @Override
    protected void onResume() {
        super.onResume();
        openDB();

        //to be able to respond to settings, the following is here:
        blacklist = new FilterManager(this, database.getStringArray(SettingsActivity.SETTINGBLACKLIST));

        //starting is actually pointless if we're not logged in
        if (!DMailService.isRunning() && Boolean.parseBoolean(database.getValue(SettingsActivity.SETTINGDMAILSERVICE)) && login.isLoggedIn()) {
            Logger.getLogger("a621").info("Starting DMail service");
            Intent mServiceIntent = new Intent(this, DMailService.class);
            //mServiceIntent.setData(Uri.parse(dataUrl));
            mServiceIntent.putExtra(DMailService.DMAILLOGINEXTRA, login.getLogin());
            startService(mServiceIntent);
        } else if (DMailService.isRunning()) {
            Logger.getLogger("a621").info("DMail Service is running");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //if (database != null) { database.close(); database = null; }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //if (database != null) { database.close(); database = null; }
    }

    @Override public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Logger.getLogger("a621").info("Memory running on level " + level);
        if (level>=TRIM_MEMORY_RUNNING_MODERATE) MiscStatics.clearMem(this);
    }

    @Override public void onLowMemory() {
        super.onLowMemory();
        Logger.getLogger("a621").info("Memory running low: cleaning...");
        MiscStatics.clearMem(this);
    }

    protected void onCreateDrawer() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        exDrawerListener toggle = new exDrawerListener(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private class exDrawerListener extends  ActionBarDrawerToggle implements DrawerLayout.DrawerListener {
        public exDrawerListener(Activity activity, DrawerLayout drawerLayout, @StringRes int openDrawerContentDescRes, @StringRes int closeDrawerContentDescRes) {
            super(activity, drawerLayout, openDrawerContentDescRes, closeDrawerContentDescRes);
        }

        public exDrawerListener(Activity activity, DrawerLayout drawerLayout, Toolbar toolbar, @StringRes int openDrawerContentDescRes, @StringRes int closeDrawerContentDescRes) {
            super(activity, drawerLayout, toolbar, openDrawerContentDescRes, closeDrawerContentDescRes);
        }

        @Override public void onDrawerSlide(View drawerView, float slideOffset) {
        }

        @Override public void onDrawerOpened(View drawerView) {
            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            Menu menu = navigationView.getMenu();
            MenuItem nav_login = menu.findItem(R.id.nav_login);
            boolean loggedIn = login.isLoggedIn();
            nav_login.setTitle(loggedIn ? "Log out" : "Sign in");
            TextView tv = (TextView) findViewById(R.id.drawerLoginName);
            tv.setText(loggedIn?login.getUsername():"Guest");
        }

        @Override public void onDrawerClosed(View drawerView) {
        }

        @Override public void onDrawerStateChanged(int newState) {
        }
    }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            /*/Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setClass(this, SettingsActivity.class);
            intent.putExtra(SettingsActivity.SETTINGDEFAULTSEARCH, database.getValue(SettingsActivity.SETTINGDEFAULTSEARCH));
            intent.putExtra(SettingsActivity.SETTINGBASEURL, database.getValue(SettingsActivity.SETTINGBASEURL));
            intent.putExtra(SettingsActivity.SETTINGBLACKLIST, database.getStringArray(SettingsActivity.SETTINGBLACKLIST));
            startActivityForResult(intent, SettingsActivity.SETTING_CONFIRM);
            /*/
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_search) {

        }

        return super.onOptionsItemSelected(item);
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == SettingsActivity.SETTING_CONFIRM) {
            if (resultCode == RESULT_OK) {
                // A contact was picked.  Here we will just display it
                // to the user.
                handleIntent(data);
            }
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_posts && !openActivity.equals(PostsActivity.class)) {
            Intent intent = new Intent(getApplicationContext(), PostsActivity.class);
            this.startActivity(intent);
        } else if (id == R.id.nav_pools) {

        } else if (id == R.id.nav_sets) {

        } else if (id == R.id.nav_blips) {

        } else if (id == R.id.nav_forum && !openActivity.equals(ForumsActivity.class)) {
            Intent intent = new Intent(getApplicationContext(), ForumsActivity.class);
            this.startActivity(intent);
        } else if (id == R.id.nav_login) {
            if (!login.isLoggedIn()) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                this.startActivity(intent);
            } else {
                login.logout();
                Toast.makeText(this, "Logged out...", Toast.LENGTH_SHORT).show();
                item.setTitle("Sign in");
            }
        } else if (id == R.id.nav_dmail) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void quickToast(CharSequence message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    void openDB() {
        if (database == null) { (database = new SQLiteDB(this)).open(); }
    }
}
