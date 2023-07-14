package com.service.provision.activity;

import static com.service.provision.activity.MyServiceListActivity.myServiceListActivity;
import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.Const.base_fare;
import static com.service.provision.constants.Const.cost_per_min;
import static com.service.provision.constants.Const.cost_per_km;
import static com.service.provision.constants.Const.surge_boost_multiplier;
import static com.service.provision.constants.Const.other_fee;
import static com.service.provision.constants.keyConst.API_URL;

import static com.service.provision.constants.Const.convertDpToPx;
import static com.service.provision.constants.Const.myVolleyError;
import static com.service.provision.constants.Const.toTitleCase;
import static com.service.provision.receiver.NetworkReceiver.activeActivity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.greysonparrelli.permiso.Permiso;
import com.makeramen.roundedimageview.RoundedImageView;
import com.service.provision.R;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmCustomer;
import com.service.provision.realm.RealmRideHistory;
import com.service.provision.receiver.NetworkReceiver;
import com.service.provision.util.MyVolleyRequest;
import com.service.provision.util.RealmUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;

public class SelectLocationActivity extends FragmentActivity implements OnMapReadyCallback, RoutingListener {

    NetworkReceiver networkReceiver;
    double pickup_longitude = 0d, pickup_latitude = 0d;
    double destination_longitude = 0d, destination_latitude = 0d;
    private static int AUTOCOMPLETE_REQUEST_CODE = 1;
    public static final int RC_DESTINATION_LOCATION = 1002;
    public static final int RC_PICKUP_LOCATION = 1003;
    public static final int RC_CONFIRM_PICKUP_LOCATION = 1004;

    TextView select_location, time, price;
    ImageView backbtn;
    TextView pickup_location, destination;
    RecyclerView recycler_view;
    LinearLayout choose_map_layout;
    LinearLayout order_layout;
    LinearLayout progress_layout;
    Button order_button;
    ProgressBar progressbar;
    LinearLayout cancel;
    RoundedImageView profile_pic;
    TextView driver_name;
    LinearLayout driver_info_layout;

    ProgressDialog progressDialog;

    private GoogleMap mMap;
    LatLng latLng;
    ArrayList<Polyline> polylines = new ArrayList<>();

    int ride_time;
    boolean location_changed = false;

    Float ride_distance;

    public static Activity selectLocationActivity;



    Timer myTimer;
    int radius = 0;


    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_location);

        selectLocationActivity = this;

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        backbtn = findViewById(R.id.backbtn);
        pickup_location = findViewById(R.id.pickup_location);
        destination = findViewById(R.id.destination);
        progressbar = findViewById(R.id.progress_bar_1);

        time = findViewById(R.id.time);
        price = findViewById(R.id.price);
        order_button = findViewById(R.id.order_button);
        order_layout = findViewById(R.id.order_layout);
        progress_layout = findViewById(R.id.progress_layout);

