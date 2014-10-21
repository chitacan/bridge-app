package com.chitacan.bridge;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
                   AddFragment.OnFragmentInteractionListener {

    private static String TAG = "MainActivity";

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, StatusFragment.newInstance(), "fragment_status")
                .commit();

        Intent intent = new Intent(this, BridgeService.class);
        startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Util.denotify(this);
        Util.registerRejectReceiver(this, TAG);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Util.unregisterRejectReceiver(this, TAG);
    }

    @Override
    public void onNavigationDrawerItemSelected(Bundle bundle) {
        // update the main content by replacing fragments
        Intent intent = new Intent();
        intent.setClass(this, ClientActivity.class);
        intent.putExtras(bundle);

        startActivity(intent);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setIcon(R.drawable.ic_launcher);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        restoreActionBar();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_add:
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.container, AddFragment.newInstance("", ""), "fragment_add")
                        .addToBackStack(null)
                        .commit();
                return true;
            case R.id.action_about:
                About.show(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
