package de.mm.android.longitude.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.plus.Plus;
import com.google.example.games.basegameutils.GameHelperUtils;

import de.mm.android.longitude.R;
import de.mm.android.longitude.common.Constants;
import de.mm.android.longitude.fragment.UpdateAble;
import de.mm.android.longitude.model.SignInFailureReason;
import de.mm.android.longitude.util.AccountUtil;
import de.mm.android.longitude.util.NetworkUtil;
import de.mm.android.longitude.util.PreferenceUtil;

/**
 * Created by Max on 02.04.2015.
 */
public abstract class GameActivity
        extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, Constants{
    private static final String TAG = GameActivity.class.getSimpleName();

    private ConnectionResult mConnectionResult;
    private LocalReceiver localReceiver;
    private boolean mIntentInProgress; // A flag indicating that a PendingIntent is in progress and prevents us from starting further intents.
    protected GoogleApiClient googleApiClient;
    protected boolean isInetAvailable;

    private class LocalReceiver extends BroadcastReceiver {
        public final String TAG = LocalReceiver.class.getSimpleName();
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive: " + action);
            switch (action) {
                case ConnectivityManager.CONNECTIVITY_ACTION:
                    isInetAvailable = NetworkUtil.isInternetAvailable(GameActivity.this);
                    Fragment f = getSupportFragmentManager().findFragmentById(R.id.ac_container);
                    if (f != null && f instanceof UpdateAble) {
                        ((UpdateAble) f).onConnectivityUpdate(isInetAvailable);
                    }
                    break;
                default:
                    Log.w(TAG, "Unknown Action " + action);
                    break;
            }
        }
    }

    /* LifeCycle */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        checkPlayServices();
        setUpGoogleApiClient();
        localReceiver = new LocalReceiver();
        isInetAvailable = NetworkUtil.isInternetAvailable(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        setLocalWifiReceiverEnabled(true);
        if (AccountUtil.isAccountValid(this, PreferenceUtil.getAccountEMail(this)) && PreferenceUtil.isUsingApp(this)) {
            googleApiClient.connect();
        }
        isInetAvailable = NetworkUtil.isInternetAvailable(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        setLocalWifiReceiverEnabled(false);
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    private void checkPlayServices() {
        Log.d(TAG, "checkPlayServices");
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, REQUEST_PLAY_SERVICES).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                showMessage(getString(R.string.error_notsupported));
                finish();
            }
        }
    }

    private void setLocalWifiReceiverEnabled(boolean isEnabled) {
        Log.d(TAG, "setLocalWifiReceiverEnabled: " + isEnabled);
        if (isEnabled) {
            registerReceiver(localReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        } else {
            unregisterReceiver(localReceiver);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult:" + "\n resp= " + GameHelperUtils.activityResponseCodeToString(resultCode));

        switch (requestCode) {
            case Constants.REQUEST_PICK_GOOGLE_ACCOUNT:
                // We're coming back from an activity that was launched to resolve a
                // connection problem. For example, the sign-in UI.
                if (resultCode == AppCompatActivity.RESULT_OK) {
                    // Ready to try to connect again.
                    Log.d(TAG, "onAR: Resolution was RESULT_OK, so connecting current client again.");
                    googleApiClient.reconnect();
                } else if (resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
                    Log.d(TAG, "onAR: Resolution was RECONNECT_REQUIRED, so reconnecting.");
                    googleApiClient.reconnect();
                } else if (resultCode == AppCompatActivity.RESULT_CANCELED) {
                    // User cancelled.
                    Log.d(TAG, "onAR: Got a cancellation result, so disconnecting.");
                    googleApiClient.disconnect();
                    onFailure(new SignInFailureReason(-1, -1));
                } else {
                    // Whatever the problem we were trying to solve, it was not
                    // solved. So give up and show an error message.
                    Log.d(TAG, "onAR: responseCode=" + GameHelperUtils.activityResponseCodeToString(resultCode) + ", so giving up.");
                    onFailure(new SignInFailureReason(mConnectionResult.getErrorCode(), resultCode));
                }
                break;
            default:
                break;
        }
    }

    /* Callback */

    /**
     * Hook, called when Google Play Login succeed
     */
    protected abstract void onSuccess();

    /**
     * Hook, called when Google Play Login failed
     */
    protected abstract void onFailure(final SignInFailureReason signInFailureReason);

    /* Game */

    private void setUpGoogleApiClient() {
        Log.d(TAG, "setUpGoogleApiClient");
        googleApiClient = new GoogleApiClient.Builder(this)
            .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
            .addApi(Games.API).addScope(Games.SCOPE_GAMES)
//            .addApi(Drive.API).addScope(Drive.SCOPE_APPFOLDER)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build();
    }

    private void resolveSignInError(){
        Log.d(TAG, "resolveSignInError: " + mConnectionResult.hasResolution());
        if (mConnectionResult.hasResolution()) {
            try {
                mIntentInProgress = true;
                mConnectionResult.startResolutionForResult(this, Constants.REQUEST_PICK_GOOGLE_ACCOUNT);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "resolveSignInError ", e);
                mIntentInProgress = false;
                googleApiClient.connect();
            }
        }
    }

    /**
     * Sign-out from google
     */
    public void signOutFromGplus() {
        Log.d(TAG, "signOutFromGplus");
        if (googleApiClient.isConnected()) {
            googleApiClient.clearDefaultAccountAndReconnect();
        }
    }

    /**
     * Revoking access from google
     */
    public void revokeGplusAccess(ResultCallback<Status> callback) {
        Log.d(TAG, "revokeGplusAccess");
        if (googleApiClient.isConnected()) {
            Plus.AccountApi
                .revokeAccessAndDisconnect(googleApiClient)
                .setResultCallback(callback);
        }
    }

    /* ConnectionCallbacks, OnConnectionFailedListener */

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");
        PreferenceUtil.setUsingApp(this, true);
        onSuccess();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended: " + i);
        googleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(TAG, "onConnectionFailed: " + result.getErrorCode());
        if (!result.hasResolution()) {
            Log.d(TAG, "onConnectionFailed.start Error Dialog");
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, 0).show();
            return;
        }

        if (!mIntentInProgress) {
            mConnectionResult = result;
            resolveSignInError();
        } else {
            onFailure(new SignInFailureReason(mConnectionResult.getErrorCode()));
        }

    }

    /* Stuff */

    protected void showMessage(@StringRes int errorMessage){
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    protected void showMessage(@NonNull String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}