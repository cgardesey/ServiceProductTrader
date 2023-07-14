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
import com.service.provision.realm.RealmService;
import com.service.provision.realm.RealmServiceImage;
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

public class RatedServiceAdapter extends RecyclerView.Adapter<RatedServiceAdapter.ViewHolder> {

    private static final String YOUR_DIALOG_TAG = "";
    RatedServiceAdapterInterface ratedServiceAdapterInterface;
    Activity mActivity;
    private ArrayList<RealmService> realmServices;

    public RatedServiceAdapter(RatedServiceAdapterInterface ratedServiceAdapterInterface, Activity mActivity, ArrayList<RealmService> realmServices) {
        this.ratedServiceAdapterInterface = ratedServiceAdapterInterface;
        this.mActivity = mActivity;
        this.realmServices = realmServices;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycle_rated_service, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        RealmService realmService = realmServices.get(position);

        String[] split = realmService.getService_category().split((" >> "));

        if (realmService.getUrl() != null && !realmService.getUrl().equals("")) {
            Glide.with(mActivity).
                    load(realmService.getUrl())
                    .into(holder.image);
        }

//        holder.servicecategory.setText(split[split.length - 1]);
        holder.ratingbar.setRating(Float.parseFloat(realmService.getRating()));
        if (realmService.getRating() != null && !realmService.getRating().equals("0")) {
            holder.rating.setText(realmService.getRating());
        }
        else {
            holder.rating.setText("0.0");
        }
        if (realmService.getVerified() == 1) {
            holder.verifiedImage.setVisibility(View.VISIBLE);
        }
        else {
            holder.verifiedImage.setVisibility(View.GONE);
        }

        holder.totalrating.setText("(" + realmService.getTotal_rating() + " ratings)");
        Realm.init(mActivity);
        double distance = SphericalUtil.computeDistanceBetween(new LatLng(mActivity.getIntent().getDoubleExtra("LATITUDE", 0.0d), mActivity.getIntent().getDoubleExtra("LONGITUDE", 0.0d)), new LatLng(realmService.getLatitude(), realmService.getLongitude()));
        holder.distance.setText(String.format("%.2f", distance / 1000) + "km");
        holder.provider.setText(realmService.getProvider_name() != null && !realmService.getProvider_name().equals("") ? realmService.getProvider_name(): StringUtils.normalizeSpace((realmService.getTitle() + " " + realmService.getFirst_name() + " " + realmService.getOther_name() + " " + realmService.getLast_name()).replace("null", "")));
        holder.availability.setText(realmService.getAvailability());

        switch (realmService.getAvailability()) {
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
        holder.contact.setOnClickListener(view -> ratedServiceAdapterInterface.onListItemClick(realmServices, position, holder));

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
                                                                     String service_id = realmService.getService_id();
                                                                     StringRequest stringRequest = new StringRequest(
                                                                             Request.Method.POST,
                                                                             API_URL + "scoped-service-images",
                                                                             response -> {
                                                                                 mProgress.dismiss();
                                                                                 if (response != null) {
                                                                                     try {
                                                                                         JSONArray jsonArray = new JSONArray(response);
                                                                                         Realm.init(mActivity);
                                                                                         Realm.getInstance(RealmUtility.getDefaultConfig(mActivity)).executeTransaction(realm -> {
                                                                                             RealmResults<RealmServiceImage> serviceImages = realm.where(RealmServiceImage.class).findAll();
                                                                                             serviceImages.deleteAllFromRealm();
                                                                                             realm.createOrUpdateAllFromJson(RealmServiceImage.class, jsonArray);
                                                                                         });
                                                                                         try {
                                                                                             mActivity.startActivity(new Intent(mActivity, ImagesActivity.class)
                                                                                                     .putExtra("SERVICEID", realmService.getService_id())
                                                                                                     .putExtra("TITLE", realmService.getService_category())
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
                                                                             params.put("service_id", service_id);
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
    }

    @Override
    public int getItemCount() {
        return realmServices.size();
    }

    public interface RatedServiceAdapterInterface {
        void onListItemClick(ArrayList<RealmService> names, int position, ViewHolder holder);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView servicecategory, provider, rating, totalrating, distance, contact, availability;
        public LinearLayout parent, serviceInfoArea;
        public RatingBar ratingbar;
        public ImageView image, verifiedImage;

        public ViewHolder(View itemView) {
            super(itemView);
            parent = itemView.findViewById(R.id.parent);
            serviceInfoArea = itemView.findViewById(R.id.serviceInfoArea);
            servicecategory = itemView.findViewById(R.id.servicecategory);
            provider = itemView.findViewById(R.id.provider);
            totalrating = itemView.findViewById(R.id.total_rating);
            ratingbar = itemView.findViewById(R.id.ratingbar);
            rating = itemView.findViewById(R.id.rating);
            image = itemView.findViewById(R.id.image);
            verifiedImage = itemView.findViewById(R.id.verifiedImage);
            distance = itemView.findViewById(R.id.distance);
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
