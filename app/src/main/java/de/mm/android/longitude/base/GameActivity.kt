package de.mm.android.longitude.base

import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.api.Status
import com.google.android.gms.games.Games
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
abstract class GameActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener, Constants {

    protected var isInetAvailable: Boolean = false
    protected lateinit var googleApiClient: GoogleApiClient
    private lateinit var localReceiver: LocalReceiver
    private lateinit var mProgressDialog: ProgressDialog

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
        isInetAvailable = NetworkUtil.isInternetAvailable(this)

        if (AccountUtil.isAccountValid(this, PreferenceUtil.getAccountEMail(this)) && PreferenceUtil.isUsingApp(this)) {
            signIn()
            val opr = Auth.GoogleSignInApi.silentSignIn(googleApiClient)
            if (opr.isDone) {
                Log.d(TAG, "Got cached sign-in")
                handleSignInResult(opr.get())
            } else {
                showProgressDialog()
                opr.setResultCallback { googleSignInResult ->
                    hideProgressDialog()
                    handleSignInResult(googleSignInResult)
                }
            }

        }
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
        setLocalWifiReceiverEnabled(false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "onActivityResult(requestCode= " + requestCode + ", resultCode= " + GameHelperUtils.activityResponseCodeToString(resultCode))

        when (requestCode) {
            Constants.REQUEST_PICK_GOOGLE_ACCOUNT -> {
                val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
                handleSignInResult(result)
            }
            else -> Log.w(TAG, "Unknown onActivityResult " + requestCode)
        }

    }

    private fun showProgressDialog() {
        mProgressDialog = ProgressDialog(this)
        mProgressDialog.setMessage("Loading")
        mProgressDialog.isIndeterminate = true
        mProgressDialog.show()
    }

    private fun hideProgressDialog() {
        if (mProgressDialog.isShowing) {
            mProgressDialog.hide()
        }
    }

    private fun handleSignInResult(result: GoogleSignInResult) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess)
        if (result.isSuccess) {
            PreferenceUtil.setUsingApp(this, true)
            onSuccess(result.signInAccount!!)
        } else {
            Log.d(TAG, result.status.statusCode.toString() + " " + result.status.statusMessage);
            // TODO error handling
            onFailure(SignInFailureReason(result.status.statusCode))
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

    /**
     * Hook, called when Google Play Login succeed
     */
    protected abstract fun onSuccess(signInAccount: GoogleSignInAccount)

    /**
     * Hook, called when Google Play Login failed
     */
    protected abstract fun onFailure(signInFailureReason: SignInFailureReason)

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

    private fun setUpGoogleApiClient() {
        Log.d(TAG, "setUpGoogleApiClient")

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(Scope(Scopes.DRIVE_APPFOLDER), Scope(Scopes.GAMES))
                .requestEmail()
                .requestIdToken(getString(R.string.app_server_id))
                .build()

        googleApiClient = GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                // .addApi(Games.API)
                .enableAutoManage(this, this)
                .build()
    }

    /**
     * Sign-int to google
     */
    private fun signIn() {
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient)
        startActivityForResult(signInIntent, Constants.REQUEST_PICK_GOOGLE_ACCOUNT)
    }

    /**
     * Sign-out from google
     */
    fun signOut(callback: ResultCallback<Status>) {
        Log.d(TAG, "signOut")
        Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(callback)
    }

    /**
     * Revoking access from google
     */
    fun revokeAccess(callback: ResultCallback<Status>) {
        Log.d(TAG, "revokeAccess")
        Auth.GoogleSignInApi.revokeAccess(googleApiClient).setResultCallback(callback)
    }

    /* OnConnectionFailedListener */

    override fun onConnectionFailed(result: ConnectionResult) {
        Log.d(TAG, "onConnectionFailed: " + result.errorCode)
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