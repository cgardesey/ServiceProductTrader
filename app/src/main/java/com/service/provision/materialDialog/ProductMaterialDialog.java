package com.service.provision.materialDialog;

import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.myVolleyError;
import static com.service.provision.fragment.ProductsFragment.layoutManager;
import static com.service.provision.fragment.ProductsFragment.recyclerView;
import static com.service.provision.fragment.ProductsFragment.productAdapter;
import static com.service.provision.fragment.ProductsFragment.productArrayList;

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
import com.service.provision.activity.ImagesActivity;
import com.service.provision.activity.ProductCategoryActivity;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmProduct;
import com.service.provision.realm.RealmProductCategory;
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

public class ProductMaterialDialog extends DialogFragment {
    String product_id, productcategorytext, productnametext, descriptiontext, unit_quantitytext, quantity_availabletext, unit_pricetext;


    EditText productname, description, unit_quantity, unit_price, min_quantity, quantity_available;
    public TextView productcategory;
    Button ok;

    public String getProduct_id() {
        return product_id;
    }

    public void setProduct_id(String product_id) {
        this.product_id = product_id;
    }

    public String getProductcategorytext() {
        return productcategorytext;
    }

    public void setProductcategorytext(String productcategorytext) {
        this.productcategorytext = productcategorytext;
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

    public String getUnit_quantitytext() {
        return unit_quantitytext;
    }

    public void setUnit_quantitytext(String unit_quantitytext) {
        this.unit_quantitytext = unit_quantitytext;
    }

    public String getQuantity_availabletext() {
        return quantity_availabletext;
    }

    public void setQuantity_availabletext(String quantity_availabletext) {
        this.quantity_availabletext = quantity_availabletext;
    }

    public String getUnit_pricetext() {
        return unit_pricetext;
    }

    public void setUnit_pricetext(String unit_pricetext) {
        this.unit_pricetext = unit_pricetext;
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.list_item_product, null);
        productcategory = view.findViewById(R.id.productcategory);
        productname = view.findViewById(R.id.productname);
        description = view.findViewById(R.id.description);
        unit_quantity = view.findViewById(R.id.unit_quantity);
        min_quantity = view.findViewById(R.id.min_quantity);
        quantity_available = view.findViewById(R.id.quantity_available);
        unit_price = view.findViewById(R.id.unit_price);
        ok = view.findViewById(R.id.ok);

        ProductMaterialDialog.this.productcategory.setText(productcategorytext);
        ProductMaterialDialog.this.productname.setText(productnametext);
        ProductMaterialDialog.this.description.setText(descriptiontext);
        ProductMaterialDialog.this.unit_quantity.setText(unit_quantitytext != null && unit_quantitytext.equals("-1") ? "" : unit_quantitytext);
        ProductMaterialDialog.this.unit_price.setText(unit_pricetext != null && unit_pricetext.equals("-1") ? "" : unit_pricetext);
        ProductMaterialDialog.this.quantity_available.setText(quantity_availabletext != null && quantity_availabletext.equals("-1") ? "" : quantity_availabletext);
        

        productcategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProgressDialog dialog = new ProgressDialog(getActivity());
                dialog.setMessage("Please wait...");
                dialog.setCancelable(false);
                dialog.setIndeterminate(true);
                dialog.show();
                StringRequest stringRequest = new StringRequest(
                        Request.Method.GET,
                        API_URL + "product-categories",
                        response -> {
                            if (response != null) {
                                dialog.dismiss();
                                try {
                                    JSONArray jsonArray = new JSONArray(response);
                                    Realm.init(getActivity());
                                    Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).executeTransaction(new Realm.Transaction() {
                                        @Override
                                        public void execute(Realm realm) {
                                            RealmResults<RealmProductCategory> realmProductCategories = realm.where(RealmProductCategory.class).findAll();
                                            realmProductCategories.deleteAllFromRealm();

                                            realm.createOrUpdateAllFromJson(RealmProductCategory.class, jsonArray);
                                        }
                                    });
                                    getActivity().startActivityForResult(new Intent(getContext(), ProductCategoryActivity.class), 1915);
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
                    String productcategory = ProductMaterialDialog.this.productcategory.getText().toString();
                    String productname = ProductMaterialDialog.this.productname.getText().toString();
                    String description = ProductMaterialDialog.this.description.getText().toString();
                    String unit_quantity = ProductMaterialDialog.this.unit_quantity.getText().toString();
                    String quantity_available = ProductMaterialDialog.this.quantity_available.getText().toString();
                    String unit_price = ProductMaterialDialog.this.unit_price.getText().toString();

                    ProgressDialog progressDialog = new ProgressDialog(getActivity());
                    progressDialog.setMessage("Please wait...");
                    progressDialog.setCancelable(false);
                    progressDialog.setIndeterminate(true);
                    progressDialog.show();
                    String url = API_URL + "products";
                    int methodType = Request.Method.POST;
                    if (product_id != null && !product_id.equals("")) {
                        url += "/" + product_id;
                        methodType = Request.Method.PATCH;
                    }

                    int finalMethodType = methodType;
                    StringRequest stringRequest = new StringRequest(
                            methodType,
                            url,
                            response -> {
                                progressDialog.dismiss();
                                if (response != null) {
                                    try {
                                        final RealmProduct[] realmProduct = new RealmProduct[1];

                                        JSONObject jsonObject = new JSONObject(response);
                                        Realm.init(getActivity());
                                        Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).executeTransaction(new Realm.Transaction() {
                                            @Override
                                            public void execute(Realm realm) {
                                                realmProduct[0] = realm.createOrUpdateObjectFromJson(RealmProduct.class, jsonObject);

                                                ProductMaterialDialog.this.productname.setText(null);
                                                ProductMaterialDialog.this.description.setText(null);
                                                ProductMaterialDialog.this.unit_quantity.setText(null);
                                                ProductMaterialDialog.this.unit_price.setText(null);

                                                RealmResults<RealmProduct> products = realm.where(RealmProduct.class).equalTo("provider_id", PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("PROVIDER_ID", "")).findAll();
                                                productArrayList.clear();
                                                for (RealmProduct product : products) {
                                                    productArrayList.add(product);
                                                }

                                                recyclerView.setLayoutManager(layoutManager);
                                                recyclerView.setHasFixedSize(true);
                                                recyclerView.setItemAnimator(new DefaultItemAnimator());
                                                recyclerView.setAdapter(productAdapter);
//                                                getActivity().startActivityForResult(getActivity().getIntent(), 10);

                                                if (finalMethodType == Request.Method.POST) {
                                                    startActivity(new Intent(getContext(), ImagesActivity.class)
                                                            .putExtra("PRODUCTID", realmProduct[0].getProduct_id())
                                                            .putExtra("TITLE", realmProduct[0].getProduct_category())
                                                    );
                                                }
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
                            params.put("product_category", productcategory);
                            params.put("name", productname);
                            params.put("description", description);
                            params.put("unit_quantity", unit_quantity.equals("") ? "-1" : unit_quantity);
                            params.put("quantity_available", quantity_available.equals("") ? "-1" : quantity_available);
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

    public boolean validate() {
        boolean validated = true;

        /*if (TextUtils.isEmpty(productcategory.getText())) {
            productcategory.setError(getString(R.string.error_field_required));
            validated = false;
        }*/
        if (TextUtils.isEmpty(productcategory.getText())) {
            productcategory.setError(getString(R.string.error_field_required));
            validated = false;
        }
        if (TextUtils.isEmpty(unit_price.getText())) {
            unit_price.setError(getString(R.string.error_field_required));
            validated = false;
        }
        if (TextUtils.isEmpty(unit_quantity.getText())) {
            unit_quantity.setError(getString(R.string.error_field_required));
            validated = false;
        }
        if (TextUtils.isEmpty(quantity_available.getText())) {
            quantity_available.setError(getString(R.string.error_field_required));
            validated = false;
        }
        return validated;
    }
}