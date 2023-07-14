package com.service.provision.activity;

import static com.service.provision.constants.Const.cancelNotification;
import static com.service.provision.constants.Const.clearAppData;
import static com.service.provision.receiver.NetworkReceiver.activeActivity;
import static com.service.provision.service.LocationUpdateService.*;
import static com.service.provision.service.LocationUpdateService.PUBLISHER_ID;
import static com.service.provision.service.LocationUpdateService.locationCallback;
import static com.service.provision.service.LocationUpdateService.mFusedLocationClient;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.service.provision.R;
import com.service.provision.receiver.NetworkReceiver;
import com.service.provision.service.LocationUpdateService;

import uk.co.senab.photoview.PhotoViewAttacher;


public class StopSharingActivity extends AppCompatActivity {
    NetworkReceiver networkReceiver;
    public static Bitmap idPicBitmap;

    private ImageView photoImageView;

    Button stop_sharing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stop_sharing);

        stop_sharing = findViewById(R.id.stop_sharing);

        stop_sharing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(StopSharingActivity.this);
                builder.setMessage("Stop location sharing");
                builder.setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    mFusedLocationClient.removeLocationUpdates(locationCallback);

                    if (socket != null) {
                        socket.leave("location:" + PUBLISHER_ID);
                        socket.clearListeners();
                        socket.close();
                        socket.terminate();
                        socket = null;
                    }
                    ContextCompat.startForegroundService(getApplicationContext(), new Intent(getApplicationContext(), LocationUpdateService.class)
                            .putExtra("STOP", true)
                    );

                    finish();
                });
                builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                   dialog.dismiss();
                });
                builder
                        .setCancelable(false)
                        .show();
            }
        });
    }
}
