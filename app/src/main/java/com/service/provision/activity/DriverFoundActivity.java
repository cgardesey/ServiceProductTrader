package com.service.provision.activity;

import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.activity.SelectLocationActivity.RC_DESTINATION_LOCATION;
import static com.service.provision.activity.SelectLocationActivity.getTime;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.keyConst.WS_URL;
import static com.service.provision.constants.Const.myVolleyError;
import static com.service.provision.receiver.NetworkReceiver.activeActivity;
import static com.service.provision.service.MyFirebaseMessagingService.*;
import static com.service.provision.util.Socket.EVENT_CLOSED;
import static com.service.provision.util.Socket.EVENT_OPEN;
import static com.service.provision.util.Socket.EVENT_RECONNECT_ATTEMPT;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
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
import com.google.android.libraries.places.api.Places;
import com.greysonparrelli.permiso.Permiso;
import com.greysonparrelli.permiso.PermisoActivity;
import com.makeramen.roundedimageview.RoundedImageView;
import com.service.provision.R;
import com.service.provision.materialDialog.CallProviderMaterialDialog;
import com.service.provision.materialDialog.RatingMaterialDialog;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmCustomer;
import com.service.provision.realm.RealmProvider;
import com.service.provision.realm.RealmRideHistory;
import com.service.provision.receiver.NetworkReceiver;
import com.service.provision.util.RealmUtility;
import com.service.provision.util.Socket;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;

public class DriverFoundActivity extends PermisoActivity implements OnMapReadyCallback, RoutingListener {

    double longitude = 0d, latitude = 0d;

    private GoogleMap mMap;


    public String TAG = "PLaceManish";

    private int request_code = 1001;
    public static final int RC_CONFIRM_LOCATION = 1001;

    MarkerOptions markerOptions;
    LatLng latLng;
    private double lat, lng;
    private float v;
    private Marker carMarker;
    private LatLng startPosition;
    private LatLng endPosition;
    NetworkReceiver networkReceiver;

    Float base_fare = 5.0F;
    Float cost_per_min = 0.15F;
    Float cost_per_km = 1.25F;
    Float ride_distance;
    int ride_time;
    Float surge_boost_multiplier = 1.0F;
    Float other_fee = 0.0F;

    public static double long0 = 0d;
    public static double lat0 = 0d;

