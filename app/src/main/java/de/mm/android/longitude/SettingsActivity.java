package de.mm.android.longitude;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.mikepenz.aboutlibraries.LibsBuilder;

import org.jetbrains.annotations.Nullable;

import de.mm.android.longitude.base.GameActivity;
import de.mm.android.longitude.model.SignInFailureReason;
import de.mm.android.longitude.fragment.SettingsFragment;
import de.mm.android.longitude.network.GCMRegUtil;
import de.mm.android.longitude.network.RestService;
import de.mm.android.longitude.util.PreferenceUtil;
import retrofit.RetrofitError;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class SettingsActivity extends GameActivity implements SettingsFragment.ISettings {
    private static final String TAG = SettingsActivity.class.getSimpleName();

    private ProgressDialog dialog;
    private RestService webService;
    private Action1<Throwable> errorAction = t -> {
        Log.e(TAG, "errorAction", t);
        RetrofitError error = (RetrofitError) t;
        showMessage(error.getResponse().getStatus() + " " + error.getResponse().getReason());
        dialog.dismiss();
    };

    /* LifeCycle */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_settings);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setSubtitle(R.string.menu_settings);

        webService = RestService.Creator.create(this);

        dialog = new ProgressDialog(this);
        dialog.setTitle(R.string.please_wait);
        dialog.setInverseBackgroundForced(true);
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, getResources().getString(android.R.string.cancel), (dialog, which) -> dialog.dismiss()); // ATM.: no way no cancel Retrofit Requests

        getFragmentManager()
            .beginTransaction()
            .replace(R.id.ac_container, SettingsFragment.newInstance())
            .commit();
    }

    /* GameActivity */

    @Override
    public void onSuccess(@Nullable GoogleSignInAccount signInAccount) {
        Log.d(TAG, "onSuccess");
    }

    @Override
    public void onFailure(@NonNull final SignInFailureReason reason) {
        Log.d(TAG, "onFailure" + reason);
    }

    /* ISettings */

    @Override
    public void onInstantUploadClicked() {
        Log.d(TAG, "onInstantUploadClicked");
//        if (!isInetAvailable) {
//            showMessage(R.string.error_noNetworkConnectionFound);
//        } else {
//        dialog.show();
//        List<ProcessEntry> data = MyDBDelegate.selectProcessSince(this, PreferenceUtil.getLatestTimeStamp(this));
//        webService.uploadProcess(data);
//        }
    }

    @Override
    public void onGCMRegIDRenovationClicked() {
        Log.d(TAG, "onGCMRegIDRenovationClicked");
        if (!isInetAvailable()) {
            showMessage(R.string.error_noNetworkConnectionFound);
        } else {
            dialog.show();
            GCMRegUtil
                .getNewGCMRegID(this)
                .flatMap(webService::addGCMRegID)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(networkResponse -> {
                    dialog.dismiss();
                    showMessage(networkResponse.isSuccess() ? networkResponse.getData().getMessage() : networkResponse.getError().getMessage());
                }, errorAction);
        }
    }

    @Override
    public void onSignOut() {
        Log.d(TAG, "onSignOut");
        if (!isInetAvailable()) {
            showMessage(R.string.error_noNetworkConnectionFound);
        } else {
            dialog.show();
            revokeAccess(status -> {
                dialog.dismiss();
                PreferenceUtil.setUsingApp(this, false);
                PreferenceUtil.setAccountEMail(this, null);
                PreferenceUtil.setAccountName(this, null);
                finish();
            });
        }
    }

    @Override
    public void onLicensePressed() {
        new LibsBuilder()
            .withFields(R.string.class.getFields())
            .withAutoDetect(true)
            .withActivityTitle("Open Source License")
            .withVersionShown(false)
            .start(this);
    }

}