package com.service.provision.activity;

import static com.service.provision.activity.DriverFoundActivity.realmProvider;
import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.Const.*;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.convertDpToPx;
import static com.service.provision.constants.Const.myVolleyError;
import static com.service.provision.receiver.NetworkReceiver.activeActivity;

import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
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
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.makeramen.roundedimageview.RoundedImageView;
import com.service.provision.R;
import com.service.provision.materialDialog.RatingMaterialDialog;
import com.service.provision.materialDialog.RideEndedMaterialDialog;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmRideHistory;
import com.service.provision.receiver.NetworkReceiver;
import com.service.provision.util.RealmUtility;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.realm.Realm;

public class DrivingToDestinationActivity extends FragmentActivity implements OnMapReadyCallback, RoutingListener {

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
    TextView destination, time, rider_name, vehicle_registration_number, change_destination, rate_trip;
    RoundedImageView profile_pic;
    ArrayList<Polyline> polylines = new ArrayList<>();
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};
    List<LatLng> latLngs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driving_to_destination);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Places.initialize(getApplicationContext(), getResources().getString(R.string.google_maps_key));
        destination = findViewById(R.id.destination);
        time = findViewById(R.id.time);
        rider_name = findViewById(R.id.rider_name);
        vehicle_registration_number = findViewById(R.id.vehicle_registration_number);
        profile_pic = findViewById(R.id.profile_pic);
        rate_trip = findViewById(R.id.rate_trip);
        rider_name.setText(realmProvider.getFirst_name());
        vehicle_registration_number.setText(realmProvider.getVehicle_registration_number());
        destination.setText(getIntent().getStringExtra("DESTINATION"));
        SlidingUpPanelLayout layout = findViewById(R.id.slidingUp);

        rate_trip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProgressDialog mProgress = new ProgressDialog(DrivingToDestinationActivity.this);
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
                                    if(ratingMaterialDialog != null && ratingMaterialDialog.isAdded()) {

                                    } else {
                                        ratingMaterialDialog.setService_id(getIntent().getStringExtra("SERVICE_ID"));
                                        ratingMaterialDialog.setName_text(realmProvider.getProvider_name());
                                        ratingMaterialDialog.setProfilepic_text(realmProvider.getProfile_image_url());
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
                        params.put("service_id", getIntent().getStringExtra("SERVICE_ID"));
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

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Preparing map...");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.show();

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
        Routing routing = new Routing.Builder()
                .travelMode(Routing.TravelMode.DRIVING)
                .withListener(DrivingToDestinationActivity.this)
                .waypoints(new LatLng(getIntent().getDoubleExtra("DESTINATION_LATITUDE", 0.0d), getIntent().getDoubleExtra("DESTINATION_LONGITUDE", 0.0d)), new LatLng(getIntent().getDoubleExtra("PICKUP_LATITUDE", 0.0d), getIntent().getDoubleExtra("PICKUP_LONGITUDE", 0.0d)))
                .key(getResources().getString(R.string.google_maps_key))
                .build();
        routing.execute();
    }

    private void startAutocompleteActivity() {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME);
        // Start the autocomplete intent.
        Intent intent = new Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY, fields)
                .build(this);
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
        // Start marker
        MarkerOptions startOptions = new MarkerOptions();
        startOptions.position(new LatLng(getIntent().getDoubleExtra("DESTINATION_LATITUDE", 0.0d), getIntent().getDoubleExtra("DESTINATION_LONGITUDE", 0.0d)));
        startOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.car_top));
//        Marker markerStart = mMap.addMarker(startOptions);

        // End marker
        MarkerOptions endOptions = new MarkerOptions();
        endOptions.position(new LatLng(getIntent().getDoubleExtra("PICKUP_LATITUDE", 0.0d), getIntent().getDoubleExtra("PICKUP_LONGITUDE", 0.0d)));
        endOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_start));
        Marker markerEnd = mMap.addMarker(endOptions);


        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (int j = 0; j < route.size(); j++) {
            List<LatLng> points = route.get(j).getPoints();
            for (int k=0; k<points.size();k++) {
                builder.include(points.get(k));
            }
        }

        LatLngBounds bounds = builder.build();
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels - (int) convertDpToPx(getApplicationContext(), 210);
        int padding = (int) (width * 0.15); // offset from edges of the map 15% of screen

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
            latLngs = route.get(j).getPoints();
            polyOptions.addAll(latLngs);
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

