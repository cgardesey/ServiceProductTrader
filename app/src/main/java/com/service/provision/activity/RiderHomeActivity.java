package com.service.provision.activity;

import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.isServiceRunning;
import static com.service.provision.constants.Const.myVolleyError;
import static com.service.provision.fragment.ProductsFragment.productMaterialDialog;
import static com.service.provision.fragment.ServicesFragment.serviceMaterialDialog;
import static com.service.provision.other.InitApplication.versionName;
import static com.service.provision.receiver.NetworkReceiver.activeActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telecom.TelecomManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.messaging.FirebaseMessaging;
import com.greysonparrelli.permiso.PermisoActivity;
import com.service.provision.R;
import com.service.provision.fragment.AroundDriverMapFragment;
import com.service.provision.fragment.CustomerSettingsFragment;
import com.service.provision.fragment.RiderSettingsFragment;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmAppUserFee;
import com.service.provision.realm.RealmCourse;
import com.service.provision.realm.RealmDialcode;
import com.service.provision.realm.RealmEnrolment;
import com.service.provision.realm.RealmInstitution;
import com.service.provision.realm.RealmInstructor;
import com.service.provision.realm.RealmInstructorCourse;
import com.service.provision.realm.RealmPayment;
import com.service.provision.realm.RealmPeriod;
import com.service.provision.realm.RealmProvider;
import com.service.provision.realm.RealmRideHistory;
import com.service.provision.realm.RealmStudent;
import com.service.provision.realm.RealmTimetable;
import com.service.provision.realm.RealmUser;
import com.service.provision.receiver.NetworkReceiver;
import com.service.provision.service.LocationUpdateService;
import com.service.provision.util.RealmUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;

