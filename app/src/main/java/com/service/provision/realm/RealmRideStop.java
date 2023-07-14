package com.service.provision.realm;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;


public class RealmRideStop extends RealmObject {

    private int id;
    @PrimaryKey
    private String ride_stop_id;
    private double latitude;
    private double longitude;
    private String tag;
    private String ride_history_id;
    private String created_at;
    private String updated_at;

    public RealmRideStop() {

    }

    public RealmRideStop(int id, String ride_stop_id, double latitude, double longitude, String tag, String ride_history_id, String created_at, String updated_at) {
        this.id = id;
        this.ride_stop_id = ride_stop_id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.tag = tag;
        this.ride_history_id = ride_history_id;
        this.created_at = created_at;
        this.updated_at = updated_at;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRide_stop_id() {
        return ride_stop_id;
    }

    public void setRide_stop_id(String ride_stop_id) {
        this.ride_stop_id = ride_stop_id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getRide_history_id() {
        return ride_history_id;
    }

    public void setRide_history_id(String ride_history_id) {
        this.ride_history_id = ride_history_id;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }
}
