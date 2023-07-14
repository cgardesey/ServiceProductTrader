package com.service.provision.service;

import static com.service.provision.activity.CustomerHomeActivity.customerHomeActivity;
import static com.service.provision.activity.DriverFoundActivity.*;
import static com.service.provision.activity.DriverFoundActivity.driverFoundActivity;
import static com.service.provision.activity.RiderHomeActivity.APITOKEN;
import static com.service.provision.activity.RiderHomeActivity.riderHomeActivity;
import static com.service.provision.activity.SelectLocationActivity.*;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.keyConst.FCM_MESSAGE_URL;
import static com.service.provision.constants.keyConst.FCM_SERVER_KEY;
import static com.service.provision.constants.Const.isServiceRunning;
import static com.service.provision.constants.Const.myVolleyError;
import static com.service.provision.fragment.AroundDriverMapFragment.initDriverMap;
import static com.service.provision.fragment.ChatIndexFragment.chatIndexFragmentContext;
import static com.service.provision.fragment.ChatIndexFragment.populateChatIndex;
import static com.service.provision.receiver.NetworkReceiver.activeActivity;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.service.provision.R;
import com.service.provision.activity.CustomerHomeActivity;
import com.service.provision.activity.DriverFoundActivity;
import com.service.provision.activity.MessageActivity;
import com.service.provision.activity.ProviderHomeActivity;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmChat;
import com.service.provision.realm.RealmProvider;
import com.service.provision.realm.RealmRideHistory;
import com.service.provision.receiver.AlarmReceiver;
import com.service.provision.receiver.NetworkReceiver;
import com.service.provision.util.MyWorker;
import com.service.provision.util.RealmUtility;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;

