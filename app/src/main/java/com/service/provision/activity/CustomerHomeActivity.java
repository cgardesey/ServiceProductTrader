package com.service.provision.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.messaging.FirebaseMessaging;
import com.greysonparrelli.permiso.PermisoActivity;
import com.service.provision.R;
import com.service.provision.fragment.CartListFragment;
import com.service.provision.fragment.ChatIndexFragment;
import com.service.provision.fragment.SearchProductFragment;
import com.service.provision.fragment.SearchServiceFragment;
import com.service.provision.fragment.CustomerSettingsFragment;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmAppUserFee;
import com.service.provision.realm.RealmCart;
import com.service.provision.realm.RealmCourse;
import com.service.provision.realm.RealmCustomer;
import com.service.provision.realm.RealmDialcode;
import com.service.provision.realm.RealmEnrolment;
import com.service.provision.realm.RealmInstitution;
import com.service.provision.realm.RealmInstructor;
import com.service.provision.realm.RealmInstructorCourse;
import com.service.provision.realm.RealmPayment;
import com.service.provision.realm.RealmPeriod;
import com.service.provision.realm.RealmStudent;
import com.service.provision.realm.RealmTimetable;
import com.service.provision.realm.RealmUser;
import com.service.provision.receiver.NetworkReceiver;
import com.service.provision.util.RealmUtility;
import com.service.provision.util.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;

import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.keyConst.WS_URL;
import static com.service.provision.constants.Const.clearAppData;
import static com.service.provision.constants.Const.isNetworkAvailable;
import static com.service.provision.receiver.NetworkReceiver.activeActivity;
import static com.service.provision.util.Socket.EVENT_CLOSED;
import static com.service.provision.util.Socket.EVENT_OPEN;
import static com.service.provision.util.Socket.EVENT_RECONNECT_ATTEMPT;

public class CustomerHomeActivity extends PermisoActivity implements CustomerSettingsFragment.Callbacks {

    private String TAG;
    public static String MYUSERID = "MYUSERID";
    public static String APITOKEN = "APITOKEN";
    public static String NUMBER_VERIFIED = "NUMBER_VERIFIED";
    public static String ACCESSTOKEN = "ACCESSTOKEN";
    public static String GUID = "GUID";
    public static String JUSTENROLLED = "JUSTENROLLED";
    public static int RC_ACCOUNT = 435;
    public static final int FILE_PICKER_REQUEST_CODE = 4389;
    NetworkReceiver networkReceiver;
    static BottomNavigationView navigation;
    FloatingActionButton close;
    public static Context context;
    public static Activity customerHomeActivity;

