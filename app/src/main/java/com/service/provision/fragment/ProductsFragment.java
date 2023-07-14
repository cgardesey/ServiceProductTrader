package com.service.provision.fragment;

import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.myVolleyError;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.greysonparrelli.permiso.Permiso;
import com.service.provision.R;
import com.service.provision.activity.ImagesActivity;
import com.service.provision.adapter.ProductAdapter;
import com.service.provision.materialDialog.ProductMaterialDialog;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmProduct;
import com.service.provision.realm.RealmProductImage;
import com.service.provision.util.RealmUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by 2CLearning on 12/13/2017.
 */

public class ProductsFragment extends Fragment {
    private static final String TAG = "PersonalProviderAccountFragment6";
    Button add, remove;
    public static RecyclerView recyclerView;
    Context mContext;
    LinearLayout clickToAdd;
    CardView cardView;
    public static ArrayList<RealmProduct> productArrayList;
    public static ProductAdapter productAdapter;
    public static RecyclerView.LayoutManager layoutManager;
    public static ProductMaterialDialog productMaterialDialog = new ProductMaterialDialog();
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_products, container, false);


        productArrayList = new ArrayList<>();
        mContext= getContext();
        cardView = rootView.findViewById(R.id.cardView);
        clickToAdd = rootView.findViewById(R.id.clickToAdd);
        add = rootView.findViewById(R.id.add);
        remove = rootView.findViewById(R.id.remove);
        recyclerView = rootView.findViewById(R.id.recyclerView);

        layoutManager = new LinearLayoutManager(getActivity());

        init();

        clickToAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(productMaterialDialog != null && productMaterialDialog.isAdded()) {

                } else {
                    productMaterialDialog.setCancelable(false);
                    productMaterialDialog.show(getFragmentManager(), "addProductMaterialDialog");
                    productMaterialDialog.setCancelable(true);
                }
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        init();
    }

    @Override
    public void onResume() {
        super.onResume();

        productAdapter.notifyDataSetChanged();

        final boolean[] imageSet = new boolean[1];
        imageSet[0] = true;
        final String[] product_category = new String[1];
        final String[] product_id = new String[1];
        Realm.init(getContext());
        Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).executeTransaction(realm -> {

            RealmResults<RealmProduct> realmProducts = realm.where(RealmProduct.class).equalTo("provider_id", PreferenceManager.getDefaultSharedPreferences(mContext).getString("PROVIDER_ID", "")).findAll();
            for (RealmProduct realmProduct : realmProducts) {
                imageSet[0] = realm.where(RealmProductImage.class).equalTo("product_id", realmProduct.getProduct_id()).findAll().size() > 0;
                if (!imageSet[0]) {
                    product_category[0] = realmProduct.getProduct_category();
                    product_id[0] = realmProduct.getProduct_id();
                    break;
                }
            }
        });
        if (!imageSet[0]) {
            startActivity(new Intent(getContext(), ImagesActivity.class)
                    .putExtra("PRODUCTID", product_id[0])
                    .putExtra("TITLE", product_category[0])
            );
        }
    }

    public void init() {
        Realm.init(getContext());
        Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).executeTransaction(realm -> {

            RealmResults<RealmProduct> realmProducts = realm.where(RealmProduct.class).equalTo("provider_id", PreferenceManager.getDefaultSharedPreferences(mContext).getString("PROVIDER_ID", "")).findAll();
            productArrayList.clear();
            for (RealmProduct realmProduct : realmProducts) {
               productArrayList.add(realmProduct);
            }
        });

        productAdapter = new ProductAdapter(new ProductAdapter.ProductAdapterInterface() {
            @Override
            public void onListItemClick(ArrayList<RealmProduct> realmProducts, int position, ProductAdapter.ViewHolder holder) {
                RealmProduct realmProduct = realmProducts.get(position);

                PopupMenu popup = new PopupMenu(mContext, holder.more_details);

                popup.inflate(R.menu.product_menu);

                popup.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.images) {
                        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
                                                                     @Override
                                                                     public void onPermissionResult(Permiso.ResultSet resultSet) {
                                                                         if (resultSet.isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE) && resultSet.isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                                                             ProgressDialog mProgress = new ProgressDialog(getActivity());
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
                                                                                                 Realm.init(getContext());
                                                                                                 Realm.getInstance(RealmUtility.getDefaultConfig(getContext())).executeTransaction(realm -> {
                                                                                                     RealmResults<RealmProductImage> productImages = realm.where(RealmProductImage.class).equalTo("product_id", product_id).findAll();
                                                                                                     productImages.deleteAllFromRealm();
                                                                                                     realm.createOrUpdateAllFromJson(RealmProductImage.class, jsonArray);
                                                                                                 });
                                                                                                 try {
                                                                                                     startActivity(new Intent(getContext(), ImagesActivity.class)
                                                                                                             .putExtra("PRODUCTID", product_id)
                                                                                                             .putExtra("TITLE", realmProduct.getProduct_category())
                                                                                                     );
                                                                                                 } catch (Exception e) {
                                                                                                     e.printStackTrace();
                                                                                                 }
                                                                                             } catch (JSONException e) {
                                                                                                 e.printStackTrace();
                                                                                             }
                                                                                         }
                                                                                     },
                                                                                     error -> {
                                                                                         mProgress.dismiss();
                                                                                         error.printStackTrace();
                                                                                         myVolleyError(mContext, error);
                                                                                         Log.d("Cyrilll", error.toString());
                                                                                     }
                                                                             ) {
                                                                                 @Override
                                                                                 public Map getHeaders() throws AuthFailureError {
                                                                                     HashMap headers = new HashMap();
                                                                                     headers.put("accept", "application/json");
                                                                                     headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(mContext).getString(APITOKEN, ""));
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
                                                                         Permiso.getInstance().showRationaleInDialog(getActivity().getString(R.string.permissions), getActivity().getString(R.string.this_permission_is_mandatory_pls_allow_access), null, callback);
                                                                     }
                                                                 },
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        return true;
                    } else if (itemId == R.id.edit) {
                        ProductMaterialDialog productMaterialDialog = new ProductMaterialDialog();
                        if (productMaterialDialog != null && productMaterialDialog.isAdded()) {

                        } else {
                            productMaterialDialog.setProduct_id(realmProduct.getProduct_id());
                            productMaterialDialog.setProductcategorytext(realmProduct.getProduct_category());
                            productMaterialDialog.setProductnametext(realmProduct.getName());
                            productMaterialDialog.setDescriptiontext(realmProduct.getDescription());
                            productMaterialDialog.setUnit_pricetext(String.format("%.2f", realmProduct.getUnit_price()));
                            productMaterialDialog.setUnit_quantitytext(String.valueOf(realmProduct.getUnit_quantity()));
                            productMaterialDialog.setQuantity_availabletext(String.valueOf(realmProduct.getQuantity_available()));

                            productMaterialDialog.setCancelable(false);
                            productMaterialDialog.show(getFragmentManager(), "editProductMaterialDialog");
                            productMaterialDialog.setCancelable(true);
                        }
                        return true;
                    } else if (itemId == R.id.remove) {
                        String product_id = realmProduct.getProduct_id();
                        StringRequest stringRequestDelete = new StringRequest(
                                Request.Method.DELETE,
                                API_URL + "products/" + realmProduct.getProduct_id(),
                                response -> {
                                    if (response != null) {
                                        try {
                                            JSONObject jsonObject = new JSONObject(response);
                                            if (jsonObject.getBoolean("status")) {
                                                Realm.init(getActivity());
                                                Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).executeTransaction(realm -> {
                                                    realmProducts.get(position).deleteFromRealm();
                                                });
                                                realmProducts.remove(position);
                                                productAdapter.notifyDataSetChanged();
                                                Toast.makeText(mContext, "Successfully deleted.", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(mContext, "Error deleting.", Toast.LENGTH_SHORT).show();
                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                },
                                error -> {
                                    error.printStackTrace();
                                    myVolleyError(mContext, error);
                                    Log.d("Cyrilll", error.toString());
                                }
                        ) {
                            @Override
                            public Map getHeaders() throws AuthFailureError {
                                HashMap headers = new HashMap();
                                headers.put("accept", "application/json");
                                headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(mContext).getString(APITOKEN, ""));
                                return headers;
                            }

                            @Override
                            public Map<String, String> getParams() throws AuthFailureError {
                                Map<String, String> params = new HashMap<>();
                                params.put("product_id", product_id);
                                return params;
                            }
                        };
                        stringRequestDelete.setRetryPolicy(new DefaultRetryPolicy(
                                0,
                                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                        InitApplication.getInstance().addToRequestQueue(stringRequestDelete);
                        return true;
                    }
                    return false;
                });
                popup.show();
            }
        }, productArrayList);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        //  myrecyclerview.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(productAdapter);
    }

    public boolean validate (){
        boolean validated = true;
        return validated;
    }
}