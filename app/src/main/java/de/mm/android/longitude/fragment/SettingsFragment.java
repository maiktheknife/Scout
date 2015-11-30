package de.mm.android.longitude.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Set;

import de.mm.android.longitude.R;
import de.mm.android.longitude.common.ButtonPreference;
import de.mm.android.longitude.common.Constants;
import de.mm.android.longitude.location.Tracking;
import de.mm.android.longitude.util.NetworkUtil;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    public interface ISettings {
        void onSignOut();
        void onInstantUploadClicked();
        void onGCMRegIDRenovationClicked();
        void onLicensePressed();
    }

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    private static final String TAG = SettingsFragment.class.getSimpleName();
    private ISettings callBack;

	/* LifeCycle */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(Constants.PREFS_NAME_SETTINGS);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
        addPreferencesFromResource(R.xml.prefs_settings);
        if (getActivity() instanceof ISettings) {
            callBack = (ISettings) getActivity();
        } else {
            throw new IllegalStateException(getActivity().getClass().getName() + " must implement " + ISettings.class.getName());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            initSummary(getPreferenceScreen().getPreference(i));
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable final Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        findPreference(getString(R.string.prefs_key_location_instantupload)).setOnPreferenceClickListener(preference -> {
            callBack.onInstantUploadClicked();
            return true;
        });

        findPreference(getString(R.string.prefs_key_gcm_renovate)).setOnPreferenceClickListener(preference -> {
            callBack.onGCMRegIDRenovationClicked();
            return true;
        });

        findPreference(getString(R.string.prefs_key_about_license)).setOnPreferenceClickListener(preference -> {
            callBack.onLicensePressed();
            return true;
        });

        ((ButtonPreference) findPreference(getString(R.string.prefs_key_account_email))).setOnButtonClickListener(callBack::onSignOut);

        /* Set Values */
        Preference p5 = findPreference(getString(R.string.prefs_key_about_version));
        String thisVersion;
        try {
            PackageInfo pi = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            thisVersion = pi.versionName  + " (" + pi.versionCode + ")";
        } catch (PackageManager.NameNotFoundException e) {
            thisVersion = "Could not get version name from manifest!";
        }
        p5.setSummary(thisVersion);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

	/* OnSharedPreferenceChangeListener */

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePrefSummary(findPreference(key));
    }

    private void initSummary(Preference p) {
        if (p instanceof PreferenceGroup) { // PreferenceCategory oder PreferenceScreen
            PreferenceGroup pCat = (PreferenceGroup) p;
            for (int i = 0; i < pCat.getPreferenceCount(); i++) {
                initSummary(pCat.getPreference(i));
            }
        } else {
            updatePrefSummary(p);
        }
    }

    private void updatePrefSummary(Preference p) {

        if (p == null || p instanceof PreferenceScreen || p instanceof PreferenceCategory || p.getKey() == null) {
            return;
        }

        if (p instanceof ListPreference) {
            ListPreference listPref = (ListPreference) p;
            p.setSummary(listPref.getEntry());

        } else if (p instanceof MultiSelectListPreference) {
            MultiSelectListPreference m = (MultiSelectListPreference) p;

            String s = "";
            Set<String> c = m.getValues();
            CharSequence[] x = m.getEntryValues();
            CharSequence[] y = m.getEntries();

            for (int i = 0; i < x.length; i++) {
                if (c.contains(x[i])) {
                    s += y[i] + ", ";
                }
            }
            if (s.length() > 2) {
                s = s.substring(0, s.length() - 2);
            } else {
                s = "Keine";
            }

            m.setSummary(s);

        }  else if (p.getKey().equals(getString(R.string.prefs_key_account_email))){
            String account = getPreferenceManager().getSharedPreferences().getString(getString(R.string.prefs_key_account_email), "");
            p.setSummary(account);

        } else if (p.getKey().equals(getString(R.string.prefs_key_location_tracking))) {
            TwoStatePreference box = (TwoStatePreference) p;
            if (box.isChecked()) {
                new Tracking().start(getActivity());
            } else {
                new Tracking().stop(getActivity());
            }

        } else if (p.getKey().equals(getString(R.string.prefs_key_location_interval))) {
            new Tracking().start(getActivity());

        } else if (p.getKey().equals(getString(R.string.prefs_key_location_locating))) {

            String locating = "";
            boolean isGPS = NetworkUtil.isGPSEnabled(getActivity());
            boolean isNetwork = NetworkUtil.isNetworkEnabled(getActivity());

            if (isGPS && isNetwork) {
                locating += "Netzwerk und GPS ";
            } else if (isGPS) {
                locating += "GPS ";
            } else if (isNetwork) {
                locating += "Netzwerk";
            } else {
                locating = "deaktiviert";
            }
            p.setSummary(locating);

        }
    }

}