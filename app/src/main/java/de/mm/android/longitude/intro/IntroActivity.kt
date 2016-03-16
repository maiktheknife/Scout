package de.mm.android.longitude.intro

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log

import com.github.paolorotolo.appintro.AppIntro2
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.games.Games
import com.google.android.gms.games.GamesActivityResultCodes
import com.google.android.gms.plus.Plus
import com.google.android.gms.plus.model.people.Person
import com.google.example.games.basegameutils.GameHelperUtils

import java.net.HttpURLConnection

import de.mm.android.longitude.MainActivity
import de.mm.android.longitude.R
import de.mm.android.longitude.model.SignInFailureReason
import de.mm.android.longitude.common.Constants
import de.mm.android.longitude.fragment.LoginFragment
import de.mm.android.longitude.fragment.IntroFragment
import de.mm.android.longitude.network.GCMRegUtil
import de.mm.android.longitude.network.RestService
import de.mm.android.longitude.util.NetworkUtil
import de.mm.android.longitude.util.PreferenceUtil
import retrofit.RetrofitError
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Func1

/**
 * Created by Max on 09.08.2015.
 */
class IntroActivity : AppIntro2(), LoginFragment.ILoginCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private var mIntentInProgress: Boolean = false // A flag indicating that a PendingIntent is in progress and prevents us from starting further intents.
    private lateinit var googleApiClient: GoogleApiClient
    private var mConnectionResult: ConnectionResult? = null
    private lateinit var restService: RestService
    private var tmp_email: String? = null

    override fun init(bundle: Bundle?) {
        addSlide(IntroFragment.newInstance(R.layout.intro1))
        addSlide(IntroFragment.newInstance(R.layout.intro2))
        addSlide(IntroFragment.newInstance(R.layout.intro3))
        addSlide(LoginFragment.newInstance())
        isVibrateOn = false
        isProgressButtonEnabled = false
        setUpGoogleApiClient()
        restService = RestService.Creator.create(this)
    }

    private fun setUpGoogleApiClient() {
        Log.d(TAG, "setUpGoogleApiClient")
        googleApiClient = GoogleApiClient.Builder(this)
            .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
            .addApi(Games.API).addScope(Games.SCOPE_GAMES)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "onActivityResult:" + GameHelperUtils.activityResponseCodeToString(resultCode))

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

    override fun onDonePressed() {}

    override fun onNextPressed() {}

    override fun onSlideChanged() {}

    /* ConnectionCallbacks, OnConnectionFailedListener */

    override fun onConnected(bundle: Bundle?) {
        Log.d(TAG, "onConnected")
        if (NetworkUtil.isInternetAvailable(this)) {
            var f: LoginFragment? = null
            val fragments = slides
            for (tmp in fragments) {
                if (tmp is LoginFragment) {
                    f = tmp
                    break
                }
            }

            tmp_email = Plus.AccountApi.getAccountName(googleApiClient)
            PreferenceUtil.setAccountEMail(this, tmp_email)
            PreferenceUtil.setAccountToken(this, "notEmpty")
            f!!.enableStepTwo(Plus.PeopleApi.getCurrentPerson(googleApiClient).displayName)
        } else {
            showMessage(getString(R.string.error_noNetworkConnectionFound))
        }
    }

    override fun onConnectionSuspended(i: Int) {
        Log.d(TAG, "onConnectionSuspended: " + i)
        googleApiClient.disconnect()
    }

    override fun onConnectionFailed(result: ConnectionResult) {
        Log.d(TAG, "onConnectionFailed: " + result.errorCode + " " + mIntentInProgress)
        if (!result.hasResolution()) {
            Log.d(TAG, "onConnectionFailed.start Error Dialog")
            GooglePlayServicesUtil.getErrorDialog(result.errorCode, this, 0).show()
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

    private fun onFailure(signInFailureReason: SignInFailureReason) {
        showMessage("" + signInFailureReason.getActivityResultText() + " " + signInFailureReason.getServiceErrorText())
    }

    private fun showMessage(message: String) {
        Log.d(TAG, "showMessage: " + message)
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
    }

    /* ILoginCallback */

    override fun onSignInPressed() {
        Log.d(TAG, "onSignInPressed: " + googleApiClient.isConnected)
        if (googleApiClient.isConnected) {
            googleApiClient.clearDefaultAccountAndReconnect()
        } else {
            googleApiClient.connect()
        }
    }

    override fun onLoginPressed(name: String) {
        Log.d(TAG, "onLoginPressed $name")

        GCMRegUtil
            .getNewGCMRegID(this)
            .flatMap { gcmId ->
                Log.d(TAG, "onLoginPressed.getNewGCMRegID.callback")
                val mee = Plus.PeopleApi.getCurrentPerson(googleApiClient)
                PreferenceUtil.setGCM(this@IntroActivity, gcmId)
                restService.addUser(tmp_email, name, mee.id, gcmId)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe (
                { response ->
                    Log.d(TAG, "onLoginPressed.addUser.callback")
                    if (response.isSuccess()) {
                        startMainActivity(tmp_email!!, name, response.getData().getPersonId())
                    } else {
                        // wtf ???
                        showMessage("$response.error.statusCode  $response.error.message")
                    }
                },
                { t ->
                    val error = t as RetrofitError
                    if (error.response.status == HttpURLConnection.HTTP_CONFLICT) { // user already registered --> login
                        val response = error.body as RestService.NetworkResponse
                        startMainActivity(tmp_email!!, name, response.error.persond_id)
                    } else {
                        // sth went really wrong so give up
                        showMessage(t.message ?: "unknown Error")
                    }
                }
        )

    }

    private fun startMainActivity(email: String, name: String, person_id: Int) {
        Log.d(TAG, "startMainActivity")
        PreferenceUtil.setUsingApp(this, true)
        PreferenceUtil.setAccountEMail(this, email)
        PreferenceUtil.setAccountName(this, name)
        finish()
        startActivity(Intent(this, MainActivity::class.java))
    }

    companion object {
        private val TAG = IntroActivity::class.java.simpleName
    }

}