//            time.setText(getTime(route.get(j).getDurationValue()));
            ride_time = route.get(j).getDurationValue() / 60;
            ride_distance = route.get(j).getDistanceValue() / 1000.0F;
        }

        ProgressDialog mProgress = new ProgressDialog(DrivingToDestinationActivity.this);
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
                            JSONObject jsonObject = new JSONObject(response);
                            Realm.init(getApplicationContext());
                            Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).executeTransaction(realm -> {
                                realm.createOrUpdateObjectFromJson(RealmRideHistory.class, jsonObject);
                            });

                            final Marker[] marker = {null};
                            Timer myTimer = new Timer();
                            int size = latLngs.size();
                            Log.d("12345qwer", "Size: " + String.valueOf(size));
                            for (int k = 1; k < size; k++) {
                                final int[] finalK = {k};
                                myTimer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        DrivingToDestinationActivity.this.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                List<LatLng> mylatlngs = new ArrayList<LatLng>();
                                                mylatlngs.add(latLngs.get(0));
                                                mylatlngs.add(latLngs.get(1));
                                                animateCarOnMap(mylatlngs);

                                                if (latLngs.size() > 0) {
                                                    latLngs.remove(0);
                                                }

                                                if (polylines.size() > 0) {
                                                    for (Polyline poly : polylines) {
                                                        poly.remove();
                                                    }
                                                }

                                                if (latLngs.size() > 0) {
                                                    polylines = new ArrayList<>();

                                                    PolylineOptions polyOptions = new PolylineOptions();
                                                    polyOptions.color(getResources().getColor(COLORS[0]));
                                                    polyOptions.width(10 + 0 * 3);

                                                    polyOptions.addAll(latLngs);
                                                    Polyline polyline = mMap.addPolyline(polyOptions);
                                                    polylines.add(polyline);
                                                }
                                                Log.d("asdfds3", String.valueOf(latLngs.size()));
                                                if (latLngs.size() == 1) {
                                                    ProgressDialog mProgress = new ProgressDialog(DrivingToDestinationActivity.this);
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
                                                                        JSONObject jsonObject = new JSONObject(response);
                                                                        Realm.init(getApplicationContext());
                                                                        Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).executeTransaction(realm -> {
                                                                            realm.createOrUpdateObjectFromJson(RealmRideHistory.class, jsonObject);
                                                                        });

                                                                        RideEndedMaterialDialog rideEndedMaterialDialog = new RideEndedMaterialDialog();
                                                                        if(rideEndedMaterialDialog != null && rideEndedMaterialDialog.isAdded()) {

                                                                        } else {
                                                                            rideEndedMaterialDialog.setService_id(getIntent().getStringExtra("SERVICE_ID"));

                                                                            try {
                                                                                rideEndedMaterialDialog.setDuration((int) ((dateTimeFormat.parse(jsonObject.getString("end_time")).getTime() - dateTimeFormat.parse(jsonObject.getString("start_time")).getTime()) / (1000 * 60)));

                                                                                rideEndedMaterialDialog.show(getFragmentManager(), "RatingMaterialDialog");
                                                                            } catch (ParseException e) {
                                                                                e.printStackTrace();
                                                                            } catch (JSONException e) {
                                                                                e.printStackTrace();
                                                                            }
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
                                                            params.put("end_time", "0");
                                                            params.put("end_long", String.valueOf(latLngs.get(0).longitude));
                                                            params.put("end_lat", String.valueOf(latLngs.get(0).latitude));
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
                                            }
                                        });
                                    }
                                }, 1000 * k);
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
                params.put("start_time", "0");
                params.put("start_long", String.valueOf(latLngs.get(0).longitude));
                params.put("start_lat", String.valueOf(latLngs.get(0).latitude));
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


    @Override
    public void onRoutingCancelled() {

    }

    // An AsyncTask class for accessing the GeoCoding Web Service
    private class GeocoderTask extends AsyncTask<String, Void, List<Address>> {

        @Override
        protected List<Address> doInBackground(String... locationName) {
            // Creating an instance of Geocoder class
            Geocoder geocoder = new Geocoder(DrivingToDestinationActivity.this);
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
                Toast.makeText(DrivingToDestinationActivity.this, "No Location found", Toast.LENGTH_SHORT).show();
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
