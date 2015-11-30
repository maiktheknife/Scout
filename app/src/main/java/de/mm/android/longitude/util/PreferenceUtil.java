package de.mm.android.longitude.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import de.mm.android.longitude.R;
import de.mm.android.longitude.common.Constants;
import de.mm.android.longitude.network.GCMListenerService.GCMAttitude;

public class PreferenceUtil {

	public static SharedPreferences getMyPreferencesSettings(Context context) {
		return context.getSharedPreferences(Constants.PREFS_NAME_SETTINGS, Context.MODE_PRIVATE); 
	}

	public static SharedPreferences getMyPreferencesStuff(Context context) {
		return context.getSharedPreferences(Constants.PREFS_NAME_STUFF, Context.MODE_PRIVATE); 
	}

	public static SharedPreferences getMyPreferencesGoogle(Context context) {
        return context.getSharedPreferences(Constants.PREFS_NAME_GOOGLE, Context.MODE_PRIVATE);
    }

    private static String getString(Context c, int id){
        return c.getResources().getString(id);
    }

	/* APP Usage */

	public static boolean isUsingApp(Context context) {
		return getMyPreferencesStuff(context).getBoolean(getString(context, R.string.prefs_key_common_usage), false);
	}

	public static void setUsingApp(Context context, boolean useApp) {
		getMyPreferencesStuff(context)
				.edit()
				.putBoolean(getString(context, R.string.prefs_key_common_usage), useApp)
				.commit();
	}


	/* ID */

//	public static int getAccountPersonID(Context context) {
//		return getMyPreferencesStuff(context).getInt((getString(context, R.string.prefs_key_account_id)), -1);
//	}
//
//	public static void setAccountPersonID(Context context, int personId) {
//		getMyPreferencesStuff(context)
//				.edit()
//				.putInt(getString(context, R.string.prefs_key_account_id), personId)
//				.commit();
//	}

	/* E-Mail */
	
	public static String getAccountEMail(Context context) {
		return getMyPreferencesSettings(context).getString(getString(context, R.string.prefs_key_account_email), "");
	}
	
	public static void setAccountEMail(Context context, String account) {
		getMyPreferencesSettings(context)
			.edit()
			.putString(getString(context, R.string.prefs_key_account_email), account)
			.commit();
	}

    /* Acccount Name */

    public static String getAccountName(Context context) {
        return getMyPreferencesSettings(context).getString(getString(context, R.string.prefs_key_account_name), "");
    }

    public static void setAccountName(Context context, String displayName) {
        getMyPreferencesSettings(context)
                .edit()
                .putString(getString(context, R.string.prefs_key_account_name), displayName)
                .commit();
    }

	/* Token */
	
	public static String getAccountToken(Context context) {
        return getMyPreferencesSettings(context).getString(getString(context, R.string.prefs_key_account_token), "");
	}
	
	public static void setAccountToken(Context context, String token) {
		getMyPreferencesSettings(context)
			.edit()
			.putString(getString(context, R.string.prefs_key_account_token), token)
			.commit();
	}
	
	/* MapMode */
	
	public static int getMapMode(Context context) {
        return getMyPreferencesStuff(context).getInt(getString(context, R.string.prefs_key_map_mode), 1);
    }
	
	public static void setMapMode(Context context, int mode) {
        getMyPreferencesStuff(context)
			.edit()
			.putInt(getString(context, R.string.prefs_key_map_mode), mode)
			.commit();
	}

    /* MapZoom */

    public static float getMapZoom(Context context) {
        return getMyPreferencesStuff(context).getFloat(getString(context, R.string.prefs_key_map_zoom), 15);
    }

    public static void setMapZoom(Context context, float zoom) {
        getMyPreferencesStuff(context)
            .edit()
            .putFloat(getString(context, R.string.prefs_key_map_zoom), zoom)
            .commit();
    }
	
	/* Tracking */
	
	public static boolean isTrackingEnabled(Context context) {
		return getMyPreferencesSettings(context).getBoolean(getString(context, R.string.prefs_key_location_tracking), false);
	}
	
	public static void setTrackingEnabled(Context context, boolean isEnabled) {
		getMyPreferencesSettings(context)
			.edit()
			.putBoolean(getString(context, R.string.prefs_key_location_tracking), isEnabled)
			.commit();
	}
	
	/* Interval */
	
	public static int getInterval(Context context) {
		return Integer.parseInt(getMyPreferencesSettings(context).getString(getString(context, R.string.prefs_key_location_interval), "120"));
	}
	
	public static void setInterval(Context context, int interval) {
		getMyPreferencesSettings(context)
			.edit()
			.putString(getString(context, R.string.prefs_key_location_interval), String.valueOf(interval))
			.commit();
	}
	
