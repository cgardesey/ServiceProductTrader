package com.service.provision.activity;

import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.myVolleyError;
import static com.service.provision.receiver.NetworkReceiver.activeActivity;

import android.app.Activity;
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
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.greysonparrelli.permiso.PermisoActivity;
import com.service.provision.R;
import com.service.provision.adapter.RatedServiceAdapter;
import com.service.provision.materialDialog.ChooseServiceContactMethodMaterialDialog;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmCustomer;
import com.service.provision.realm.RealmService;
import com.service.provision.receiver.NetworkReceiver;
import com.service.provision.util.PixelUtil;
import com.service.provision.util.RealmUtility;
import com.yalantis.ucrop.UCrop;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;


public class SearchServicesActivity extends PermisoActivity {

    NetworkReceiver networkReceiver;
    RecyclerView recyclerview;
    RatedServiceAdapter ratedServiceAdapter;
    private EditText searchtext;
    ArrayList<RealmService> realmServices = new ArrayList<>(), newRealmServices = new ArrayList<>();
    public static Activity searchServiceActivity;
    TextView title;
    ImageView loadinggif, search;
    private String tag = "SEARCH_TAG";
    static int offset = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_search_services);

        recyclerview = findViewById(R.id.recyclerview);
        searchServiceActivity = this;
        searchtext = findViewById(R.id.searchtext);

        loadinggif = findViewById(R.id.loadinggif);
        searchtext = findViewById(R.id.searchtext);
        search = findViewById(R.id.search);

        searchtext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                realmServices.clear();
                ratedServiceAdapter.notifyDataSetChanged();
                offset = 0;
                if (!searchtext.getText().toString().equals("")) {
                    populateFilteredServices(searchtext.getText().toString());
                }
                else {
                    realmServices.clear();
                    ratedServiceAdapter.notifyDataSetChanged();
                }
            }
        });

        ratedServiceAdapter = new RatedServiceAdapter(new RatedServiceAdapter.RatedServiceAdapterInterface() {
            @Override
            public void onListItemClick(ArrayList<RealmService> realmServices, int position, RatedServiceAdapter.ViewHolder holder) {
                RealmService realmService = realmServices.get(position);
                ChooseServiceContactMethodMaterialDialog chooseServiceContactMethodMaterialDialog = new ChooseServiceContactMethodMaterialDialog();
                if(chooseServiceContactMethodMaterialDialog != null && chooseServiceContactMethodMaterialDialog.isAdded()) {

                } else {
                    chooseServiceContactMethodMaterialDialog.setProvider_id(realmService.getProvider_id());
                    Realm.init(SearchServicesActivity.this);
                    String customer_id = Realm.getInstance(RealmUtility.getDefaultConfig(SearchServicesActivity.this)).where(RealmCustomer.class).findFirst().getCustomer_id();
                    chooseServiceContactMethodMaterialDialog.setCustomer_id(customer_id);
                    chooseServiceContactMethodMaterialDialog.show(getSupportFragmentManager(), "chooseContactMethodMaterialDialog");
                    chooseServiceContactMethodMaterialDialog.setCancelable(true);
                }
            }
        }, searchServiceActivity, realmServices);

        recyclerview.setLayoutManager(new LinearLayoutManager(this));
        recyclerview.setNestedScrollingEnabled(false);
        recyclerview.setItemAnimator(new DefaultItemAnimator());
        recyclerview.addItemDecoration(new DividerItemDecoration(getApplicationContext(), DividerItemDecoration.VERTICAL));
        recyclerview.setAdapter(ratedServiceAdapter);

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

    private void populateFilteredServices(String search) {
        try {
            loadinggif.setVisibility(View.VISIBLE);
            InitApplication.getInstance().mRequestQueue.cancelAll(tag);
            StringRequest stringRequest = new StringRequest(
                    Request.Method.POST,
                    API_URL + "filtered-services",
                    response -> {
                        loadinggif.setVisibility(View.GONE);
                        if (response != null) {
                            try {
                                JSONArray jsonArray = new JSONArray(response);
                                if (jsonArray.length() > 0) {
                                    Realm.init(getApplicationContext());
                                    Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).executeTransaction(realm -> {
                                        realm.where(RealmService.class).findAll().deleteAllFromRealm();
                                        realm.createOrUpdateAllFromJson(RealmService.class, jsonArray);
                                        RealmResults<RealmService> services = realm.where(RealmService.class).findAll();
                                        newRealmServices.clear();
                                        for (RealmService realmService : services) {
                                            newRealmServices.add(realmService);
                                        }
                                    });
                                    realmServices.clear();
                                    realmServices.addAll(newRealmServices);
                                    ratedServiceAdapter.notifyDataSetChanged();
                                }
                                else {
                                    realmServices.clear();
                                    ratedServiceAdapter.notifyDataSetChanged();
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
}
