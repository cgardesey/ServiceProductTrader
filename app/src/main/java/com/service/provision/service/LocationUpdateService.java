package com.service.provision.service;

import static com.service.provision.constants.keyConst.WS_URL;
import static com.service.provision.constants.Const.isBetterLocation;
import static com.service.provision.util.Socket.EVENT_CLOSED;
import static com.service.provision.util.Socket.EVENT_OPEN;
import static com.service.provision.util.Socket.EVENT_RECONNECT_ATTEMPT;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.service.provision.R;
import com.service.provision.activity.StopSharingActivity;
import com.service.provision.pojo.LocationDTO;
import com.service.provision.util.LocationUpdateEvent;
import com.service.provision.util.Socket;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Starts location updates on background and publish LocationUpdateEvent upon
 * each new location result.
 */
public class LocationUpdateService extends Service {

    //region data
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    public static FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;

    public static final String CHANNEL_ID = "channel_id";
    public static final int LOCATION_SERVICE_NOTIF_ID = 876898;
    private final String CHANNEL_NAME = "NOTIFICATION_CHANNEL";
    private int SERVICE_LOCATION_REQUEST_CODE = 1901;

    public static String PUBLISHER_ID = null;
    private static String ROLE = null;
    public static Context context;


    public static Location location1;

    public static Socket socket;
    //endregion

    //onCreate
    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        initData();
        initSocket();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFusedLocationClient.removeLocationUpdates(locationCallback);

        if (socket != null) {
            socket.leave("location:" + PUBLISHER_ID);
            socket.clearListeners();
            socket.close();
            socket.terminate();
            socket = null;
        }
    }

    //Location Callback
    public static LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            Location currentLocation = locationResult.getLastLocation();
            Log.d("Locations", PUBLISHER_ID + currentLocation.getLatitude() + "," + currentLocation.getLongitude());
            //Share/Publish Location
            LocationDTO location = new LocationDTO();
            location.latitude = currentLocation.getLatitude();
            location.longitude = currentLocation.getLongitude();
            location.speed = currentLocation.getSpeed();

            EventBus.getDefault().post(new LocationUpdateEvent(location));

            if (socket.getState() == Socket.State.OPEN) {
                if (socket != null) {
                    Location location2 = new Location("");
                    location2.setLatitude(location.latitude);//your coords of course
                    location2.setLongitude(location.longitude);

                    if (isBetterLocation(location2, location1)) {
                        Log.d("Locations", "isBetterLocation");
                        location1 = location2;

                        PreferenceManager
                                .getDefaultSharedPreferences(context)
                                .edit()
                                .putString("CURRENT_LAT", String.valueOf(location.latitude))
                                .putString("CURRENT_LONG", String.valueOf(location.longitude))
                                .apply();

                        JSONObject jsonData = null;
                        try {
                            jsonData = new JSONObject()
                                    .put("publisher_id", PUBLISHER_ID)
                                    .put("role", ROLE)
                                    .put("latitude", location.latitude)
                                    .put("longitude", location.longitude);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        socket.send("location:" + PUBLISHER_ID, jsonData.toString());
                    }
                }
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && intent.hasExtra("STOP")) {
            stopForeground(true);
            stopSelf();
        }
        else {
            location1 = null;

            if (intent != null) {
                PUBLISHER_ID = intent.getStringExtra("PUBLISHER_ID");
                ROLE = intent.getStringExtra("ROLE");
            }

            if (socket != null) {
                socket.clearListeners();
            }

            socket.onEvent(EVENT_OPEN, new Socket.OnEventListener() {
                @Override
                public void onMessage(String event) {
                    Log.d("mywebsocketLUS1", "Connected");

                    if (PUBLISHER_ID != null && !PUBLISHER_ID.equals("")) {
                        socket.join("location:" + PUBLISHER_ID);
                        Log.d("mywebsocketLUS1", "subscribed to topic");
                    }
                }
            });

            socket.onEvent(EVENT_RECONNECT_ATTEMPT, new Socket.OnEventListener() {
                @Override
                public void onMessage(String event) {
                    Log.d("mywebsocketLUS1", "reconnecting");
                }
            });
            socket.onEvent(EVENT_CLOSED, new Socket.OnEventListener() {
                @Override
                public void onMessage(String event) {
                    Log.d("mywebsocketLUS1", "connection closed");
                }
            });

            prepareForegroundNotification();
            startLocationUpdates();
        }

        return START_STICKY;
    }

    private void startLocationUpdates() {
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
        mFusedLocationClient.requestLocationUpdates(this.locationRequest,
                this.locationCallback, Looper.myLooper());
    }

    private void prepareForegroundNotification() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
        Intent notificationIntent = new Intent(this, StopSharingActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                SERVICE_LOCATION_REQUEST_CODE,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SuperFix live location is active")
                .setSmallIcon(R.drawable.live_red)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(LOCATION_SERVICE_NOTIF_ID, notification);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initData() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setFastestInterval (5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
    }

    public void initSocket() {
        socket = Socket
                .Builder.with(WS_URL)
                .build();
        socket.connect();
    }
}
