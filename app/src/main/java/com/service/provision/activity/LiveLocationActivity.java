package com.service.provision.activity;

import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.WS_URL;
import static com.service.provision.receiver.NetworkReceiver.activeActivity;
import static com.service.provision.util.Socket.EVENT_CLOSED;
import static com.service.provision.util.Socket.EVENT_OPEN;
import static com.service.provision.util.Socket.EVENT_RECONNECT_ATTEMPT;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.ui.IconGenerator;
import com.makeramen.roundedimageview.Corner;
import com.service.provision.R;
import com.service.provision.constants.Const;
import com.service.provision.constants.keyConst;
import com.service.provision.other.InitApplication;
import com.service.provision.receiver.NetworkReceiver;
import com.service.provision.util.LocationUpdateEvent;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.makeramen.roundedimageview.RoundedImageView;
import com.service.provision.util.Socket;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;

public class LiveLocationActivity extends FragmentActivity implements OnMapReadyCallback {

    public static double long0 = 0d;
    public static double lat0 = 0d;

    private static GoogleMap mMap;


    public String TAG = "PLaceManish";
    private int request_code = 1001;
    public static final int RC_CONFIRM_LOCATION = 1001;

    NetworkReceiver networkReceiver;

    MarkerOptions markerOptions;
    LatLng latLng;
    private static float v;
    TextView stop_sharing, nametextview;
    static RoundedImageView profileimg;
    private static Marker liveMarker;

    private static LatLng startPosition;
    private LatLng endPosition;

    public static Bitmap bitmap;

    public static boolean is_mine;

    String PUBLISHER_ID;

    static Socket socket;

    Toolbar toolbar;
    ProgressDialog progressDialog;
    boolean location_changed = false;

    private static Marker carMarker;