    private static Socket guidSocket;
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        public Fragment fragment;

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            int itemId = item.getItemId();
            if (itemId == R.id.service) {
                fragment = new SearchServiceFragment();
                loadFragment(fragment);
                return true;
            } else if (itemId == R.id.product) {
                fragment = new SearchProductFragment();
                loadFragment(fragment);
                return true;
            } else if (itemId == R.id.cart) {
                fragment = new CartListFragment();
                loadFragment(fragment);
                return true;
            } else if (itemId == R.id.chat) {
                fragment = new ChatIndexFragment();
                loadFragment(fragment);
                return true;
            } else if (itemId == R.id.navigation_settings) {
                fragment = new CustomerSettingsFragment();
                loadFragment(fragment);
                return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        customerHomeActivity = this;
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);
        if (InitApplication.getInstance().isNightModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

//        Permiso.getInstance().setActivity(this);
        setContentView(R.layout.activity_customer_home);

        guidSocket = Socket
                .Builder.with(WS_URL)
                .build();
        guidSocket.connect();

        guidSocket.onEvent(EVENT_OPEN, new Socket.OnEventListener() {
            @Override
            public void onMessage(String event) {
                Log.d("mywebsocket2", "Connected");

                guidSocket.join("guid:" + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(MYUSERID, ""));

                guidSocket.onEventResponse("guid:" + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(MYUSERID, ""), new Socket.OnEventResponseListener() {
                    @Override
                    public void onMessage(String event, String data) {

                    }
                });

                guidSocket.setMessageListener(new Socket.OnMessageListener() {
                    @Override
                    public void onMessage(String data) {
                        JSONObject jsonObject = null;
                        JSONObject jsonResponse = null;
                        String message = "";
                        try {
                            jsonObject = new JSONObject(data);
                            switch (jsonObject.getInt("t")) {
                                case 0:
                                    break;
                                case 1:
                                    break;
                                case 2:
                                    break;
                                case 3:
                                    break;
                                case 4:
                                    break;
                                case 5:
                                    break;
                                case 6:
                                    break;
                                case 7:
                                    jsonResponse = jsonObject.getJSONObject("d");
                                    Log.d("mywebsocket2", jsonResponse.toString());
                                    Realm.init(activeActivity);
                                    JSONObject finalJsonResponse = jsonResponse;
                                    if (finalJsonResponse.getJSONObject("data").has("guid")) {
                                        if (!PreferenceManager.getDefaultSharedPreferences(activeActivity).getString(GUID, "").equals("") && !finalJsonResponse.getJSONObject("data").getString("guid").equals(PreferenceManager.getDefaultSharedPreferences(activeActivity).getString(GUID, ""))) {
                                            Log.d("d7410852", "local: " + PreferenceManager.getDefaultSharedPreferences(activeActivity).getString(GUID, "") + "server : " + finalJsonResponse.getJSONObject("data").getString("guid"));
                                            AlertDialog.Builder builder = new AlertDialog.Builder(activeActivity);
                                            builder.setTitle("Duplicate Account Detected");
                                            builder.setMessage("You can only use your account on one device at a time.");
                                            builder.setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                                clearAppData(activeActivity);
                                            });
                                            builder
                                                    .setCancelable(false)
                                                    .show();
                                        }
                                    }
                                    break;
                                case 8:
                                    break;
                                case 9:
                                    break;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

                try {
                    JSONObject jsonData = new JSONObject()
                            .put(
                                    "guid", PreferenceManager.getDefaultSharedPreferences(customerHomeActivity).getString(GUID, "")
                            );
                    if (isNetworkAvailable(customerHomeActivity)) {

                        if (guidSocket.getState() == Socket.State.OPEN) {
                            if (guidSocket != null) {
                                guidSocket.send("guid:" + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(MYUSERID, ""), jsonData.toString());
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        guidSocket.onEvent(EVENT_RECONNECT_ATTEMPT, new Socket.OnEventListener() {
            @Override
            public void onMessage(String event) {
                Log.d("mywebsocket2", "reconnecting");
            }
        });
        guidSocket.onEvent(EVENT_CLOSED, new Socket.OnEventListener() {
            @Override
            public void onMessage(String event) {
                Log.d("mywebsocket2", "connection closed");
            }
        });

        close = findViewById(R.id.close);
        close.setOnClickListener(v -> {
//            changeDefaultDialer(HomeActivity.this, getPackagesOfDialerApps(getApplicationContext()).get(0));
            Intent intent = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
//            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            finish();
        });
//        new FCMAsyncTask(getApplicationContext()).execute();

        /*FirebaseMessaging.getInstance().subscribeToTopic(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(MYUSERID, ""))
                .addOnCompleteListener(task -> {
                    String msg = "successfully subscribed";
                    if (!task.isSuccessful()) {
                        msg = "unsuccessfully subscribed";
                    }
                    Log.d("engineer:sub_status:", msg);
                });*/

        Realm.init(getApplicationContext());
        String customer_id = Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).where(RealmCustomer.class).findFirst().getCustomer_id();

        Realm.init(getApplicationContext());
        Realm.getInstance(RealmUtility.getDefaultConfig(CustomerHomeActivity.this)).executeTransaction(realm -> {
            RealmResults<RealmCart> realmCarts = realm.where(RealmCart.class)
                    .distinct("provider_id")
                    .equalTo("customer_id", customer_id)
                    .findAll();
            for (RealmCart realmCart : realmCarts) {
                String topic = customer_id + realmCart.getProvider_id();
                FirebaseMessaging.getInstance().subscribeToTopic(topic)
                        .addOnCompleteListener(task -> {
                            String msg = "successfully subscribed";
                            if (!task.isSuccessful()) {
                                msg = "unsuccessfully unsubscribed";
                            }
                            Log.d("engineer:sub_status:", msg);
                        });
            }
        });


        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();

                        // Log and toast
                        Log.d("engineer", token);
                        retriev_current_registration_token(getApplicationContext(), token);
                    }
                });

        loadFragment(new SearchServiceFragment());
        //navigation.setSelectedItemId(R.id.navigation_home);

        networkReceiver = new NetworkReceiver();


        navigation = findViewById(R.id.navigation);
        //BottomNavigationViewHelper.removeShiftMode(navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);


        if (!(activeActivity instanceof DriverFoundActivity) && (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("DRIVING_TO_PICKUP", false) ||
                        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("DRIVING_TO_DESTINATION", false))) {
            startActivity(new Intent(getApplicationContext(), DriverFoundActivity.class));
        }
    }

    public static void guidCheck(Context context) {

        if (!PreferenceManager.getDefaultSharedPreferences(context).getString(GUID, "").equals("")) {
            try {
                StringRequest stringRequest = new StringRequest(
                        Request.Method.POST,
                        API_URL + "guid-check",
                        response -> {
                            if (response != null) {
                                try {
                                    JSONObject jsonObject = new JSONObject(response);
                                    Log.d("d7410852", Boolean.toString(jsonObject.getBoolean("guid_changed")) + " : " + PreferenceManager.getDefaultSharedPreferences(context).getString(GUID, ""));
                                    if (jsonObject.getBoolean("guid_changed")) {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                        builder.setTitle("Duplicate Account Detected");
                                        builder.setMessage("You can only use your account on one device at a time.");
                                        builder.setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                            clearAppData(context);
                                        });
                                        builder
                                                .setCancelable(false)
                                                .show();
                                    }
//                                    new broadcastWithFirebase().execute();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        error -> {
                            error.printStackTrace();
                            Log.d("Cyrilll", error.toString());
                            //                                myVolleyError(context, error);
                        }
                ) {
                    @Override
                    public Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> params = new HashMap<>();
                        params.put("guid", PreferenceManager.getDefaultSharedPreferences(context).getString(GUID, ""));
                        return params;
                    }

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

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (guidSocket != null) {
            guidSocket.leave("guid:" + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(MYUSERID, ""));
            guidSocket.clearListeners();
            guidSocket.close();
            guidSocket.terminate();
            guidSocket = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        activeActivity = this;
        registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));


    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(networkReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_ACCOUNT) {
            Realm.init(getApplicationContext());
            Realm.getInstance(RealmUtility.getDefaultConfig(CustomerHomeActivity.this)).executeTransaction(realm -> {
                boolean accountInfoNotSet = realm.where(RealmStudent.class).equalTo("infoid", PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(MYUSERID, "")).findFirst().getFirstname() == null;
                if (accountInfoNotSet) {
                    finish();
                }
            });
        }
    }

    private void loadFragment(Fragment fragment) {
        // load fragment
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.homeframe, fragment);
        transaction.commit();
    }


    @Override

    public void onChangeNightMOde() {
        if (InitApplication.getInstance().isNightModeEnabled()) {
            InitApplication.getInstance().setIsNightModeEnabled(false);
            Intent intent = getIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            finish();
            startActivity(intent);

        } else {
            InitApplication.getInstance().setIsNightModeEnabled(true);
            Intent intent = getIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            finish();
            startActivity(intent);
        }

    }

    public static void dialog(Context context, boolean value) {

        if (value) {
            //   tv_check_connection.setVisibility(View.VISIBLE);

        } else {
            Snackbar snackbar = Snackbar
                    .make(navigation, context.getString(R.string.no_internet_connection), Snackbar.LENGTH_LONG)
                    .setAction(context.getString(R.string.ok).toUpperCase(), view -> {

                    });

            snackbar.show();
        }
    }

    public static void persistAll(Realm realm, JSONObject responseJson) throws JSONException {
//        realm.createOrUpdateAllFromJson(RealmAssignment.class, responseJson.getJSONArray("assignments"));
//        realm.createOrUpdateAllFromJson(RealmAttendance.class, responseJson.getJSONArray("attendances"));
//        realm.createOrUpdateAllFromJson(RealmAudio.class, responseJson.getJSONArray("audios"));
//        realm.createOrUpdateAllFromJson(RealmChat.class, responseJson.getJSONArray("chats"));
        realm.createOrUpdateAllFromJson(RealmCourse.class, responseJson.getJSONArray("courses"));
        realm.createOrUpdateAllFromJson(RealmEnrolment.class, responseJson.getJSONArray("enrolments"));
        realm.createOrUpdateAllFromJson(RealmInstructor.class, responseJson.getJSONArray("instructors"));
        realm.createOrUpdateAllFromJson(RealmInstructorCourse.class, responseJson.getJSONArray("instructor_courses"));
        realm.createOrUpdateAllFromJson(RealmPayment.class, responseJson.getJSONArray("payments"));
        realm.createOrUpdateAllFromJson(RealmInstitution.class, responseJson.getJSONArray("institutions"));
        realm.createOrUpdateAllFromJson(RealmStudent.class, responseJson.getJSONArray("students"));
//        realm.createOrUpdateAllFromJson(RealmSubmittedAssignment.class, responseJson.getJSONArray("submitted_assignments"));
        realm.createOrUpdateAllFromJson(RealmUser.class, responseJson.getJSONArray("users"));
        realm.createOrUpdateAllFromJson(RealmTimetable.class, responseJson.getJSONArray("timetables"));
        realm.createOrUpdateAllFromJson(RealmPeriod.class, responseJson.getJSONArray("periods"));
//        realm.createOrUpdateAllFromJson(RealmInstructorCourseRating.class, responseJson.getJSONArray("instructor_course_ratings"));
//        realm.createOrUpdateAllFromJson(RealmQuiz.class, responseJson.getJSONArray("quizzes"));
//        realm.createOrUpdateAllFromJson(RealmSubmittedQuiz.class, responseJson.getJSONArray("submitted_quizzes"));
        realm.createOrUpdateAllFromJson(RealmDialcode.class, responseJson.getJSONArray("dialcodes"));
//        realm.createOrUpdateAllFromJson(RealmRecordedVideo.class, responseJson.getJSONArray("recorded_videos"));
//        realm.createOrUpdateAllFromJson(RealmRecordedVideoStream.class, responseJson.getJSONArray("recorded_video_streams"));
//        realm.createOrUpdateAllFromJson(RealmRecordedAudioStream.class, responseJson.getJSONArray("recorded_audio_streams"));
        realm.createOrUpdateAllFromJson(RealmAppUserFee.class, responseJson.getJSONArray("app_user_fees"));
//        realm.createOrUpdateAllFromJson(RealmDrawingCoordinate.class, responseJson.getJSONArray("drawing_coordinates"));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static String getDefaultDialerPackage(Context context) {
        TelecomManager manger = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            manger = (TelecomManager) context.getSystemService(TELECOM_SERVICE);
        }
        String name = manger.getDefaultDialerPackage();
        return name;
    }

    public static void retriev_current_registration_token(Context context, String
            confirmation_token) {
        Realm.init(context);
        RealmCustomer realmCustomer = Realm.getInstance(RealmUtility.getDefaultConfig(context)).where(RealmCustomer.class).findFirst();
        String customer_id = realmCustomer.getCustomer_id();

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,
                API_URL + "customers/" + customer_id,
                response -> {
                    if (response != null) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            Realm.init(context);
                            Realm.getInstance(RealmUtility.getDefaultConfig(context)).executeTransaction(realm -> {
                                realm.createOrUpdateObjectFromJson(RealmCustomer.class, jsonObject);
                            });

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                error -> {

                }
        ) {
            @Override
            public Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("confirmation_token", confirmation_token);
                return params;
            }

            /** Passing some request headers* */
            @Override
            public Map getHeaders() throws AuthFailureError {
                HashMap headers = new HashMap();
                headers.put("accept", "application/json");
                headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(NetworkReceiver.activeActivity).getString(APITOKEN, ""));
                return headers;
            }
        };
        ;

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        InitApplication.getInstance().addToRequestQueue(stringRequest);
    }
}
