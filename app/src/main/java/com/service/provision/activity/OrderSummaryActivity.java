package com.service.provision.activity;

import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.base_fare;
import static com.service.provision.constants.Const.cost_per_km;
import static com.service.provision.constants.Const.cost_per_min;
import static com.service.provision.constants.Const.myVolleyError;
import static com.service.provision.constants.Const.other_fee;
import static com.service.provision.constants.Const.surge_boost_multiplier;
import static com.service.provision.receiver.NetworkReceiver.activeActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.model.LatLng;
import com.greysonparrelli.permiso.PermisoActivity;
import com.service.provision.R;
import com.service.provision.materialDialog.ChoosePaymentMethodMaterialDialog;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmCart;
import com.service.provision.realm.RealmCustomer;
import com.service.provision.realm.RealmProvider;
import com.service.provision.receiver.NetworkReceiver;
import com.service.provision.util.RealmUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;


public class OrderSummaryActivity extends PermisoActivity implements RoutingListener {

    NetworkReceiver networkReceiver;
    Button pay;
    TextView name, location, contact, summary, subtotal, totalfee, shippingfee, edit;
    public static Activity orderSummaryActivity;
    public static RealmProvider realmProvider;

    ProgressDialog progressDialog;

    int ride_time;

    Float ride_distance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_summary);

        orderSummaryActivity = this;

        name = findViewById(R.id.name);
        edit = findViewById(R.id.edit);
        location = findViewById(R.id.location);
        contact = findViewById(R.id.contact);
        summary = findViewById(R.id.summary);
        subtotal = findViewById(R.id.subtotal);
        totalfee = findViewById(R.id.totalfee);
        shippingfee = findViewById(R.id.shippingfee);

        pay = findViewById(R.id.pay);

        pay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChoosePaymentMethodMaterialDialog choosePaymentMethodMaterialDialog = new ChoosePaymentMethodMaterialDialog();
                if (choosePaymentMethodMaterialDialog != null && choosePaymentMethodMaterialDialog.isAdded()) {

                } else {
                    choosePaymentMethodMaterialDialog.setAmount(String.valueOf(getIntent().getFloatExtra("SHIPPING_FEE", 0.00F) + getIntent().getFloatExtra("SUB_TOTAL", 0.00F)));
                    choosePaymentMethodMaterialDialog.setCart_id(getIntent().getStringExtra("CART_ID"));
                    choosePaymentMethodMaterialDialog.show(getSupportFragmentManager(), "choosePaymentMethodMaterialDialog");
                    choosePaymentMethodMaterialDialog.setCancelable(true);
                }
            }
        });

        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), CustomerAccountActivity.class));
            }
        });

        summary.setText("Order Summary (" + getIntent().getIntExtra("ITEM_COUNT", 0) + ")");
        subtotal.setText("GHC" + String.format("%.2f", getIntent().getFloatExtra("SUB_TOTAL", 0.00F)));
        networkReceiver = new NetworkReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();

        activeActivity = this;
        registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.show();

        final RealmCustomer[] realmCustomer = new RealmCustomer[1];
        Realm.init(getApplicationContext());
        Realm.getInstance(RealmUtility.getDefaultConfig(orderSummaryActivity)).executeTransaction(realm -> {
            realmCustomer[0] = realm.where(RealmCustomer.class).findFirst();
            name.setText(realmCustomer[0].getName());
            location.setText(realmCustomer[0].getStreet_address());
            String contact = realmCustomer[0].getPrimary_contact();
            String auxiliary_contact = realmCustomer[0].getAuxiliary_contact();
            if (auxiliary_contact != null && !auxiliary_contact.equals("")) {
                contact += " / " + auxiliary_contact;
            }
            this.contact.setText(contact);
        });


        Routing routing = new Routing.Builder()
                .travelMode(Routing.TravelMode.DRIVING)
                .withListener(this)
                .waypoints(new LatLng(realmCustomer[0].getLatitude(), realmCustomer[0].getLongitude()), new LatLng(realmProvider.getLatitude(), realmProvider.getLongitude()))
                .key(getResources().getString(R.string.google_maps_key))
                .build();
        routing.execute();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(networkReceiver);
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
        Float shiping_fee = Float.valueOf(Math.round(base_fare + ((cost_per_min * ride_time) + (cost_per_km * ride_distance) * surge_boost_multiplier) + other_fee));
        StringRequest stringRequest = new StringRequest(
                Request.Method.PATCH,
                API_URL + "carts/" + getIntent().getStringExtra("CART_ID"),
                response -> {
                    if (response != null) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            Realm.init(getApplicationContext());
                            Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    realm.createOrUpdateObjectFromJson(RealmCart.class, jsonObject);
                                }
                            });
                            shippingfee.setText("GHC" + String.format("%.2f", shiping_fee));
                            totalfee.setText("GHC" + String.format("%.2f", shiping_fee + getIntent().getFloatExtra("SUB_TOTAL", 0.00F)));
                            progressDialog.dismiss();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                error -> {
                    error.printStackTrace();
                    Log.d("Cyrilll", error.toString());
                    progressDialog.dismiss();
                    myVolleyError(getApplicationContext(), error);
                }
        ) {
            @Override
            public Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("shipping_fee", String.valueOf(shiping_fee));
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
}