    public static Activity liveLocationActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_location);

        liveLocationActivity = this;

        bitmap = null;
        liveMarker = null;

        long0 = 0d;
        lat0 = 0d;

        is_mine = getIntent().getBooleanExtra("IS_MINE", false);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        toolbar = findViewById(R.id.toolbar);
        stop_sharing = findViewById(R.id.stop_sharing);
        nametextview = findViewById(R.id.nametextview);
        profileimg = findViewById(R.id.profileimg);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Preparing map...");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.show();


        if (getIntent().getBooleanExtra("LOCATION_ENDED", false)) {
            stop_sharing.setText("Live location ended.");
        } else {
            stop_sharing.setText("Stop sharing");
        }

        stop_sharing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringRequest stringRequest = null;
                ProgressDialog mProgress = new ProgressDialog(getApplicationContext());
                mProgress.setCancelable(false);
                mProgress.setIndeterminate(true);

                mProgress.setTitle("Please wait...");
                mProgress.show();

                stringRequest = new StringRequest(
                        Request.Method.PATCH,
                        keyConst.API_URL + "chats/" + getIntent().getStringExtra("CHAT_ID"),
                        response -> {
                            mProgress.dismiss();
                            if (response != null) {
                                try {
                                    JSONObject jsonObject = new JSONObject(response);
                                    /*Realm.init(getApplicationContext());
                                    Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).executeTransaction(realm -> {
                                        realm.createOrUpdateObjectFromJson(RealmChat.class, response);
                                        realm.where(RealmTopic.class).equalTo("topic", topic).findAll().deleteAllFromRealm();
                                        if (realm.where(RealmTopic.class).findAll().size() < 1) {
                                            mFusedLocationClient.removeLocationUpdates(locationCallback);
                                        }
                                    });*/
                                    stop_sharing.setText("Live location ended.");
                                    Toast.makeText(getApplicationContext(), "Location sharing successfully stopped.", Toast.LENGTH_SHORT).show();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        error -> {
                            mProgress.dismiss();
                            error.printStackTrace();
                            Const.myVolleyError(getApplicationContext(), error);
                            Log.d("Cyrilll", error.toString());
                        }
                ) {
                    @Override
                    public Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> params = new HashMap<>();
                        params.put("tag", "live_location_ended");
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

        nametextview.setText(getIntent().getStringExtra("NAME"));
        String PROFILE_IMAGE_URL = getIntent().getStringExtra("PROFILE_IMAGE_URL");
        if (PROFILE_IMAGE_URL != null && !PROFILE_IMAGE_URL.equals("")) {
            Glide.with(getApplicationContext())
                    .asBitmap()
                    .load(PROFILE_IMAGE_URL)
                    .apply(new RequestOptions().centerCrop().placeholder(R.drawable.avatar))
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                            bitmap = resource;
                            profileimg.setImageBitmap(bitmap);

                            if (liveMarker != null) {
                                liveMarker.remove();
                            }
                            addMarkerToMap();
                        }
                    });
        }

        networkReceiver = new NetworkReceiver();

        long0 = getIntent().getDoubleExtra("LONGITUDE", 0d);
        lat0 = getIntent().getDoubleExtra("LATITUDE", 0d);

        PUBLISHER_ID = getIntent().getStringExtra("PUBLISHER_ID");

        initSocket();
    }


    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
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

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                long0 = getIntent().getDoubleExtra("LONGITUDE", 0d);
                lat0 = getIntent().getDoubleExtra("LATITUDE", 0d);

                /*List<LatLng> mylatlngs = new ArrayList<>();
                mylatlngs.add(new LatLng(lat0, long0));
                mylatlngs.add(new LatLng(lat0, long0));
                animateMarkerOnMap(mylatlngs);*/

                latLng = new LatLng(lat0, long0);

                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(latLng)      // Sets the center of the map to Mountain View
                        .zoom(17)                   // Sets the zoom
                        .bearing(0)                // Sets the orientation of the camera to east
                        .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                        .build();                   // Creates a CameraPosition from the builder
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(LocationUpdateEvent event) {
        //handle updates here

        Log.d("asdffds0967", "Location update received");
        if (is_mine) {
            List<LatLng> mylatlngs = new ArrayList<>();
            mylatlngs.add(new LatLng(lat0, long0));
            mylatlngs.add(new LatLng(event.getLocation().latitude, event.getLocation().longitude));


            Location location0 = new Location("");//provider name is unnecessary
            location0.setLatitude(lat0);//your coords of course
            location0.setLongitude(long0);

            Location location1 = new Location("");//provider name is unnecessary
            location1.setLatitude(event.getLocation().latitude);//your coords of course
            location1.setLongitude(event.getLocation().longitude);

            float distance = location0.distanceTo(location1);
            Log.d("Locations:distance", Float.toString(distance));
            if (distance > 0.0F) {
                animateMarkerOnMap(mylatlngs);
                lat0 = event.getLocation().latitude;
                long0 = event.getLocation().longitude;
            }
        }
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
                Log.d("mywebsocketLL", "Connected on " + PUBLISHER_ID);

                socket.join("location:" + PUBLISHER_ID);

                socket.onEventResponse("location:" + PUBLISHER_ID, new Socket.OnEventResponseListener() {
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
                                    Log.d("mywebsocketLL", jsonResponse.toString());
                                    Realm.init(liveLocationActivity);
                                    JSONObject finalJsonResponse = jsonResponse;

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
                                    Log.d("Locations:distance2", String.valueOf(distance));
                                    if (distance > 0.0F) {
                                        animateMarkerOnMap(mylatlngs);
                                        lat0 = latitude;
                                        long0 = longitude;
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
                Log.d("mywebsocketLL", "reconnecting");
            }
        });
        socket.onEvent(EVENT_CLOSED, new Socket.OnEventListener() {
            @Override
            public void onMessage(String event) {
                Log.d("mywebsocketLL", "connection closed");
            }
        });
    }

    private static void addMarkerToMap() {
        RoundedImageView roundedImageView = new RoundedImageView(liveLocationActivity);
        roundedImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        roundedImageView.setLayoutParams(new ViewGroup.LayoutParams(150, 150));
        roundedImageView.setCornerRadius(Corner.TOP_LEFT, 90);
        roundedImageView.setCornerRadius(Corner.TOP_RIGHT, 90);
        roundedImageView.setCornerRadius(Corner.BOTTOM_LEFT, 90);
        roundedImageView.setCornerRadius(Corner.BOTTOM_RIGHT, 90);
        roundedImageView.setOval(true);
        roundedImageView.mutateBackground(true);
        IconGenerator mIconGenerator = new IconGenerator(liveLocationActivity);
        mIconGenerator.setContentView(roundedImageView);
        roundedImageView.setImageBitmap(bitmap);
        Bitmap iconBitmap = mIconGenerator.makeIcon();

        liveMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(lat0, long0))
                .flat(true)
                .icon(BitmapDescriptorFactory.fromBitmap(iconBitmap)));
    }

    public static void animateMarkerOnMap(final List<LatLng> latLngs) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng latLng : latLngs) {
            builder.include(latLng);
        }
        LatLngBounds bounds = builder.build();
        CameraUpdate mCameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 2);
        mMap.animateCamera(mCameraUpdate);
        if (liveMarker == null) {
            addMarkerToMap();
        }
        liveMarker.setPosition(latLngs.get(0));
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
                liveMarker.setPosition(newPos);
                liveMarker.setAnchor(0.5f, 0.5f);
//                liveMarker.setRotation(getBearing(latLngs.get(0), newPos));
                mMap.moveCamera(CameraUpdateFactory
                        .newCameraPosition
                                (new CameraPosition.Builder()
                                        .target(newPos)
                                        .zoom(17.5f)
                                        .build()));

                startPosition = liveMarker.getPosition();
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
}
