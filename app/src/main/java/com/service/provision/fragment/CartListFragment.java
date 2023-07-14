package com.service.provision.fragment;

import static com.service.provision.activity.OrderSummaryActivity.*;
import static com.service.provision.activity.CustomerOrdersActivity.*;
import static com.service.provision.activity.CustomerOrdersActivity.mTabLayout;
import static com.service.provision.activity.CustomerOrdersActivity.statuses;
import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.myVolleyError;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.service.provision.R;
import com.service.provision.activity.CartItemsActivity;
import com.service.provision.activity.OrderSummaryActivity;
import com.service.provision.adapter.CartListAdapter;
import com.service.provision.materialDialog.ChooseServiceContactMethodMaterialDialog;
import com.service.provision.other.InitApplication;
import com.service.provision.pagerAdapter.OrdersPagerAdapter;
import com.service.provision.realm.RealmCart;
import com.service.provision.realm.RealmCartProduct;
import com.service.provision.realm.RealmCustomer;
import com.service.provision.realm.RealmProvider;
import com.service.provision.util.RealmUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;

public class CartListFragment extends Fragment {
    RecyclerView recyclerview;
    TextView no_data;
    CartListAdapter cartListAdapter;
    ArrayList<RealmCart> cartArrayList = new ArrayList<>(), newCart = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final View rootView = inflater.inflate(R.layout.fragment_cart_list, container, false);
        recyclerview = rootView.findViewById(R.id.recyclerview);
        no_data = rootView.findViewById(R.id.no_data);

        cartListAdapter = new CartListAdapter(new CartListAdapter.CartAdapterInterface() {
            @Override
            public void onViewClick(ArrayList<RealmCart> realmCarts, int position, CartListAdapter.ViewHolder holder) {
                RealmCart realmCart = realmCarts.get(position);
                String cart_id = realmCart.getCart_id();
                ProgressDialog dialog = new ProgressDialog(getActivity());
                dialog.setMessage("Please wait...");
                dialog.setCancelable(false);
                dialog.setIndeterminate(true);
                dialog.show();

                Realm.init(getActivity());
                String customer_id = Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).where(RealmCustomer.class).findFirst().getCustomer_id();


                StringRequest stringRequest = new StringRequest(
                        com.android.volley.Request.Method.POST,
                        API_URL + "scoped-cart-products",
                        response -> {
                            if (response != null) {
                                dialog.dismiss();
                                try {
                                    final float[] sub_total = {0.00F};
                                    JSONArray jsonArray = new JSONArray(response);
                                    Realm.init(getActivity());
                                    Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).executeTransaction(realm -> {
                                        realm.where(RealmCartProduct.class).findAll().deleteAllFromRealm();
                                        realm.createOrUpdateAllFromJson(RealmCartProduct.class, jsonArray);

                                        for (int i = 0; i < jsonArray.length(); i++) {
                                            try {
                                                sub_total[0] += (float) jsonArray.getJSONObject(i).getDouble("price");
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });

                                    startActivity(
                                            new Intent(getActivity(), CartItemsActivity.class)
                                                    .putExtra("IS_INVOICE", realmCart.getStatus() != null && realmCart.getStatus().equals("SUCCESS"))
                                                    .putExtra("INVOICE_SUB_TOTAL", sub_total[0])
                                                    .putExtra("SHIPPING_FEE", (float) realmCart.getShipping_fee())
                                                    .putExtra("CART_ID", realmCart.getCart_id())
                                    );
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        error -> {
                            error.printStackTrace();
                            myVolleyError(getActivity(), error);
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
                        headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(APITOKEN, ""));
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
                    chooseServiceContactMethodMaterialDialog.setOrder_id(realmCart.getOrder_id());
                    chooseServiceContactMethodMaterialDialog.show(getChildFragmentManager(), "chooseContactMethodMaterialDialog");
                    chooseServiceContactMethodMaterialDialog.setCancelable(true);
                }
            }

            @Override
            public void onOrderClick(ArrayList<RealmCart> realmCarts, int position, CartListAdapter.ViewHolder holder) {
                RealmCart realmCart = realmCarts.get(position);

                String cart_id = realmCart.getCart_id();
                ProgressDialog dialog = new ProgressDialog(getActivity());
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
                                    Realm.init(getActivity());
                                    Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).executeTransaction(realm -> {
                                        try {
                                            realmProvider = realm.createOrUpdateObjectFromJson(RealmProvider.class, jsonObject.getJSONObject("provider"));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    });
                                    startActivity(new Intent(getActivity(), OrderSummaryActivity.class)
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
                            myVolleyError(getActivity(), error);
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
                        headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(APITOKEN, ""));
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

            }

            @Override
            public void onMarkAsDeliveredClick(ArrayList<RealmCart> realmCarts, int position, CartListAdapter.ViewHolder holder) {
                RealmCart realmCart = realmCarts.get(position);
                ProgressDialog dialog = new ProgressDialog(getActivity());
                dialog.setMessage("Please wait...");
                dialog.setCancelable(false);
                dialog.setIndeterminate(true);
                dialog.show();

                StringRequest stringRequest = new StringRequest(
                        Request.Method.PATCH,
                        API_URL + "carts/" + realmCart.getCart_id(),
                        response -> {
                            if (response != null) {
                                dialog.dismiss();
                                try {
                                    JSONObject jsonObject = new JSONObject(response);
                                    Realm.init(getActivity());
                                    Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).executeTransaction(realm -> {
                                        realm.createOrUpdateObjectFromJson(RealmCart.class, jsonObject);
                                    });
                                    mViewPager.setAdapter(new OrdersPagerAdapter(getFragmentManager(), statuses));
                                    mTabLayout.setViewPager(mViewPager);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        error -> {
                            error.printStackTrace();
                            myVolleyError(getActivity(), error);
                            dialog.dismiss();
                            Log.d("Cyrilll", error.toString());
                        }
                ) {
                    @Override
                    public Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> params = new HashMap<>();
                        params.put("delivered", "1");
                        return params;
                    }

                    /** Passing some request headers* */
                    @Override
                    public Map getHeaders() throws AuthFailureError {
                        HashMap headers = new HashMap();
                        headers.put("accept", "application/json");
                        headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(APITOKEN, ""));
                        return headers;
                    }
                };
                stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                        0,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                InitApplication.getInstance().addToRequestQueue(stringRequest);
            }
        }, getActivity(), cartArrayList);
        recyclerview.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerview.setNestedScrollingEnabled(false);
        recyclerview.setItemAnimator(new DefaultItemAnimator());
        recyclerview.setAdapter(cartListAdapter);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        populateCart(getActivity());
    }

    @Override
    public void onPause() {
        super.onPause();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
