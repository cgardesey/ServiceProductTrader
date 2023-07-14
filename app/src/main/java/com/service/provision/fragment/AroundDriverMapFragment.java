package com.service.provision.fragment;

import static com.service.provision.activity.PictureActivity.*;
import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.cancelNotification;
import static com.service.provision.constants.Const.isServiceRunning;
import static com.service.provision.constants.Const.myVolleyError;
import static com.service.provision.fragment.CustomerAccountFragment1.PICTURE_TYPE;
import static com.service.provision.fragment.CustomerAccountFragment1.TYPE_PROFILE_PIC;
import static com.service.provision.service.LocationUpdateService.*;
import static com.service.provision.service.LocationUpdateService.locationCallback;
import static com.service.provision.service.LocationUpdateService.mFusedLocationClient;
import static com.service.provision.service.MyFirebaseMessagingService.notifyWithFCM;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.greysonparrelli.permiso.Permiso;
import com.makeramen.roundedimageview.RoundedDrawable;
import com.makeramen.roundedimageview.RoundedImageView;
import com.service.provision.R;
import com.service.provision.activity.MessageActivity;
import com.service.provision.activity.PictureActivity;
import com.service.provision.materialDialog.CallProviderMaterialDialog;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmProvider;
import com.service.provision.receiver.NetworkReceiver;
import com.service.provision.service.LocationUpdateService;
import com.service.provision.util.RealmUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;

public class AroundDriverMapFragment extends Fragment implements OnMapReadyCallback, RoutingListener {

    double longitude = 0d, latitude = 0d;

    private GoogleMap mMap;


    public String TAG = "PLaceManish";
    private int request_code = 1001;
    public static final int RC_CONFIRM_LOCATION = 1001;

    NetworkReceiver networkReceiver;

    MarkerOptions markerOptions;
    LatLng latLng;
    private double lat, lng;
    private float v;
    private Marker carMarker;
    private LatLng startPosition;
    private LatLng endPosition;

    Float base_fare = 5.0F;
    Float cost_per_min = 0.15F;
    Float cost_per_km = 1.25F;
    Float ride_distance;
    int ride_time;
    Float surge_boost_multiplier = 1.0F;
    Float other_fee = 0.0F;

