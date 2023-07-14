package com.service.provision.fragment;

import static androidx.core.content.ContextCompat.checkSelfPermission;
import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.activity.ProviderHomeActivity.homeactivity;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.myVolleyError;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.service.provision.R;
import com.service.provision.activity.MyProductListActivity;
import com.service.provision.activity.ProductsActivity;
import com.service.provision.activity.SearchProductsActivity;
import com.service.provision.adapter.ProductListAdapter;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmBanner;
import com.service.provision.realm.RealmProduct;
import com.service.provision.realm.RealmProductCategory;
import com.service.provision.util.RealmUtility;
import com.service.provision.util.carousel.ViewPagerCarouselView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;


public class SearchProductFragment extends Fragment {
    ArrayList<RealmBanner> realmBannerArrayList = new ArrayList<>();
    ArrayList<RealmProductCategory> realmProductCategories = new ArrayList<>();
    RecyclerView recyclerView;
    private ShimmerFrameLayout shimmer_view_container;

    static ViewPagerCarouselView viewPagerCarouselView;
    public static RelativeLayout searchlayout;
    public static LinearLayout error_loading;
    ProductListAdapter listAdapter;
    Button retrybtn;
    FrameLayout frame;
    Activity activity;

    private FusedLocationProviderClient fusedLocationClient;

    String product_category = "";

    public SearchProductFragment() {
        
    }

    public SearchProductFragment(@NonNull ActivityResultRegistry registry) {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_search_product, container, false);

        activity = getActivity();


        viewPagerCarouselView = rootView.findViewById(R.id.carousel_view);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        frame = rootView.findViewById(R.id.frame);
        searchlayout = rootView.findViewById(R.id.searchlayout);

        searchlayout.setOnClickListener(view -> startActivity(new Intent(getContext(), SearchProductsActivity.class)));
        recyclerView = rootView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        listAdapter = new ProductListAdapter((realmProviderCategories, position, holder) -> {
            RealmProductCategory realmProductCategory = realmProviderCategories.get(position);

            startActivityForResult(new Intent(getContext(), MyProductListActivity.class)
                            .putExtra("title", realmProductCategory.getTitle())
                            .putExtra("tag", realmProductCategory.getTag())
                            .putExtra("initiator", "SearchProductsActivity"),
                    1915
            );

        }, getActivity(), realmProductCategories, "");

        shimmer_view_container = rootView.findViewById(R.id.shimmer_view_container);
        shimmer_view_container.startShimmerAnimation();
        error_loading = rootView.findViewById(R.id.error_loading);