/**
 * NOTE: There can only be one service in each app that receives FCM messages. If multiple
 * are declared in the Manifest then the first one will be chosen.
 * <p>
 * In order to make this Java sample functional, you must remove the following from the Kotlin messaging
 * service in the AndroidManifest.xml:
 * <p>
 * <intent-filter>
 * <action android:name="com.google.firebase.MESSAGING_EVENT" />
 * </intent-filter>
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages
        // are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data
        // messages are the type
        // traditionally used with GCM. Notification messages are only received here in
        // onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated
        // notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages
        // containing both notification
        // and data payloads are treated as notification messages. The Firebase console always
        // sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "engineer_From: " + remoteMessage.getFrom());


        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d("engineer", "Message data payload: " + remoteMessage.getData());

            Map<String, String> data = remoteMessage.getData();
            String type = data.get("type");

            if (type != null) {
                if (type.equals("chat")) {
                    JSONObject jsonResponse = new JSONObject(data);

                    Realm.init(getApplicationContext());
                    JSONObject finalJsonResponse = jsonResponse;
                    Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).executeTransaction(realm -> {
                        try {
                            JSONObject chatresponse = new JSONObject(finalJsonResponse.getString("chatresponse"));
                            realm.createOrUpdateObjectFromJson(RealmChat.class, new JSONObject(chatresponse.getString("chat")));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });

                    boolean chatInForeground = NetworkReceiver.activeActivity instanceof MessageActivity;
                    if (!chatInForeground) {

                        if (chatIndexFragmentContext != null) {
                            chatIndexFragmentContext.runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    populateChatIndex(chatIndexFragmentContext);
                                }
                            });
                        }

                        Intent intent = null;
                        try {
                            intent = new Intent(getApplicationContext(), AlarmReceiver.class)
                                    .putExtra("TYPE", "chat")
                                    .putExtra("TITLE", finalJsonResponse.getString("title"))
                                    .putExtra("BODY", finalJsonResponse.getString("body"))
                                    .putExtra("CUSTOMER_ID", data.get("CUSTOMER_ID"))
                                    .putExtra("PROVIDER_ID", data.get("PROVIDER_ID"))
                                    .putExtra("NAME", data.get("NAME"))
                                    .putExtra("PROFILE_IMAGE_URL", data.get("PROFILE_IMAGE_URL"))
                                    .putExtra("AVAILABILITY", data.get("AVAILABILITY"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 23424243, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent);
                    }
                }
                else if (type.equals("ride_cancelled_by_driver")) {
                    PreferenceManager
                            .getDefaultSharedPreferences(activeActivity)
                            .edit()
                            .putBoolean("DRIVING_TO_PICKUP", false)
                            .putBoolean("DRIVING_TO_DESTINATION", false)
                            .apply();
                    if (activeActivity != null) {
                        activeActivity.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                AlertDialog.Builder builder = new AlertDialog.Builder(activeActivity);
                                builder.setTitle("Ride Cancelled");
                                builder.setMessage("Ride request has been cancelled by driver.");
                                builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                    if (driverFoundActivity != null) {
                                        driverFoundActivity.finish();
                                    }
                                });
                                builder
                                        .setCancelable(false)
                                        .show();
                            }
                        });
                    } else {
                        Intent intent = null;
                        intent = new Intent(getApplicationContext(), AlarmReceiver.class)
                                .putExtra("TYPE", "ride_cancelled_by_driver")
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);


                        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 33424243, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent);
                    }
                }
                else if (type.equals("ride_cancelled_by_customer")) {
                    PreferenceManager
                            .getDefaultSharedPreferences(activeActivity)
                            .edit()
                            .putBoolean("DRIVING_TO_PICKUP", false)
                            .putBoolean("DRIVING_TO_DESTINATION", false)
                            .apply();
                    if (activeActivity != null) {
                        activeActivity.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                if (riderHomeActivity != null) {
                                    initDriverMap(activeActivity);
                                }

                                AlertDialog.Builder builder = new AlertDialog.Builder(activeActivity);
                                builder.setTitle("Ride Cancelled");
                                builder.setMessage("Ride request has been cancelled by customer.");
                                builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                    dialog.dismiss();
                                });
                                builder
                                        .setCancelable(false)
                                        .show();
                            }
                        });
                    } else {
                        Intent intent = null;
                        intent = new Intent(getApplicationContext(), AlarmReceiver.class)
                                .putExtra("TYPE", "ride_cancelled_by_customer")
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);


                        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 33424243, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent);
                    }
                }
                else if (type.equals("driver_arrived")) {
                    PreferenceManager
                            .getDefaultSharedPreferences(getApplicationContext())
                            .edit()
                            .putBoolean("DRIVING_TO_PICKUP", false)
                            .putBoolean("DRIVING_TO_DESTINATION", true)
                            .putString("PROVIDER_LAT", data.get("provider_lat"))
                            .putString("PROVIDER_LONG", data.get("provider_long"))
                            .apply();
                    if (activeActivity != null) {
                        activeActivity.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                if (driverFoundActivity != null) {
                                    driverFoundInit(driverFoundActivity);
                                }
                            }
                        });
                    }

                    Intent intent = null;
                    intent = new Intent(getApplicationContext(), AlarmReceiver.class)
                            .putExtra("TYPE", "driver_arrived")
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);


                    PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 33424243, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                    AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                    alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent);
                }
                else if (type.equals("ride_request")) {

                    PreferenceManager
                            .getDefaultSharedPreferences(getApplicationContext())
                            .edit()
                            .putString("DESTINATION_LAT", data.get("destination_lat"))
                            .putString("DESTINATION_LONG", data.get("destination_long"))
                            .putString("DESTINATION_ADDRESS", data.get("destination_address"))

                            .putString("RIDE_HISTORY_ID", data.get("ride_history_id"))
                            .putString("DISTANCE", data.get("distance"))
                            .putString("PICKUP_LAT", data.get("pickup_lat"))
                            .putString("PICKUP_LONG", data.get("pickup_long"))
                            .putString("CUSTOMER_NAME", data.get("customer_name"))
                            .putString("CUSTOMER_PROFILE_IMAGE_URL", data.get("customer_profile_image_url"))
                            .putString("PICKUP_ADDRESS", data.get("pickup_address"))
                            .putString("SERVICE_ID", data.get("service_id"))
                            .putString("CUSTOMER_PRIMARY_CONTACT", data.get("customer_primary_contact"))
                            .putString("CUSTOMER_CONFIRMATION_TOKEN", data.get("customer_confirmation_token"))
                            .putString("CUSTOMER_ID", data.get("customer_id"))
                            .putString("PROVIDER_ID", data.get("provider_id"))
                            .apply();
                    if (riderHomeActivity != null) {
                        riderHomeActivity.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                Realm.init(riderHomeActivity);
                                realmProvider = Realm.getInstance(RealmUtility.getDefaultConfig(riderHomeActivity)).where(RealmProvider.class).equalTo("provider_id", PreferenceManager.getDefaultSharedPreferences(riderHomeActivity).getString("PROVIDER_ID", "")).findFirst();
                                String msg;
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    msg = String.valueOf(Html.fromHtml("You have a ride request <font color='#228C22'><u>" + String.valueOf((int)Double.parseDouble(data.get("distance"))) + " meters " + "</u></font>away<br/><br/>Accept?", Html.FROM_HTML_MODE_COMPACT));
                                } else {
                                    msg = String.valueOf(Html.fromHtml("You have a ride request <font color='#228C22'><u>" + String.valueOf((int)Double.parseDouble(data.get("distance"))) + " meters " + "</u></font>away<br/><br/>Accept?"));
                                }

                                AlertDialog.Builder builder = new AlertDialog.Builder(riderHomeActivity);
                                builder.setTitle("Ride Request Received");
                                builder.setMessage(msg);
                                builder.setPositiveButton(android.R.string.yes, (dialog, which) -> {

                                    ProgressDialog mProgress = new ProgressDialog(riderHomeActivity);
                                    mProgress.setCancelable(false);
                                    mProgress.setIndeterminate(true);

                                    mProgress.setTitle("Please wait...");
                                    mProgress.show();

                                    StringRequest stringRequest = new StringRequest(
                                            Request.Method.PATCH,
                                            API_URL + "ride-histories/" + data.get("ride_history_id"),
                                            response -> {
                                                mProgress.dismiss();
                                                if (response != null) {
                                                    Uri gmmIntentUri = Uri.parse("google.navigation:q=" + data.get("pickup_lat") + "," + data.get("pickup_long"));
                                                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                                                    mapIntent.setPackage("com.google.android.apps.maps");
                                                    mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                    startActivity(mapIntent);

                                                    if (!isServiceRunning(getApplicationContext(), "com.service.provision.service.LocationUpdateService")) {
                                                        ContextCompat.startForegroundService(getApplicationContext(), new Intent(getApplicationContext(), LocationUpdateService.class)
                                                                .putExtra("PUBLISHER_ID", data.get("provider_id"))
                                                                .putExtra("ROLE", PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("ROLE", ""))
                                                        );
                                                    }

                                                    PreferenceManager
                                                            .getDefaultSharedPreferences(getApplicationContext())
                                                            .edit()
                                                            .putBoolean("DRIVING_TO_PICKUP", true)
                                                            .putBoolean("DRIVING_TO_DESTINATION", false)
                                                            .apply();

                                                    dialog.dismiss();
                                                }
                                            },
                                            error -> {
                                                mProgress.dismiss();
                                                error.printStackTrace();
                                                myVolleyError(getApplicationContext(), error);
                                                Log.d("Cyrilll", error.toString());
                                            }
                                    ) {
                                        @Override
                                        public Map<String, String> getParams() throws AuthFailureError {
                                            Map<String, String> params = new HashMap<>();
                                            params.put("ride_cancelled", "0");
                                            return params;
                                        }

                                        @Override
                                        public Map getHeaders() throws AuthFailureError {
                                            HashMap headers = new HashMap();
                                            headers.put("accept", "application/json");
                                            headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(APITOKEN, ""));
                                            return headers;
                                        }
                                    };
                                    stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                                            0,
                                            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                                    InitApplication.getInstance().addToRequestQueue(stringRequest);
                                });
                                builder.setNegativeButton(android.R.string.no, (dialog, which) -> {
                                    ProgressDialog mProgress = new ProgressDialog(riderHomeActivity);
                                    mProgress.setCancelable(false);
                                    mProgress.setIndeterminate(true);

                                    mProgress.setTitle("Please wait...");
                                    mProgress.show();

                                    StringRequest stringRequest = new StringRequest(
                                            Request.Method.PATCH,
                                            API_URL + "ride-histories/" + data.get("RIDE_HISTORY_ID"),
                                            response -> {
                                                mProgress.dismiss();
                                                if (response != null) {
                                                    try {
                                                        PreferenceManager
                                                                .getDefaultSharedPreferences(getApplicationContext())
                                                                .edit()
                                                                .putBoolean("DRIVING_TO_PICKUP", false)
                                                                .putBoolean("DRIVING_TO_DESTINATION", false)
                                                                .apply();

                                                        JSONObject jsonObject = new JSONObject(response);
                                                        Realm.init(getApplicationContext());
                                                        Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).executeTransaction(realm -> {
                                                            realm.createOrUpdateObjectFromJson(RealmRideHistory.class, jsonObject);
                                                        });

                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            },
                                            error -> {
                                                mProgress.dismiss();
                                                error.printStackTrace();
                                                myVolleyError(getApplicationContext(), error);
                                                Log.d("Cyrilll", error.toString());
                                            }
                                    ) {
                                        @Override
                                        public Map<String, String> getParams() throws AuthFailureError {
                                            Map<String, String> params = new HashMap<>();
                                            params.put("ride_cancelled", "1");
                                            return params;
                                        }

                                        @Override
                                        public Map getHeaders() throws AuthFailureError {
                                            HashMap headers = new HashMap();
                                            headers.put("accept", "application/json");
                                            headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(APITOKEN, ""));
                                            return headers;
                                        }
                                    };
                                    stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                                            0,
                                            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                                    InitApplication.getInstance().addToRequestQueue(stringRequest);
                                });
                                builder
                                        .setCancelable(false)
                                        .show();
                            }
                        });
                    } else {
                        Intent intent = null;
                        intent = new Intent(getApplicationContext(), AlarmReceiver.class)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);


                        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 33424243, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent);
                    }
                }
                else if (type.equals("ride_request_accepted")) {

                    PreferenceManager
                            .getDefaultSharedPreferences(getApplicationContext())
                            .edit()
                            .putString("TYPE", "ride_request_accepted")
                            .putString("TITLE", "Ride Request Accepted")
                            .putString("RIDE_HISTORY_ID", data.get("ride_history_id"))
                            .putString("PICKUP_LAT", data.get("pickup_lat"))
                            .putString("PICKUP_LONG", data.get("pickup_long"))
                            .putString("DESTINATION_LAT", data.get("destination_lat"))
                            .putString("DESTINATION_LONG", data.get("destination_long"))
                            .putString("PROVIDER_LAT", data.get("provider_lat"))
                            .putString("PROVIDER_LONG", data.get("provider_long"))
                            .putString("PROVIDER_NAME", data.get("provider_name"))
                            .putString("PROVIDER_PROFILE_IMAGE_URL", data.get("provider_profile_image_url"))
                            .putString("PICKUP_ADDRESS", data.get("pickup_address"))
                            .putString("DESTINATION_ADDRESS", data.get("destination_address"))
                            .putString("SERVICE_ID", data.get("service_id"))
                            .putString("PROVIDER_PRIMARY_CONTACT", data.get("provider_primary_contact"))
                            .putString("VEHICLE_TYPE", data.get("vehicle_type"))
                            .putString("VEHICLE_REGISTRATION_NUMBER", data.get("vehicle_registration_number"))
                            .putString("PROVIDER_CONFIRMATION_TOKEN", data.get("provider_confirmation_token"))
                            .putString("CUSTOMER_ID", data.get("customer_id"))
                            .putString("PROVIDER_ID", data.get("provider_id"))
                            .apply();

                    PreferenceManager
                            .getDefaultSharedPreferences(getApplicationContext())
                            .edit()
                            .putBoolean("DRIVING_TO_PICKUP", true)
                            .putBoolean("DRIVING_TO_DESTINATION", false)
                            .apply();

                    if (customerHomeActivity != null) {
                        customerHomeActivity.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                if (selectLocationActivity != null) {
                                    selectLocationActivity.finish();
                                }
                                startActivity(new Intent(getApplicationContext(), DriverFoundActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                            }
                        });
                    }
                    else {
                        Intent intent = null;
                        intent = new Intent(getApplicationContext(), AlarmReceiver.class)
                                .putExtra("TYPE", "ride_request_accepted")
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);


                        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 33424243, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent);
                    }
                }
                else if (type.equals("ride_ended")) {
                    PreferenceManager
                            .getDefaultSharedPreferences(activeActivity)
                            .edit()
                            .putBoolean("DRIVING_TO_PICKUP", false)
                            .putBoolean("DRIVING_TO_DESTINATION", false)
                            .apply();
                    if (activeActivity != null) {
                        activeActivity.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                AlertDialog.Builder builder = new AlertDialog.Builder(activeActivity);
                                builder.setTitle("Ride Cost");
                                builder.setMessage(data.get("price"));
                                builder.setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                    if (driverFoundActivity != null) {
                                        driverFoundActivity.finish();
                                    }
                                })
                                        .setCancelable(false)
                                        .show();
                            }
                        });
                    } else {
                        Intent intent = null;
                        intent = new Intent(getApplicationContext(), AlarmReceiver.class)
                                .putExtra("TYPE", "ride_ended")
                                .putExtra("TITLE", "Ride has Ended")
                                .putExtra("PRICE", data.get("price"))
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);


                        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 33424243, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent);
                    }
                }
                else if (type.equals("destination_changed")) {
                    PreferenceManager
                            .getDefaultSharedPreferences(getApplicationContext())
                            .edit()
                            .putString("DESTINATION_LAT", data.get("destination_lat"))
                            .putString("DESTINATION_LONG", data.get("destination_long"))
                            .putString("DESTINATION_ADDRESS", data.get("destination_address"))
                            .apply();
                    if (riderHomeActivity != null) {
                        riderHomeActivity.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                Realm.init(riderHomeActivity);
                                realmProvider = Realm.getInstance(RealmUtility.getDefaultConfig(riderHomeActivity)).where(RealmProvider.class).equalTo("provider_id", PreferenceManager.getDefaultSharedPreferences(riderHomeActivity).getString("PROVIDER_ID", "")).findFirst();

                                AlertDialog.Builder builder = new AlertDialog.Builder(riderHomeActivity);
                                builder.setTitle("Destination Changed");
                                builder.setMessage("Destination has been updated");
                                builder.setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                    initDriverMap(riderHomeActivity);

                                    dialog.dismiss();
                                });
                                builder
                                        .setCancelable(false)
                                        .show();
                            }
                        });
                    } else {
                        Intent intent = null;
                        intent = new Intent(getApplicationContext(), AlarmReceiver.class)
                                .putExtra("TYPE", "destination_changed")
                                .putExtra("DESTINATION_LAT", data.get("destination_lat"))
                                .putExtra("DESTINATION_LONG", data.get("destination_long"))
                                .putExtra("DESTINATION_ADDRESS", data.get("destination_address"))
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);


                        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 33424243, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent);
                    }
                }
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
    // [END receive_message]


    // [START on_new_token]

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(token);
    }
    // [END on_new_token]

    /**
     * Schedule async work using WorkManager.
     */
    private void scheduleJob() {
        // [START dispatch_job]
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(MyWorker.class)
                .build();
        WorkManager.getInstance().beginWith(work).enqueue();
        // [END dispatch_job]
    }

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private void handleNow() {
        Log.d(TAG, "Short lived task is done.");
    }

    /**
     * Persist token to third-party servers.
     * <p>
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        // TODO: Implement this method to send token to your app server.
        Realm.init(getApplicationContext());
        Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).executeTransaction(realm -> {
            String role = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("ROLE", "");
            if (role.equals("CUSTOMER")) {
                CustomerHomeActivity.retriev_current_registration_token(getApplicationContext(), token);
            } else {
                ProviderHomeActivity.retriev_current_registration_token(getApplicationContext(), token);
            }
        });
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     * ;
     *
     * @param messageBody FCM message body received.
     */
    private void sendNotification(String messageBody) {
        Intent intent = new Intent(this, ProviderHomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.service)
                        .setContentTitle("Message Title")
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

    /*public static class notifyWithFCM extends AsyncTask<Void, Integer, String> {
        Context context;
        String confirmation_token;
        JSONObject dataPayload;

        public notifyWithFCM(Context context, String confirmation_token, JSONObject dataPayload) {
            this.context = context;
            this.confirmation_token = confirmation_token;
            this.dataPayload = dataPayload;
        }

        @Override
        protected String doInBackground(Void... params) {

            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject()
                        *//*.put("notification", new JSONObject()
                                .put("body", body)
                                .put("title", title)
                        )*//*
                        .put("data", dataPayload

                        )
                        .put("android", new JSONObject()
                                .put("priority", "high")
                        )
                        .put("registration_ids", new JSONArray().put(confirmation_token));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            String content = jsonObject.toString();
            RequestBody requestBody = RequestBody.create(mediaType, content);
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(FCM_MESSAGE_URL)
                    .method("POST", requestBody)
                    .addHeader("Authorization", "key=" + FCM_SERVER_KEY)
                    .build();
            okhttp3.Response response = null;
            try {
                response = client.newCall(request).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {

            // Init and show dialog

        }

        @Override
        protected void onPostExecute(String result) {

        }
    }*/

    public static void notifyWithFCM (Context context, String confirmation_token, JSONObject dataPayload) {

        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject()
                    /*.put("notification", new JSONObject()
                            .put("body", body)
                            .put("title", title)
                    )*/
                    .put("data", dataPayload

                    )
                    .put("android", new JSONObject()
                            .put("priority", "high")
                    )
                    .put("registration_ids", new JSONArray().put(confirmation_token));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                FCM_MESSAGE_URL,
                jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (response != null) {

                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        myVolleyError(context, error);
                    }
                }
        )
        {
            /** Passing some request headers* */
            @Override
            public Map getHeaders() throws AuthFailureError {
                HashMap headers = new HashMap();
                headers.put("accept", "application/json");
                headers.put("Authorization", "key=" + FCM_SERVER_KEY);
                return headers;
            }
        };
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        InitApplication.getInstance().addToRequestQueue(jsonObjectRequest);
    }
}
