package com.service.provision.materialDialog;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DefaultItemAnimator;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.service.provision.R;
import com.service.provision.activity.ServiceCategoryActivity;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmService;
import com.service.provision.realm.RealmServiceCategory;
import com.service.provision.util.RealmUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.realm.Realm;
import io.realm.RealmResults;

import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.myVolleyError;
import static com.service.provision.fragment.ServicesFragment.layoutManager;
import static com.service.provision.fragment.ServicesFragment.serviceAdapter;
import static com.service.provision.fragment.ServicesFragment.serviceArrayList;
import static com.service.provision.fragment.ServicesFragment.recyclerView;

public class ServiceMaterialDialog extends DialogFragment {
    String service_id, servicecategorytext, servicenametext, descriptiontext, min_charge_amounttext, max_charge_amounttext;


    EditText servicename, description, min_charge_amount, max_charge_amount;
    public TextView servicecategory;
    Button ok;

    public String getService_id() {
        return service_id;
    }

    public void setService_id(String service_id) {
        this.service_id = service_id;
    }

    public String getServicecategorytext() {
        return servicecategorytext;
    }

    public void setServicecategorytext(String servicecategorytext) {
        this.servicecategorytext = servicecategorytext;
    }

    public String getServicenametext() {
        return servicenametext;
    }

    public void setServicenametext(String servicenametext) {
        this.servicenametext = servicenametext;
    }

    public String getDescriptiontext() {
        return descriptiontext;
    }

    public void setDescriptiontext(String descriptiontext) {
        this.descriptiontext = descriptiontext;
    }

    public String getMin_charge_amounttext() {
        return min_charge_amounttext;
    }

    public void setMin_charge_amounttext(String min_charge_amounttext) {
        this.min_charge_amounttext = min_charge_amounttext;
    }

    public String getMax_charge_amounttext() {
        return max_charge_amounttext;
    }

    public void setMax_charge_amounttext(String max_charge_amounttext) {
        this.max_charge_amounttext = max_charge_amounttext;
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.list_item_service, null);
        servicecategory = view.findViewById(R.id.servicecategory);
        servicename = view.findViewById(R.id.servicename);
        description = view.findViewById(R.id.description);
        min_charge_amount = view.findViewById(R.id.min_charge_amount);
        max_charge_amount = view.findViewById(R.id.max_charge_amount);
        ok = view.findViewById(R.id.ok);

        ServiceMaterialDialog.this.servicecategory.setText(servicecategorytext);
        ServiceMaterialDialog.this.servicename.setText(servicenametext);
        ServiceMaterialDialog.this.description.setText(descriptiontext);
        ServiceMaterialDialog.this.min_charge_amount.setText(min_charge_amounttext != null && min_charge_amounttext.equals("-1.00") ? "" : min_charge_amounttext);
        ServiceMaterialDialog.this.max_charge_amount.setText(max_charge_amounttext != null && max_charge_amounttext.equals("-1.00") ? "" : max_charge_amounttext);

        servicecategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProgressDialog dialog = new ProgressDialog(getActivity());
                dialog.setMessage("Please wait...");
                dialog.setCancelable(false);
                dialog.setIndeterminate(true);
                dialog.show();
                StringRequest stringRequest = new StringRequest(
                        Request.Method.GET,
                        API_URL + "service-categories",
                        response -> {
                            if (response != null) {
                                dialog.dismiss();
                                try {
                                    JSONArray jsonArray = new JSONArray(response);
                                    Realm.init(getActivity());
                                    Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).executeTransaction(new Realm.Transaction() {
                                        @Override
                                        public void execute(Realm realm) {
                                            RealmResults<RealmServiceCategory> realmServiceCategories = realm.where(RealmServiceCategory.class).findAll();
                                            realmServiceCategories.deleteAllFromRealm();

                                            realm.createOrUpdateAllFromJson(RealmServiceCategory.class, jsonArray);
                                        }
                                    });
                                    getActivity().startActivityForResult(new Intent(getContext(), ServiceCategoryActivity.class), 1914);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        error -> {
                            dialog.dismiss();
                            myVolleyError(getActivity(), error);
                        }
                ) {
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
        });

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (validate()) {
                    String servicecategory = ServiceMaterialDialog.this.servicecategory.getText().toString();
                    String servicename = ServiceMaterialDialog.this.servicename.getText().toString();
                    String description = ServiceMaterialDialog.this.description.getText().toString();
                    String min_charge_amount = ServiceMaterialDialog.this.min_charge_amount.getText().toString();
                    String max_charge_amount = ServiceMaterialDialog.this.max_charge_amount.getText().toString();

                    ProgressDialog progressDialog = new ProgressDialog(getActivity());
                    progressDialog.setMessage("Please wait...");
                    progressDialog.setCancelable(false);
                    progressDialog.setIndeterminate(true);
                    progressDialog.show();
                    String url = API_URL + "services";
                    int methodType = Request.Method.POST;
                    if (service_id != null && !service_id.equals("")) {
                        url += "/" + service_id;
                        methodType = Request.Method.PATCH;
                    }

                    StringRequest stringRequest = new StringRequest(
                            methodType,
                            url,
                            response -> {
                                progressDialog.dismiss();
                                if (response != null) {
                                    try {
                                        JSONObject jsonObject = new JSONObject(response);
                                        Realm.init(getActivity());
                                        Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).executeTransaction(new Realm.Transaction() {
                                            @Override
                                            public void execute(Realm realm) {
                                                realm.createOrUpdateObjectFromJson(RealmService.class, jsonObject);

                                                ServiceMaterialDialog.this.servicename.setText(null);
                                                ServiceMaterialDialog.this.description.setText(null);
                                                ServiceMaterialDialog.this.min_charge_amount.setText(null);
                                                ServiceMaterialDialog.this.max_charge_amount.setText(null);

                                                RealmResults<RealmService> services = realm.where(RealmService.class).equalTo("provider_id", PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("PROVIDER_ID", "")).findAll();
                                                serviceArrayList.clear();
                                                for (RealmService service : services) {
                                                    serviceArrayList.add(service);
                                                }

                                                recyclerView.setLayoutManager(layoutManager);
                                                recyclerView.setHasFixedSize(true);
                                                recyclerView.setItemAnimator(new DefaultItemAnimator());
                                                recyclerView.setAdapter(serviceAdapter);
                                                getActivity().startActivityForResult(getActivity().getIntent(), 10);
                                                dismiss();
                                            }
                                        });

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            },
                            error -> {
                                error.printStackTrace();
                                Log.d("Cyrilll", error.toString());
                                progressDialog.dismiss();
                                myVolleyError(getActivity(), error);
                            }
                    ) {
                        @Override
                        public Map<String, String> getParams() throws AuthFailureError {
                            Map<String, String> params = new HashMap<>();
                            params.put("service_category", servicecategory);
                            params.put("name", servicename);
                            params.put("description", description);
                            params.put("min_charge_amount", min_charge_amount.equals("") ? "-1" : min_charge_amount);
                            params.put("max_charge_amount", max_charge_amount.equals("") ? "-1" : max_charge_amount);
                            params.put("provider_id", PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("PROVIDER_ID", ""));
                            return params;
                        }

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

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

    }

    public boolean validate() {
        boolean validated = true;

        /*if (TextUtils.isEmpty(servicename.getText())) {
            servicename.setError(getString(R.string.error_field_required));
            validated = false;
        }*/
        if (TextUtils.isEmpty(servicecategory.getText())) {
            servicecategory.setError(getString(R.string.error_field_required));
            validated = false;
        }
        return validated;
    }
}