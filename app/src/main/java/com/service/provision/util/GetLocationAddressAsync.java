package com.service.provision.util;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.service.provision.activity.MapsActivity;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

public class GetLocationAddressAsync extends AsyncTask<String, Void, List<Address>> {

    Activity activity;
    OnTaskCompletedInterface onTaskCompletedInterface;
    OnTaskCancelledInterface onTaskCancelledInterface;
    Double latitude;
    Double longitude;

    public GetLocationAddressAsync(Activity activity, Double latitude, Double longitude, OnTaskCompletedInterface onTaskCompletedInterface, OnTaskCancelledInterface onTaskCancelledInterface) {
        this.onTaskCompletedInterface = onTaskCompletedInterface;
        this.onTaskCancelledInterface = onTaskCancelledInterface;
        this.activity = activity;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    protected List<Address> doInBackground(String... locationName) {
        // Creating an instance of Geocoder class
        Geocoder geocoder = new Geocoder(activity);
        List<Address> addresses = null;

        try {
            // Getting a maximum of 3 Address that matches the input text
            addresses = geocoder.getFromLocation(latitude, longitude, 3);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return addresses;
    }

    @Override
    protected void onPostExecute(List<Address> addresses) {
        super.onPostExecute(addresses);
        onTaskCompletedInterface.onTaskCompleted(addresses);
    }


    @Override
    protected void onCancelled() {
        super.onCancelled();
        onTaskCancelledInterface.onTaskCancelled();
    }

    public interface OnTaskCompletedInterface {
        void onTaskCompleted(List<Address> addresses);
    }

    public interface OnTaskCancelledInterface {
        void onTaskCancelled();
    }
}
