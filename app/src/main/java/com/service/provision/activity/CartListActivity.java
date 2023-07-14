package com.service.provision.activity;

import static com.service.provision.activity.OrderSummaryActivity.realmProvider;
import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.myVolleyError;
import static com.service.provision.receiver.NetworkReceiver.activeActivity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.toolbox.StringRequest;
import com.greysonparrelli.permiso.Permiso;
import com.service.provision.R;
import com.service.provision.adapter.CartListAdapter;
import com.service.provision.materialDialog.ChooseServiceContactMethodMaterialDialog;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmCart;
import com.service.provision.realm.RealmCartProduct;
import com.service.provision.realm.RealmCustomer;
import com.service.provision.realm.RealmProvider;
import com.service.provision.receiver.NetworkReceiver;
import com.service.provision.util.RealmUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;

public class CartListActivity extends AppCompatActivity {
    RecyclerView recyclerview;
    CartListAdapter cartListAdapter;
    ArrayList<RealmCart> cartArrayList = new ArrayList<>(), newCart = new ArrayList<>();
    TextView no_data;
    NetworkReceiver networkReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart_list);
        recyclerview = findViewById(R.id.recyclerview);
        no_data = findViewById(R.id.no_data);
        cartListAdapter = new CartListAdapter(new CartListAdapter.CartAdapterInterface() {
            @Override
            public void onViewClick(ArrayList<RealmCart> realmCarts, int position, CartListAdapter.ViewHolder holder) {
                RealmCart realmCart = realmCarts.get(position);
                String cart_id = realmCart.getCart_id();
                ProgressDialog dialog = new ProgressDialog(CartListActivity.this);
                dialog.setMessage("Please wait...");
                dialog.setCancelable(false);
                dialog.setIndeterminate(true);
                dialog.show();

                Realm.init(CartListActivity.this);
                String customer_id = Realm.getInstance(RealmUtility.getDefaultConfig(CartListActivity.this)).where(RealmCustomer.class).findFirst().getCustomer_id();


                StringRequest stringRequest = new StringRequest(
                        com.android.volley.Request.Method.POST,
                        API_URL + "scoped-cart-products",
                        response -> {
                            if (response != null) {
                                dialog.dismiss();
                                try {
                                    final float[] sub_total = {0.00F};
                                    JSONArray jsonArray = new JSONArray(response);
                                    Realm.init(getApplicationContext());
                                    Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).executeTransaction(realm -> {
                                        realm.where(RealmCartProduct.class).findAll().deleteAllFromRealm();
                                        realm.createOrUpdateAllFromJson(RealmCartProduct.class, jsonArray);

                                        for (int i = 0; i < jsonArray.length(); i++) {
                                            try {
                                                sub_total[0] += (float)jsonArray.getJSONObject(i).getDouble("price");
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });

                                    startActivity(
                                            new Intent(getApplicationContext(), CartItemsActivity.class)
                                                    .putExtra("IS_INVOICE", realmCart.getStatus() != null && realmCart.getStatus().equals("SUCCESS"))
                                                    .putExtra("INVOICE_SUB_TOTAL", sub_total[0])
                                                    .putExtra("SHIPPING_FEE", (float)realmCart.getShipping_fee())
                                                    .putExtra("CART_ID", realmCart.getCart_id())
                                    );
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        error -> {
                            error.printStackTrace();
                            myVolleyError(getApplicationContext(), error);
                            dialog.dismiss();
                            Log.d("Cyrilll", error.toString());
                        }
                ) {
                    @Override
                    public Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> params = new HashMap<>();
                        params.put("cart_id", cart_id);
                        return params;
                    }

                    /** Passing some request headers* */
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
            public void onContactClick(ArrayList<RealmCart> realmCarts, int position, CartListAdapter.ViewHolder holder) {
                RealmCart realmCart = realmCarts.get(position);
                ChooseServiceContactMethodMaterialDialog chooseServiceContactMethodMaterialDialog = new ChooseServiceContactMethodMaterialDialog();
                if (chooseServiceContactMethodMaterialDialog != null && chooseServiceContactMethodMaterialDialog.isAdded()) {

                } else {
                    chooseServiceContactMethodMaterialDialog.setProvider_id(realmCart.getProvider_id());
                    chooseServiceContactMethodMaterialDialog.setCustomer_id(realmCart.getCustomer_id());
                    chooseServiceContactMethodMaterialDialog.show(getSupportFragmentManager(), "chooseContactMethodMaterialDialog");
                    chooseServiceContactMethodMaterialDialog.setCancelable(true);
                }
            }

            @Override
            public void onOrderClick(ArrayList<RealmCart> realmCarts, int position, CartListAdapter.ViewHolder holder) {
                RealmCart realmCart = realmCarts.get(position);

                String cart_id = realmCart.getCart_id();
                ProgressDialog dialog = new ProgressDialog(CartListActivity.this);
                dialog.setMessage("Please wait...");
                dialog.setCancelable(false);
                dialog.setIndeterminate(true);
                dialog.show();

                StringRequest stringRequest = new StringRequest(
                        com.android.volley.Request.Method.POST,
                        API_URL + "cart-total",
                        response -> {
                            if (response != null) {
                                dialog.dismiss();
                                try {
                                    JSONObject jsonObject = new JSONObject(response);
                                    Realm.init(getApplicationContext());
                                    Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).executeTransaction(realm -> {
                                        try {
                                            realmProvider = realm.createOrUpdateObjectFromJson(RealmProvider.class, jsonObject.getJSONObject("provider"));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    });
                                    startActivity(new Intent(getApplicationContext(), OrderSummaryActivity.class)
                                            .putExtra("ITEM_COUNT", realmCart.getItem_count())
                                            .putExtra("SUB_TOTAL", (float) jsonObject.getDouble("cart_total"))

                                            .putExtra("CART_ID", realmCart.getCart_id())
                                    );

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        error -> {
                            error.printStackTrace();
                            myVolleyError(getApplicationContext(), error);
                            dialog.dismiss();
                            Log.d("Cyrilll", error.toString());
                        }
                ) {
                    @Override
                    public Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> params = new HashMap<>();
                        params.put("cart_id", cart_id);
                        return params;
                    }

                    /** Passing some request headers* */
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
            public void onDeliveryClick(ArrayList<RealmCart> realmCarts, int position, CartListAdapter.ViewHolder holder) {
                Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
                                                             @Override
                                                             public void onPermissionResult(Permiso.ResultSet resultSet) {
                                                                 if (resultSet.isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION) && resultSet.isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                                                                     RealmCart realmCart = realmCarts.get(position);
                                                                     Uri location = Uri.parse("geo:" + realmCart.getProvider_latitude() + "," + realmCart.getProvider_longitude() + "?z=14"); // z param is zoom level
                                                                     Intent mapIntent = new Intent(Intent.ACTION_VIEW, location);
                                                                     startActivity(mapIntent);
                                                                 }
                                                             }

                                                             @Override
                                                             public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                                                                 Permiso.getInstance().showRationaleInDialog(getApplicationContext().getString(R.string.permissions), getApplicationContext().getString(R.string.this_permission_is_mandatory_pls_allow_access), null, callback);
                                                             }
                                                         },
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION);
            }

            @Override
            public void onMarkAsDeliveredClick(ArrayList<RealmCart> realmCarts, int position, CartListAdapter.ViewHolder holder) {

            }
        }, CartListActivity.this, cartArrayList);
        recyclerview.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerview.setNestedScrollingEnabled(false);
        recyclerview.setItemAnimator(new DefaultItemAnimator());
        recyclerview.setAdapter(cartListAdapter);

        networkReceiver = new NetworkReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        activeActivity = this;
        registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        populateCart(getApplicationContext());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(networkReceiver);
    }

    void populateCart(final Context context) {
        Realm.init(context);
        Realm.getInstance(RealmUtility.getDefaultConfig(context)).executeTransaction(realm -> {
            RealmResults<RealmCart> results;
            results = realm.where(RealmCart.class)
                    .notEqualTo("status", "SUCCESS")
                    .findAll();
            newCart.clear();
            if (results.size() > 0) {
                no_data.setVisibility(View.GONE);
                recyclerview.setVisibility(View.VISIBLE);
            } else {
                no_data.setVisibility(View.VISIBLE);
                recyclerview.setVisibility(View.GONE);
            }
            for (RealmCart realmCart : results) {
                newCart.add(realmCart);
            }
            cartArrayList.clear();
            cartArrayList.addAll(newCart);
            cartListAdapter.notifyDataSetChanged();
        });
    }
}
