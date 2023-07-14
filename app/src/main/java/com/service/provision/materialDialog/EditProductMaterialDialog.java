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
import com.android.volley.Request;
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
import io.realm.RealmResults;

import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.myVolleyError;
import static com.service.provision.fragment.ProductsFragment.layoutManager;
import static com.service.provision.fragment.ProductsFragment.productAdapter;
import static com.service.provision.fragment.ProductsFragment.recyclerView;
import static com.service.provision.fragment.ProductsFragment.productArrayList;

public class EditProductMaterialDialog extends DialogFragment {
    String product_id, productnametext, descriptiontext, min_quantitytext, unit_quantitytext, unit_pricetext;

    public String getProduct_id() {
        return product_id;
    }

    public void setProduct_id(String product_id) {
        this.product_id = product_id;
    }

    public String getProductnametext() {
        return productnametext;
    }

    public void setProductnametext(String productnametext) {
        this.productnametext = productnametext;
    }

    public String getDescriptiontext() {
        return descriptiontext;
    }

    public void setDescriptiontext(String descriptiontext) {
        this.descriptiontext = descriptiontext;
    }

    public String getMin_quantitytext() {
        return min_quantitytext;
    }

    public void setMin_quantitytext(String min_quantitytext) {
        this.min_quantitytext = min_quantitytext;
    }

    public String getUnit_quantitytext() {
        return unit_quantitytext;
    }

    public void setUnit_quantitytext(String unit_quantitytext) {
        this.unit_quantitytext = unit_quantitytext;
    }

    public String getUnit_pricetext() {
        return unit_pricetext;
    }

    public void setUnit_pricetext(String unit_pricetext) {
        this.unit_pricetext = unit_pricetext;
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

        EditProductMaterialDialog.this.productname.setText(productnametext);
        EditProductMaterialDialog.this.description.setText(descriptiontext);
        EditProductMaterialDialog.this.min_quantity.setText(min_quantitytext.equals("-1") ? "" : min_quantitytext);
        EditProductMaterialDialog.this.unit_quantity.setText(unit_quantitytext.equals("-1") ? "" : unit_quantitytext);
        EditProductMaterialDialog.this.unit_price.setText(unit_pricetext.equals("-1") ? "" : unit_pricetext);
        
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextUtils.isEmpty(productname.getText())) {
                    productname.setError(getString(R.string.error_field_required));
                }
                else {
                    String productname = EditProductMaterialDialog.this.productname.getText().toString();
                    String description = EditProductMaterialDialog.this.description.getText().toString();
                    String min_quantity = EditProductMaterialDialog.this.min_quantity.getText().toString();
                    String unit_quantity = EditProductMaterialDialog.this.unit_quantity.getText().toString();
                    String unit_price = EditProductMaterialDialog.this.unit_price.getText().toString();
                    
                    ProgressDialog progressDialog = new ProgressDialog(getActivity());
                    progressDialog.setMessage("Please wait...");
                    progressDialog.setCancelable(false);
                    progressDialog.setIndeterminate(true);
                    progressDialog.show();
                    StringRequest stringRequest = new StringRequest(
                            Request.Method.PATCH,
                            API_URL + "products/" + product_id,
                            response -> {
                                progressDialog.dismiss();
                                if (response != null) {
                                    try {
                                        JSONObject jsonObject = new JSONObject(response);
                                        Realm.init(getActivity());
                                        Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).executeTransaction(new Realm.Transaction() {
                                            @Override
                                            public void execute(Realm realm) {
                                                realm.createOrUpdateObjectFromJson(RealmProduct.class, jsonObject);

                                                EditProductMaterialDialog.this.productname.setText(null);
                                                EditProductMaterialDialog.this.description.setText(null);
                                                EditProductMaterialDialog.this.min_quantity.setText(null);
                                                EditProductMaterialDialog.this.unit_quantity.setText(null);
                                                EditProductMaterialDialog.this.unit_price.setText(null);

                                                RealmResults<RealmProduct> services = realm.where(RealmProduct.class).equalTo("provider_id", PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("PROVIDER_ID", "")).findAll();
                                                productArrayList.clear();
                                                for (RealmProduct service : services) {
                                                    productArrayList.add(service);
                                                }

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