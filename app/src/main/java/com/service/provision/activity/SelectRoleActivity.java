package com.service.provision.activity;

import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.activity.ProviderHomeActivity.MYUSERID;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.myVolleyError;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.service.provision.R;
import com.service.provision.materialDialog.ChooseProviderAccountMaterialDialog;
import com.service.provision.materialDialog.ChooseProviderAccountTypeMaterialDialog;
import com.service.provision.materialDialog.ChooseRiderAccountMaterialDialog;
import com.service.provision.materialDialog.CustomerNameMaterialDialog;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmBanner;
import com.service.provision.realm.RealmCart;
import com.service.provision.realm.RealmChat;
import com.service.provision.realm.RealmCustomer;
import com.service.provision.realm.RealmProductCategory;
import com.service.provision.realm.RealmProvider;
import com.service.provision.realm.RealmServiceCategory;
import com.service.provision.util.RealmUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

public class SelectRoleActivity extends AppCompatActivity {
    Activity selectRoleActivity;
    CardView provider, customer, superRider;
    TextView guest;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_role);

        selectRoleActivity = this;

        provider = findViewById(R.id.provider);
        customer = findViewById(R.id.customer);
        superRider = findViewById(R.id.superRider);
        guest = findViewById(R.id.guest);

        provider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Realm.init(getApplicationContext());

                RealmList<RealmProvider> providers = new RealmList();

                Realm.getInstance(RealmUtility.getDefaultConfig(SelectRoleActivity.this)).executeTransaction(realm -> {
                    RealmResults<RealmProvider> realmProviders = Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).where(RealmProvider.class)
                            .equalTo("user_id", PreferenceManager.getDefaultSharedPreferences(SelectRoleActivity.this).getString(MYUSERID, ""))
                            .notEqualTo("category", "Rider")
                            .findAll();

                    for (RealmProvider realmProvider : realmProviders) {
                        providers.add(realmProvider);
                    }
                });

                if (providers.size() == 0) {
                    ChooseProviderAccountTypeMaterialDialog chooseProviderAccountTypeMaterialDialog;
                    chooseProviderAccountTypeMaterialDialog = new ChooseProviderAccountTypeMaterialDialog();
                    if(chooseProviderAccountTypeMaterialDialog != null && chooseProviderAccountTypeMaterialDialog.isAdded()) {

                    } else {
                        chooseProviderAccountTypeMaterialDialog.setCancelable(false);
                        chooseProviderAccountTypeMaterialDialog.show(getSupportFragmentManager(), "chooseProviderAccountTypeMaterialDialog");
                        chooseProviderAccountTypeMaterialDialog.setCancelable(true);
                    }
                } else {
                    ChooseProviderAccountMaterialDialog chooseProviderAccountMaterialDialog = new ChooseProviderAccountMaterialDialog();
                    if(chooseProviderAccountMaterialDialog != null && chooseProviderAccountMaterialDialog.isAdded()) {

                    } else {
                        chooseProviderAccountMaterialDialog.setRealmProviders(providers);
                        chooseProviderAccountMaterialDialog.setCancelable(false);
                        chooseProviderAccountMaterialDialog.show(getSupportFragmentManager(), "ChooseProviderAccountMaterialDialog");
                        chooseProviderAccountMaterialDialog.setCancelable(true);
                    }
                }
            }
        });

        superRider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Realm.init(getApplicationContext());

                RealmList<RealmProvider> providers = new RealmList();

                Realm.getInstance(RealmUtility.getDefaultConfig(SelectRoleActivity.this)).executeTransaction(realm -> {
                    RealmResults<RealmProvider> realmProviders = Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).where(RealmProvider.class)
                            .equalTo("user_id", PreferenceManager.getDefaultSharedPreferences(SelectRoleActivity.this).getString(MYUSERID, ""))
                            .equalTo("category", "Rider")
                            .findAll();

                    for (RealmProvider realmProvider : realmProviders) {
                        providers.add(realmProvider);
                    }
                });

                if (providers.size() == 0) {
                    startActivity(new Intent(getApplicationContext(), RiderProviderAccountActivity.class));
                } else {
                    ChooseRiderAccountMaterialDialog chooseRiderAccountMaterialDialog = new ChooseRiderAccountMaterialDialog();
                    if(chooseRiderAccountMaterialDialog != null && chooseRiderAccountMaterialDialog.isAdded()) {

                    } else {
                        chooseRiderAccountMaterialDialog.setRealmProviders(providers);
                        chooseRiderAccountMaterialDialog.setCancelable(false);
                        chooseRiderAccountMaterialDialog.show(getSupportFragmentManager(), "ChooseRiderAccountMaterialDialog");
                        chooseRiderAccountMaterialDialog.setCancelable(true);
                    }
                }
            }
        });

        customer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final boolean[] customerAccountExist = {false};
                Realm.init(getApplicationContext());
                Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).executeTransaction(realm -> {

                    customerAccountExist[0] = realm.where(RealmCustomer.class).findAll().size() > 0;
                });
                if (customerAccountExist[0]) {
                    Realm.init(SelectRoleActivity.this);
                    String customer_id = Realm.getInstance(RealmUtility.getDefaultConfig(SelectRoleActivity.this)).where(RealmCustomer.class).findFirst().getCustomer_id();
                    ProgressDialog mProgress = new ProgressDialog(SelectRoleActivity.this);
                    mProgress.setTitle("Processing...");
                    mProgress.setMessage("Please wait...");
                    mProgress.setCancelable(false);
                    mProgress.setIndeterminate(true);
                    mProgress.show();
                    StringRequest stringRequest = new StringRequest(
                            Request.Method.POST,
                            API_URL + "customer-home-data",
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    mProgress.dismiss();
                                    if (response != null) {
                                        try {
                                            JSONObject jsonObject = new JSONObject(response);
                                            Realm.init(getApplicationContext());
                                            Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).executeTransaction(realm -> {
                                                try {
                                                    realm.where(RealmBanner.class).findAll().deleteAllFromRealm();
                                                    realm.where(RealmServiceCategory.class).findAll().deleteAllFromRealm();
                                                    realm.where(RealmProductCategory.class).findAll().deleteAllFromRealm();
                                                    realm.where(RealmCart.class).findAll().deleteAllFromRealm();
                                                    realm.where(RealmChat.class).findAll().deleteAllFromRealm();

                                                    realm.createOrUpdateAllFromJson(RealmBanner.class, jsonObject.getJSONArray("banners"));
                                                    realm.createOrUpdateAllFromJson(RealmServiceCategory.class, jsonObject.getJSONArray("service_categories"));
                                                    realm.createOrUpdateAllFromJson(RealmProductCategory.class, jsonObject.getJSONArray("product_categories"));
                                                    realm.createOrUpdateAllFromJson(RealmCart.class, jsonObject.getJSONArray("scoped_carts"));
                                                    realm.createOrUpdateAllFromJson(RealmChat.class, jsonObject.getJSONArray("chats"));


                                                    if (jsonObject.has("ride_history")) {
                                                        JSONObject ride_historyJson = jsonObject.getJSONObject("ride_history");
                                                        JSONObject providerJson = jsonObject.getJSONObject("provider");

                                                        Realm.init(getApplicationContext());
                                                        RealmCustomer realmCustomer = Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).where(RealmCustomer.class).findFirst();
                                                        String customer_id = realmCustomer.getCustomer_id();

                                                        PreferenceManager
                                                                .getDefaultSharedPreferences(getApplicationContext())
                                                                .edit()
                                                                .putString("RIDE_HISTORY_ID", ride_historyJson.getString("ride_history_id"))
                                                                .putString("PICKUP_LAT", String.valueOf(ride_historyJson.getDouble("pickup_latitude")))
                                                                .putString("PICKUP_LONG", String.valueOf(ride_historyJson.getDouble("pickup_longitude")))
                                                                .putString("DESTINATION_LAT", String.valueOf(ride_historyJson.getDouble("destination_latitude")))
                                                                .putString("DESTINATION_LONG", String.valueOf(ride_historyJson.getDouble("destination_longitude")))
                                                                .putString("PROVIDER_NAME", providerJson.getString("first_name"))
                                                                .putString("PROVIDER_LAT", providerJson.getString("latitude"))
                                                                .putString("PROVIDER_LONG", providerJson.getString("longitude"))
                                                                .putString("PROVIDER_PROFILE_IMAGE_URL", providerJson.getString("profile_image_url"))
                                                                .putString("PICKUP_ADDRESS", ride_historyJson.getString("pickup_address"))
                                                                .putString("DESTINATION_ADDRESS", ride_historyJson.getString("destination_address"))
                                                                .putString("SERVICE_ID", ride_historyJson.getString("service_id"))
                                                                .putString("PROVIDER_PRIMARY_CONTACT", providerJson.getString("primary_contact"))
                                                                .putString("VEHICLE_TYPE", providerJson.getString("vehicle_type"))
                                                                .putString("VEHICLE_REGISTRATION_NUMBER", providerJson.getString("vehicle_registration_number"))
                                                                .putString("PROVIDER_CONFIRMATION_TOKEN", providerJson.getString("confirmation_token"))
                                                                .putString("CUSTOMER_ID", customer_id)
                                                                .putString("PROVIDER_ID", providerJson.getString("provider_id"))
                                                                .apply();


                                                        if (ride_historyJson.isNull("start_time") || ride_historyJson.getString("start_time").equals("null")) {
                                                            PreferenceManager
                                                                    .getDefaultSharedPreferences(getApplicationContext())
                                                                    .edit()
                                                                    .putBoolean("DRIVING_TO_PICKUP", true)
                                                                    .putBoolean("DRIVING_TO_DESTINATION", false)
                                                                    .apply();
                                                        }
                                                        else {
                                                            PreferenceManager
                                                                    .getDefaultSharedPreferences(getApplicationContext())
                                                                    .edit()
                                                                    .putBoolean("DRIVING_TO_PICKUP", false)
                                                                    .putBoolean("DRIVING_TO_DESTINATION", true)
                                                                    .apply();
                                                        }
                                                    }
                                                    else {
                                                        PreferenceManager
                                                                .getDefaultSharedPreferences(getApplicationContext())
                                                                .edit()
                                                                .putBoolean("DRIVING_TO_PICKUP", false)
                                                                .putBoolean("DRIVING_TO_DESTINATION", false)
                                                                .apply();
                                                    }

                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            });
                                            PreferenceManager
                                                    .getDefaultSharedPreferences(getApplicationContext())
                                                    .edit()
                                                    .putString("ROLE", "CUSTOMER")
                                                    .apply();
                                            startActivity(new Intent(getApplicationContext(), CustomerHomeActivity.class));
                                            finish();
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    mProgress.dismiss();
                                    myVolleyError(getApplicationContext(), error);
                                }
                            }
                    )
                    {
                        /** Passing some request headers* */
                        @Override
                        public Map getHeaders() throws AuthFailureError {
                            HashMap headers = new HashMap();
                            headers.put("accept", "application/json");
                            headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(APITOKEN, ""));
                            return headers;
                        }

                        @Override
                        public Map<String, String> getParams() throws AuthFailureError {

                            Map<String, String> params = new HashMap<>();
                            params.put("customer_id", customer_id);
                            return params;
                        }
                    };
                    stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                            0,
                            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                    InitApplication.getInstance().addToRequestQueue(stringRequest);
                } else {
                    CustomerNameMaterialDialog customerNameMaterialDialog = new CustomerNameMaterialDialog();
                    if(customerNameMaterialDialog != null && customerNameMaterialDialog.isAdded()) {

                    } else {
                        customerNameMaterialDialog.show(getSupportFragmentManager(), "CustomerNameMaterialDialog");
                    }
                }
            }
        });
    }
}