        recyclerView.setAdapter(listAdapter);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        initSearchProductFragment();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case 1915:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        if (data != null) {
                            product_category = data.getStringExtra("PRODUCT_CATEGORY");
                            if (checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                fusedLocationClient.getLastLocation()
                                        .addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                                            @Override
                                            public void onSuccess(Location location) {
                                                // Got last known location. In some rare situations this can be null.
                                                if (location != null) {
                                                    launchRatedProducts(location.getLongitude(), location.getLatitude());
                                                }
                                                else {
                                                    launchRatedProducts( -0.205874, 5.614818);
                                                }
                                            }
                                        });
                            }
                            else {
                                ActivityResultLauncher<String[]> locationPermissionRequest = registerForActivityResult(new ActivityResultContracts
                                                .RequestMultiplePermissions(), result -> {
                                            Boolean fineLocationGranted = null;
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                                fineLocationGranted = result.getOrDefault(
                                                        Manifest.permission.ACCESS_FINE_LOCATION, false);
                                            }
                                            Boolean coarseLocationGranted = null;
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                                coarseLocationGranted = result.getOrDefault(
                                                        Manifest.permission.ACCESS_COARSE_LOCATION, false);
                                            }
                                            if (fineLocationGranted != null && fineLocationGranted) {
                                                // Precise location access granted.
                                                fusedLocationClient.getLastLocation()
                                                        .addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                                                            @Override
                                                            public void onSuccess(Location location) {
                                                                // Got last known location. In some rare situations this can be null.
                                                                if (location != null) {
                                                                    launchRatedProducts(location.getLongitude(), location.getLatitude());
                                                                }
                                                                else {
                                                                    launchRatedProducts( -0.205874, 5.614818);
                                                                }
                                                            }
                                                        });
                                                /*new GoogleMap.OnMyLocationChangeListener() {
                                                    @Override
                                                    public void onMyLocationChange(Location location) {
                                                        Toast.makeText(, "", Toast.LENGTH_SHORT).show();
                                                    }
                                                };*/
                                            } else if (coarseLocationGranted != null && coarseLocationGranted) {
                                                // Only approximate location access granted.
                                                fusedLocationClient.getLastLocation()
                                                        .addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                                                            @Override
                                                            public void onSuccess(Location location) {
                                                                // Got last known location. In some rare situations this can be null.
                                                                if (location != null) {
                                                                    launchRatedProducts(location.getLongitude(), location.getLatitude());
                                                                }
                                                                else {
                                                                    launchRatedProducts( -0.205874, 5.614818);
                                                                }
                                                            }
                                                        });

                                            } else {
                                                // No location access granted.
                                            }
                                        }
                                );

                                // ...

                                // Before you perform the actual permission request, check whether your app
                                // already has the permissions, and whether your app needs to show a permission
                                // rationale dialog. For more details, see Request permissions.
                                locationPermissionRequest.launch(new String[]{
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                });
                            }
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        // some stuff that will happen if there's no result
                        break;
                }
                break;
            default:
                break;
        }
    }

    private void launchRatedProducts(double longitude, double lattitude) {
        try {
            ProgressDialog mProgress = new ProgressDialog(getContext());
            mProgress.setMessage("Please wait...");
            mProgress.setCancelable(false);
            mProgress.setIndeterminate(true);
            StringRequest stringRequest = new StringRequest(
                    Request.Method.POST,
                    API_URL + "sub-products",
                    response -> {
                        mProgress.dismiss();
                        if (response != null) {
                            try {
                                JSONArray jsonArray = new JSONArray(response);
                                Realm.init(getContext());
                                Realm.getInstance(RealmUtility.getDefaultConfig(getContext())).executeTransaction(realm -> {
                                    RealmResults<RealmProduct> realmProducts = realm.where(RealmProduct.class).findAll();
                                    realmProducts.deleteAllFromRealm();

                                    realm.createOrUpdateAllFromJson(RealmProduct.class, jsonArray);
                                });
                                if (jsonArray.length() == 0) {
                                    Toast.makeText(getContext(), "No matching providers available!", Toast.LENGTH_SHORT).show();
                                } else {
                                    startActivity(new Intent(getContext(), ProductsActivity.class)
                                            .putExtra("TITLE", product_category)
                                            .putExtra("LONGITUDE", longitude)
                                            .putExtra("LATITUDE", lattitude)
                                    );
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    },
                    error -> {
                        mProgress.dismiss();
                        myVolleyError(getContext(), error);
                    }
            ) {
                @Override
                public Map<String, String> getParams() throws AuthFailureError {

                    Map<String, String> params = new HashMap<>();
                    params.put("product_category", product_category);
                    return params;
                }
                @Override
                public Map getHeaders() throws AuthFailureError {
                    HashMap headers = new HashMap();
                    headers.put("accept", "application/json");
                    headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(getContext()).getString(APITOKEN, ""));
                    return headers;
                }
            };
            stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                    0,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            InitApplication.getInstance().addToRequestQueue(stringRequest);

        } catch (Exception e) {
            Log.e("My error", e.toString());
        }
    }

    public void initSearchProductFragment() {
        Realm.init(getContext());
        Realm.init(getContext());
        Realm.getInstance(RealmUtility.getDefaultConfig(homeactivity)).executeTransaction(realm -> {
            RealmResults<RealmBanner> realmBannerRealmResults = realm.where(RealmBanner.class).findAll();
            if (realmBannerRealmResults.size() > 0) {
                realmBannerArrayList.clear();
                for (RealmBanner banner : realmBannerRealmResults) {
                    realmBannerArrayList.add(banner);
                }
                viewPagerCarouselView.setData(getChildFragmentManager(), realmBannerArrayList, 3500);
                frame.setVisibility(View.VISIBLE);
                //  error_loading.setVisibility(View.GONE);

            } else {
                ///  error_loading.setVisibility(View.VISIBLE);
            }
            RealmResults<RealmProductCategory> realmProviderCategories = realm.where(RealmProductCategory.class)
                    .equalTo("description", "").findAll();
            realmProductCategories.clear();
            for (RealmProductCategory realmProviderCategory : realmProviderCategories) {
                realmProductCategories.add(realmProviderCategory);
            }
            if (realmProductCategories.size() > 0) {
                listAdapter.notifyDataSetChanged();
            }
        });
    }
}