	/* Is first Launch */
	
	public static boolean isFirstLaunch(Context context, String which) {
		return getMyPreferencesStuff(context).getBoolean("is_firstlaunch_" + which, true);
	}

	public static void setFirstLaunch(Context context, String which, boolean isfirstlaunch) {
		getMyPreferencesStuff(context)
			.edit()
			.putBoolean("is_firstlaunch_" + which, isfirstlaunch)
			.commit();
	}
	
	/* Latest Location */

	public static Location getLatestLocation(Context context){
		SharedPreferences sp = getMyPreferencesStuff(context);
        if (sp.contains(getString(context,R.string.prefs_key_location_latest_lat))) {
            Location l = new Location("LongitudeStorageProvider");
            l.setLatitude(sp.getFloat(getString(context, R.string.prefs_key_location_latest_lat), 0));
            l.setLongitude(sp.getFloat(getString(context, R.string.prefs_key_location_latest_lon), 0));
            return l;
        }else {
            return null;
        }
	}
	
	public static void setLatestLocation(Context context, Location location){
		getMyPreferencesStuff(context)
			.edit()
			.putFloat(getString(context, R.string.prefs_key_location_latest_lat), (float) location.getLatitude())
			.putFloat(getString(context, R.string.prefs_key_location_latest_lon), (float) location.getLongitude())
			.commit();
	}
	
	/* GCM */

	public static String getGCM(Context context) {
		return getMyPreferencesGoogle(context).getString(getString(context, R.string.prefs_key_gcm_id), "");
	}
	
	public static void setGCM(Context context, String reg_id) {
        getMyPreferencesGoogle(context)
			.edit()
			.putString(getString(context, R.string.prefs_key_gcm_id), reg_id)
			.commit();
	}

	/* Process */

	public static Calendar getLatestTimeStamp(Context context) {
		long latestUpload = getMyPreferencesStuff(context).getLong(getString(context, R.string.prefs_key_upload_timestamp), 0);
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(latestUpload);
		return c;
	}

	public static void setLatestTimeStamp(Context context, long when) {
		getMyPreferencesStuff(context)
			.edit()
			.putLong(getString(context, R.string.prefs_key_upload_timestamp), when)
			.commit();
	}
	
	/* new Results */

//	public static void setIsNewResults(Context context, boolean b) {
//		getMyPreferencesStuff(context)
//			.edit()
//			.putBoolean(Constants.PROPERTY_NEW_RESULTS, b)
//			.commit();
//	}
//
//	public static boolean isNewResults(Context context) {
//		return getMyPreferencesStuff(context).getBoolean(Constants.PROPERTY_NEW_RESULTS, true);
//	}
//
//	public static void setIncognito(Context context, boolean b) {
//		getMyPreferencesSettings(context)
//			.edit()
//			.putBoolean(Constants.PROPERTY_INCOGNITO, b)
//			.commit();
//	}

	/* GCM Verhalten */
	
	public static GCMAttitude getGCMAttitude(Context context) {
		int type = Integer.parseInt(getMyPreferencesSettings(context).getString(getString(context, R.string.prefs_key_gcm_attitude), "0"));
		GCMAttitude attitude;
		switch (type) {
		case 0:
			attitude = GCMAttitude.ASK;
			break;
		case 1:
			attitude = GCMAttitude.ALWAYS;
			break;
		case 2:
			attitude = GCMAttitude.NEVER;
			break;
		default:
			attitude = GCMAttitude.ASK;
			break;
		}
		return attitude;
	}
	
	public static Set<Integer> getGCMNotification(Context context) {
		
		Set<Integer> result = new HashSet<Integer>();
		Set<String> answer = getMyPreferencesSettings(context).getStringSet(getString(context, R.string.prefs_key_gcm_notification), new HashSet<String>());
		
		for (String string : answer) {
			result.add(Integer.valueOf(string));
		}
		
		return result;
	}

	/* goneDistance */

	public static float getTraveledDistance(Context context){
		return getMyPreferencesStuff(context).getFloat(getString(context, R.string.prefs_key_traveleddistance), 0);
	}

	public static void setTraveledDistance(Context context, float distance){
        getMyPreferencesStuff(context)
				.edit()
				.putFloat(getString(context, R.string.prefs_key_traveleddistance), distance)
				.apply();
	}

    private PreferenceUtil(){}

	/* App Version */

	public static String getVersion(Context context) {
        return getMyPreferencesStuff(context).getString(getString(context, R.string.prefs_key_common_version), "nothing there");
	}

	public static void setVersion(Context context, String version) {
		getMyPreferencesStuff(context)
			.edit()
			.putString(getString(context, R.string.prefs_key_common_version), version)
			.apply();
	}
}