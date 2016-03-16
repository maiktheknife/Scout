package de.mm.android.longitude.network;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.mm.android.longitude.BuildConfig;
import de.mm.android.longitude.model.ContactData;
import de.mm.android.longitude.util.PreferenceUtil;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.http.DELETE;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import rx.Observable;

/**
 * Created by Max on 02.08.2015.
 */
public interface RestService {

    /* User Group */

    @FormUrlEncoded
    @POST("/users")
    Observable<NetworkResponse> addUser(@Field("email") String email, @Field("name") String userName, @Field("plus_id") String plusID, @Field("gcm_reg_id") String gcm_reg_id);

    @DELETE("/users/{userid}")
    Observable<NetworkResponse> deleteUser(@Path("userid") int userId);

    /* Friend Group */

    @GET("/friends")
    Observable<NetworkResponse> getFriends();

    @FormUrlEncoded
    @POST("/friends")
    Observable<NetworkResponse> addFriend(@Field("friendemail") String email);

    @GET("/friends/unconfirmed")
    Observable<NetworkResponse> getNewFriends();

    @GET("/friends/poke")
    Observable<NetworkResponse> pokeFriends();

    @FormUrlEncoded
    @PUT("/friends/{friendid}")
    Observable<NetworkResponse> confirmFriend(@Path("friendid") int friendId, @Field("accepted") boolean isAccepted);

    @DELETE("/friends/{friendid}")
    Observable<NetworkResponse> deleteFriend(@Path("friendid") int friendId);

    @GET("/friends/{friendid}/poke")
    Observable<NetworkResponse> pokeFriend(@Path("friendid") int friendId);

    /* Device Group */

    @FormUrlEncoded
    @POST("/device")
    Observable<NetworkResponse> addGCMRegID(@Field("gcm_reg_id") String gcm_reg_id);

    /* Location Group */

    @FormUrlEncoded
    @POST("/locations")
    Observable<NetworkResponse> addLocation(
            @Field("latitude") double latitude, @Field("longitude") double longitude,
            @Field("altitude") double altitude, @Field("accuracy") double accuracy,
            @Field("address") String address, @Field("updated_on") String updatedOn);

//    http://stackoverflow.com/questions/28536522/intercept-and-retry-call-by-means-of-okhttp-interceptors
//    http://stackoverflow.com/questions/12806386/standard-json-api-response-format
//    http://www.sitepoint.com/best-practices-rest-api-scratch-introduction/
//    http://blog.mwaysolutions.com/2014/06/05/10-best-practices-for-better-restful-api/
//    http://www.vinaysahni.com/best-practices-for-a-pragmatic-restful-api

    class Creator {
        private static final String TAG = Creator.class.getSimpleName();
        private static RestService INSTANCE;

        private Creator() {}

        public static RestService create(final Context context) {
            if (INSTANCE != null) {
                return INSTANCE;
            }

            OkHttpClient httpClient = new OkHttpClient();
            httpClient.setConnectTimeout(10, TimeUnit.SECONDS);
            httpClient.interceptors().add(chain -> {
                Request request = chain
                        .request()
                        .newBuilder()
                        .addHeader("token", PreferenceUtil.getAccountToken(context))
                        .build();
                Response response = chain.proceed(request);
                if (!response.isSuccessful() && response.code() == HttpURLConnection.HTTP_UNAUTHORIZED || response.code() == HttpURLConnection.HTTP_FORBIDDEN) {
                    Log.d(TAG, "intercept because of failure --> get new token");
                    try {
                        String token = GoogleAuthUtil.getToken(context, PreferenceUtil.getAccountEMail(context), BuildConfig.GOOGLE_LOGIN_SCOPE, null);
                        PreferenceUtil.setAccountToken(context, token);
                        Request newRequest = chain.request();
                        newRequest = newRequest.newBuilder()
                                .addHeader("token", token)
                                .build();
                        response = chain.proceed(newRequest);

                    } catch (UserRecoverableAuthException e) {
                        Log.e(TAG, "RestService.UserRecoverableAuthException", e);
//                            context.startActivityForResult(e.getIntent(), Constants.REQUEST_PICK_GOOGLE_ACCOUNT);
                        throw new RuntimeException(e);

                    } catch (GoogleAuthException e) {
                        Log.e(TAG, "RestService.GoogleAuthException", e);
                        throw new RuntimeException(e);
                    }
                }
                return response;
            });

            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint(BuildConfig.BACKEND_ENDPOINT)
                    .setLogLevel(RestAdapter.LogLevel.BASIC)
                    .setClient(new OkClient(httpClient))
                    .build();

            INSTANCE = restAdapter.create(RestService.class);
            return INSTANCE;
        }
    }

    class NetworkResponse {
        private boolean isSuccess;
        private Error error;
        private Data data;

        public class Error{
            int statusCode;
            int persond_id;
            String message;

            public int getStatusCode() {
                return statusCode;
            }

            public int getPersond_id() {
                return persond_id;
            }

            public String getMessage() {
                return message;
            }

            @Override
            public String toString() {
                return "Error{" +
                        "statusCode=" + statusCode +
                        ", persond_id=" + persond_id +
                        ", message='" + message + '\'' +
                        '}';
            }
        }
        public class Data {
            int person_id;
            String message;
            List<ContactData> friends;
            List<ContactData> newfriends;

            public int getPersonId() {
                return person_id;
            }

            public String getMessage() {
                return message;
            }

            public List<ContactData> getFriends() {
                return friends;
            }

            public List<ContactData> getNewFriends() {
                return newfriends;
            }

            @Override
            public String toString() {
                return "Data{" +
                        "person_id=" + person_id +
                        ", message='" + message + '\'' +
                        ", friends=" + friends +
                        ", newfriends=" + newfriends +
                        '}';
            }
        }

        public boolean isSuccess() {
            return isSuccess;
        }

        public Data getData() {
            return data;
        }

        public Error getError() {
            return error;
        }

        @Override
        public String toString() {
            return "NetworkResponse{" +
                    "isSuccess=" + isSuccess +
                    ", error=" + error +
                    ", data=" + data +
                    '}';
        }

    }

}