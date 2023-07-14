package com.service.provision.receiver;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.API_URL;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.service.provision.R;
import com.service.provision.activity.DriverFoundActivity;
import com.service.provision.activity.MessageActivity;
import com.service.provision.activity.RiderHomeActivity;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmCart;
import com.service.provision.realm.RealmChat;
import com.service.provision.realm.RealmProvider;
import com.service.provision.util.RealmUtility;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by Andy on 11/8/2019.
 */


public class AlarmReceiver extends BroadcastReceiver {

    private static final String NOTIFICATION_CHANNEL_ID = "channel_id";
    private static final String CHANNEL_NAME = "NOTIFICATION_CHANNEL";

    @Override
    public void onReceive(Context context, Intent intent) {
        //Remember in the SetAlarm file we made an intent to this, this is way this work, otherwise you would have to put an action
        /*Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(2000);*/


        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        // Importance applicable to all the notifications in this Channel
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        // Notification channel should only be created for devices running Android 26
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, CHANNEL_NAME, importance);
            //Boolean value to set if lights are enabled for Notifications from this Channel
            notificationChannel.enableLights(true);
            //Boolean value to set if vibration are enabled for Notifications from this Channel
            notificationChannel.enableVibration(true);
            //Sets the color of Notification Light
            notificationChannel.setLightColor(Color.GREEN);
            //Set the vibration pattern for notifications. Pattern is in milliseconds with the format {delay,play,sleep,play,sleep...}
            notificationChannel.setVibrationPattern(new long[]{500, 500, 500, 500, 500});
            notificationManager.createNotificationChannel(notificationChannel);
            //Sets whether notifications from these Channel should be visible on Lockscreen or not
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        }

        Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.logo_splash);
        String type = intent.getStringExtra("TYPE");
        String title = "";
        String body = "";

        if (type == null || type.equals("")) {
            return;
        }

        switch (type) {
            case "chat":
                title = intent.getStringExtra("NAME");
                body = intent.getStringExtra("BODY");
                break;
            case "ride_request":
                if (!getRole(context).equals("DRIVER")) {
                    return;
                }
                title = "Ride Request";
                body = "You have received a ride request";
                break;
            case "ride_request_accepted":
                if (!getRole(context).equals("CUSTOMER")) {
                    return;
                }
                title = "Driver Arriving.";
                body = "Your driver is on his way";
                break;
            case "ride_ended":
                if (!getRole(context).equals("CUSTOMER")) {
                    return;
                }
                title = "Ride Ended";
                body = "Your trip has ended.\n\nPay " + intent.getStringExtra("PRICE") + " to your driver";
                break;
            case "ride_cancelled_by_driver":
                if (!getRole(context).equals("CUSTOMER")) {
                    return;
                }
                title = "Ride Cancelled";
                body = "Ride cancelled by driver";
                break;
            case "ride_cancelled_by_customer":
                if (!getRole(context).equals("DRIVER")) {
                    return;
                }
                title = "Ride Cancelled";
                body = "Ride cancelled by customer";
                break;
            case "driver_arrived":
                if (!getRole(context).equals("CUSTOMER")) {
                    return;
                }
                title = "Driver Arrived";
                body = "Your driver has arrived";
                break;
            case "destination_changed":
                if (!getRole(context).equals("DRIVER")) {
                    return;
                }
                title = "Destination Changed";
                body = "Destination has been updated";
                break;

        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.service)
                .setContentTitle(title)
                .setContentText(body)
                .setLargeIcon(icon)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true);

        final Intent[] notificationIntent = {null};

        switch (type) {
            case "chat":
                String customer_id = intent.getStringExtra("CUSTOMER_ID");
                String provider_id = intent.getStringExtra("PROVIDER_ID");
                String role = PreferenceManager.getDefaultSharedPreferences(context).getString("ROLE", "");
                if (role.equals("PROVIDER")) {
                    final String[] customer_name = new String[1];
                    final String[] profile_image_url = new String[1];

                    Realm.init(context);
                    final RealmCart[] realmCart = {Realm.getInstance(RealmUtility.getDefaultConfig(context)).where(RealmCart.class).equalTo("customer_id", customer_id).findFirst()};
                    customer_name[0] = realmCart[0].getCustomer_name();
                    profile_image_url[0] = realmCart[0].getCustomer_image_url();

                    StringRequest stringRequest = new StringRequest(
                            Request.Method.POST,
                            API_URL + "chat-data",
                            response -> {
                                if (response != null) {
                                    
                                    try {
                                        JSONObject jsonObject = new JSONObject(response);
                                        Realm.init(context);
                                        Realm.getInstance(RealmUtility.getDefaultConfig(context)).executeTransaction(realm -> {
                                            try {
                                                realmCart[0] = realm.createOrUpdateObjectFromJson(RealmCart.class, jsonObject.getJSONObject("cart"));
                                                realm.createOrUpdateAllFromJson(RealmChat.class, jsonObject.getJSONArray("chats"));
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            customer_name[0] = realmCart[0].getCustomer_name();
                                            profile_image_url[0] = realmCart[0].getCustomer_image_url();
                                        });

                                        notificationIntent[0] = new  Intent(context, MessageActivity.class)
                                                .putExtra("CUSTOMER_ID", customer_id)
                                                .putExtra("CUSTOMER_NAME", customer_name[0])
                                                .putExtra("PROFILE_IMAGE_URL", profile_image_url[0]);

                                        notificationIntent[0].addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                                        PendingIntent contentIntent = PendingIntent.getActivity(context, 1000, notificationIntent[0],
                                                PendingIntent.FLAG_UPDATE_CURRENT);
                                        builder.setContentIntent(contentIntent);
                                        // Add as notification
                                        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                                        manager.notify(1000, builder.build());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            },
                            error -> {
                                error.printStackTrace();
                                notificationIntent[0] = new  Intent(context, MessageActivity.class)
                                        .putExtra("CUSTOMER_ID", customer_id)
                                        .putExtra("CUSTOMER_NAME", customer_name[0])
                                        .putExtra("PROFILE_IMAGE_URL", profile_image_url[0]);
                                notificationIntent[0].addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                                PendingIntent contentIntent = PendingIntent.getActivity(context, 1000, notificationIntent[0],
                                        PendingIntent.FLAG_UPDATE_CURRENT);
                                builder.setContentIntent(contentIntent);
                                // Add as notification
                                NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                                manager.notify(1000, builder.build());
                                Log.d("Cyrilll", error.toString());
                            }
                    ) {
                        @Override
                        public Map<String, String> getParams() throws AuthFailureError {
                            Map<String, String> params = new HashMap<>();
                            params.put("customer_id", customer_id);
                            params.put("provider_id", provider_id);
                            Realm.init(context);
                            Realm.getInstance(RealmUtility.getDefaultConfig(context)).executeTransaction(realm -> {
                                RealmResults<RealmChat> results = realm.where(RealmChat.class)
                                        .sort("id", Sort.DESCENDING)
                                        .equalTo("provider_id", provider_id)
                                        .equalTo("customer_id", customer_id)
                                        .findAll();
                                ArrayList<RealmChat> myArrayList = new ArrayList<>();
                                for (RealmChat realmChat : results) {
                                    if (!(realmChat.getChat_id().startsWith("z"))) {
                                        myArrayList.add(realmChat);
                                    }
                                }
                                if (results.size() < 3) {
                                    params.put("id", "0");
                                }
                                else{
                                    params.put("id", String.valueOf(myArrayList.get(0).getId()));
                                }
                            });
                            return params;
                        }
                        /** Passing some request headers* */
                        @Override
                        public Map getHeaders() throws AuthFailureError {
                            HashMap headers = new HashMap();
                            headers.put("accept", "application/json");
                            headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(context).getString(APITOKEN, ""));
                            return headers;
                        }
                    };
                    stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                            0,
                            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                    InitApplication.getInstance().addToRequestQueue(stringRequest);
                } else {
                    final String[] provider_name = new String[1];
                    final String[] profile_image_url = new String[1];
                    final String[] availability = new String[1];

                    Realm.init(context);
                    final RealmCart[] realmCart = {Realm.getInstance(RealmUtility.getDefaultConfig(context)).where(RealmCart.class).equalTo("provider_id", provider_id).findFirst()};
                    provider_name[0] = realmCart[0].getProvider_title() != null && !realmCart[0].getProvider_title().equals("") ? StringUtils.normalizeSpace((realmCart[0].getProvider_title() + " " + realmCart[0].getProvider_first_name() + " " + realmCart[0].getProvider_other_name() + " " + realmCart[0].getProvider_last_name()).replace("null", "")) : realmCart[0].getProvider_name();
                    profile_image_url[0] = realmCart[0].getProvider_image_url();
                    availability[0] = realmCart[0].getProvider_availability();

                    StringRequest stringRequest = new StringRequest(
                            Request.Method.POST,
                            API_URL + "chat-data",
                            response -> {
                                if (response != null) {
                                    
                                    try {
                                        JSONObject jsonObject = new JSONObject(response);
                                        Realm.init(context);
                                        Realm.getInstance(RealmUtility.getDefaultConfig(context)).executeTransaction(realm -> {
                                            try {
                                                realmCart[0] = realm.createOrUpdateObjectFromJson(RealmCart.class, jsonObject.getJSONObject("cart"));
                                                realm.createOrUpdateAllFromJson(RealmChat.class, jsonObject.getJSONArray("chats"));
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            provider_name[0] = realmCart[0].getProvider_title() != null && !realmCart[0].getProvider_title().equals("") ? StringUtils.normalizeSpace((realmCart[0].getProvider_title() + " " + realmCart[0].getProvider_first_name() + " " + realmCart[0].getProvider_other_name() + " " + realmCart[0].getProvider_last_name()).replace("null", "")) : realmCart[0].getProvider_name();
                                            profile_image_url[0] = realmCart[0].getProvider_image_url();
                                            availability[0] = realmCart[0].getProvider_availability();
                                        });

                                        notificationIntent[0] = new  Intent(context, MessageActivity.class)
                                                .putExtra("PROVIDER_ID", provider_id)
                                                .putExtra("PROVIDER_NAME", provider_name[0])
                                                .putExtra("PROFILE_IMAGE_URL", profile_image_url[0])
                                                .putExtra("AVAILABILITY", availability[0]);

                                        notificationIntent[0].addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                                        PendingIntent contentIntent = PendingIntent.getActivity(context, 1000, notificationIntent[0],
                                                PendingIntent.FLAG_UPDATE_CURRENT);
                                        builder.setContentIntent(contentIntent);
                                        // Add as notification
                                        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                                        manager.notify(1000, builder.build());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            },
                            error -> {
                                error.printStackTrace();
                                notificationIntent[0] = new Intent(context, MessageActivity.class)
                                        .putExtra("PROVIDER_ID", provider_id)
                                        .putExtra("PROVIDER_NAME", provider_name[0])
                                        .putExtra("PROFILE_IMAGE_URL", profile_image_url[0])
                                        .putExtra("AVAILABILITY", availability[0]);

                                notificationIntent[0].addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                                PendingIntent contentIntent = PendingIntent.getActivity(context, 1000, notificationIntent[0],
                                        PendingIntent.FLAG_UPDATE_CURRENT);
                                builder.setContentIntent(contentIntent);
                                // Add as notification
                                NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                                manager.notify(1000, builder.build());
                                Log.d("Cyrilll", error.toString());
                            }
                    ) {
                        @Override
                        public Map<String, String> getParams() throws AuthFailureError {
                            Map<String, String> params = new HashMap<>();
                            params.put("provider_id", provider_id);
                            params.put("customer_id", customer_id);
                            Realm.init(context);
                            Realm.getInstance(RealmUtility.getDefaultConfig(context)).executeTransaction(realm -> {
                                RealmResults<RealmChat> results = realm.where(RealmChat.class)
                                        .sort("id", Sort.DESCENDING)
                                        .equalTo("provider_id", provider_id)
                                        .equalTo("customer_id", customer_id)
                                        .findAll();
                                ArrayList<RealmChat> myArrayList = new ArrayList<>();
                                for (RealmChat realmChat : results) {
                                    if (!(realmChat.getChat_id().startsWith("z"))) {
                                        myArrayList.add(realmChat);
                                    }
                                }
                                if (results.size() < 3) {
                                    params.put("id", "0");
                                }
                                else{
                                    params.put("id", String.valueOf(myArrayList.get(0).getId()));
                                }
                            });
                            return params;
                        }
                        /** Passing some request headers* */
                        @Override
                        public Map getHeaders() throws AuthFailureError {
                            HashMap headers = new HashMap();
                            headers.put("accept", "application/json");
                            headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(context).getString(APITOKEN, ""));
                            return headers;
                        }
                    };
                    stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                            0,
                            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                    InitApplication.getInstance().addToRequestQueue(stringRequest);
                }

                break;
            case "ride_request":
                notificationIntent[0] = new  Intent(context, RiderHomeActivity.class)
                        .putExtra("LAUNCHED_FROM_NOTIFICATION", true)
                        .putExtra("RIDE_HISTORY_ID", intent.getStringExtra("RIDE_HISTORY_ID"))
                        .putExtra("DISTANCE", intent.getStringExtra("DISTANCE"))
                        .putExtra("PICKUP_LAT", intent.getStringExtra("PICKUP_LAT"))
                        .putExtra("PICKUP_LONG", intent.getStringExtra("PICKUP_LONG"))
                        .putExtra("DESTINATION_LAT", intent.getStringExtra("DESTINATION_LAT"))
                        .putExtra("DESTINATION_LONG", intent.getStringExtra("DESTINATION_LONG"))
                        .putExtra("CUSTOMER_NAME", intent.getStringExtra("CUSTOMER_NAME"))
                        .putExtra("CUSTOMER_PROFILE_IMAGE_URL", intent.getStringExtra("CUSTOMER_PROFILE_IMAGE_URL"))
                        .putExtra("PICKUP_ADDRESS", intent.getStringExtra("PICKUP_ADDRESS"))
                        .putExtra("DESTINATION_ADDRESS", intent.getStringExtra("DESTINATION_ADDRESS"))
                        .putExtra("SERVICE_ID", intent.getStringExtra("SERVICE_ID"))
                        .putExtra("CUSTOMER_PRIMARY_CONTACT", intent.getStringExtra("CUSTOMER_PRIMARY_CONTACT"))
                        .putExtra("CUSTOMER_CONFIRMATION_TOKEN", intent.getStringExtra("CUSTOMER_CONFIRMATION_TOKEN"))
                        .putExtra("CUSTOMER_ID", intent.getStringExtra("CUSTOMER_ID"))
                        .putExtra("PROVIDER_ID", intent.getStringExtra("PROVIDER_ID"));
                notificationIntent[0].addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                PendingIntent contentIntent = PendingIntent.getActivity(context, 1000, notificationIntent[0],
                        PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setContentIntent(contentIntent);
                // Add as notification
                NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                manager.notify(1000, builder.build());
                break;

            case "ride_request_accepted":
                notificationIntent[0] = new  Intent(context, DriverFoundActivity.class)
                        .putExtra("PROVIDER_LONG", intent.getStringExtra("PROVIDER_LONG"))
                        .putExtra("CUSTOMER_NAME", intent.getStringExtra("CUSTOMER_NAME"));
                notificationIntent[0].addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                PendingIntent contentIntent2 = PendingIntent.getActivity(context, 1000, notificationIntent[0],
                        PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setContentIntent(contentIntent2);
                // Add as notification
                NotificationManager manager2 = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                manager2.notify(1000, builder.build());
                break;
            case "destination_changed":
                notificationIntent[0] = new  Intent(context, RiderHomeActivity.class)
                        .putExtra("LAUNCHED_FROM_NOTIFICATION", true)
                        .putExtra("DESTINATION_LAT", intent.getStringExtra("DESTINATION_LAT"))
                        .putExtra("DESTINATION_LONG", intent.getStringExtra("DESTINATION_LONG"))
                        .putExtra("DESTINATION_ADDRESS", intent.getStringExtra("DESTINATION_ADDRESS"));
                notificationIntent[0].addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                PendingIntent contentIntent3 = PendingIntent.getActivity(context, 1000, notificationIntent[0],
                        PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setContentIntent(contentIntent3);
                // Add as notification
                NotificationManager manager3 = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                manager3.notify(1000, builder.build());
                break;
            case "driver_arrived":
            case "ride_ended":
            case "ride_cancelled_by_driver":
            case "ride_cancelled_by_customer":
                // Add as notification
                NotificationManager manager4 = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                manager4.notify(1000, builder.build());
                break;
        }
    }

    private String getRole(Context context) {
        final String[] ROLE = {""};
        Realm.init(context);
        Realm.getInstance(RealmUtility.getDefaultConfig(context)).executeTransaction(realm -> {
            String role = PreferenceManager.getDefaultSharedPreferences(context).getString("ROLE", "");
            if (role.equals("CUSTOMER")) {
                ROLE[0] = "CUSTOMER";
            } else if (role.equals("PROVIDER")) {
                String provider_id = PreferenceManager.getDefaultSharedPreferences(context).getString("PROVIDER_ID", "");
                RealmProvider realmProvider = realm.where(RealmProvider.class).equalTo("provider_id", provider_id).findFirst();
                if (realmProvider.getVehicle_type() != null && !realmProvider.getVehicle_type().equals("")) {
                    ROLE[0] = "DRIVER";
                } else {
                    ROLE[0] = "PROVIDER";
                }
            } else {
                ROLE[0] = "CUSTOMER";
            }

        });
        return ROLE[0];
    }
}

