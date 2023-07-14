package com.service.provision.activity;

import static com.service.provision.activity.OrderSummaryActivity.realmProvider;
import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.myVolleyError;
import static com.service.provision.receiver.NetworkReceiver.activeActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.snackbar.Snackbar;
import com.greysonparrelli.permiso.PermisoActivity;
import com.service.provision.R;
import com.service.provision.adapter.RatedProductAdapter;
import com.service.provision.materialDialog.ChooseQuantityMaterialDialog;
import com.service.provision.materialDialog.ChooseServiceContactMethodMaterialDialog;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmCart;
import com.service.provision.realm.RealmCustomer;
import com.service.provision.realm.RealmProduct;
import com.service.provision.realm.RealmProductImage;
import com.service.provision.realm.RealmProvider;
import com.service.provision.receiver.NetworkReceiver;
import com.service.provision.util.PixelUtil;
import com.service.provision.util.RealmUtility;
import com.yalantis.ucrop.UCrop;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;


public class ProductsActivity extends PermisoActivity implements ChooseQuantityMaterialDialog.ChooseQuantityMDInterface{

    NetworkReceiver networkReceiver;
    RecyclerView recyclerview;
    RatedProductAdapter ratedProductAdapter;
    private ImageView cartIcon, backbtn;
    ArrayList<RealmProduct> realmProducts = new ArrayList<>(), newRealmProducts = new ArrayList<>();
    public static Activity productActivity;
    TextView title;
    RelativeLayout parent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_products);

        recyclerview = findViewById(R.id.recyclerview);
        productActivity = this;
        backbtn = findViewById(R.id.backbtn1);
        title = findViewById(R.id.title);
        cartIcon = findViewById(R.id.cartIcon);
        parent = findViewById(R.id.parent);

        title.setText(getIntent().getStringExtra("TITLE"));

        cartIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProgressDialog dialog = new ProgressDialog(ProductsActivity.this);
                dialog.setMessage("Please wait...");
                dialog.setCancelable(false);
                dialog.setIndeterminate(true);
                dialog.show();

                Realm.init(ProductsActivity.this);
                String customer_id = Realm.getInstance(RealmUtility.getDefaultConfig(ProductsActivity.this)).where(RealmCustomer.class).findFirst().getCustomer_id();




                StringRequest stringRequest = new StringRequest(
                        com.android.volley.Request.Method.POST,
                        API_URL + "scoped-carts",
                        response -> {
                            if (response != null) {
                                dialog.dismiss();
                                try {
                                    JSONArray jsonArray = new JSONArray(response);
                                    if (jsonArray.length() > 0) {
                                        Realm.init(getApplicationContext());
                                        Realm.getInstance(RealmUtility.getDefaultConfig(ProductsActivity.this)).executeTransaction(realm -> {
                                            realm.where(RealmCart.class).findAll().deleteAllFromRealm();
                                            realm.createOrUpdateAllFromJson(RealmCart.class, jsonArray);
                                        });

                                        startActivity(new Intent(getApplicationContext(), CartListActivity.class));
                                    } else {
                                        Toast.makeText(getApplicationContext(), "No cart items available.", Toast.LENGTH_SHORT).show();
                                    }
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
                        params.put("customer_id", customer_id);
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
        });

        backbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        ratedProductAdapter = new RatedProductAdapter(new RatedProductAdapter.ContactMethodAdapterInterface() {
            @Override
            public void onListItemClick(ArrayList<RealmProduct> realmProducts, int position, RatedProductAdapter.ViewHolder holder) {
                RealmProduct realmProduct = realmProducts.get(position);
                ChooseServiceContactMethodMaterialDialog chooseServiceContactMethodMaterialDialog = new ChooseServiceContactMethodMaterialDialog();
                if (chooseServiceContactMethodMaterialDialog != null && chooseServiceContactMethodMaterialDialog.isAdded()) {

                } else {
                    chooseServiceContactMethodMaterialDialog.setProvider_id(realmProduct.getProvider_id());
                    Realm.init(ProductsActivity.this);
                    String customer_id = Realm.getInstance(RealmUtility.getDefaultConfig(ProductsActivity.this)).where(RealmCustomer.class).findFirst().getCustomer_id();
                    chooseServiceContactMethodMaterialDialog.setCustomer_id(customer_id);
                    chooseServiceContactMethodMaterialDialog.show(getSupportFragmentManager(), "chooseContactMethodMaterialDialog");
                    chooseServiceContactMethodMaterialDialog.setCancelable(true);
                }
            }
        },
                new RatedProductAdapter.AddToCartAdapterInterface() {
                    @Override
                    public void onListItemClick(ArrayList<RealmProduct> names, int position, RatedProductAdapter.ViewHolder holder) {
                        RealmProduct realmProduct = realmProducts.get(position);
                        if (realmProduct.getQuantity_available() <= realmProduct.getUnit_quantity()) {
                            Toast.makeText(productActivity, "This item is out of stock.", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            ProgressDialog mProgress = new ProgressDialog(productActivity);
                            mProgress.setCancelable(false);
                            mProgress.setIndeterminate(true);

                            mProgress.setTitle("Please wait...");
                            mProgress.show();
                            String product_id = realmProduct.getProduct_id();
                            StringRequest stringRequest = new StringRequest(
                                    Request.Method.POST,
                                    API_URL + "scoped-product-images",
                                    response -> {
                                        mProgress.dismiss();
                                        if (response != null) {
                                            try {
                                                JSONArray jsonArray = new JSONArray(response);
                                                Realm.init(productActivity);
                                                Realm.getInstance(RealmUtility.getDefaultConfig(productActivity)).executeTransaction(realm -> {
                                                    RealmResults<RealmProductImage> productImages = realm.where(RealmProductImage.class).findAll();
                                                    productImages.deleteAllFromRealm();
                                                    realm.createOrUpdateAllFromJson(RealmProductImage.class, jsonArray);
                                                });
                                                ChooseQuantityMaterialDialog chooseQuantityMaterialDialog = new ChooseQuantityMaterialDialog();
                                                if (chooseQuantityMaterialDialog != null && chooseQuantityMaterialDialog.isAdded()) {

                                                } else {
                                                    chooseQuantityMaterialDialog.setProvider_id(realmProduct.getProvider_id());
                                                    chooseQuantityMaterialDialog.setProduct_id(realmProduct.getProduct_id());
                                                    chooseQuantityMaterialDialog.setQuantity_available(realmProduct.getQuantity_available());
                                                    chooseQuantityMaterialDialog.setUnit_quantity(realmProduct.getUnit_quantity());
                                                    chooseQuantityMaterialDialog.setUnit_price(realmProduct.getUnit_price());
                                                    chooseQuantityMaterialDialog.setCancelable(false);
                                                    chooseQuantityMaterialDialog.show(getSupportFragmentManager(), "chooseQuantityMaterialDialog");
                                                    chooseQuantityMaterialDialog.setCancelable(true);
                                                }
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    },
                                    error -> {
                                        mProgress.dismiss();
                                        error.printStackTrace();
                                        myVolleyError(productActivity, error);
                                        Log.d("Cyrilll", error.toString());
                                    }
                            ) {
                                @Override
                                public Map getHeaders() throws AuthFailureError {
                                    HashMap headers = new HashMap();
                                    headers.put("accept", "application/json");
                                    headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(productActivity).getString(APITOKEN, ""));
                                    return headers;
                                }
                                @Override
                                public Map<String, String> getParams() throws AuthFailureError {
                                    Map<String, String> params = new HashMap<>();
                                    params.put("product_id", product_id);
                                    return params;
                                }
                            };
                            stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                                    0,
                                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                            InitApplication.getInstance().addToRequestQueue(stringRequest);
                        }
                    }
                }, productActivity, realmProducts);

        recyclerview.setLayoutManager(new LinearLayoutManager(this));
//        recyclerview.setLayoutManager(new GridLayoutManager(getApplicationContext(), 2));
        recyclerview.setNestedScrollingEnabled(false);
        recyclerview.setItemAnimator(new DefaultItemAnimator());
        recyclerview.addItemDecoration(new DividerItemDecoration(getApplicationContext(), DividerItemDecoration.VERTICAL));
        recyclerview.setAdapter(ratedProductAdapter);

        populateProducts(getApplicationContext());
        ratedProductAdapter.notifyDataSetChanged();

        networkReceiver = new NetworkReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        activeActivity = this;
        registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(networkReceiver);
    }

    private UCrop.Options imgOptions() {
        UCrop.Options options = new UCrop.Options();
        options.setStatusBarColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
        options.setToolbarColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary));
        options.setCropFrameColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
        options.setCropFrameStrokeWidth(PixelUtil.dpToPx(getApplicationContext(), 4));
        options.setCropGridColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary));
        options.setCropGridStrokeWidth(PixelUtil.dpToPx(getApplicationContext(), 2));
        options.setActiveWidgetColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary));
        options.setToolbarTitle(getString(R.string.crop_image));

        // set rounded cropping guide
        options.setCircleDimmedLayer(true);
        return options;
    }

    void populateProducts(final Context context) {
        newRealmProducts.clear();

        Realm.init(context);
        Realm.getInstance(RealmUtility.getDefaultConfig(context)).executeTransaction(realm -> {
            String stringExtra;
            String fieldName;
            RealmResults<RealmProduct> realmProducts = realm.where(RealmProduct.class)
                    .findAll();


            for (RealmProduct realmProduct : realmProducts) {
                newRealmProducts.add(realmProduct);
            }

            this.realmProducts.clear();
            this.realmProducts.addAll(newRealmProducts);

            ratedProductAdapter.notifyDataSetChanged();
            recyclerview.setVisibility(View.VISIBLE);
        });
    }

    public static void order(Context context, JSONObject jsonObject) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.setIndeterminate(true);
        dialog.show();

        StringRequest stringRequest = new StringRequest(
                com.android.volley.Request.Method.POST,
                API_URL + "cart-total-count",
                response -> {
                    if (response != null) {
                        dialog.dismiss();
                        try {
                            JSONObject myJsonObject = new JSONObject(response);
                            Realm.init(context);
                            Realm.getInstance(RealmUtility.getDefaultConfig(context)).executeTransaction(realm -> {
                                try {
                                    realmProvider = realm.createOrUpdateObjectFromJson(RealmProvider.class, myJsonObject.getJSONObject("provider"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            });
                            context.startActivity(new Intent(context, OrderSummaryActivity.class)
                                    .putExtra("ITEM_COUNT", myJsonObject.getInt("item_count"))
                                    .putExtra("SUB_TOTAL", (float) myJsonObject.getDouble("cart_total"))
                                    .putExtra("CART_ID", jsonObject.getString("cart_id"))
                            );

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                error -> {
                    error.printStackTrace();
                    myVolleyError(context, error);
                    dialog.dismiss();
                    Log.d("Cyrilll", error.toString());
                }
        ) {
            @Override
            public Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                try {
                    params.put("cart_id", jsonObject.getString("cart_id"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return params;
            }

            /** Passing some request headers* */
            @Override
            public Map getHeaders() throws AuthFailureError {
                HashMap headers = new HashMap();
                headers.put("accept", "application/json");
                headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(context).getString(APITOKEN, ""));
                return headers;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        InitApplication.getInstance().addToRequestQueue(stringRequest);
    }

    public static void viewCart(Context context, JSONObject jsonObject) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.setIndeterminate(true);
        dialog.show();

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,
                API_URL + "scoped-carts",
                response -> {
                    if (response != null) {
                        dialog.dismiss();
                        try {
                            JSONArray jsonArray = new JSONArray(response);
                            if (jsonArray.length() > 0) {
                                Realm.init(context);
                                Realm.getInstance(RealmUtility.getDefaultConfig(context)).executeTransaction(realm -> {
                                    realm.where(RealmCart.class).findAll().deleteAllFromRealm();
                                    realm.createOrUpdateAllFromJson(RealmCart.class, jsonArray);
                                });

                                context.startActivity(new Intent(context, CartListActivity.class));
                            } else {
                                Toast.makeText(context, "No cart items available.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                error -> {
                    error.printStackTrace();
                    myVolleyError(context, error);
                    dialog.dismiss();
                    Log.d("Cyrilll", error.toString());
                }
        ) {
            @Override
            public Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();

                Realm.init(Realm.getApplicationContext());
                String customer_id = Realm.getInstance(RealmUtility.getDefaultConfig(context.getApplicationContext())).where(RealmCustomer.class).findFirst().getCustomer_id();

                params.put("customer_id", customer_id);
                return params;
            }
            /** Passing some request headers* */
            @Override
            public Map getHeaders() throws AuthFailureError {
                HashMap headers = new HashMap();
                headers.put("accept", "application/json");
                headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(context).getString(APITOKEN, ""));
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
    public void onViewClick(String message, JSONObject jsonObject) {
        // Create the Snackbar
        LinearLayout.LayoutParams objLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        final Snackbar snackbar = Snackbar.make(parent, message, Snackbar.LENGTH_LONG);

        // Get the Snackbar layout view
        Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbar.getView();

        // Inflate our courseListMaterialDialog viewBitmap bitmap = ((RoundedDrawable)profilePic.getDrawable()).getSourceBitmap();
        View snackView = getLayoutInflater().inflate(R.layout.snackbar, null);

        TextView messageTextView = snackView.findViewById(R.id.message);
        messageTextView.setText(message);
        TextView textViewOne = snackView.findViewById(R.id.first_text_view);
        textViewOne.setText("View");
        textViewOne.setOnClickListener(v -> {
            viewCart(ProductsActivity.this, jsonObject);
        });

        final TextView textViewTwo = snackView.findViewById(R.id.second_text_view);

        textViewTwo.setText("Order");
        textViewTwo.setOnClickListener(v -> {
            order(ProductsActivity.this, jsonObject);
        });

        // Add our courseListMaterialDialog view to the Snackbar's layout
        layout.addView(snackView, objLayoutParams);

        // Show the Snackbar
        snackbar.show();
    }

    @Override
    public void onStockCartViewClick(String message, JSONObject jsonObject) {

    }
}
