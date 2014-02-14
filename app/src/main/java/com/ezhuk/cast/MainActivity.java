package com.ezhuk.cast;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;

import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.support.v7.media.MediaRouteSelector;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast.MessageReceivedCallback;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.io.IOException;
import java.sql.Connection;

public class MainActivity extends ActionBarActivity {
    private MediaRouter mMediaRouter;
    private MediaRouter.Callback mMediaRouterCallback;
    private MediaRouteSelector mMediaRouteSelector;
    private CastDevice mCastDevice;
    private GoogleApiClient mGoogleApiClient;
    private Cast.Listener mCastListener;
    private ConnectionCallbacks mConnectionCallbacks;
    private ConnectionFailedListener mConnectionFailedListener;
    private TextChannel mTextChannel;
    private boolean mApplicationStarted;
    private boolean mWaitingForReconnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(
                        CastMediaControlIntent.categoryForCast(
                                getResources().getString(R.string.app_id))).build();

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

    public void onSendText(View view) {
        EditText editText = (EditText) findViewById(R.id.text);
        String text = editText.getText().toString();
        if (!text.isEmpty()) {
            sendText(text);
            editText.setText("");
        } else {
            Toast.makeText(MainActivity.this, "Please enter text", Toast.LENGTH_SHORT)
                    .show();
        }
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
                                    // TODO
                                }
                            }
                        });
            } catch (Exception e) {
                // TODO
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
            mConnectionCallbacks = new ConnectionCallbacks();
            mConnectionFailedListener = new ConnectionFailedListener();
            Cast.CastOptions.Builder optionsBuilder = Cast.CastOptions
                    .builder(mCastDevice, mCastListener);
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Cast.API, optionsBuilder.build())
                    .addConnectionCallbacks(mConnectionCallbacks)
                    .addOnConnectionFailedListener(mConnectionFailedListener)
                    .build();
            mGoogleApiClient.connect();
        } catch (Exception e) {
            // TODO
        }
    }

    private void doneCast() {
        if (null != mGoogleApiClient) {
            if (mApplicationStarted) {
                try {
                    Cast.CastApi.stopApplication(mGoogleApiClient);
                    if (null != mTextChannel) {
                        Cast.CastApi.removeMessageReceivedCallbacks(mGoogleApiClient,
                                mTextChannel.getNamespace());
                        mTextChannel = null;
                    }
                } catch (IOException e) {
                    // TODO
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

    private class MediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteSelected(MediaRouter router, RouteInfo info) {
            initCast(info.getExtras());
        }

        @Override
        public void onRouteUnselected(MediaRouter router, RouteInfo info) {
            doneCast();
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

                    if ((null != connectionHint)
                            && connectionHint.getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) {
                        doneCast();
                    } else {
                        try {
                            Cast.CastApi.setMessageReceivedCallbacks(mGoogleApiClient,
                                    mTextChannel.getNamespace(),
                                    mTextChannel);
                        } catch (IOException e) {
                            // TODO
                        }
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
                                        try {
                                            Cast.CastApi.setMessageReceivedCallbacks(mGoogleApiClient,
                                                    mTextChannel.getNamespace(),
                                                    mTextChannel);
                                        } catch (IOException e) {
                                            // TODO
                                        }
                                        sendText("TODO");
                                    } else {
                                        doneCast();
                                    }
                                }
                            });

                }
            } catch (Exception e) {
                // TODO
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

    private class TextChannel implements MessageReceivedCallback {
        public String getNamespace() {
            return getString(R.string.text_namespace);
        }

        @Override
        public void onMessageReceived(CastDevice device, String namespace,
                String message) {
            // TODO
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

}