public class RiderHomeActivity extends PermisoActivity implements CustomerSettingsFragment.Callbacks {

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
    public static Context context;
    public static Activity riderHomeActivity;
    public static RealmProvider realmProvider = new RealmProvider();
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        public Fragment fragment;

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                fragment = new AroundDriverMapFragment();
                loadFragment(fragment);
                return true;
            } else if (itemId == R.id.navigation_settings) {
                fragment = new RiderSettingsFragment();
                loadFragment(fragment);
                return true;
            }
            return false;
        }
    };
    RelativeLayout rootview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        riderHomeActivity = this;
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);
        if (InitApplication.getInstance().isNightModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        setContentView(R.layout.activity_rider_home);


        loadFragment(new AroundDriverMapFragment());
        //navigation.setSelectedItemId(R.id.navigation_home);

        networkReceiver = new NetworkReceiver();

        navigation = findViewById(R.id.navigation);
        rootview = findViewById(R.id.container);
        //BottomNavigationViewHelper.removeShiftMode(navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        if (getIntent().getBooleanExtra("LAUNCHED_FROM_NOTIFICATION", false)) {

            Realm.init(context);
            realmProvider = Realm.getInstance(RealmUtility.getDefaultConfig(context)).where(RealmProvider.class).equalTo("provider_id", PreferenceManager.getDefaultSharedPreferences(context).getString("PROVIDER_ID", "")).findFirst();


            if (getIntent().hasExtra("RIDE_HISTORY_ID")) {

                AlertDialog.Builder builder = new AlertDialog.Builder(riderHomeActivity);
                builder.setTitle("Ride Request Received");
                builder.setMessage("Accept?");
                builder.setPositiveButton(android.R.string.yes, (dialog, which) -> {

                    ProgressDialog mProgress = new ProgressDialog(riderHomeActivity);
                    mProgress.setCancelable(false);
                    mProgress.setIndeterminate(true);

                    mProgress.setTitle("Please wait...");
                    mProgress.show();

                    StringRequest stringRequest = new StringRequest(
                            Request.Method.PATCH,
                            API_URL + "ride-histories/" + getIntent().getStringExtra("RIDE_HISTORY_ID"),
                            response -> {
                                mProgress.dismiss();
                                if (response != null) {
                                    Uri gmmIntentUri = Uri.parse("google.navigation:q=" + getIntent().getStringExtra("PICKUP_LAT") + "," + getIntent().getStringExtra("PICKUP_LONG"));
                                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                                    mapIntent.setPackage("com.google.android.apps.maps");
                                    mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(mapIntent);

                                    if (!isServiceRunning(getApplicationContext(), "com.service.provision.service.LocationUpdateService")) {
                                        ContextCompat.startForegroundService(getApplicationContext(), new Intent(getApplicationContext(), LocationUpdateService.class)
                                                .putExtra("PUBLISHER_ID", getIntent().getStringExtra("provider_id"))
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
                            API_URL + "ride-histories/" + getIntent().getStringExtra("RIDE_HISTORY_ID"),
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
            else {
                AlertDialog.Builder builder = new AlertDialog.Builder(riderHomeActivity);
                builder.setTitle("Destination Changed");
                builder.setMessage("Destination has been updated");
                builder.setPositiveButton(android.R.string.yes, (dialog, which) -> {

                    dialog.dismiss();
                });
                builder
                        .setCancelable(false)
                        .show();
            }
        }

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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {

            case 1914:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        if (data != null) {
                            serviceMaterialDialog.servicecategory.setText(data.getStringExtra("SERVICE_CATEGORY"));
                            serviceMaterialDialog.servicecategory.setError(null);
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        // some stuff that will happen if there's no result
                        break;
                }
                break;
            case 1915:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        if (data != null) {
                            productMaterialDialog.productcategory.setText(data.getStringExtra("PRODUCT_CATEGORY"));
                            productMaterialDialog.productcategory.setError(null);
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        // some stuff that will happen if there's no result
                        break;
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void loadFragment(Fragment fragment) {
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

    public static void versionCheck(Context context) {
        try {
            StringRequest stringRequest = new StringRequest(
                    Request.Method.GET,
                    API_URL + "student-project-info",
                    response -> {
                        if (response != null) {
                            try {
                                JSONObject jsonObject = new JSONObject(response);
                                if (!jsonObject.getString("version").equals(versionName)) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder((AppCompatActivity) context);
                                    builder.setTitle("Critical Update Available!");
                                    builder.setMessage("Update app to continue using this app.");
                                    builder.setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                        Intent i = new Intent(Intent.ACTION_VIEW);
                                        i.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.univirtual.student"));
                                        context.startActivity(i);
                                    })
                                            .setCancelable(false)
                                            .show();

                                }
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

    public static void fetchAllMyData(Context context) {
        try {
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                    Request.Method.GET,
                    API_URL + "all-data",
                    null,
                    responseJson -> {
                        if (responseJson != null) {
                            Realm.init(context);
                            Realm.getInstance(RealmUtility.getDefaultConfig(context)).executeTransaction(realm -> {
                                try {
                                    persistAll(realm, responseJson);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    },
                    error -> {

                    }
            ) {
                /** Passing some request headers* */
                @Override
                public Map getHeaders() throws AuthFailureError {
                    HashMap headers = new HashMap();
                    headers.put("accept", "application/json");
                    headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(context).getString(APITOKEN, ""));
                    return headers;
                }
            };
            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                    0,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            InitApplication.getInstance().addToRequestQueue(jsonObjectRequest);

        } catch (Exception e) {
            e.printStackTrace();
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

    public static void retriev_current_registration_token(Context context, String confirmation_token) {
        Realm.init(context);
        RealmProvider realmProvider = Realm.getInstance(RealmUtility.getDefaultConfig(context)).where(RealmProvider.class).equalTo("provider_id", PreferenceManager.getDefaultSharedPreferences(context).getString("PROVIDER_ID", "")).findFirst();
        String provider_id = realmProvider.getProvider_id();

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,
                API_URL + "providers/" + provider_id,
                response -> {
                    if (response != null) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            Realm.init(context);
                            Realm.getInstance(RealmUtility.getDefaultConfig(context)).executeTransaction(realm -> {
                                realm.createOrUpdateObjectFromJson(RealmProvider.class, jsonObject);

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
