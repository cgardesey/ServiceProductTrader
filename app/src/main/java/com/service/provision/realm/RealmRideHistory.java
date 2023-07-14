package com.service.provision.realm;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;


public class RealmRideHistory extends RealmObject {

    private int id;
    @PrimaryKey
    private String ride_history_id;
    double pickup_latitude;
    double pickup_longitude;
    double destination_latitude;
    double destination_longitude;
    String pickup_address;
    String destination_address;
    private String start_time;
    private String end_time;
    private int one_star;
    private int two_star;
    private int three_star;
    private int four_star;
    private int five_star;
    private String review;
    private int ride_cancelled;
    private String service_id;
    private String customer_id;
    private String created_at;
    private String updated_at;

    public RealmRideHistory() {

    }

    public RealmRideHistory(int id, String ride_history_id, double pickup_latitude, double pickup_longitude, double destination_latitude, double destination_longitude, String pickup_address, String destination_address, String start_time, String end_time, int one_star, int two_star, int three_star, int four_star, int five_star, String review, int ride_cancelled, String service_id, String customer_id, String created_at, String updated_at) {
        this.id = id;
        this.ride_history_id = ride_history_id;
        this.pickup_latitude = pickup_latitude;
        this.pickup_longitude = pickup_longitude;
        this.destination_latitude = destination_latitude;
        this.destination_longitude = destination_longitude;
        this.pickup_address = pickup_address;
        this.destination_address = destination_address;
        this.start_time = start_time;
        this.end_time = end_time;
        this.one_star = one_star;
        this.two_star = two_star;
        this.three_star = three_star;
        this.four_star = four_star;
        this.five_star = five_star;
        this.review = review;
        this.ride_cancelled = ride_cancelled;
        this.service_id = service_id;
        this.customer_id = customer_id;
        this.created_at = created_at;
        this.updated_at = updated_at;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRide_history_id() {
        return ride_history_id;
    }

    public void setRide_history_id(String ride_history_id) {
        this.ride_history_id = ride_history_id;
    }

    public double getPickup_latitude() {
        return pickup_latitude;
    }

    public void setPickup_latitude(double pickup_latitude) {
        this.pickup_latitude = pickup_latitude;
    }

    public double getPickup_longitude() {
        return pickup_longitude;
    }

    public void setPickup_longitude(double pickup_longitude) {
        this.pickup_longitude = pickup_longitude;
    }

    public double getDestination_latitude() {
        return destination_latitude;
    }

    public void setDestination_latitude(double destination_latitude) {
        this.destination_latitude = destination_latitude;
    }

    public double getDestination_longitude() {
        return destination_longitude;
    }

    public void setDestination_longitude(double destination_longitude) {
        this.destination_longitude = destination_longitude;
    }

    public String getPickup_address() {
        return pickup_address;
    }

    public void setPickup_address(String pickup_address) {
        this.pickup_address = pickup_address;
    }

    public String getDestination_address() {
        return destination_address;
    }

    public void setDestination_address(String destination_address) {
        this.destination_address = destination_address;
    }

    public String getStart_time() {
        return start_time;
    }

    public void setStart_time(String start_time) {
        this.start_time = start_time;
    }

    public String getEnd_time() {
        return end_time;
    }

    public void setEnd_time(String end_time) {
        this.end_time = end_time;
    }

    public int getOne_star() {
        return one_star;
    }

    public void setOne_star(int one_star) {
        this.one_star = one_star;
    }

    public int getTwo_star() {
        return two_star;
    }

    public void setTwo_star(int two_star) {
        this.two_star = two_star;
    }

    public int getThree_star() {
        return three_star;
    }

    public void setThree_star(int three_star) {
        this.three_star = three_star;
    }

    public int getFour_star() {
        return four_star;
    }

    public void setFour_star(int four_star) {
        this.four_star = four_star;
    }

    public int getFive_star() {
        return five_star;
    }

    public void setFive_star(int five_star) {
        this.five_star = five_star;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public int getRide_cancelled() {
        return ride_cancelled;
    }

    public void setRide_cancelled(int ride_cancelled) {
        this.ride_cancelled = ride_cancelled;
    }

    public String getService_id() {
        return service_id;
    }

    public void setService_id(String service_id) {
        this.service_id = service_id;
    }

    public String getCustomer_id() {
        return customer_id;
    }

    public void setCustomer_id(String customer_id) {
        this.customer_id = customer_id;
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
