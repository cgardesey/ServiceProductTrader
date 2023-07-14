package com.service.provision.materialDialog;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DefaultItemAnimator;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.toolbox.StringRequest;
import com.service.provision.R;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmProduct;
import com.service.provision.util.RealmUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.realm.Realm;

import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.myVolleyError;
import static com.service.provision.fragment.ProductsFragment.layoutManager;
import static com.service.provision.fragment.ProductsFragment.recyclerView;
import static com.service.provision.fragment.ProductsFragment.productAdapter;
import static com.service.provision.fragment.ProductsFragment.productArrayList;

public class AddProductMaterialDialog extends DialogFragment {
    String provider_id;

    public String getProvider_id() {
        return provider_id;
    }

    public void setProvider_id(String provider_id) {
        this.provider_id = provider_id;
    }

    EditText productname, description, min_quantity, unit_quantity, unit_price;
    Button ok;

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.list_item_add_product,null);
        productname = view.findViewById(R.id.productname);
        description = view.findViewById(R.id.description);
        min_quantity = view.findViewById(R.id.min_quantity);
        unit_quantity = view.findViewById(R.id.unit_quantity);
        unit_price = view.findViewById(R.id.unit_price);
        ok = view.findViewById(R.id.ok);

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextUtils.isEmpty(productname.getText())) {
                    productname.setError(getString(R.string.error_field_required));
                }
                else {
                    String productname = AddProductMaterialDialog.this.productname.getText().toString();
                    String description = AddProductMaterialDialog.this.description.getText().toString();
                    String min_quantity = AddProductMaterialDialog.this.min_quantity.getText().toString();
                    String unit_quantity = AddProductMaterialDialog.this.unit_quantity.getText().toString();
                    String unit_price = AddProductMaterialDialog.this.unit_price.getText().toString();

                    ProgressDialog progressDialog = new ProgressDialog(getActivity());
                    progressDialog.setMessage("Please wait...");
                    progressDialog.setCancelable(false);
                    progressDialog.setIndeterminate(true);
                    progressDialog.show();
                    StringRequest stringRequest = new StringRequest(
                            com.android.volley.Request.Method.POST,
                            API_URL + "products",
                            response -> {
                                progressDialog.dismiss();
                                if (response != null) {
                                    try {
                                        JSONObject jsonObject = new JSONObject(response);
                                        Realm.init(getActivity());
                                        Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).executeTransaction(new Realm.Transaction() {
                                            @Override
                                            public void execute(Realm realm) {
                                                RealmProduct realmProduct = realm.createOrUpdateObjectFromJson(RealmProduct.class, jsonObject);

                                                AddProductMaterialDialog.this.productname.setText(null);
                                                AddProductMaterialDialog.this.description.setText(null);
                                                AddProductMaterialDialog.this.min_quantity.setText(null);
                                                AddProductMaterialDialog.this.unit_quantity.setText(null);
                                                AddProductMaterialDialog.this.unit_price.setText(null);

                                                productArrayList.add(realmProduct);
                                                recyclerView.setLayoutManager(layoutManager);
                                                recyclerView.setHasFixedSize(true);
                                                recyclerView.setItemAnimator(new DefaultItemAnimator());
                                                recyclerView.setAdapter(productAdapter);
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
                            params.put("name", productname);
                            params.put("description", description);
                            params.put("min_quantity", min_quantity.equals("") ? "-1" : min_quantity);
                            params.put("unit_quantity", unit_quantity.equals("") ? "-1" : unit_quantity);
                            params.put("unit_price", unit_price.equals("") ? "-1" : unit_price);
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
}