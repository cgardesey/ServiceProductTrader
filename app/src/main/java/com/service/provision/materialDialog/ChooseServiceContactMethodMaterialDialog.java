package com.service.provision.materialDialog;

import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.API_URL;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.service.provision.R;
import com.service.provision.activity.MessageActivity;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmCart;
import com.service.provision.realm.RealmChat;
import com.service.provision.realm.RealmCustomer;
import com.service.provision.realm.RealmProvider;
import com.service.provision.util.RealmUtility;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class ChooseServiceContactMethodMaterialDialog extends DialogFragment {
    public static ProgressDialog dialog1;
    private static final String MY_LOGIN_ID = "MY_LOGIN_ID";
    LinearLayout chat, call;

    String provider_id;
    String customer_id;
    String order_id;

    public String getCustomer_id() {
        return customer_id;
    }

    public void setCustomer_id(String customer_id) {
        this.customer_id = customer_id;
    }

    public String getProvider_id() {
        return provider_id;
    }

    public void setProvider_id(String provider_id) {
        this.provider_id = provider_id;
    }

    public String getOrder_id() {
        return order_id;
    }

    public void setOrder_id(String order_id) {
        this.order_id = order_id;
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.list_item_choose_contact_method,null);
        chat = view.findViewById(R.id.chat);
        call = view.findViewById(R.id.call);

        chat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProgressDialog dialog = new ProgressDialog(getActivity());
                dialog.setMessage("Please wait...");
                dialog.setCancelable(false);
                dialog.setIndeterminate(true);
                dialog.show();


                String role = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("ROLE", "");
                if (role.equals("PROVIDER")) {
                    final String[] customer_name = new String[1];
                    final String[] profile_image_url = new String[1];;

                    Realm.init(getActivity());
                    final RealmCart[] realmCart = {Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).where(RealmCart.class).equalTo("customer_id", customer_id).findFirst()};
                    customer_name[0] = realmCart[0].getCustomer_name();
                    profile_image_url[0] = realmCart[0].getCustomer_image_url();

                    StringRequest stringRequest = new StringRequest(
                            Request.Method.POST,
                            API_URL + "chat-data",
                            response -> {
                                if (response != null) {
                                    dialog.dismiss();
                                    try {
                                        JSONObject jsonObject = new JSONObject(response);
                                        Realm.init(getActivity());
                                        Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).executeTransaction(realm -> {
                                            try {
                                                realmCart[0] = realm.createOrUpdateObjectFromJson(RealmCart.class, jsonObject.getJSONObject("cart"));
                                                realm.createOrUpdateAllFromJson(RealmChat.class, jsonObject.getJSONArray("chats"));
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            customer_name[0] = realmCart[0].getCustomer_name();
                                            profile_image_url[0] = realmCart[0].getCustomer_image_url();
                                        });

                                        startActivity(new Intent(getActivity(), MessageActivity.class)
                                                .putExtra("CUSTOMER_ID", customer_id)
                                                .putExtra("CUSTOMER_NAME", customer_name[0])
                                                .putExtra("PROFILE_IMAGE_URL", profile_image_url[0])
                                        );
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            },
                            error -> {
                                error.printStackTrace();
                                startActivity(new Intent(getActivity(), MessageActivity.class)
                                        .putExtra("CUSTOMER_ID", customer_id)
                                        .putExtra("CUSTOMER_NAME", customer_name[0])
                                        .putExtra("PROFILE_IMAGE_URL", profile_image_url[0])
                                );
                                dialog.dismiss();
                                Log.d("Cyrilll", error.toString());
                            }
                    ) {
                        @Override
                        public Map<String, String> getParams() throws AuthFailureError {
                            Map<String, String> params = new HashMap<>();
                            params.put("customer_id", customer_id);
                            params.put("provider_id", provider_id);
                            Realm.init(getActivity());
                            Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).executeTransaction(realm -> {
                                RealmResults<RealmChat> results = realm.where(RealmChat.class)
                                        .sort("id", Sort.DESCENDING)
                                        .equalTo("provider_id", provider_id)
                                        .equalTo("customer_id", customer_id)
                                        .findAll();
                                ArrayList<RealmChat> myArrayList = new ArrayList<>();
                                for (RealmChat realmChat : results) {
                                    if (!(realmChat.getChat_id().startsWith("z"))) {
                                        myArrayList.add(realmChat);
                                    }
                                }
                                if (results.size() < 3) {
                                    params.put("id", "0");
                                }
                                else{
                                    params.put("id", String.valueOf(myArrayList.get(0).getId()));
                                }
                            });
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
                } else {
                    final String[] provider_name = new String[1];
                    final String[] profile_image_url = new String[1];
                    final String[] availability = new String[1];

                    Realm.init(getActivity());
                    final RealmCart[] realmCart = {Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).where(RealmCart.class).equalTo("provider_id", provider_id).findFirst()};
                    provider_name[0] = realmCart[0].getProvider_title() != null && !realmCart[0].getProvider_title().equals("") ? StringUtils.normalizeSpace((realmCart[0].getProvider_title() + " " + realmCart[0].getProvider_first_name() + " " + realmCart[0].getProvider_other_name() + " " + realmCart[0].getProvider_last_name()).replace("null", "")) : realmCart[0].getProvider_name();
                    profile_image_url[0] = realmCart[0].getProvider_image_url();
                    availability[0] = realmCart[0].getProvider_availability();

                    StringRequest stringRequest = new StringRequest(
                            Request.Method.POST,
                            API_URL + "chat-data",
                            response -> {
                                if (response != null) {
                                    dialog.dismiss();
                                    try {
                                        JSONObject jsonObject = new JSONObject(response);
                                        Realm.init(getActivity());
                                        Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).executeTransaction(realm -> {
                                            try {
                                                realmCart[0] = realm.createOrUpdateObjectFromJson(RealmCart.class, jsonObject.getJSONObject("cart"));
                                                realm.createOrUpdateAllFromJson(RealmChat.class, jsonObject.getJSONArray("chats"));
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            provider_name[0] = realmCart[0].getProvider_title() != null && !realmCart[0].getProvider_title().equals("") ? StringUtils.normalizeSpace((realmCart[0].getProvider_title() + " " + realmCart[0].getProvider_first_name() + " " + realmCart[0].getProvider_other_name() + " " + realmCart[0].getProvider_last_name()).replace("null", "")) : realmCart[0].getProvider_name();
                                            profile_image_url[0] = realmCart[0].getProvider_image_url();
                                            availability[0] = realmCart[0].getProvider_availability();
                                        });

                                        startActivity(new Intent(getActivity(), MessageActivity.class)
                                                .putExtra("PROVIDER_ID", provider_id)
                                                .putExtra("PROVIDER_NAME", provider_name[0])
                                                .putExtra("PROFILE_IMAGE_URL", profile_image_url[0])
                                                .putExtra("AVAILABILITY", availability[0])
                                        );
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            },
                            error -> {
                                error.printStackTrace();
                                startActivity(new Intent(getActivity(), MessageActivity.class)
                                        .putExtra("PROVIDER_ID", provider_id)
                                        .putExtra("PROVIDER_NAME", provider_name[0])
                                        .putExtra("PROFILE_IMAGE_URL", profile_image_url[0])
                                        .putExtra("AVAILABILITY", availability[0])
                                );
                                dialog.dismiss();
                                Log.d("Cyrilll", error.toString());
                            }
                    ) {
                        @Override
                        public Map<String, String> getParams() throws AuthFailureError {
                            Map<String, String> params = new HashMap<>();
                            params.put("provider_id", provider_id);
                            params.put("customer_id", customer_id);
                            Realm.init(getActivity());
                            Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).executeTransaction(realm -> {
                                RealmResults<RealmChat> results = realm.where(RealmChat.class)
                                        .sort("id", Sort.DESCENDING)
                                        .equalTo("provider_id", provider_id)
                                        .equalTo("customer_id", customer_id)
                                        .findAll();
                                ArrayList<RealmChat> myArrayList = new ArrayList<>();
                                for (RealmChat realmChat : results) {
                                    if (!(realmChat.getChat_id().startsWith("z"))) {
                                        myArrayList.add(realmChat);
                                    }
                                }
                                if (results.size() < 3) {
                                    params.put("id", "0");
                                }
                                else{
                                    params.put("id", String.valueOf(myArrayList.get(0).getId()));
                                }
                            });
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
            }
        });

        call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallProviderMaterialDialog callProviderMaterialDialog = new CallProviderMaterialDialog();
                if (callProviderMaterialDialog != null && callProviderMaterialDialog.isAdded()) {

                } else {
                    String role = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("ROLE", "");
                    String primary_contact;
                    if (role.equals("CUSTOMER")) {
                        Realm.init(getActivity());
                        primary_contact = Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).where(RealmCustomer.class).findFirst().getPrimary_contact();
                    }
                    else {
                        primary_contact = Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).where(RealmProvider.class).equalTo("provider_id", PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("PROVIDER_ID", "")).findFirst().getPrimary_contact();
                    }
                    callProviderMaterialDialog.setPhone_number(primary_contact);
                    Realm.init(getActivity());
                    String customer_id = Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).where(RealmCustomer.class).findFirst().getCustomer_id();
                    callProviderMaterialDialog.setCustomer_id(customer_id);
                    callProviderMaterialDialog.setProvider_id(provider_id);
                    callProviderMaterialDialog.show(getFragmentManager(), "");
                }
            }
        });
        
        // doneBtn.setOnClickListener(doneAction);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // If you want to modify a view in your Activity
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    }
                });
            }
        }, 5);
        return builder.create();

    }


}