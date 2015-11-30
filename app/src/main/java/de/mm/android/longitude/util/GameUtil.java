package de.mm.android.longitude.util;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;

import de.mm.android.longitude.R;

/**
 * Created by Max on 22.04.2015.
 */
public class GameUtil {
    private static final String TAG = GameUtil.class.getSimpleName();

    private static final float MOON = 384_400; // km
    private static final float MOON_STEP = MOON / 100f; // km

    private GameUtil() {}

    public static void inviteFriends(Context context){
        Intent sendIntent = new Intent()
                .setAction(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "Try Scout!, " + context.getString(R.string.website));
        context.startActivity(Intent.createChooser(sendIntent, "Einladen"));
    }

    /* Achievement */

    public static void incrementAppUsage(Context context, GoogleApiClient googleApiClient) {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            Games.Achievements.increment(googleApiClient, context.getResources().getString(R.string.achievement_addicted), 1);
        }
    }

    public static void incrementDistance(Context context, GoogleApiClient googleApiClient, Location curLocation) {
        Log.d(TAG, "incrementDistance");

        Location latestLocation = PreferenceUtil.getLatestLocation(context);
        if (latestLocation == null) {
            Log.w(TAG, "keine Alt Location, schöne scheiße ...");
            return;
        }

        final float traveledDistanceMeter = latestLocation.distanceTo(curLocation); // in m
        final float alreadyGoneMeter = PreferenceUtil.getTraveledDistance(context); // in m

//        final float alreadyGoneMeter = 900f; // in m
//        final float traveledDistanceMeter = 1200f; // in m

//        final float alreadyGoneMeter = 500f; // in m
//        final float traveledDistanceMeter = 400f; // in m

//        final float alreadyGoneMeter = 900f; // in m
//        final float traveledDistanceMeter = MOON_STEP * 1_000f; // in m

//        final float alreadyGoneMeter = 500f; // in m
//        final float traveledDistanceMeter = 1000_000f; // in m

        float traveledDistance = traveledDistanceMeter / 1_000f; // in km
        float alreadyGone = alreadyGoneMeter / 1_000f; // in km
        double nextStep = Math.max(Math.ceil(alreadyGone), 1);
        float nextMoonStep = getNextStepToMoon(alreadyGone);

        Log.d(TAG, "alreadyGone: " + alreadyGone + "km, nextStep: " + nextStep + "km, traveledDistance: " + traveledDistance + "km");
        alreadyGone += traveledDistance;
        Log.d(TAG, "neuer alreadyGone: " + alreadyGone + "km");

        if (alreadyGone >= nextStep && googleApiClient != null && googleApiClient.isConnected()) {
            int step = (int) Math.ceil(alreadyGone - nextStep);
            Log.d(TAG, "increment: snail + adventurer: " + (alreadyGone - nextStep) + " " + step);
            Games.Achievements.increment(googleApiClient, context.getResources().getString(R.string.achievement_snail), step);
            Games.Achievements.increment(googleApiClient, context.getResources().getString(R.string.achievement_adventurer), step);

            if (alreadyGone >= 1337) {
                Log.d(TAG, "unlock: 1337");
                Games.Achievements.unlock(googleApiClient, context.getResources().getString(R.string.achievement_1337));
            }

            if (alreadyGone >= nextMoonStep) {
                step = (int) Math.ceil(alreadyGone - nextMoonStep);
                Log.d(TAG, "increment: moon: " + (alreadyGone - nextMoonStep) + " " + step);
                Games.Achievements.increment(googleApiClient, context.getResources().getString(R.string.achievement_widely_traveled), step);
            }

            if (traveledDistance >= 1000) {
                Log.d(TAG, "unlock: Faster then light");
                Games.Achievements.unlock(googleApiClient, context.getResources().getString(R.string.achievement_faster_than_light));
            }

        } else {
            Log.d(TAG, "reicht nicht für eine Erhöhung: " + alreadyGone + "km");
        }

        final float nextValue = traveledDistanceMeter + alreadyGoneMeter;
        Log.d(TAG, "setTraveledDistance: " + nextValue + "m");
        PreferenceUtil.setTraveledDistance(context, nextValue); // in m
    }

    public static void incrementPokeCount(Context context, GoogleApiClient googleApiClient) {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            Games.Achievements.increment(googleApiClient, context.getResources().getString(R.string.achievement_novice_penetrator), 1);
            Games.Achievements.increment(googleApiClient, context.getResources().getString(R.string.achievement_master_penetrator), 1);
        }
    }

    public static void incrementFriendCount(Context context, GoogleApiClient googleApiClient) {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            Games.Achievements.increment(googleApiClient, context.getResources().getString(R.string.achievement_popular), 1);
        }
    }

    public static void incrementPromotionCount(Context context, GoogleApiClient googleApiClient) {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            Games.Achievements.increment(googleApiClient, context.getResources().getString(R.string.achievement_who_is_afraid_of_the_nsa), 1);
        }
    }

    /* Leaderboard */

    public static void submitDistanceScore(Context context, GoogleApiClient googleApiClient, long distance) {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            Games.Leaderboards.submitScore(googleApiClient, context.getResources().getString(R.string.leaderboard_traveled_distance), distance);
        }
    }

    /* Helper */

    private static float getNextStepToMoon(float distance) {
        float step = 0;
        while (step < distance) {
            step += MOON_STEP;
        }
        return step;
    }

}