//        recycler_view = findViewById(R.id.recycler_view);
        driver_name = findViewById(R.id.driver_name);
        driver_info_layout = findViewById(R.id.driver_info_layout);
        profile_pic = findViewById(R.id.profile_pic);
        cancel = findViewById(R.id.cancel);


        pickup_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
                                                             @Override
                                                             public void onPermissionResult(Permiso.ResultSet resultSet) {
                                                                 if (resultSet.isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION) && resultSet.isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                                                                     Realm.init(SelectLocationActivity.this);
                                                                     RealmCustomer realmCustomer = Realm.getInstance(RealmUtility.getDefaultConfig(SelectLocationActivity.this)).where(RealmCustomer.class).findFirst();
                                                                     startActivityForResult(new Intent(getApplicationContext(), MapsActivity.class)
                                                                                     .putExtra("LONGITUDE", realmCustomer.getLongitude())
                                                                                     .putExtra("LATITUDE", realmCustomer.getLatitude())
                                                                                     .putExtra("BUTTON_TEXT", "CONFIRM PICKUP"),

                                                                             RC_PICKUP_LOCATION);

                                                                 }
                                                             }

                                                             @Override
                                                             public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                                                                 Permiso.getInstance().showRationaleInDialog(getApplicationContext().getString(R.string.permissions), getApplicationContext().getString(R.string.this_permission_is_mandatory_pls_allow_access), null, callback);
                                                             }
                                                         },
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION);

            }
        });
        destination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
                                                             @Override
                                                             public void onPermissionResult(Permiso.ResultSet resultSet) {
                                                                 if (resultSet.isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION) && resultSet.isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                                                                     Realm.init(SelectLocationActivity.this);
                                                                     RealmCustomer realmCustomer = Realm.getInstance(RealmUtility.getDefaultConfig(SelectLocationActivity.this)).where(RealmCustomer.class).findFirst();
                                                                     startActivityForResult(new Intent(getApplicationContext(), MapsActivity.class)
                                                                             .putExtra("LONGITUDE", realmCustomer.getLongitude())
                                                                             .putExtra("LATITUDE", realmCustomer.getLatitude())
                                                                             .putExtra("BUTTON_TEXT", "CONFIRM DESTINATION"), RC_DESTINATION_LOCATION);
                                                                 }
                                                             }

                                                             @Override
                                                             public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                                                                 Permiso.getInstance().showRationaleInDialog(getApplicationContext().getString(R.string.permissions), getApplicationContext().getString(R.string.this_permission_is_mandatory_pls_allow_access), null, callback);
                                                             }
                                                         },
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION);
            }
        });
        order_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
                                                             @Override
                                                             public void onPermissionResult(Permiso.ResultSet resultSet) {
                                                                 if (resultSet.isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION) && resultSet.isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                                                                     Realm.init(SelectLocationActivity.this);
                                                                     RealmCustomer realmCustomer = Realm.getInstance(RealmUtility.getDefaultConfig(SelectLocationActivity.this)).where(RealmCustomer.class).findFirst();
                                                                     startActivityForResult(new Intent(getApplicationContext(), MapsActivity.class)
                                                                                     .putExtra("LONGITUDE", pickup_longitude)
                                                                                     .putExtra("LATITUDE", pickup_latitude)
                                                                                     .putExtra("BUTTON_TEXT", "CONFIRM PICKUP"),

                                                                             RC_CONFIRM_PICKUP_LOCATION);

                                                                 }
                                                             }

                                                             @Override
                                                             public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                                                                 Permiso.getInstance().showRationaleInDialog(getApplicationContext().getString(R.string.permissions), getApplicationContext().getString(R.string.this_permission_is_mandatory_pls_allow_access), null, callback);
                                                             }
                                                         },
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION);
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                InitApplication.getInstance().cancelPendingRequests("FINDING_DRIVER");

                ProgressDialog mProgress = new ProgressDialog(getApplicationContext());
                mProgress.setCancelable(false);
                mProgress.setIndeterminate(true);

                mProgress.setTitle("Please wait...");
                mProgress.show();

                StringRequest stringRequest = new StringRequest(
                        Request.Method.PATCH,
                        API_URL + "cancel-latest-pending-ride",
                        response -> {
                            mProgress.dismiss();
                            if (response != null) {

                                try {
                                    JSONObject jsonObject = new JSONObject(response);
                                    if (!jsonObject.isNull("ride_history"))
                                        Realm.init(getApplicationContext());
                                    Realm.getInstance(RealmUtility.getDefaultConfig(SelectLocationActivity.this)).executeTransaction(realm -> {
                                        try {
                                            realm.createOrUpdateObjectFromJson(RealmRideHistory.class, jsonObject.getJSONObject("ride_history"));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    });
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                PreferenceManager
                                        .getDefaultSharedPreferences(getApplicationContext())
                                        .edit()
                                        .putBoolean("DRIVING_TO_PICKUP", false)
                                        .putBoolean("DRIVING_TO_DESTINATION", false)
                                        .apply();
                                driver_name.setText(null);
                                profile_pic.setImageBitmap(null);
                                progress_layout.setVisibility(View.GONE);
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
                        Realm.init(getApplicationContext());
                        RealmCustomer realmCustomer = Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).where(RealmCustomer.class).findFirst();
                        String customer_id = realmCustomer.getCustomer_id();
                        params.put("customer_id", String.valueOf(customer_id));
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
            }
        });

        networkReceiver = new NetworkReceiver();
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
    protected void onDestroy() {
        super.onDestroy();
        if (myTimer != null) {
            myTimer.cancel();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_PICKUP_LOCATION:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        if (data != null) {
                            pickup_longitude = data.getDoubleExtra("LONGITUDE", 0.0d);
                            pickup_latitude = data.getDoubleExtra("LATITUDE", 0.0d);
                            DecimalFormat formatter = new DecimalFormat("#0.00");
                            progressDialog = new ProgressDialog(SelectLocationActivity.this);
                            progressDialog.setMessage("Please wait...");
                            progressDialog.setCancelable(false);
                            progressDialog.setIndeterminate(true);
                            progressDialog.show();

                            Realm.init(getApplicationContext());
                            String customer_id = Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).where(RealmCustomer.class).findFirst().getCustomer_id();

                            StringRequest stringRequest = new StringRequest(
                                    com.android.volley.Request.Method.POST,
                                    "https://ghanapostgps.sperixlabs.org/get-address",
                                    response -> {
                                        if (response != null) {
                                            try {
                                                JSONObject jsonObject = new JSONObject(response);
                                                if (jsonObject.getBoolean("found")) {
                                                    pickup_location.setText(jsonObject.getJSONObject("data").getJSONArray("Table").getJSONObject(0).getString("Street"));
                                                } else {
                                                    pickup_location.setText("Unnamed Location");
                                                }
                                                route();
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    },
                                    error -> {
                                        error.printStackTrace();
                                        myVolleyError(getApplicationContext(), error);
                                        progressDialog.dismiss();
                                        Log.d("Cyrilll", error.toString());
                                    }
                            ) {
                                @Override
                                public Map<String, String> getParams() throws AuthFailureError {
                                    Map<String, String> params = new HashMap<>();
                                    params.put("lat", String.valueOf(pickup_latitude));
                                    params.put("long", String.valueOf(pickup_longitude));
                                    return params;
                                }

                                /** Passing some request headers* */
                                @Override
                                public Map getHeaders() throws AuthFailureError {
                                    HashMap headers = new HashMap();
                                    headers.put("Content-Type", "application/x-www-form-urlencoded");
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
                    case Activity.RESULT_CANCELED:
                        // some stuff that will happen if there's no result
                        break;
                }
                break;
            case RC_DESTINATION_LOCATION:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        if (data != null) {
                            destination_longitude = data.getDoubleExtra("LONGITUDE", 0.0d);
                            destination_latitude = data.getDoubleExtra("LATITUDE", 0.0d);
                            DecimalFormat formatter = new DecimalFormat("#0.00");

                            progressDialog = new ProgressDialog(SelectLocationActivity.this);
                            progressDialog.setMessage("Please wait...");
                            progressDialog.setCancelable(false);
                            progressDialog.setIndeterminate(true);
                            progressDialog.show();

                            Realm.init(getApplicationContext());
                            String customer_id = Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).where(RealmCustomer.class).findFirst().getCustomer_id();

                            StringRequest stringRequest = new StringRequest(
                                    com.android.volley.Request.Method.POST,
                                    "https://ghanapostgps.sperixlabs.org/get-address",
                                    response -> {
                                        if (response != null) {
                                            try {
                                                JSONObject jsonObject = new JSONObject(response);
                                                if (jsonObject.getBoolean("found")) {
                                                    destination.setText(jsonObject.getJSONObject("data").getJSONArray("Table").getJSONObject(0).getString("Street"));
                                                } else {
                                                    destination.setText("Unnamed Location");
                                                }
                                                route();
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    },
                                    error -> {
                                        error.printStackTrace();
                                        myVolleyError(getApplicationContext(), error);
                                        progressDialog.dismiss();
                                        Log.d("Cyrilll", error.toString());
                                    }
                            ) {
                                @Override
                                public Map<String, String> getParams() throws AuthFailureError {
                                    Map<String, String> params = new HashMap<>();
                                    params.put("lat", String.valueOf(destination_latitude));
                                    params.put("long", String.valueOf(destination_longitude));
                                    return params;
                                }

                                /** Passing some request headers* */
                                @Override
                                public Map getHeaders() throws AuthFailureError {
                                    HashMap headers = new HashMap();
                                    headers.put("Content-Type", "application/x-www-form-urlencoded");
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
                    case Activity.RESULT_CANCELED:
                        // some stuff that will happen if there's no result
                        break;
                }
                break;
            case RC_CONFIRM_PICKUP_LOCATION:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        if (data != null) {
                            pickup_longitude = data.getDoubleExtra("LONGITUDE", 0.0d);
                            pickup_latitude = data.getDoubleExtra("LATITUDE", 0.0d);
                            progress_layout.setVisibility(View.VISIBLE);
                            order_layout.setVisibility(View.GONE);

                            radius += 1000;
                            findDriver(radius);
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

    private void findDriver(int radius) {
        Map<String, String> params = new HashMap<>();
        params.put("pickup_lat", String.valueOf(pickup_latitude));
        params.put("pickup_long", String.valueOf(pickup_longitude));
        params.put("destination_lat", String.valueOf(destination_latitude));
        params.put("destination_long", String.valueOf(destination_longitude));
        params.put("service_category", getIntent().getStringExtra("SERVICE_CATEGORY"));
        params.put("radius", String.valueOf(radius));
        Realm.init(getApplicationContext());
        RealmCustomer realmCustomer = Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).where(RealmCustomer.class).findFirst();
        String customer_id = realmCustomer.getCustomer_id();
        params.put("customer_id", String.valueOf(customer_id));


        HashMap headers = new HashMap();
        headers.put("accept", "application/json");
        headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(APITOKEN, ""));


        final int[] finalRadius = {radius};
        new MyVolleyRequest(
                SelectLocationActivity.this,
                API_URL + "nearest-rider",
                "FINDING_DRIVER",
                headers,
                params,
                new MyVolleyRequest.OnResponse() {
                    @Override
                    public void onResponse(String response) throws JSONException {
                        JSONObject jsonObject = new JSONObject(response);

                        if (jsonObject.has("no_riders_available") && finalRadius[0] > 5000) {
                            finalRadius[0] = 0;
                            progress_layout.setVisibility(View.GONE);
                            new AlertDialog.Builder(SelectLocationActivity.this)
                                    .setTitle(toTitleCase("No Riders"))
                                    .setMessage("There are no nearby riders at the moment.\n\nPlease try again later.")

                                    // Specifying a listener allows you to take an action before dismissing the dialog.
                                    // The dialog is automatically dismissed when a dialog button is clicked.
                                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                        destination.setText(null);
                                        if (polylines.size() > 0) {
                                            for (Polyline poly : polylines) {
                                                poly.remove();
                                            }
                                        }
                                        dialog.dismiss();
                                    })
                                    .setCancelable(false)
                                    .show();
                        } else {
                            if (!jsonObject.has("no_riders_available")) {
                                driver_info_layout.setVisibility(View.VISIBLE);
                                JSONObject providerJson = jsonObject.getJSONArray("riders").getJSONObject(0);
                                Glide.with(getApplicationContext()).load(providerJson.get("profile_image_url")).apply(new RequestOptions().centerCrop().placeholder(R.drawable.avatar)).into(profile_pic);
                                driver_name.setText(providerJson.getString("first_name"));
                                Realm.init(getApplicationContext());
                                Realm.getInstance(RealmUtility.getDefaultConfig(SelectLocationActivity.this)).executeTransaction(realm -> {
                                    try {
                                        realm.createOrUpdateObjectFromJson(RealmRideHistory.class, jsonObject.getJSONObject("ride_history"));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                });

                            }
                            myTimer = new Timer();
                            myTimer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    // If you want to modify a view in your Activity
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            StringRequest stringRequest = new StringRequest(
                                                    Request.Method.POST,
                                                    API_URL + "pending-ride",
                                                    response -> {
                                                        if (response != null) {
                                                            try {
                                                                JSONObject jsonObject = new JSONObject(response);

                                                                if (!jsonObject.has("no_pending_ride")) {
                                                                    JSONObject ride_historyJson = jsonObject.getJSONObject("ride_history");
                                                                    JSONObject providerJson = jsonObject.getJSONObject("provider");

                                                                    Realm.init(getApplicationContext());
                                                                    RealmCustomer realmCustomer = Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).where(RealmCustomer.class).findFirst();
                                                                    String customer_id = realmCustomer.getCustomer_id();

                                                                    PreferenceManager
                                                                            .getDefaultSharedPreferences(getApplicationContext())
                                                                            .edit()
                                                                            .putString("RIDE_HISTORY_ID", ride_historyJson.getString("ride_history_id"))
                                                                            .putString("PICKUP_LAT", String.valueOf(ride_historyJson.getDouble("pickup_latitude")))
                                                                            .putString("PICKUP_LONG", String.valueOf(ride_historyJson.getDouble("pickup_longitude")))
                                                                            .putString("DESTINATION_LAT", String.valueOf(ride_historyJson.getDouble("destination_latitude")))
                                                                            .putString("DESTINATION_LONG", String.valueOf(ride_historyJson.getDouble("destination_longitude")))
                                                                            .putString("PROVIDER_NAME", providerJson.getString("first_name"))
                                                                            .putString("PROVIDER_LAT", providerJson.getString("latitude"))
                                                                            .putString("PROVIDER_LONG", providerJson.getString("longitude"))
                                                                            .putString("PROVIDER_PROFILE_IMAGE_URL", providerJson.getString("profile_image_url"))
                                                                            .putString("PICKUP_ADDRESS", ride_historyJson.getString("pickup_address"))
                                                                            .putString("DESTINATION_ADDRESS", ride_historyJson.getString("destination_address"))
                                                                            .putString("SERVICE_ID", ride_historyJson.getString("service_id"))
                                                                            .putString("PROVIDER_PRIMARY_CONTACT", providerJson.getString("primary_contact"))
                                                                            .putString("VEHICLE_TYPE", providerJson.getString("vehicle_type"))
                                                                            .putString("VEHICLE_REGISTRATION_NUMBER", providerJson.getString("vehicle_registration_number"))
                                                                            .putString("PROVIDER_CONFIRMATION_TOKEN", providerJson.getString("confirmation_token"))
                                                                            .putString("CUSTOMER_ID", customer_id)
                                                                            .putString("PROVIDER_ID", providerJson.getString("provider_id"))
                                                                            .apply();


                                                                    if (ride_historyJson.getString("start_time").equals("null")) {
                                                                        PreferenceManager
                                                                                .getDefaultSharedPreferences(getApplicationContext())
                                                                                .edit()
                                                                                .putBoolean("DRIVING_TO_PICKUP", true)
                                                                                .putBoolean("DRIVING_TO_DESTINATION", false)
                                                                                .apply();
                                                                    }
                                                                    else {
                                                                        PreferenceManager
                                                                                .getDefaultSharedPreferences(getApplicationContext())
                                                                                .edit()
                                                                                .putBoolean("DRIVING_TO_PICKUP", false)
                                                                                .putBoolean("DRIVING_TO_DESTINATION", true)
                                                                                .apply();
                                                                    }
                                                                    startActivity(new Intent(getApplicationContext(), DriverFoundActivity.class));
                                                                    finish();
                                                                }
                                                                else {
                                                                    if (SelectLocationActivity.this.radius < 6000) {
                                                                        SelectLocationActivity.this.radius += 1000;
                                                                        driver_info_layout.setVisibility(View.GONE);
                                                                        findDriver(SelectLocationActivity.this.radius);
                                                                    } else {
                                                                        SelectLocationActivity.this.radius = 0;
                                                                        progress_layout.setVisibility(View.GONE);
                                                                        new AlertDialog.Builder(SelectLocationActivity.this)
                                                                                .setTitle(toTitleCase("No Riders"))
                                                                                .setMessage("There are no nearby riders at the moment.\n\nPlease try again later.")

                                                                                // Specifying a listener allows you to take an action before dismissing the dialog.
                                                                                // The dialog is automatically dismissed when a dialog button is clicked.
                                                                                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                                                                    destination.setText(null);
                                                                                    if (polylines.size() > 0) {
                                                                                        for (Polyline poly : polylines) {
                                                                                            poly.remove();
                                                                                        }
                                                                                    }
                                                                                    dialog.dismiss();
                                                                                })
                                                                                .setCancelable(false)
                                                                                .show();
                                                                    }
                                                                }
                                                            } catch (JSONException e) {
                                                                e.printStackTrace();
                                                            }
                                                        }
                                                    },
                                                    error -> {
                                                        error.printStackTrace();
                                                        myVolleyError(getApplicationContext(), error);

                                                        if (polylines.size() > 0) {
                                                            for (Polyline poly : polylines) {
                                                                poly.remove();
                                                            }
                                                        }
                                                        progress_layout.setVisibility(View.GONE);
                                                        if (myServiceListActivity != null) {
                                                            myServiceListActivity.finish();
                                                        }
                                                        finish();
                                                    }
                                            ) {
                                                @Override
                                                public Map<String, String> getParams() throws AuthFailureError {
                                                    Map<String, String> params = new HashMap<>();

                                                    Realm.init(getApplicationContext());
                                                    RealmCustomer realmCustomer = Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).where(RealmCustomer.class).findFirst();
                                                    String customer_id = realmCustomer.getCustomer_id();
                                                    params.put("customer_id", customer_id);
                                                    return params;
                                                }

                                                /** Passing some request headers* */
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
                                        }
                                    });
                                }
                            }, 5000);
                        }
                    }
                },
                new MyVolleyRequest.OnError() {
                    @Override
                    public void onError(VolleyError error) {
                        if (polylines.size() > 0) {
                            for (Polyline poly : polylines) {
                                poly.remove();
                            }
                        }
                        progress_layout.setVisibility(View.GONE);
                        if (myServiceListActivity != null) {
                            myServiceListActivity.finish();
                        }
                        finish();
                    }
                }
        ).Query();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
        pickup_longitude = getIntent().getDoubleExtra("LONGITUDE", 0d);
        pickup_latitude = getIntent().getDoubleExtra("LATITUDE", 0d);
        if (pickup_longitude == 0d && pickup_latitude == 0d) {
            mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                @Override
                public void onMyLocationChange(@NonNull Location location) {
                    if (!location_changed) {
                        location_changed = true;
                        latLng = new LatLng(location.getLatitude(), location.getLongitude());

                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                .target(latLng)      // Sets the center of the map to Mountain View
                                .zoom(17)                   // Sets the zoom
                                .bearing(90)                // Sets the orientation of the camera to east
                                .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                                .build();                   // Creates a CameraPosition from the builder
                        pickup_latitude = location.getLatitude();
                        pickup_longitude = location.getLongitude();
                        getPickupAddressAndNearbyCars();
                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                    }

                }
            });
        } else {
            latLng = new LatLng(pickup_latitude, pickup_longitude);

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latLng)      // Sets the center of the map to Mountain View
                    .zoom(17)                   // Sets the zoom
                    .bearing(90)                // Sets the orientation of the camera to east
                    .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            getPickupAddressAndNearbyCars();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    private void getPickupAddressAndNearbyCars() {
        ProgressDialog dialog = new ProgressDialog(SelectLocationActivity.this);
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.setIndeterminate(true);
        dialog.show();

        Realm.init(getApplicationContext());
        String customer_id = Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).where(RealmCustomer.class).findFirst().getCustomer_id();

        StringRequest stringRequest = new StringRequest(
                com.android.volley.Request.Method.POST,
                API_URL + "pickup-address-and-nearby-cars",
                response -> {
                    dialog.dismiss();
                    if (response != null) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            pickup_location.setText(jsonObject.getString("street_address"));

                            JSONArray nearby_locations = jsonObject.getJSONArray("nearby_locations");
                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                            builder.include(new LatLng(pickup_latitude, pickup_longitude));
                            for (int i = 0; i < nearby_locations.length(); i++) {
                                MarkerOptions startOptions = new MarkerOptions();
                                startOptions.position(new LatLng(nearby_locations.getJSONObject(i).getDouble("latitude"), nearby_locations.getJSONObject(i).getDouble("longitude")));
                                startOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car2));
                                mMap.addMarker(startOptions);

                                builder.include(new LatLng(nearby_locations.getJSONObject(i).getDouble("latitude"), nearby_locations.getJSONObject(i).getDouble("longitude")));
                            }

                            LatLngBounds bounds = builder.build();
                            int width = getResources().getDisplayMetrics().widthPixels;
                            int height = getResources().getDisplayMetrics().heightPixels - (int) convertDpToPx(getApplicationContext(), 90);
                            int padding = (int) (width * 0.15); // offset from edges of the map 15% of screen

                            // to animate camera with some padding and bound -cover- all markers
                            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
