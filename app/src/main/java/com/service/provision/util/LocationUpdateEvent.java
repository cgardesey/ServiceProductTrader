package com.service.provision.util;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

import android.location.Location;

import com.service.provision.pojo.LocationDTO;


public class LocationUpdateEvent {
    private LocationDTO location;

    public LocationUpdateEvent(LocationDTO locationUpdate) {
        this.location = locationUpdate;
    }

    public LocationDTO getLocation() {
        return location;
    }

    public void setLocation(LocationDTO location) {
        this.location = location;
    }
}