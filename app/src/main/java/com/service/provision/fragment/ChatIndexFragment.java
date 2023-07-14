package com.service.provision.fragment;

import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.myVolleyError;

import android.app.Activity;
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

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.service.provision.R;
import com.service.provision.activity.MessageActivity;
import com.service.provision.adapter.ChatIndexAdapter;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmCart;
import com.service.provision.realm.RealmChat;
import com.service.provision.realm.RealmCustomer;
import com.service.provision.util.RealmUtility;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class ChatIndexFragment extends Fragment {
    static RecyclerView recyclerview;
    static TextView no_data;
    static ChatIndexAdapter chatIndexAdapter;
    static ArrayList<RealmChat> cartArrayList = new ArrayList<>();
    static ArrayList<RealmChat> newCart = new ArrayList<>();
    Activity activity;
    public static Activity chatIndexFragmentContext;

    public ChatIndexFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        chatIndexFragmentContext = getActivity();
        final View rootView = inflater.inflate(R.layout.fragment_chat_index, container, false);
        recyclerview = rootView.findViewById(R.id.recyclerview);
        no_data = rootView.findViewById(R.id.no_data);

        chatIndexAdapter = new ChatIndexAdapter(new ChatIndexAdapter.ChatIndexAdapterInterface() {

            @Override
            public void onItemClick(ArrayList<RealmChat> realmChats, int position, ChatIndexAdapter.ViewHolder holder) {
                RealmChat realmChat = realmChats.get(position);
                ProgressDialog dialog = new ProgressDialog(getActivity());
                dialog.setMessage("Please wait...");
                dialog.setCancelable(false);
                dialog.setIndeterminate(true);
                dialog.show();


                String role = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("ROLE", "");
                if (role.equals("PROVIDER")) {
                    final String[] customer_name = new String[1];
                    final String[] profile_image_url = new String[1];
                    String customer_id = realmChat.getCustomer_id();

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
                            params.put("provider_id", PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("PROVIDER_ID", ""));
                            Realm.init(getActivity());
                            Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).executeTransaction(realm -> {
                                RealmResults<RealmChat> results = realm.where(RealmChat.class)
                                        .sort("id", Sort.DESCENDING)
                                        .equalTo("provider_id", PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("PROVIDER_ID", ""))
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
                    String provider_id = realmChat.getProvider_id();

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
                            Realm.init(activity);
                            String customer_id = Realm.getInstance(RealmUtility.getDefaultConfig(activity)).where(RealmCustomer.class).findFirst().getCustomer_id();
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

            @Override
            public void onImageClick(ArrayList<RealmChat> realmChats, int position, ChatIndexAdapter.ViewHolder holder) {

            }
        }, getActivity(), cartArrayList);
        
        recyclerview.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerview.setNestedScrollingEnabled(false);
        recyclerview.setItemAnimator(new DefaultItemAnimator());
        recyclerview.setAdapter(chatIndexAdapter);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        populateChatIndex(activity);

        StringRequest stringRequest = new StringRequest(
                com.android.volley.Request.Method.POST,
                API_URL + "scoped-latest-chats",
                response -> {
                    if (response != null) {
                        try {
                            JSONArray jsonArray = new JSONArray(response);

                            Realm.init(getContext());
                            Realm.getInstance(RealmUtility.getDefaultConfig(getContext())).executeTransaction(realm -> {
                                realm.createOrUpdateAllFromJson(RealmChat.class, jsonArray);
                            });
                            populateChatIndex(activity);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                error -> {
                    error.printStackTrace();
                    Log.d("Cyrilll", error.toString());
                    myVolleyError(getContext(), error);
                }
        ) {
            @Override
            public Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                String role = PreferenceManager.getDefaultSharedPreferences(getContext()).getString("ROLE", "");

                if (role.equals("PROVIDER")) {
                    params.put("provider_id", PreferenceManager.getDefaultSharedPreferences(getContext()).getString("PROVIDER_ID", ""));
                } else {
                    Realm.init(getContext());
                    String customer_id = Realm.getInstance(RealmUtility.getDefaultConfig(getContext())).where(RealmCustomer.class).findFirst().getCustomer_id();
                    params.put("customer_id", customer_id);
                }
                return params;
            }

            /* Passing some request headers*/
            @Override
            public Map getHeaders() throws AuthFailureError {
                HashMap headers = new HashMap();
                headers.put("accept", "application/json");
                headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(getContext()).getString(APITOKEN, ""));
                return headers;
            }
        };
        ;
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        InitApplication.getInstance().addToRequestQueue(stringRequest);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }

    public static void populateChatIndex(final Context context) {
        Realm.init(context);
        Realm.getInstance(RealmUtility.getDefaultConfig(context)).executeTransaction(realm -> {
            RealmResults<RealmChat> results;

            String role = PreferenceManager.getDefaultSharedPreferences(context).getString("ROLE", "");
            if (role.equals("CUSTOMER")) {
                results = realm.where(RealmChat.class)
                        .sort("id", Sort.DESCENDING)
                        .distinct("provider_id")
                        .equalTo("customer_id", realm.where(RealmCustomer.class).findFirst().getCustomer_id())
                        .findAll();
            }
            else {
                results = realm.where(RealmChat.class)
                        .sort("id", Sort.DESCENDING)
                        .distinct("customer_id")
                        .equalTo("provider_id", PreferenceManager.getDefaultSharedPreferences(context).getString("PROVIDER_ID", ""))
                        .findAll();
            }

            if (results.size() < 1) {
                no_data.setVisibility(View.VISIBLE);
                recyclerview.setVisibility(View.GONE);
            }
            else {
                no_data.setVisibility(View.GONE);
                recyclerview.setVisibility(View.VISIBLE);
            }
            newCart.clear();
            for (RealmChat realmChat : results) {
                newCart.add(realmChat);
            }
            cartArrayList.clear();
            cartArrayList.addAll(newCart);
            chatIndexAdapter.notifyDataSetChanged();
        });
    }
}
