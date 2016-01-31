package de.mm.android.longitude.intro;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.github.paolorotolo.appintro.AppIntro2;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.example.games.basegameutils.GameHelperUtils;

import java.net.HttpURLConnection;
import java.util.List;

import de.mm.android.longitude.MainActivity;
import de.mm.android.longitude.R;
import de.mm.android.longitude.model.SignInFailureReason;
import de.mm.android.longitude.common.Constants;
import de.mm.android.longitude.fragment.LoginFragment;
import de.mm.android.longitude.fragment.IntroFragment;
import de.mm.android.longitude.network.GCMRegUtil;
import de.mm.android.longitude.network.RestService;
import de.mm.android.longitude.util.NetworkUtil;
import de.mm.android.longitude.util.PreferenceUtil;
import retrofit.RetrofitError;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by Max on 09.08.2015.
 */
public class IntroActivity extends AppIntro2
implements LoginFragment.ILoginCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = IntroActivity.class.getSimpleName();
    private boolean mIntentInProgress; // A flag indicating that a PendingIntent is in progress and prevents us from starting further intents.
    private GoogleApiClient googleApiClient;
    private ConnectionResult mConnectionResult;
    private RestService restService;
    private String tmp_email;

    @Override
    public void init(Bundle bundle) {
        addSlide(IntroFragment.newInstance(R.layout.intro1));
        addSlide(IntroFragment.newInstance(R.layout.intro2));
        addSlide(IntroFragment.newInstance(R.layout.intro3));
        addSlide(LoginFragment.newInstance());
        setVibrate(false);
        setProgressButtonEnabled(false);
        setUpGoogleApiClient();
    }

    @Override
    protected void onStart() {
        super.onStart();
        restService = RestService.Creator.create(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        restService = null;
    }

    private void setUpGoogleApiClient() {
        Log.d(TAG, "setUpGoogleApiClient");
        googleApiClient = new GoogleApiClient.Builder(this)
            .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
            .addApi(Games.API).addScope(Games.SCOPE_GAMES)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build();
    }

    private void resolveSignInError() {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult:" + GameHelperUtils.activityResponseCodeToString(resultCode));

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

    @Override
    public void onDonePressed() {}

    @Override
    public void onNextPressed() {}

    @Override
    public void onSlideChanged() {

    }

    /* ConnectionCallbacks, OnConnectionFailedListener */

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");
        if (NetworkUtil.isInternetAvailable(this)) {
            LoginFragment f = null;
            List<Fragment> fragments = getSlides();
            for (Fragment tmp: fragments) {
                if (tmp instanceof LoginFragment) {
                    f = (LoginFragment) tmp;
                    break;
                }
            }

            tmp_email = Plus.AccountApi.getAccountName(googleApiClient);
            PreferenceUtil.setAccountEMail(this, tmp_email);
            PreferenceUtil.setAccountToken(this, "notEmpty");
            f.enableStepTwo(Plus.PeopleApi.getCurrentPerson(googleApiClient).getDisplayName());
        } else {
            showMessage(getString(R.string.error_noNetworkConnectionFound));
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended: " + i);
        googleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(TAG, "onConnectionFailed: " + result.getErrorCode() + " " + mIntentInProgress);
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

    private void onFailure(SignInFailureReason signInFailureReason) {
        showMessage("" + signInFailureReason.geActivityResultText() + " " + signInFailureReason.getServiceErrorText());
    }

    private void showMessage(String message) {
        Log.d(TAG, "showMessage: " + message);
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
    }

    /* ILoginCallback */

    @Override
    public void onSignInPressed() {
        Log.d(TAG, "onSignInPressed: " + googleApiClient.isConnected());
        if (googleApiClient.isConnected()) {
            googleApiClient.clearDefaultAccountAndReconnect();
        }else {
            googleApiClient.connect();
        }
    }

    @Override
    public void onLoginPressed(final String name) {
        Log.d(TAG, "onLoginPressed");

        GCMRegUtil
            .getNewGCMRegID(this)
            .flatMap(gcmId -> {
                Log.d(TAG, "onLoginPressed.getNewGCMRegID.callback");
                Person mee = Plus.PeopleApi.getCurrentPerson(googleApiClient);
                PreferenceUtil.setGCM(IntroActivity.this, gcmId);
                return restService.addUser(tmp_email, name, mee.getId(), gcmId);
            })
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(response -> {
                Log.d(TAG, "onLoginPressed.addUser.callback");
                if (response.isSuccess()) {
                    startMainActivity(tmp_email, name, response.getData().getPersonId());
                } else { // wtf ???
                    showMessage(response.getError().getStatusCode() + " " + response.getError().getMessage());
                }
            }, t -> {
                RetrofitError error = (RetrofitError) t;
                if (error.getResponse().getStatus() == HttpURLConnection.HTTP_CONFLICT) { // user already registered --> login
                    RestService.NetworkResponse response = (RestService.NetworkResponse) error.getBody();
                    startMainActivity(tmp_email, name, response.getError().getPersond_id());
                } else { // sth went really wrong so give up
                    showMessage(t.getLocalizedMessage());
                }
            });
    }

    private void startMainActivity(String email, String name, int person_id) {
        Log.d(TAG, "startMainActivity");
        PreferenceUtil.setUsingApp(this, true);
        PreferenceUtil.setAccountEMail(this, email);
        PreferenceUtil.setAccountName(this, name);
//        PreferenceUtil.setAccountPersonID(this, person_id);
        finish();
        startActivity(new Intent(this, MainActivity.class));
    }

}