    Toolbar toolbar;
    ProgressDialog progressDialog;
    ArrayList<Polyline> polylines = new ArrayList<>();
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};
    List<LatLng> latLngs;
    boolean location_changed = false;
    Switch status_switch;
    static CardView ride_action_layout;
    static Button arrived;
    static Button end_ride;
    Button navigate;
    TextView cancel_ride;
    static RoundedImageView profile_pic;
    RoundedImageView call;
    RoundedImageView chat;
    static TextView customer_name;
    static TextView address;
    static TextView action_text;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final View rootView = inflater.inflate(R.layout.fragment_around_driver_map, container, false);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Places.initialize(getActivity(), getResources().getString(R.string.google_maps_key));
        toolbar = rootView.findViewById(R.id.toolbar);
        ride_action_layout = rootView.findViewById(R.id.ride_action_layout);
        arrived = rootView.findViewById(R.id.arrived);
        end_ride = rootView.findViewById(R.id.end_ride);
        navigate = rootView.findViewById(R.id.navigate);
        action_text = rootView.findViewById(R.id.action_text);
        cancel_ride = rootView.findViewById(R.id.cancel_ride);
        profile_pic = rootView.findViewById(R.id.profile_pic);
        customer_name = rootView.findViewById(R.id.customer_name);
        address = rootView.findViewById(R.id.address);
        call = rootView.findViewById(R.id.call);
        chat = rootView.findViewById(R.id.chat);

        Realm.init(getContext());
        RealmProvider realmProvider = Realm.getInstance(RealmUtility.getDefaultConfig(getContext())).where(RealmProvider.class).equalTo("provider_id", PreferenceManager.getDefaultSharedPreferences(getContext()).getString("PROVIDER_ID", "")).findFirst();
        String provider_id = realmProvider.getProvider_id();

        status_switch = rootView.findViewById(R.id.status_switch);

        Realm.init(getContext());
        String availability = Realm.getInstance(RealmUtility.getDefaultConfig(getContext())).where(RealmProvider.class).equalTo("provider_id", PreferenceManager.getDefaultSharedPreferences(getContext()).getString("PROVIDER_ID", "")).findFirst().getAvailability();
        status_switch.setChecked(!availability.equals("Closed"));
        if (status_switch.isChecked()) {
            PreferenceManager
                    .getDefaultSharedPreferences(getActivity())
                    .edit()
                    .putBoolean("LOCATION_SHARING_ACTIVE", true)
                    .apply();

            if (!isServiceRunning(getActivity(), "com.service.provision.service.LocationUpdateService")) {
                ContextCompat.startForegroundService(getActivity(), new Intent(getActivity(), LocationUpdateService.class)
                        .putExtra("PUBLISHER_ID", provider_id)
                        .putExtra("ROLE", PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("ROLE", ""))
                );
            }
        }
        status_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                ProgressDialog dialog = new ProgressDialog(getActivity());
                dialog.setMessage("Please wait...");
                dialog.setCancelable(false);
                dialog.setIndeterminate(true);
                dialog.show();

                StringRequest stringRequest = new StringRequest(
                        Request.Method.POST,
                        API_URL + "providers/" + provider_id,
                        response -> {
                            dialog.dismiss();
                            if (response != null) {
                                JSONObject jsonObjectResponse = null;
                                try {
                                    jsonObjectResponse = new JSONObject(response);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                Realm.init(getActivity());
                                JSONObject finalJsonObjectResponse = jsonObjectResponse;
                                Realm.getInstance(RealmUtility.getDefaultConfig(NetworkReceiver.activeActivity)).executeTransaction(realm -> {
                                    realm.createOrUpdateObjectFromJson(RealmProvider.class, finalJsonObjectResponse);
                                });
                                if (isChecked) {
                                    PreferenceManager
                                            .getDefaultSharedPreferences(getActivity())
                                            .edit()
                                            .putBoolean("LOCATION_SHARING_ACTIVE", true)
                                            .apply();


                                    if (!isServiceRunning(getActivity(), "com.service.provision.service.LocationUpdateService")) {
                                        ContextCompat.startForegroundService(getActivity(), new Intent(getActivity(), LocationUpdateService.class)
                                                .putExtra("PUBLISHER_ID", provider_id)
                                                .putExtra("ROLE", PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("ROLE", ""))
                                        );
                                    }
                                }
                                else {
                                    PreferenceManager
                                            .getDefaultSharedPreferences(getActivity())
                                            .edit()
                                            .putBoolean("LOCATION_SHARING_ACTIVE", false)
                                            .apply();
                                    mFusedLocationClient.removeLocationUpdates(locationCallback);

                                    if (socket != null) {
                                        socket.leave("location:" + provider_id);
                                        socket.clearListeners();
                                        socket.close();
                                        socket.terminate();
                                        socket = null;
                                    }

                                    cancelNotification(getActivity(), LOCATION_SERVICE_NOTIF_ID);
                                }
                            }
                        },
                        error -> {
                            status_switch.toggle();
                            dialog.dismiss();
                            error.printStackTrace();
                            Log.d("Cyrilll", error.toString());
                            myVolleyError(NetworkReceiver.activeActivity, error);
                        }
                ) {
                    @Override
                    public Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> params = new HashMap<>();
                        if (status_switch.isChecked()) {
                            params.put("availability", "Available");
                        }
                        else {
                            params.put("availability", "Closed");
                        }
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
        });
        status_switch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        navigate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("DRIVING_TO_PICKUP", false)) {
                    Uri gmmIntentUri = Uri.parse("google.navigation:q=" + PreferenceManager.getDefaultSharedPreferences(getContext()).getString("PICKUP_LAT", "") + "," + PreferenceManager.getDefaultSharedPreferences(getContext()).getString("PICKUP_LONG", ""));
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(mapIntent);
                } else if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("DRIVING_TO_DESTINATION", false)) {
                    Uri gmmIntentUri = Uri.parse("google.navigation:q=" + PreferenceManager.getDefaultSharedPreferences(getContext()).getString("DESTINATION_LAT", "") + "," + PreferenceManager.getDefaultSharedPreferences(getContext()).getString("DESTINATION_LONG", ""));
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(mapIntent);
                }
            }
        });

        cancel_ride.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProgressDialog mProgress = new ProgressDialog(getActivity());
                mProgress.setCancelable(false);
                mProgress.setIndeterminate(true);

                mProgress.setTitle("Please wait...");
                mProgress.show();

                StringRequest stringRequest = new StringRequest(
                        Request.Method.PATCH,
                        API_URL + "ride-histories/" + PreferenceManager.getDefaultSharedPreferences(getContext()).getString("RIDE_HISTORY_ID", ""),
                        response -> {
                            mProgress.dismiss();
                            if (response != null) {
                                PreferenceManager
                                        .getDefaultSharedPreferences(getActivity())
                                        .edit()
                                        .putBoolean("DRIVING_TO_PICKUP", false)
                                        .putBoolean("DRIVING_TO_DESTINATION", false)
                                        .apply();
                                ride_action_layout.setVisibility(View.GONE);

                                /*try {
                                    new MyFirebaseMessagingService.notifyWithFCM(
                                            riderHomeActivity,
                                            PreferenceManager.getDefaultSharedPreferences(getContext()).getString("CUSTOMER_CONFIRMATION_TOKEN", ""),
                                            new JSONObject()
                                                    .put("type", "ride_cancelled_by_driver")
                                    ).execute();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }*/

                                try {
                                    notifyWithFCM(getActivity(), PreferenceManager.getDefaultSharedPreferences(getContext()).getString("CUSTOMER_CONFIRMATION_TOKEN", ""), new JSONObject()
                                            .put("type", "ride_cancelled_by_driver"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        error -> {
                            mProgress.dismiss();
                            error.printStackTrace();
                            myVolleyError(getActivity(), error);
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
                        headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(APITOKEN, ""));
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


        arrived.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProgressDialog mProgress = new ProgressDialog(getActivity());
                mProgress.setCancelable(false);
                mProgress.setIndeterminate(true);

                mProgress.setTitle("Please wait...");
                mProgress.show();

                StringRequest stringRequest = new StringRequest(
                        Request.Method.PATCH,
                        API_URL + "ride-histories/" + PreferenceManager.getDefaultSharedPreferences(getContext()).getString("RIDE_HISTORY_ID", ""),
                        response -> {
                            mProgress.dismiss();
                            if (response != null) {
                                PreferenceManager
                                        .getDefaultSharedPreferences(getActivity())
                                        .edit()
                                        .putBoolean("DRIVING_TO_PICKUP", false)
                                        .putBoolean("DRIVING_TO_DESTINATION", true)
                                        .apply();
                                action_text.setText("Driving to Destination");
                                address.setText("Driving to " + PreferenceManager.getDefaultSharedPreferences(getContext()).getString("PICKUP_ADDRESS", ""));
                                arrived.setVisibility(View.GONE);
                                end_ride.setVisibility(View.VISIBLE);

                                /*try {
                                    new notifyWithFCM(
                                            riderHomeActivity,
                                            PreferenceManager.getDefaultSharedPreferences(getContext()).getString("CUSTOMER_CONFIRMATION_TOKEN", ""),
                                            new JSONObject()
                                                    .put("type", "driver_arrived")
                                                    .put("provider_lat", PreferenceManager.getDefaultSharedPreferences(getContext()).getString("CURRENT_LAT", ""))
                                                    .put("provider_long", PreferenceManager.getDefaultSharedPreferences(getContext()).getString("CURRENT_LONG", ""))
                                    ).execute();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                };*/


                                try {
                                    notifyWithFCM(getActivity(), PreferenceManager.getDefaultSharedPreferences(getContext()).getString("CUSTOMER_CONFIRMATION_TOKEN", ""), new JSONObject()
                                            .put("type", "driver_arrived")
                                            .put("provider_lat", PreferenceManager.getDefaultSharedPreferences(getContext()).getString("CURRENT_LAT", ""))
                                            .put("provider_long", PreferenceManager.getDefaultSharedPreferences(getContext()).getString("CURRENT_LONG", ""))
                                    );
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        error -> {
                            mProgress.dismiss();
                            error.printStackTrace();
                            myVolleyError(getActivity(), error);
                            Log.d("Cyrilll", error.toString());
                        }
                ) {
                    @Override
                    public Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> params = new HashMap<>();
                        params.put("arrived", "arrived");
                        params.put("provider_id", PreferenceManager.getDefaultSharedPreferences(getContext()).getString("PROVIDER_ID", ""));
                        return params;
                    }

                    @Override
                    public Map getHeaders() throws AuthFailureError {
                        HashMap headers = new HashMap();
                        headers.put("accept", "application/json");
                        headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(APITOKEN, ""));
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

        end_ride.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ProgressDialog dialog = new ProgressDialog(getActivity());
                dialog.setMessage("Please wait...");
                dialog.setCancelable(false);
                dialog.setIndeterminate(true);
                dialog.show();

                StringRequest stringRequest = new StringRequest(
                        Request.Method.POST,
                        API_URL + "scoped-ride-stops",
                        response -> {
                            if (response != null) {
                                dialog.dismiss();
                                try {
                                    JSONObject jsonObject = new JSONObject(response);
                                    ride_time = jsonObject.getInt("time");
                                    JSONArray stopJsonArray = jsonObject.getJSONArray("stops");
                                    LatLng[] latLngs = new LatLng[stopJsonArray.length()];
                                    for (int i = 0; i < stopJsonArray.length(); i++) {
                                        JSONObject stopJson = stopJsonArray.getJSONObject(i);
                                        latLngs[i] = new LatLng(stopJson.getDouble("latitude"), stopJson.getDouble("longitude"));
                                    }

                                    Routing routing = new Routing.Builder()
                                            .travelMode(Routing.TravelMode.DRIVING)
                                            .withListener(AroundDriverMapFragment.this)
                                            .waypoints(latLngs)
                                            .key(getResources().getString(R.string.google_maps_key))
                                            .build();
                                    routing.execute();

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        error -> {
                            error.printStackTrace();
                            myVolleyError(getActivity(), error);
                            dialog.dismiss();
                            Log.d("Cyrilll", error.toString());
                        }
                ) {
                    @Override
                    public Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> params = new HashMap<>();
                        params.put("ride_history_id", PreferenceManager.getDefaultSharedPreferences(getContext()).getString("RIDE_HISTORY_ID", ""));
                        params.put("service_id", PreferenceManager.getDefaultSharedPreferences(getContext()).getString("SERVICE_ID", ""));
                        return params;
                    }

                    /** Passing some request headers* */
                    @Override
                    public Map getHeaders() throws AuthFailureError {
                        HashMap headers = new HashMap();
                        headers.put("accept", "application/json");
                        headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(APITOKEN, ""));
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

        profile_pic.setOnClickListener(view -> {
            if (profile_pic.getDrawable() == null) {
                //Toast.makeText(mContext, getString(R.string.image_not_set), Toast.LENGTH_SHORT).show();
            } else {
                idPicBitmap = ((RoundedDrawable) profile_pic.getDrawable()).getSourceBitmap();
                Intent intent = new Intent(getActivity(), PictureActivity.class);
                intent.putExtra(PICTURE_TYPE, TYPE_PROFILE_PIC);
                getActivity().startActivity(intent);
            }
        });

        chat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), MessageActivity.class)
                        .putExtra("CUSTOMER_ID", PreferenceManager.getDefaultSharedPreferences(getContext()).getString("CUSTOMER_ID", ""))
                        .putExtra("CUSTOMER_NAME", PreferenceManager.getDefaultSharedPreferences(getContext()).getString("CUSTOMER_NAME", ""))
                        .putExtra("PROFILE_IMAGE_URL", PreferenceManager.getDefaultSharedPreferences(getContext()).getString("CUSTOMER_PROFILE_IMAGE_URL", ""))
                        .putExtra("AVAILABILITY", "Available")
                );
            }
        });

        call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProgressDialog dialog = new ProgressDialog(getActivity());
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
                                        callProviderMaterialDialog.setCustomer_id(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("CUSTOMER_ID", ""));
                                        callProviderMaterialDialog.setProvider_id(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("PROVIDER_ID", ""));
                                        callProviderMaterialDialog.show(getChildFragmentManager(), "");
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        error -> {
                            error.printStackTrace();
                            myVolleyError(getActivity(), error);
                            dialog.dismiss();
                            Log.d("Cyrilll", error.toString());
                        }
                ) {
                    @Override
                    public Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> params = new HashMap<>();
                        params.put("customer_id", PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("CUSTOMER_ID", ""));
                        return params;
                    }

                    /** Passing some request headers* */
                    @Override
                    public Map getHeaders() throws AuthFailureError {
                        HashMap headers = new HashMap();
                        headers.put("accept", "application/json");
                        headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(APITOKEN, ""));
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

        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage("Preparing map...");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.show();


        networkReceiver = new NetworkReceiver();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        initDriverMap(getActivity());
    }

    public static void initDriverMap(Activity activity) {
        if (PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("DRIVING_TO_PICKUP", false)) {
            ride_action_layout.setVisibility(View.VISIBLE);
            arrived.setVisibility(View.VISIBLE);
            end_ride.setVisibility(View.GONE);
            String customer_profile_image_url = PreferenceManager.getDefaultSharedPreferences(activity).getString("CUSTOMER_PROFILE_IMAGE_URL", "");
            customer_name.setText(PreferenceManager.getDefaultSharedPreferences(activity).getString("CUSTOMER_NAME", ""));
            String pickup_address = PreferenceManager.getDefaultSharedPreferences(activity).getString("PICKUP_ADDRESS", "");
            address.setText("Meet at " + pickup_address);
            action_text.setText("Driving to Pickup");
//            cancel_ride.setVisibility(View.VISIBLE);
            if (customer_profile_image_url != null && !customer_profile_image_url.equals("")) {
                Glide.with(activity).load(customer_profile_image_url).apply(new RequestOptions().centerCrop().placeholder(R.drawable.avatar)).into(profile_pic);
            }
        }
        else if (PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("DRIVING_TO_DESTINATION", false)) {
            ride_action_layout.setVisibility(View.VISIBLE);
            arrived.setVisibility(View.GONE);
            end_ride.setVisibility(View.VISIBLE);
            String customer_profile_image_url = PreferenceManager.getDefaultSharedPreferences(activity).getString("CUSTOMER_PROFILE_IMAGE_URL", "");
            customer_name.setText(PreferenceManager.getDefaultSharedPreferences(activity).getString("CUSTOMER_NAME", ""));
            action_text.setText("Driving to Destination");
            address.setText("Driving to " + PreferenceManager.getDefaultSharedPreferences(activity).getString("DESTINATION_ADDRESS", ""));
//            cancel_ride.setVisibility(View.GONE);
            if (customer_profile_image_url != null && !customer_profile_image_url.equals("")) {
                Glide.with(activity).load(customer_profile_image_url).apply(new RequestOptions().centerCrop().placeholder(R.drawable.avatar)).into(profile_pic);
            }
        }
        else {
            ride_action_layout.setVisibility(View.GONE);
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
        mMap = googleMap;
        progressDialog.dismiss();
        /*Routing routing = new Routing.Builder()
                .travelMode(Routing.TravelMode.DRIVING)
                .withListener(this)
                .waypoints(new LatLng(getActivity().getIntent().getDoubleExtra("DESTINATION_LATITUDE", 0.0d), getActivity().getIntent().getDoubleExtra("DESTINATION_LONGITUDE", 0.0d)), new LatLng(getActivity().getIntent().getDoubleExtra("PICKUP_LATITUDE", 0.0d), getActivity().getIntent().getDoubleExtra("PICKUP_LONGITUDE", 0.0d)))
                .key(getResources().getString(R.string.google_maps_key))
                .build();
        routing.execute();*/


        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
                                                     @SuppressLint("MissingPermission")
                                                     @Override
                                                     public void onPermissionResult(Permiso.ResultSet resultSet) {
                                                         if (resultSet.isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION) && resultSet.isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                                                             mMap.setMyLocationEnabled(true);
                                                             mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                                                                 @Override
                                                                 public void onMyLocationChange(@NonNull Location location) {
                                                                     if (!location_changed) {
                                                                         location_changed = true;
                                                                         Log.d("asdffdsss", Boolean.toString(location_changed));
                                                                         longitude = location.getLongitude();
                                                                         latitude = location.getLatitude();

                                                                         latLng = new LatLng(latitude, longitude);

                                                                         CameraPosition cameraPosition = new CameraPosition.Builder()
                                                                                 .target(latLng)      // Sets the center of the map to Mountain View
                                                                                 .zoom(17)                   // Sets the zoom
                                                                                 .bearing(90)                // Sets the orientation of the camera to east
                                                                                 .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                                                                                 .build();                   // Creates a CameraPosition from the builder
                                                                         mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                                                                     }
                                                                 }
                                                             });
                                                         }
                                                     }

                                                     @Override
                                                     public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                                                         Permiso.getInstance().showRationaleInDialog(getActivity().getString(R.string.permissions), getActivity().getString(R.string.this_permission_is_mandatory_pls_allow_access), null, callback);
                                                     }
                                                 },
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void startAutocompleteActivity() {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME);
        // Start the autocomplete intent.
        Intent intent = new Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY, fields)
                .build(getActivity());
        startActivityForResult(intent, request_code);
    }

    @Override
    public void onRoutingFailure(RouteException e) {

    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int i) {
        ride_time = route.get(0).getDurationValue() / 60;
        ride_distance = route.get(0).getDistanceValue() / 1000.0F;
        Float ride_cost = Float.valueOf(Math.round(base_fare + ((cost_per_min * ride_time) + (cost_per_km * ride_distance) * surge_boost_multiplier) + other_fee));
        String price = "GHC" + String.format("%.2f", ride_cost);
        /*try {
            new notifyWithFCM(
                    riderHomeActivity,
                    PreferenceManager.getDefaultSharedPreferences(getContext()).getString("CUSTOMER_CONFIRMATION_TOKEN", ""),
                    new JSONObject()
                            .put("type", "ride_ended")
                            .put("price", price)
            ).execute();
        } catch (JSONException e) {
            e.printStackTrace();
        }*/

        try {
            notifyWithFCM(getActivity(), PreferenceManager.getDefaultSharedPreferences(getContext()).getString("CUSTOMER_CONFIRMATION_TOKEN", ""), new JSONObject()
                    .put("type", "ride_ended")
                    .put("price", price)
            );
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ride_action_layout.setVisibility(View.GONE);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Ride Cost");
        builder.setMessage(price);
        builder.setPositiveButton(android.R.string.yes, (dialog, which) -> {
            PreferenceManager
                    .getDefaultSharedPreferences(getActivity())
                    .edit()
                    .putBoolean("DRIVING_TO_PICKUP", false)
                    .putBoolean("DRIVING_TO_DESTINATION", false)
                    .apply();
        })
                .setCancelable(false)
                .show();
    }


    @Override
    public void onRoutingCancelled() {

    }

    // An AsyncTask class for accessing the GeoCoding Web Service
    private class GeocoderTask extends AsyncTask<String, Void, List<Address>> {

        @Override
        protected List<Address> doInBackground(String... locationName) {
            // Creating an instance of Geocoder class
            Geocoder geocoder = new Geocoder(getActivity());
            List<Address> addresses = null;

            try {
                // Getting a maximum of 3 Address that matches the input text
                addresses = geocoder.getFromLocationName(locationName[0], 3);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return addresses;
        }

        @Override
        protected void onPostExecute(List<Address> addresses) {

            if (addresses == null || addresses.size() == 0) {
                Toast.makeText(getActivity(), "No Location found", Toast.LENGTH_SHORT).show();
            }

            // Clears all the existing markers on the map
            mMap.clear();

            // Adding Markers on Google Map for each matching address
            for (int i = 0; i < addresses.size(); i++) {

                Address address = (Address) addresses.get(i);

                System.out.println("svsfssfjfjf " + address);
                // Creating an instance of GeoPoint, to display in Google Map
                latLng = new LatLng(address.getLatitude(), address.getLongitude());
                longitude = latLng.longitude;
                latitude = latLng.latitude;

                System.out.println("aadtetsetse " + address.getAddressLine(0));
                markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title(address.getAddressLine(0));
                markerOptions.draggable(true);
//                mMap.addMarker(markerOptions);

                // Locate the first location
                if (i == 0)
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));

                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(latLng)      // Sets the center of the map to Mountain View
                        .zoom(17)                   // Sets the zoom
                        .bearing(90)                // Sets the orientation of the camera to east
                        .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                        .build();                   // Creates a CameraPosition from the builder
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        }
    }

    private float getBearing(LatLng begin, LatLng end) {
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

    private void startBikeAnimation(final LatLng start, final LatLng end) {

        Log.i(TAG, "startBikeAnimation called...");

        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.setDuration(3000);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {

                //LogMe.i(TAG, "Car Animation Started...");
                v = valueAnimator.getAnimatedFraction();
                lng = v * end.longitude + (1 - v)
                        * start.longitude;
                lat = v * end.latitude + (1 - v)
                        * start.latitude;

                LatLng newPos = new LatLng(lat, lng);
                carMarker.setPosition(newPos);
                carMarker.setAnchor(0.5f, 0.5f);
                carMarker.setRotation(getBearing(start, end));

                // todo : Shihab > i can delay here
                mMap.moveCamera(CameraUpdateFactory
                        .newCameraPosition
                                (new CameraPosition.Builder()
                                        .target(newPos)
                                        .zoom(15.5f)
                                        .build()));

                startPosition = carMarker.getPosition();

            }

        });
        valueAnimator.start();
    }

    private void animateCarOnMap(final List<LatLng> latLngs) {
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
                                        .zoom(15.5f)
                                        .build()));

                startPosition = carMarker.getPosition();
            }
        });
        valueAnimator.start();
    }
}
