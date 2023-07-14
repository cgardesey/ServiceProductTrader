package com.service.provision.realm;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by 2CLearning on 12/16/2017.
 */

public class RealmCart extends RealmObject {

    private int id;
    @PrimaryKey
    private String cart_id;
    private String order_id;
    private String provider_id;
    private String customer_id;
    private int delivered;
    private String created_at;
    private String updated_at;

    private String status;
    private double shipping_fee;

    private double provider_longitude;
    private double provider_latitude;
    private String provider_name;
    private int verified;
    private String provider_availability;
    private String provider_image_url;
    private String provider_title;
    private String provider_first_name;
    private String provider_last_name;
    private String provider_other_name;
    private double customer_longitude;
    private double customer_latitude;
    private String customer_name;
    private String customer_image_url;
    private int item_count;

    public RealmCart() {

    }

    public RealmCart(int id, String cart_id, String order_id, String provider_id, String customer_id, int delivered, String created_at, String updated_at, String status, double shipping_fee, double provider_longitude, double provider_latitude, String provider_name, int verified, String provider_availability, String provider_image_url, String provider_title, String provider_first_name, String provider_last_name, String provider_other_name, double customer_longitude, double customer_latitude, String customer_name, String customer_image_url, int item_count) {
        this.id = id;
        this.cart_id = cart_id;
        this.order_id = order_id;
        this.provider_id = provider_id;
        this.customer_id = customer_id;
        this.delivered = delivered;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.status = status;
        this.shipping_fee = shipping_fee;
        this.provider_longitude = provider_longitude;
        this.provider_latitude = provider_latitude;
        this.provider_name = provider_name;
        this.verified = verified;
        this.provider_availability = provider_availability;
        this.provider_image_url = provider_image_url;
        this.provider_title = provider_title;
        this.provider_first_name = provider_first_name;
        this.provider_last_name = provider_last_name;
        this.provider_other_name = provider_other_name;
        this.customer_longitude = customer_longitude;
        this.customer_latitude = customer_latitude;
        this.customer_name = customer_name;
        this.customer_image_url = customer_image_url;
        this.item_count = item_count;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCart_id() {
        return cart_id;
    }

    public void setCart_id(String cart_id) {
        this.cart_id = cart_id;
    }

    public String getOrder_id() {
        return order_id;
    }

    public void setOrder_id(String order_id) {
        this.order_id = order_id;
    }

    public String getProvider_id() {
        return provider_id;
    }

    public void setProvider_id(String provider_id) {
        this.provider_id = provider_id;
    }

    public String getCustomer_id() {
        return customer_id;
    }

    public void setCustomer_id(String customer_id) {
        this.customer_id = customer_id;
    }

    public int getDelivered() {
        return delivered;
    }

    public void setDelivered(int delivered) {
        this.delivered = delivered;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getShipping_fee() {
        return shipping_fee;
    }

    public void setShipping_fee(double shipping_fee) {
        this.shipping_fee = shipping_fee;
    }

    public double getProvider_longitude() {
        return provider_longitude;
    }

    public void setProvider_longitude(double provider_longitude) {
        this.provider_longitude = provider_longitude;
    }

    public double getProvider_latitude() {
        return provider_latitude;
    }

    public void setProvider_latitude(double provider_latitude) {
        this.provider_latitude = provider_latitude;
    }

    public String getProvider_name() {
        return provider_name;
    }

    public void setProvider_name(String provider_name) {
        this.provider_name = provider_name;
    }

    public int getVerified() {
        return verified;
    }

    public void setVerified(int verified) {
        this.verified = verified;
    }

    public String getProvider_availability() {
        return provider_availability;
    }

    public void setProvider_availability(String provider_availability) {
        this.provider_availability = provider_availability;
    }

    public String getProvider_image_url() {
        return provider_image_url;
    }

    public void setProvider_image_url(String provider_image_url) {
        this.provider_image_url = provider_image_url;
    }

    public String getProvider_title() {
        return provider_title;
    }

    public void setProvider_title(String provider_title) {
        this.provider_title = provider_title;
    }

    public String getProvider_first_name() {
        return provider_first_name;
    }

    public void setProvider_first_name(String provider_first_name) {
        this.provider_first_name = provider_first_name;
    }

    public String getProvider_last_name() {
        return provider_last_name;
    }

    public void setProvider_last_name(String provider_last_name) {
        this.provider_last_name = provider_last_name;
    }

    public String getProvider_other_name() {
        return provider_other_name;
    }

    public void setProvider_other_name(String provider_other_name) {
        this.provider_other_name = provider_other_name;
    }

    public double getCustomer_longitude() {
        return customer_longitude;
    }

    public void setCustomer_longitude(double customer_longitude) {
        this.customer_longitude = customer_longitude;
    }

    public double getCustomer_latitude() {
        return customer_latitude;
    }

    public void setCustomer_latitude(double customer_latitude) {
        this.customer_latitude = customer_latitude;
    }

    public String getCustomer_name() {
        return customer_name;
    }

    public void setCustomer_name(String customer_name) {
        this.customer_name = customer_name;
    }

    public String getCustomer_image_url() {
        return customer_image_url;
    }

    public void setCustomer_image_url(String customer_image_url) {
        this.customer_image_url = customer_image_url;
    }

    public int getItem_count() {
        return item_count;
    }

    public void setItem_count(int item_count) {
        this.item_count = item_count;
    }
}
