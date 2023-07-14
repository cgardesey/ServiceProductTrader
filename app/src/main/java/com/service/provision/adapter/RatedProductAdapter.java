package com.service.provision.adapter;

/**
 * Created by Nana on 11/10/2017.
 */

import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.myVolleyError;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import com.greysonparrelli.permiso.Permiso;
import com.service.provision.R;
import com.service.provision.activity.ImagesActivity;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmProduct;
import com.service.provision.realm.RealmProductImage;
import com.service.provision.util.RealmUtility;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by Belal on 6/6/2017.
 */

public class RatedProductAdapter extends RecyclerView.Adapter<RatedProductAdapter.ViewHolder> {

    private static final String YOUR_DIALOG_TAG = "";
    ContactMethodAdapterInterface contactMethodAdapterInterface;
    AddToCartAdapterInterface addToCartAdapterInterface;
    Activity mActivity;
    private ArrayList<RealmProduct> realmProducts;

    public RatedProductAdapter(ContactMethodAdapterInterface contactMethodAdapterInterface, AddToCartAdapterInterface addToCartAdapterInterface, Activity mActivity, ArrayList<RealmProduct> realmProducts) {
        this.contactMethodAdapterInterface = contactMethodAdapterInterface;
        this.addToCartAdapterInterface = addToCartAdapterInterface;
        this.mActivity = mActivity;
        this.realmProducts = realmProducts;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycle_rated_product, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        RealmProduct realmProduct = realmProducts.get(position);

        String[] split = realmProduct.getProduct_category().split((" >> "));

        if (realmProduct.getUrl() != null && !realmProduct.getUrl().equals("")) {
            Glide.with(mActivity).
                    load(realmProduct.getUrl())
                    .into(holder.image);
        }

        holder.ratingbar.setRating(Float.parseFloat(realmProduct.getRating()));
        if (realmProduct.getRating() != null && !realmProduct.getRating().equals("0")) {
            holder.rating.setText(realmProduct.getRating());
        }
        else {
            holder.rating.setText("0.0");
        }
        if (realmProduct.getVerified() == 1) {
            holder.verifiedImage.setVisibility(View.VISIBLE);
        }
        else {
            holder.verifiedImage.setVisibility(View.GONE);
        }

        holder.totalrating.setText("(" + realmProduct.getTotal_rating() + " ratings)");
        Realm.init(mActivity);
        double distance = SphericalUtil.computeDistanceBetween(new LatLng(mActivity.getIntent().getDoubleExtra("LATITUDE", 0.0d), mActivity.getIntent().getDoubleExtra("LONGITUDE", 0.0d)), new LatLng(realmProduct.getLatitude(), realmProduct.getLongitude()));
        holder.distance.setText(String.format("%.2f", distance / 1000) + "km");
        holder.provider.setText(realmProduct.getProvider_name() != null && !realmProduct.getProvider_name().equals("") ? realmProduct.getProvider_name(): StringUtils.normalizeSpace((realmProduct.getTitle() + " " + realmProduct.getFirst_name() + " " + realmProduct.getOther_name() + " " + realmProduct.getLast_name()).replace("null", "")));
        holder.availability.setText(realmProduct.getAvailability());

        switch (realmProduct.getAvailability()) {
            case "Closed":
                holder.availability.setTextColor(Color.RED);
                break;
            case "Busy":
                holder.availability.setTextColor(0xFFDAA520);
                break;
            case "Available":
                holder.availability.setTextColor(0xFF32CD32);
                break;
            default:
                break;
        }
        holder.addtocart.setOnClickListener(view -> addToCartAdapterInterface.onListItemClick(realmProducts, position, holder));

        holder.image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
                                                             @Override
                                                             public void onPermissionResult(Permiso.ResultSet resultSet) {
                                                                 if (resultSet.isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE) && resultSet.isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                                                     ProgressDialog mProgress = new ProgressDialog(mActivity);
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
                                                                                         Realm.init(mActivity);
                                                                                         Realm.getInstance(RealmUtility.getDefaultConfig(mActivity)).executeTransaction(realm -> {
                                                                                             RealmResults<RealmProductImage> productImages = realm.where(RealmProductImage.class).findAll();
                                                                                             productImages.deleteAllFromRealm();
                                                                                             realm.createOrUpdateAllFromJson(RealmProductImage.class, jsonArray);
                                                                                         });
                                                                                         try {
                                                                                             mActivity.startActivity(new Intent(mActivity, ImagesActivity.class)
                                                                                                     .putExtra("PRODUCTID", realmProduct.getProduct_id())
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
                                                                                 myVolleyError(mActivity, error);
                                                                                 Log.d("Cyrilll", error.toString());
                                                                             }
                                                                     ) {
                                                                         @Override
                                                                         public Map getHeaders() throws AuthFailureError {
                                                                             HashMap headers = new HashMap();
                                                                             headers.put("accept", "application/json");
                                                                             headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(mActivity).getString(APITOKEN, ""));
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
                                                                 Permiso.getInstance().showRationaleInDialog(mActivity.getString(R.string.permissions), mActivity.getString(R.string.this_permission_is_mandatory_pls_allow_access), null, callback);
                                                             }
                                                         },
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        });

        holder.contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                contactMethodAdapterInterface.onListItemClick(realmProducts, position, holder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return realmProducts.size();
    }

    public interface ContactMethodAdapterInterface {
        void onListItemClick(ArrayList<RealmProduct> realmProducts, int position, ViewHolder holder);
    }

    public interface AddToCartAdapterInterface {
        void onListItemClick(ArrayList<RealmProduct> realmProducts, int position, ViewHolder holder);
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView productcategory, provider, rating, totalrating, distance, contact, availability;
        public LinearLayout parent, productInfoArea;
        public RatingBar ratingbar;
        public ImageView image, verifiedImage;
        public Button addtocart;

        public ViewHolder(View itemView) {
            super(itemView);
            parent = itemView.findViewById(R.id.parent);
            productInfoArea = itemView.findViewById(R.id.productInfoArea);
            productcategory = itemView.findViewById(R.id.productcategory);
            provider = itemView.findViewById(R.id.provider);
            totalrating = itemView.findViewById(R.id.total_rating);
            ratingbar = itemView.findViewById(R.id.ratingbar);
            rating = itemView.findViewById(R.id.rating);
            image = itemView.findViewById(R.id.image);
            verifiedImage = itemView.findViewById(R.id.verifiedImage);
            distance = itemView.findViewById(R.id.distance);
            addtocart = itemView.findViewById(R.id.addtocart);
            contact = itemView.findViewById(R.id.contact);
            availability = itemView.findViewById(R.id.availability);
        }
    }

    public double CalculationByDistance(LatLng StartP, LatLng EndP) {
        int Radius = 6371;// radius of earth in Km
        double lat1 = StartP.latitude;
        double lat2 = EndP.latitude;
        double lon1 = StartP.longitude;
        double lon2 = EndP.longitude;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double valueResult = Radius * c;
        double km = valueResult / 1;
        DecimalFormat newFormat = new DecimalFormat("####");
        int kmInDec = Integer.valueOf(newFormat.format(km));
        double meter = valueResult % 1000;
        int meterInDec = Integer.valueOf(newFormat.format(meter));
        Log.i("Radius Value", "" + valueResult + "   KM  " + kmInDec
                + " Meter   " + meterInDec);

        return Radius * c;
    }
}