    Toolbar toolbar;
    ProgressDialog progressDialog;
    public static RealmProvider realmProvider;
    static TextView address;
    TextView time;
    TextView rider_name;
    TextView vehicle_registration_number;
    static TextView destination;
    TextView change_destination;
    TextView rating;
    TextView total_rating;
    static TextView cancel_ride, rate_trip;
    RatingBar rating_bar;
    RoundedImageView profile_pic, call, chat;
    static LinearLayout destination_layout;
    static RelativeLayout cancel_layout;
    ArrayList<Polyline> polylines = new ArrayList<>();
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};
    List<LatLng> latLngs;
    private int emission = 0;
    public static Activity driverFoundActivity;
    static Socket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_found);
        driverFoundActivity = this;
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
                                                     @Override
                                                     public void onPermissionResult(Permiso.ResultSet resultSet) {
                                                         if (resultSet.isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION) && resultSet.isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                                                             mapFragment.getMapAsync(DriverFoundActivity.this);
                                                         }
                                                         else {
                                                             finish();
                                                         }
                                                     }

                                                     @Override
                                                     public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                                                         Permiso.getInstance().showRationaleInDialog(getApplicationContext().getString(R.string.permissions), getApplicationContext().getString(R.string.this_permission_is_mandatory_pls_allow_access), null, callback);
                                                     }
                                                 },
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION);

        Places.initialize(getApplicationContext(), getResources().getString(R.string.google_maps_key));

        String provider_lat = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("PROVIDER_LAT", "");
        if (!provider_lat.equals("")) {
            lat0 = Double.parseDouble(provider_lat);
            long0 = Double.parseDouble(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("PROVIDER_LONG", ""));
        }
        address = findViewById(R.id.address);
        destination_layout = findViewById(R.id.destination_layout);
        cancel_layout = findViewById(R.id.cancel_layout);
        time = findViewById(R.id.time);
        rider_name = findViewById(R.id.rider_name);
        vehicle_registration_number = findViewById(R.id.vehicle_registration_number);
        destination = findViewById(R.id.destination);
        change_destination = findViewById(R.id.change_destination);
        rating = findViewById(R.id.rating);
        total_rating = findViewById(R.id.total_rating);
        cancel_ride = findViewById(R.id.cancel_ride);
        profile_pic = findViewById(R.id.profile_pic);
        call = findViewById(R.id.call);
        chat = findViewById(R.id.chat);
        rating_bar = findViewById(R.id.rating_bar);
        destination = findViewById(R.id.destination);
        change_destination = findViewById(R.id.change_destination);

        rating_bar.setRating(3.5F);
        total_rating.setText("(2)");
        rider_name.setText(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("PROVIDER_NAME", ""));
        vehicle_registration_number.setText(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("VEHICLE_REGISTRATION_NUMBER", ""));
        String provider_profile_image_url = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("PROVIDER_PROFILE_IMAGE_URL", "");
        if (provider_profile_image_url != null && !provider_profile_image_url.equals("")) {
            Glide.with(getApplicationContext()).load(provider_profile_image_url).apply(new RequestOptions().centerCrop().placeholder(R.drawable.avatar)).into(profile_pic);

        }

        chat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DriverFoundActivity.this, MessageActivity.class)
                        .putExtra("CUSTOMER_ID", PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("CUSTOMER_ID", ""))
                        .putExtra("PROVIDER_NAME", PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("PROVIDER_NAME", ""))
                        .putExtra("PROFILE_IMAGE_URL", PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("PROVIDER_PROFILE_IMAGE_URL", ""))
                        .putExtra("AVAILABILITY", "Available")
                );
            }
        });

        call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProgressDialog dialog = new ProgressDialog(DriverFoundActivity.this);
                dialog.setMessage("Please wait...");
                dialog.setCancelable(false);
                dialog.setIndeterminate(true);
                dialog.show();

                StringRequest stringRequest = new StringRequest(
                        Request.Method.POST,
                        API_URL + "user-phone-number",
                        response -> {
                            if (response != null) {
                                dialog.dismiss();
                                try {
                                    JSONObject jsonObject = new JSONObject(response);
                                    String phone_number = jsonObject.getString("phone_number");


                                    CallProviderMaterialDialog callProviderMaterialDialog = new CallProviderMaterialDialog();
                                    if (callProviderMaterialDialog != null && callProviderMaterialDialog.isAdded()) {

                                    } else {
                                        callProviderMaterialDialog.setPhone_number(phone_number);
                                        callProviderMaterialDialog.setCustomer_id(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("CUSTOMER_ID", ""));
                                        callProviderMaterialDialog.setProvider_id(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("PROVIDER_ID", ""));
                                        callProviderMaterialDialog.show(getSupportFragmentManager(), "");
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        error -> {
                            error.printStackTrace();
                            myVolleyError(DriverFoundActivity.this, error);
                            dialog.dismiss();
                            Log.d("Cyrilll", error.toString());
                        }
                ) {
                    @Override
                    public Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> params = new HashMap<>();
                        params.put("customer_id", PreferenceManager.getDefaultSharedPreferences(DriverFoundActivity.this).getString("CUSTOMER_ID", ""));
                        return params;
                    }

                    /** Passing some request headers* */
                    @Override
                    public Map getHeaders() throws AuthFailureError {
                        HashMap headers = new HashMap();
                        headers.put("accept", "application/json");
                        headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(DriverFoundActivity.this).getString(APITOKEN, ""));
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

        cancel_ride.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProgressDialog mProgress = new ProgressDialog(DriverFoundActivity.this);
                mProgress.setCancelable(false);
                mProgress.setIndeterminate(true);

                mProgress.setTitle("Please wait...");
                mProgress.show();

                StringRequest stringRequest = new StringRequest(
                        Request.Method.PATCH,
                        API_URL + "ride-histories/" + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("RIDE_HISTORY_ID", ""),
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

                                    /*new notifyWithFCM(
                                            riderHomeActivity,
                                            PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("PROVIDER_CONFIRMATION_TOKEN", ""),
                                            new JSONObject()
                                                    .put("type", "ride_cancelled_by_customer")
                                                    .put("ride_history_id", getIntent().getStringExtra("RIDE_HISTORY_ID"))
                                    ).execute();*/

                                    try {
                                        notifyWithFCM(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("PROVIDER_CONFIRMATION_TOKEN", ""), new JSONObject()
                                                .put("type", "ride_cancelled_by_customer")
                                                .put("ride_history_id", getIntent().getStringExtra("RIDE_HISTORY_ID"))
                                        );
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    finish();

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
            }
        });

        SlidingUpPanelLayout layout = findViewById(R.id.slidingUp);

        layout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
//                findViewById(R.id.textView).setAlpha(1 - slideOffset);
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                if (newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
//                    Toast.makeText(getApplicationContext(), "Panel expanded!", Toast.LENGTH_SHORT).show();
                } else if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
//                    Toast.makeText(getApplicationContext(), "Panel collapsed!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        toolbar = findViewById(R.id.toolbar);
        rate_trip = findViewById(R.id.rate_trip);


        rate_trip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProgressDialog mProgress = new ProgressDialog(DriverFoundActivity.this);
                mProgress.setCancelable(false);
                mProgress.setIndeterminate(true);

                mProgress.setTitle("Please wait...");
                mProgress.show();

                StringRequest stringRequest = new StringRequest(
                        Request.Method.POST,
                        API_URL + "trips-made",
                        response -> {
                            mProgress.dismiss();
                            if (response != null) {
                                try {
                                    JSONObject jsonObject = new JSONObject(response);
                                    final String[] trips_made = new String[1];
                                    Realm.init(getApplicationContext());
                                    Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).executeTransaction(realm -> {
                                        try {
                                            trips_made[0] = String.valueOf(jsonObject.getInt("trips-made"));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    });

                                    RatingMaterialDialog ratingMaterialDialog = new RatingMaterialDialog();
                                    if (ratingMaterialDialog != null && ratingMaterialDialog.isAdded()) {

                                    } else {
                                        ratingMaterialDialog.setService_id(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("SERVICE_ID", ""));
                                        ratingMaterialDialog.setName_text(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("PROVIDER_NAME", ""));
                                        ratingMaterialDialog.setProfilepic_text(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("PROVIDER_PROFILE_IMAGE_URL", ""));
                                        ratingMaterialDialog.setTrips_made_text(trips_made[0]);
                                        ratingMaterialDialog.show(getFragmentManager(), "RatingMaterialDialog");
                                    }
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
                        params.put("service_id", PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("SERVICE_ID", ""));
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

        change_destination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
                                                             @Override
                                                             public void onPermissionResult(Permiso.ResultSet resultSet) {
                                                                 if (resultSet.isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION) && resultSet.isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                                                                     Realm.init(DriverFoundActivity.this);
                                                                     RealmCustomer realmCustomer = Realm.getInstance(RealmUtility.getDefaultConfig(DriverFoundActivity.this)).where(RealmCustomer.class).findFirst();
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


        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Preparing map...");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.show();

        networkReceiver = new NetworkReceiver();

        initSocket();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socket != null) {
            socket.leave("location:" + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("PROVIDER_ID", ""));
            socket.clearListeners();
            socket.close();
            socket.terminate();
            socket = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        activeActivity = this;
        registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        driverFoundInit(this);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_DESTINATION_LOCATION:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        if (data != null) {
                            Double destination_longitude = data.getDoubleExtra("LONGITUDE", 0.0d);
                            Double destination_latitude = data.getDoubleExtra("LATITUDE", 0.0d);
                            DecimalFormat formatter = new DecimalFormat("#0.00");


                            ProgressDialog mProgress = new ProgressDialog(DriverFoundActivity.this);
                            mProgress.setCancelable(false);
                            mProgress.setIndeterminate(true);

                            mProgress.setTitle("Please wait...");
                            mProgress.show();

                            StringRequest stringRequest = new StringRequest(
                                    Request.Method.PATCH,
                                    API_URL + "ride-histories/" + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("RIDE_HISTORY_ID", ""),
                                    response -> {
                                        mProgress.dismiss();
                                        if (response != null) {
                                            try {
                                                final RealmRideHistory[] realmRideHistory = new RealmRideHistory[1];

                                                JSONObject jsonObject = new JSONObject(response);
                                                Realm.init(getApplicationContext());
                                                Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).executeTransaction(realm -> {
                                                    realmRideHistory[0] = realm.createOrUpdateObjectFromJson(RealmRideHistory.class, jsonObject);
                                                });


                                                destination.setText(realmRideHistory[0].getDestination_address());

                                                PreferenceManager
                                                        .getDefaultSharedPreferences(activeActivity)
                                                        .edit()
                                                        .putString("DESTINATION_LAT", String.valueOf(destination_latitude))
                                                        .putString("DESTINATION_LONG", String.valueOf(destination_longitude))
                                                        .putString("DESTINATION_ADDRESS", realmRideHistory[0].getDestination_address())
                                                        .apply();

                                                /*new notifyWithFCM(
                                                        DriverFoundActivity.this,
                                                        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("PROVIDER_CONFIRMATION_TOKEN", ""),
                                                        new JSONObject()
                                                                .put("type", "destination_changed")
                                                                .put("destination_lat", String.valueOf(destination_latitude))
                                                                .put("destination_long", String.valueOf(destination_longitude))
                                                                .put("destination_address", realmRideHistory[0].getDestination_address())
                                                ).execute();*/

                                                try {
                                                    notifyWithFCM(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("PROVIDER_CONFIRMATION_TOKEN", ""), new JSONObject()
                                                            .put("type", "destination_changed")
                                                            .put("destination_lat", String.valueOf(destination_latitude))
                                                            .put("destination_long", String.valueOf(destination_longitude))
                                                            .put("destination_address", realmRideHistory[0].getDestination_address())
                                                    );
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }

                                                driverFoundInit(DriverFoundActivity.this);

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
                                    params.put("destination_latitude", String.valueOf(destination_latitude));
                                    params.put("destination_longitude", String.valueOf(destination_latitude));
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

    public static void driverFoundInit(Activity activity) {
        if (PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("DRIVING_TO_PICKUP", false)) {
            address.setText("Meet at " + PreferenceManager.getDefaultSharedPreferences(activity).getString("PICKUP_ADDRESS", ""));
            cancel_layout.setVisibility(View.VISIBLE);
            destination_layout.setVisibility(View.GONE);
        } else if (PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("DRIVING_TO_DESTINATION", false)) {
            address.setText("Driving to " + PreferenceManager.getDefaultSharedPreferences(activity).getString("DESTINATION_ADDRESS", ""));
            cancel_layout.setVisibility(View.GONE);
            destination_layout.setVisibility(View.VISIBLE);

            destination.setText(PreferenceManager.getDefaultSharedPreferences(activity).getString("DESTINATION_ADDRESS", ""));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(networkReceiver);
    }

    @SuppressLint("NewApi")
    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        enterPictureInPictureMode();
    }

    @SuppressLint("NewApi")
    @Override
    public void onUserLeaveHint() {
        enterPictureInPictureMode();
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        if (isInPictureInPictureMode) {
            // Hide the full-screen UI (controls, etc.) while in picture-in-picture mode.
        } else {
            // Restore the full-screen UI.
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        progressDialog.dismiss();
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

        latLng = new LatLng(lat0, long0);

        if (lat0 == 0d && long0 == 0d) {
            return;
        }

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng)      // Sets the center of the map to Mountain View
                .zoom(17)                   // Sets the zoom
                .bearing(90)                // Sets the orientation of the camera to east
                .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        if (carMarker == null) {
            carMarker = mMap.addMarker(new MarkerOptions().position(latLng)
                    .flat(true)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car2)));
        }

        Routing routing = new Routing.Builder()
                .travelMode(Routing.TravelMode.DRIVING)
                .withListener(DriverFoundActivity.this)
                .waypoints(
                        new LatLng(
                                lat0,
                                long0
                        ),
                        new LatLng(
                                Double.parseDouble(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("DESTINATION_LAT", "")),
                                Double.parseDouble(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("DESTINATION_LONG", ""))
                        )
                )
                .key(getResources().getString(R.string.google_maps_key))
                .build();
        routing.execute();
    }

    public void initSocket() {
        socket = Socket
                .Builder.with(WS_URL)

                .build();
        socket.connect();
        socket.clearListeners();

        socket.onEvent(EVENT_OPEN, new Socket.OnEventListener() {
            @Override
            public void onMessage(String event) {
                Log.d("mywebsocket1", "Connected");

                socket.join("location:" + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("PROVIDER_ID", ""));

                socket.onEventResponse("location:" + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("PROVIDER_ID", ""), new Socket.OnEventResponseListener() {
                    @Override
                    public void onMessage(String event, String data) {

                    }
                });

                socket.setMessageListener(new Socket.OnMessageListener() {
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
                                    Log.d("mywebsocket1", jsonResponse.toString());
                                    Realm.init(driverFoundActivity);
                                    JSONObject finalJsonResponse = jsonResponse;

                                    if (true) {
                                        double latitude = finalJsonResponse.getJSONObject("data").getDouble("latitude");
                                        double longitude = finalJsonResponse.getJSONObject("data").getDouble("longitude");

                                        List<LatLng> mylatlngs = new ArrayList<>();
                                        mylatlngs.add(new LatLng(lat0, long0));
                                        mylatlngs.add(new LatLng(latitude, longitude));


                                        Location location0 = new Location("");//provider name is unnecessary
                                        location0.setLatitude(lat0);//your coords of course
                                        location0.setLongitude(long0);

                                        Location location1 = new Location("");//provider name is unnecessary
                                        location1.setLatitude(latitude);//your coords of course
                                        location1.setLongitude(longitude);

                                        float distance = location0.distanceTo(location1);
                                        Log.d("Locations:distance", String.valueOf(distance));
                                        if (distance > 0.0F) {
                                            animateCarOnMap(mylatlngs);
                                            lat0 = latitude;
                                            long0 = longitude;

                                            PreferenceManager
                                                    .getDefaultSharedPreferences(getApplicationContext())
                                                    .edit()
                                                    .putString("PROVIDER_LAT", String.valueOf("latitude"))
                                                    .putString("PROVIDER_LONG", String.valueOf("longitude"))
                                                    .apply();

                                            Routing routing = new Routing.Builder()
                                                    .travelMode(Routing.TravelMode.DRIVING)
                                                    .withListener(DriverFoundActivity.this)
                                                    .waypoints(
                                                            new LatLng(
                                                                    latitude,
                                                                    longitude
                                                            ),
                                                            new LatLng(
                                                                    Double.parseDouble(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("DESTINATION_LAT", "")),
                                                                    Double.parseDouble(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("DESTINATION_LONG", ""))
                                                            )
                                                    )
                                                    .key(getResources().getString(R.string.google_maps_key))
                                                    .build();
                                            routing.execute();
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


            }
        });

        socket.onEvent(EVENT_RECONNECT_ATTEMPT, new Socket.OnEventListener() {
            @Override
            public void onMessage(String event) {
                Log.d("mywebsocket1", "reconnecting");
            }
        });
        socket.onEvent(EVENT_CLOSED, new Socket.OnEventListener() {
            @Override
            public void onMessage(String event) {
                Log.d("mywebsocket1", "connection closed");
            }
        });
    }

    public void animateCarOnMap(final List<LatLng> latLngs) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng latLng : latLngs) {
            builder.include(latLng);
        }
        LatLngBounds bounds = builder.build();
        CameraUpdate mCameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 2);
        mMap.animateCamera(mCameraUpdate);
        if (carMarker == null) {
            carMarker = mMap.addMarker(new MarkerOptions().position(latLngs.get(0))
                    .flat(true)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car2)));
        }
        carMarker.setPosition(latLngs.get(0));
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.setDuration(1000);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                v = valueAnimator.getAnimatedFraction();
                double lng = v * latLngs.get(1).longitude + (1 - v)
                        * latLngs.get(0).longitude;
                double lat = v * latLngs.get(1).latitude + (1 - v)
                        * latLngs.get(0).latitude;
                LatLng newPos = new LatLng(lat, lng);
                carMarker.setPosition(newPos);
                carMarker.setAnchor(0.5f, 0.5f);
                carMarker.setRotation(getBearing(latLngs.get(0), newPos));
                mMap.moveCamera(CameraUpdateFactory
                        .newCameraPosition
                                (new CameraPosition.Builder()
                                        .target(newPos)
                                        .zoom(17.5f)
                                        .build()));

                startPosition = carMarker.getPosition();
            }
        });
        valueAnimator.start();
    }

    private static float getBearing(LatLng begin, LatLng end) {
        double lat = Math.abs(begin.latitude - end.latitude);
        double lng = Math.abs(begin.longitude - end.longitude);

        if (begin.latitude < end.latitude && begin.longitude < end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)));
        else if (begin.latitude >= end.latitude && begin.longitude < end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);
        else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)) + 180);
        else if (begin.latitude < end.latitude && begin.longitude >= end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 270);
        return -1;
    }

    @Override
    public void onRoutingFailure(RouteException e) {

    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int i) {
        time.setText(getTime(route.get(0).getDurationValue()));
    }

    @Override
    public void onRoutingCancelled() {

    }
}