//                            mMap.animateCamera(cu);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                error -> {
                    error.printStackTrace();
                    myVolleyError(getApplicationContext(), error);
                    dialog.dismiss();
                    Log.d("Cyrilll", error.toString());
                }
        ) {
            @Override
            public Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("lat", String.valueOf(pickup_latitude));
                params.put("long", String.valueOf(pickup_longitude));
                params.put("service_category", getIntent().getStringExtra("SERVICE_CATEGORY"));
                return params;
            }

            /** Passing some request headers* */
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
    }

    @Override
    public void onRoutingFailure(RouteException e) {
        progressDialog.dismiss();
        new AlertDialog.Builder(SelectLocationActivity.this)
                .setTitle(toTitleCase("No Accessible Roads"))
                .setMessage("No accessible route found.")

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    destination.setText(null);
                    if (polylines.size() > 0) {
                        for (Polyline poly : polylines) {
                            poly.remove();
                        }
                    }
                    order_layout.setVisibility(View.GONE);
                    latLng = new LatLng(pickup_latitude, pickup_longitude);

                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(latLng)      // Sets the center of the map to Mountain View
                            .zoom(17)                   // Sets the zoom
                            .bearing(90)                // Sets the orientation of the camera to east
                            .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                            .build();
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int i) {

        // Start marker
        MarkerOptions startOptions = new MarkerOptions();
        startOptions.position(new LatLng(pickup_latitude, pickup_longitude));
        startOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_start));
        Marker markerStart = mMap.addMarker(startOptions);

        // End marker
        MarkerOptions endOptions = new MarkerOptions();
        endOptions.position(new LatLng(destination_latitude, destination_longitude));
        endOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_end));
        Marker markerEnd = mMap.addMarker(endOptions);




        /*CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(pickup_latitude, pickup_longitude));
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(16);
        mMap.moveCamera(center);*/

        /*MapHelper mapHelper = new MapHelper(mMap);
        mapHelper.zoomToFitMarkers(markerStart, markerEnd);*/


        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (int j = 0; j < route.size(); j++) {
            List<LatLng> points = route.get(j).getPoints();
            for (int k = 0; k < points.size(); k++) {
                builder.include(points.get(k));
            }
        }

        LatLngBounds bounds = builder.build();
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels - (int) convertDpToPx(getApplicationContext(), 210);
        int padding = (int) (width * 0.05); // offset from edges of the map 5% of screen

        // to animate camera with some padding and bound -cover- all markers
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
        mMap.animateCamera(cu);

        if (polylines.size() > 0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int j = 0; j < route.size(); j++) {

            //In case of more than 5 alternative routes
            int colorIndex = j % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + j * 3);
            polyOptions.addAll(route.get(j).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            time.setText(getTime(route.get(j).getDurationValue()));
            ride_time = route.get(j).getDurationValue() / 60;
            ride_distance = route.get(j).getDistanceValue() / 1000.0F;
            Float s = Float.valueOf(Math.round(base_fare + ((cost_per_min * ride_time) + (cost_per_km * ride_distance) * surge_boost_multiplier) + other_fee));
            price.setText("GHC" + String.format("%.2f", s));
        }
        order_layout.setVisibility(View.VISIBLE);
        progress_layout.setVisibility(View.GONE);
        progressDialog.dismiss();
    }

    @Override
    public void onRoutingCancelled() {

    }

    private void route() {
        if (pickup_location.getText() != null && !pickup_location.getText().equals("") && destination.getText() != null && !destination.getText().equals("")) {
            Routing routing = new Routing.Builder()
                    .travelMode(Routing.TravelMode.DRIVING)
                    .withListener(this)
                    .waypoints(new LatLng(pickup_latitude, pickup_longitude), new LatLng(destination_latitude, destination_longitude))
                    .key(getResources().getString(R.string.google_maps_key))
                    .build();
            routing.execute();
        }
        else {
            progressDialog.dismiss();
        }
    }

    public static String getTime(int seconds) {
        int day = (int) TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) - (day * 24);
        long minute = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds) * 60);


        String time_string = "";
        if (day > 0) {
            time_string += String.valueOf(day) + "d ";
        }
        if (hours > 0) {
            time_string += String.valueOf(hours) + "hr ";
        }
        if (minute > 0) {
            time_string += String.valueOf(minute) + "min";
        }

        if (time_string.equals("")) {
            return "1 min";
        } else {
            return time_string;
        }
    }
}
