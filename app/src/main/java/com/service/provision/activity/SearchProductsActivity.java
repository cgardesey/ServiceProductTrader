package com.service.provision.activity;

import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.myVolleyError;
import static com.service.provision.receiver.NetworkReceiver.activeActivity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.greysonparrelli.permiso.Permiso;
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


public class SearchProductsActivity extends PermisoActivity implements ChooseQuantityMaterialDialog.ChooseQuantityMDInterface{

    NetworkReceiver networkReceiver;
    RecyclerView recyclerview;
    RatedProductAdapter ratedProductAdapter;
    private EditText searchtext;
    ArrayList<RealmProduct> realmProducts = new ArrayList<>(), newRealmProducts = new ArrayList<>();
    public static Activity searchProductActivity;
    TextView title;
    ImageView loadinggif, search, cartIcon;
    private String tag = "SEARCH_TAG";
    static int offset = 0;
    RelativeLayout parent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_search_products);

        recyclerview = findViewById(R.id.recyclerview);
        searchProductActivity = this;
        searchtext = findViewById(R.id.searchtext);

        loadinggif = findViewById(R.id.loadinggif);
        searchtext = findViewById(R.id.searchtext);
        search = findViewById(R.id.search);
        cartIcon = findViewById(R.id.cartIcon);
        parent = findViewById(R.id.parent);

        cartIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProgressDialog dialog = new ProgressDialog(SearchProductsActivity.this);
                dialog.setMessage("Please wait...");
                dialog.setCancelable(false);
                dialog.setIndeterminate(true);
                dialog.show();

                Realm.init(SearchProductsActivity.this);
                String customer_id = Realm.getInstance(RealmUtility.getDefaultConfig(SearchProductsActivity.this)).where(RealmCustomer.class).findFirst().getCustomer_id();




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
                                        Realm.getInstance(RealmUtility.getDefaultConfig(SearchProductsActivity.this)).executeTransaction(realm -> {
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

        searchtext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                realmProducts.clear();
                ratedProductAdapter.notifyDataSetChanged();
                offset = 0;
                if (!searchtext.getText().toString().equals("")) {
                    populateFilteredProducts(searchtext.getText().toString());
                } else {
                    realmProducts.clear();
                    ratedProductAdapter.notifyDataSetChanged();
                }
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
                    Realm.init(SearchProductsActivity.this);
                    String customer_id = Realm.getInstance(RealmUtility.getDefaultConfig(SearchProductsActivity.this)).where(RealmCustomer.class).findFirst().getCustomer_id();
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

                        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
                                                                     @Override
                                                                     public void onPermissionResult(Permiso.ResultSet resultSet) {
                                                                         if (resultSet.isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE) && resultSet.isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                                                             ProgressDialog mProgress = new ProgressDialog(searchProductActivity);
                                                                             mProgress.setCancelable(false);
                                                                             mProgress.setIndeterminate(true);

                                                                             mProgress.setTitle("Please wait...");
                                                                             mProgress.show();
                                                                             String product_id = realmProduct.getProduct_id();
                                                                             StringRequest stringRequest = new StringRequest(
                                                                                     Request.Method.POST,
                                                                                     API_URL + "scoped-product-images",
                                                                                     response -> {//
                                                                                         mProgress.dismiss();
                                                                                         if (response != null) {
                                                                                             try {
                                                                                                 JSONArray jsonArray = new JSONArray(response);
                                                                                                 Realm.init(searchProductActivity);
                                                                                                 Realm.getInstance(RealmUtility.getDefaultConfig(searchProductActivity)).executeTransaction(realm -> {
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
                                                                                         myVolleyError(searchProductActivity, error);
                                                                                         Log.d("Cyrilll", error.toString());
                                                                                     }
                                                                             ) {
                                                                                 @Override
                                                                                 public Map getHeaders() throws AuthFailureError {
                                                                                     HashMap headers = new HashMap();
                                                                                     headers.put("accept", "application/json");
                                                                                     headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(searchProductActivity).getString(APITOKEN, ""));
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

                                                                     @Override
                                                                     public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                                                                         Permiso.getInstance().showRationaleInDialog(searchProductActivity.getString(R.string.permissions), searchProductActivity.getString(R.string.this_permission_is_mandatory_pls_allow_access), null, callback);
                                                                     }
                                                                 },
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    }
                }, searchProductActivity, realmProducts);

        recyclerview.setLayoutManager(new LinearLayoutManager(this));
        recyclerview.setNestedScrollingEnabled(false);
        recyclerview.setItemAnimator(new DefaultItemAnimator());
        recyclerview.addItemDecoration(new DividerItemDecoration(getApplicationContext(), DividerItemDecoration.VERTICAL));
        recyclerview.setAdapter(ratedProductAdapter);

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

    private void populateFilteredProducts(String search) {
        try {
            loadinggif.setVisibility(View.VISIBLE);
            InitApplication.getInstance().mRequestQueue.cancelAll(tag);
            StringRequest stringRequest = new StringRequest(
                    Request.Method.POST,
                    API_URL + "filtered-products",
                    response -> {
                        loadinggif.setVisibility(View.GONE);
                        if (response != null) {
                            try {
                                JSONArray jsonArray = new JSONArray(response);
                                if (jsonArray.length() > 0) {
                                    Realm.init(getApplicationContext());
                                    Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).executeTransaction(realm -> {
                                        realm.where(RealmProduct.class).findAll().deleteAllFromRealm();
                                        realm.createOrUpdateAllFromJson(RealmProduct.class, jsonArray);
                                        RealmResults<RealmProduct> products = realm.where(RealmProduct.class).findAll();
                                        newRealmProducts.clear();
                                        for (RealmProduct realmProduct : products) {
                                            newRealmProducts.add(realmProduct);
                                        }
                                    });
                                    realmProducts.clear();
                                    realmProducts.addAll(newRealmProducts);
                                    ratedProductAdapter.notifyDataSetChanged();
                                } else {
                                    realmProducts.clear();
                                    ratedProductAdapter.notifyDataSetChanged();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    },
                    error -> {
                        loadinggif.setVisibility(View.GONE);
                        myVolleyError(getApplicationContext(), error);
                    }
            ) {
                @Override
                public Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    params.put("search", search);
                    params.put("offset", String.valueOf(offset));
                    params.put("length", "10");

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
            stringRequest.setTag(tag);
            InitApplication.getInstance().addToRequestQueue(stringRequest);

        } catch (Exception e) {
            Log.e("My error", e.toString());
        }
    }


    @Override
    public void onViewClick(String message, JSONObject jsonObject) {

    }

    @Override
    public void onStockCartViewClick(String message, JSONObject jsonObject) {

    }
}
