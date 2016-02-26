package de.mm.android.longitude.base

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.annotation.NonNull
import android.support.annotation.StringRes
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Status
import com.google.android.gms.drive.Drive
import com.google.android.gms.games.Games
import com.google.android.gms.games.GamesActivityResultCodes
import com.google.android.gms.plus.Plus
import com.google.example.games.basegameutils.GameHelperUtils

import de.mm.android.longitude.R
import de.mm.android.longitude.common.Constants
import de.mm.android.longitude.fragment.UpdateAble
import de.mm.android.longitude.model.SignInFailureReason
import de.mm.android.longitude.util.AccountUtil
import de.mm.android.longitude.util.NetworkUtil
import de.mm.android.longitude.util.PreferenceUtil

/**
 * Created by Max on 02.04.2015.
 */
abstract class GameActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, Constants {

    private var mConnectionResult: ConnectionResult? = null
    private var localReceiver: LocalReceiver? = null
    private var mIntentInProgress: Boolean = false // A flag indicating that a PendingIntent is in progress and prevents us from starting further intents.
    protected lateinit var googleApiClient: GoogleApiClient
    protected var isInetAvailable: Boolean = false

    private inner class LocalReceiver : BroadcastReceiver() {
        val TAG = LocalReceiver::class.java.simpleName
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            Log.d(TAG, "onReceive: " + action)
            when (action) {
                ConnectivityManager.CONNECTIVITY_ACTION -> {
                    isInetAvailable = NetworkUtil.isInternetAvailable(this@GameActivity)
                    val f = supportFragmentManager.findFragmentById(R.id.ac_container)
                    if (f != null && f is UpdateAble) {
                        f.onConnectivityUpdate(isInetAvailable)
                    }
                }
                else -> Log.w(TAG, "Unknown Action " + action)
            }
        }
    }

    /* LifeCycle */

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        checkPlayServices()
        setUpGoogleApiClient()
        localReceiver = LocalReceiver()
        isInetAvailable = NetworkUtil.isInternetAvailable(this)
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
        setLocalWifiReceiverEnabled(true)
        if (AccountUtil.isAccountValid(this, PreferenceUtil.getAccountEMail(this)) && PreferenceUtil.isUsingApp(this)) {
            googleApiClient.connect()
        }
        isInetAvailable = NetworkUtil.isInternetAvailable(this)
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
        setLocalWifiReceiverEnabled(false)
        if (googleApiClient.isConnected) {
            googleApiClient.disconnect()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "onActivityResult:" + "\n resp= " + GameHelperUtils.activityResponseCodeToString(resultCode))

        when (requestCode) {
            Constants.REQUEST_PICK_GOOGLE_ACCOUNT -> // We're coming back from an activity that was launched to resolve a
                // connection problem. For example, the sign-in UI.
                if (resultCode == AppCompatActivity.RESULT_OK) {
                    // Ready to try to connect again.
                    Log.d(TAG, "onAR: Resolution was RESULT_OK, so connecting current client again.")
                    googleApiClient.reconnect()
                } else if (resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
                    Log.d(TAG, "onAR: Resolution was RECONNECT_REQUIRED, so reconnecting.")
                    googleApiClient.reconnect()
                } else if (resultCode == AppCompatActivity.RESULT_CANCELED) {
                    // User cancelled.
                    Log.d(TAG, "onAR: Got a cancellation result, so disconnecting.")
                    googleApiClient.disconnect()
                    onFailure(SignInFailureReason(-1, -1))
                } else {
                    // Whatever the problem we were trying to solve, it was not
                    // solved. So give up and show an error message.
                    Log.d(TAG, "onAR: responseCode=" + GameHelperUtils.activityResponseCodeToString(resultCode) + ", so giving up.")
                    onFailure(SignInFailureReason(mConnectionResult!!.errorCode, resultCode))
                }
            else -> {
            }
        }
    }

    /* Callback */

    /**
     * Hook, called when Google Play Login succeed
     */
    protected abstract fun onSuccess()

    /**
     * Hook, called when Google Play Login failed
     */
    protected abstract fun onFailure(@NonNull signInFailureReason: SignInFailureReason)

    /* Game */

    private fun checkPlayServices() {
        Log.d(TAG, "checkPlayServices")
        val resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GoogleApiAvailability.getInstance().isUserResolvableError(resultCode)) {
                GoogleApiAvailability.getInstance().getErrorDialog(this, resultCode, Constants.REQUEST_PLAY_SERVICES).show()
            } else {
                Log.i(TAG, "This device is not supported.")
                showMessage(getString(R.string.error_notsupported))
                finish()
            }
        }
    }

    private fun setLocalWifiReceiverEnabled(isEnabled: Boolean) {
        Log.d(TAG, "setLocalWifiReceiverEnabled: " + isEnabled)
        if (isEnabled) {
            registerReceiver(localReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        } else {
            unregisterReceiver(localReceiver)
        }
    }

    private fun setUpGoogleApiClient() {
        Log.d(TAG, "setUpGoogleApiClient")

//        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//            .requestScopes(Games.SCOPE_GAMES/*, Plus.SCOPE_PLUS_LOGIN, Drive.SCOPE_APPFOLDER*/)
//            .requestEmail()
//            .requestServerAuthCode(getString(R.string.app_id))
//            .build()
//
//        googleApiClient = GoogleApiClient.Builder(this)
//            .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
//            .addApi(Games.API).addScope(Games.SCOPE_GAMES) // TODO: Auth API with Games not support yet
//            .enableAutoManage(this, this)
//            .build()

        googleApiClient = GoogleApiClient.Builder(this)
            .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
            .addApi(Games.API).addScope(Games.SCOPE_GAMES)
            //.addApi(Drive.API).addScope(Drive.SCOPE_APPFOLDER)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build();
    }

    private fun resolveSignInError() {
        Log.d(TAG, "resolveSignInError: " + mConnectionResult!!.hasResolution())
        if (mConnectionResult!!.hasResolution()) {
            try {
                mIntentInProgress = true
                mConnectionResult!!.startResolutionForResult(this, Constants.REQUEST_PICK_GOOGLE_ACCOUNT)
            } catch (e: IntentSender.SendIntentException) {
                Log.e(TAG, "resolveSignInError ", e)
                mIntentInProgress = false
                googleApiClient.connect()
            }

        }
    }

    /**
     * Sign-out from google
     */
    fun signOut(callback: ResultCallback<Status>) {
        Log.d(TAG, "signOut")
        if (googleApiClient.isConnected) {
            Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(callback)
        }
    }

    /**
     * Revoking access from google
     */
    fun revokeAccess(callback: ResultCallback<Status>) {
        Log.d(TAG, "revokeAccess")
        if (googleApiClient.isConnected) {
            Auth.GoogleSignInApi.revokeAccess(googleApiClient).setResultCallback(callback)
        }
    }

    /* ConnectionCallbacks, OnConnectionFailedListener */

    override fun onConnected(bundle: Bundle?) {
        Log.d(TAG, "onConnected")
        PreferenceUtil.setUsingApp(this, true)
        onSuccess()
    }

    override fun onConnectionSuspended(i: Int) {
        Log.d(TAG, "onConnectionSuspended: " + i)
        googleApiClient.disconnect()
    }

    override fun onConnectionFailed(result: ConnectionResult) {
        Log.d(TAG, "onConnectionFailed: " + result.errorCode)
        if (!result.hasResolution()) {
            Log.d(TAG, "onConnectionFailed.start Error Dialog")
            GoogleApiAvailability.getInstance().getErrorDialog(this, result.errorCode, 0).show()
            return
        }

        if (!mIntentInProgress) {
            mConnectionResult = result
            resolveSignInError()
        } else {
            onFailure(SignInFailureReason(mConnectionResult!!.errorCode))
        }

    }

    /* Stuff */

    protected fun showMessage(@StringRes errorMessage: Int) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
    }

    protected fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private val TAG = GameActivity::class.java.simpleName
    }

}