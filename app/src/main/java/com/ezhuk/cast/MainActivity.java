// Copyright (c) 2014 Eugene Zhuk.
// Use of this source code is governed by the MIT license that can be found
// in the LICENSE file.

package com.ezhuk.cast;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.support.v7.media.MediaRouteSelector;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.Cast.MessageReceivedCallback;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;


public class MainActivity extends ActionBarActivity {
    private static final String TAG = "MainActivity";

    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;

    private HashMap<String, String> mRouteInfos;
    public ArrayList<String> mDeviceNames;

    private MediaRouter mMediaRouter;
    private MediaRouter.Callback mMediaRouterCallback;
    private MediaRouteSelector mMediaRouteSelector;
    private CastDevice mCastDevice;
    private GoogleApiClient mGoogleApiClient;
    private Cast.Listener mCastListener;
    private TextChannel mTextChannel;

    private boolean mApplicationStarted;
    private boolean mWaitingForReconnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRouteInfos = new HashMap<String, String>();
        mDeviceNames = new ArrayList<String>();

        setupActionBar();
        setupDrawer();

        if (savedInstanceState == null) {
            selectDrawerItem(0);
        }

        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast(getResources()
                        .getString(R.string.app_id)))
                .build();
        mMediaRouterCallback = new MediaRouterCallback();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem mediaRouteItem = menu.findItem(R.id.media_route_item);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteItem);
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMediaRouter.addCallback(mMediaRouteSelector,
                mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    @Override
    protected void onPause() {
        if (isFinishing()) {
            mMediaRouter.removeCallback(mMediaRouterCallback);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        doneCast();
        super.onDestroy();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggle.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    public void onSendText(View view) {
        EditText editText = (EditText) findViewById(R.id.text);
        String text = editText.getText().toString();
        if (!text.isEmpty()) {
            editText.setText("");
            sendText(text);
        } else {
            Toast.makeText(MainActivity.this, "Enter text", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void setupActionBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private void setupDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.drawer_list);
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_item,
                getResources().getStringArray(R.array.drawer_array)));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer,
                R.string.drawer_open,
                R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View view) {
                invalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private void sendText(String text) {
        if (null != mGoogleApiClient && null != mTextChannel) {
            try {
                Cast.CastApi.sendMessage(mGoogleApiClient,
                        mTextChannel.getNamespace(), text)
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status status) {
                                if (!status.isSuccess()) {
                                    Log.e(TAG, "sendText: could not send text");
                                }
                            }
                        });
            } catch (Exception ex) {
                Log.e(TAG, "sendText: ", ex);
            }
        } else {
            Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void initCast(Bundle bundle) {
        try {
            mCastDevice = CastDevice.getFromBundle(bundle);
            mCastListener = new CastListener();
            Cast.CastOptions.Builder optionsBuilder = Cast.CastOptions
                    .builder(mCastDevice, mCastListener);
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Cast.API, optionsBuilder.build())
                    .addConnectionCallbacks(new ConnectionCallbacks())
                    .addOnConnectionFailedListener(new ConnectionFailedListener())
                    .build();
            mGoogleApiClient.connect();
        } catch (Exception ex) {
            Log.e(TAG, "initCast: ", ex);
        }
    }

    private void doneCast() {
        if (null != mGoogleApiClient) {
            if (mApplicationStarted) {
                try {
                    Cast.CastApi.stopApplication(mGoogleApiClient);
                    if (null != mTextChannel) {
                        Cast.CastApi.removeMessageReceivedCallbacks(
                                mGoogleApiClient, mTextChannel.getNamespace());
                        mTextChannel = null;
                    }
                } catch (IOException ex) {
                    Log.e(TAG, "doneCast: ", ex);
                }
                mApplicationStarted = false;
            }

            if (mGoogleApiClient.isConnected()) {
                mGoogleApiClient.disconnect();
            }
            mGoogleApiClient = null;
        }

        mCastDevice = null;
        mWaitingForReconnect = false;
    }

    private void setChannel() {
        try {
            Cast.CastApi.setMessageReceivedCallbacks(mGoogleApiClient,
                    mTextChannel.getNamespace(),
                    mTextChannel);
        } catch (IOException ex) {
            Log.e(TAG, "setChannel: ", ex);
        }
    }

    private void updateDevicesFragment() {
        Fragment fragment = getFragmentManager()
                .findFragmentById(R.id.drawer_frame);
        if (fragment.getClass() == DevicesFragment.class) {
            DevicesFragment devicesFragment = (DevicesFragment) fragment;
            mDeviceNames.clear();
            mDeviceNames.addAll(mRouteInfos.values());
            devicesFragment.updateDevicesList();
        }
    }

    private class MediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteAdded(MediaRouter router, RouteInfo info) {
            mRouteInfos.put(info.getId(), info.getName());
            updateDevicesFragment();
        }

        @Override
        public void onRouteRemoved(MediaRouter router, RouteInfo info) {
            mRouteInfos.remove(info.getId());
            updateDevicesFragment();
        }

        @Override
        public void onRouteSelected(MediaRouter router, RouteInfo info) {
            initCast(info.getExtras());
        }

        @Override
        public void onRouteUnselected(MediaRouter router, RouteInfo info) {
            doneCast();
        }
    }

    public class TextChannel implements MessageReceivedCallback {
        public String getNamespace() {
            return getString(R.string.namespace_id);
        }

        @Override
        public void onMessageReceived(CastDevice device, String namespace,
                                      String message) {
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private class CastListener extends Cast.Listener {
        @Override
        public void onApplicationDisconnected(int errorCode) {
            doneCast();
        }
    }

    private class ConnectionCallbacks implements
            GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle connectionHint) {
            if (null == mGoogleApiClient)
                return;

            try {
                if (mWaitingForReconnect) {
                    mWaitingForReconnect = false;

                    if ((null != connectionHint) && connectionHint
                            .getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) {
                        doneCast();
                    } else {
                        setChannel();
                    }
                } else {
                    Cast.CastApi.launchApplication(mGoogleApiClient,
                            getString(R.string.app_id), false)
                            .setResultCallback(
                                    new ResultCallback<Cast.ApplicationConnectionResult>() {
                                @Override
                                public void onResult(Cast.ApplicationConnectionResult result) {
                                    Status status = result.getStatus();
                                    if (status.isSuccess()) {
                                        mApplicationStarted = true;
                                        mTextChannel = new TextChannel();
                                        setChannel();
                                        sendText(getResources()
                                                .getString(R.string.app_name));
                                    } else {
                                        doneCast();
                                    }
                                }
                            });

                }
            } catch (Exception ex) {
                Log.e(TAG, "onConnected: ", ex);
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            mWaitingForReconnect = true;
        }
    }

    private class ConnectionFailedListener implements
            GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult result) {
            doneCast();
        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectDrawerItem(position);
        }
    }

    private void selectDrawerItem(int position) {
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.drawer_frame,
                (0 == position) ? new DevicesFragment() : new TextFragment())
                .commit();

        mDrawerList.setItemChecked(position, true);
        mDrawerLayout.closeDrawer(mDrawerList);
    }
}
