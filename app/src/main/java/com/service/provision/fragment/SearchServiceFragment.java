package com.service.provision.fragment;

import static androidx.core.content.ContextCompat.checkSelfPermission;

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
import com.service.provision.activity.DriverFoundActivity;
import com.service.provision.activity.MyServiceListActivity;
import com.service.provision.activity.RadioActivity;
import com.service.provision.activity.SearchServicesActivity;
import com.service.provision.activity.SelectLocationActivity;
import com.service.provision.activity.ServicesActivity;
import com.service.provision.adapter.ServiceListAdapter;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmBanner;
import com.service.provision.realm.RealmCustomer;
import com.service.provision.realm.RealmService;
import com.service.provision.realm.RealmServiceCategory;
import com.service.provision.util.RealmUtility;
import com.service.provision.util.carousel.ViewPagerCarouselView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;

import static com.service.provision.activity.DriverFoundActivity.driverFoundActivity;
import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.activity.ProviderHomeActivity.homeactivity;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.myVolleyError;


public class SearchServiceFragment extends Fragment {
    ArrayList<RealmBanner> realmBannerArrayList = new ArrayList<>();
    ArrayList<RealmServiceCategory> realmServiceCategoryArrayList = new ArrayList<>();
    RecyclerView recyclerView;
    private ShimmerFrameLayout shimmer_view_container;

    static ViewPagerCarouselView viewPagerCarouselView;
    public static RelativeLayout searchlayout;
    public static LinearLayout error_loading;
    ServiceListAdapter listAdapter;
    Button retrybtn;
    FrameLayout frame;
    Activity activity;

    private FusedLocationProviderClient fusedLocationClient;

    String service_category = "";

    public SearchServiceFragment() {
        // Required empty public constructor
    }

    public SearchServiceFragment(@NonNull ActivityResultRegistry registry) {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_search_service, container, false);

        activity = getActivity();


        viewPagerCarouselView = rootView.findViewById(R.id.carousel_view);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        frame = rootView.findViewById(R.id.frame);
        searchlayout = rootView.findViewById(R.id.searchlayout);

        searchlayout.setOnClickListener(view -> startActivity(new Intent(getContext(), SearchServicesActivity.class)));
        recyclerView = rootView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        listAdapter = new ServiceListAdapter((realmProviderCategories, position, holder) -> {
            RealmServiceCategory realmServiceCategory = realmProviderCategories.get(position);

            if (realmServiceCategory.getTitle().equals("SuperRide")) {
                ProgressDialog mProgress = new ProgressDialog(getContext());
                mProgress.setMessage("Please wait...");
                mProgress.setCancelable(false);
                mProgress.setIndeterminate(true);
                StringRequest stringRequest = new StringRequest(
                        Request.Method.POST,
                        API_URL + "unfinished-ride-check",
                        response -> {
                            mProgress.dismiss();
                            if (response != null) {
                                try {
                                    JSONObject jsonObject = new JSONObject(response);
                                    Realm.init(getContext());
                                    Realm.getInstance(RealmUtility.getDefaultConfig(getContext())).executeTransaction(realm -> {

                                        if (!jsonObject.isNull("ride_history")) {
                                            JSONObject ride_historyJson = null;
                                            try {
                                                ride_historyJson = jsonObject.getJSONObject("ride_history");

                                                JSONObject providerJson = jsonObject.getJSONObject("provider");

                                                Realm.init(getActivity());
                                                RealmCustomer realmCustomer = Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).where(RealmCustomer.class).findFirst();
                                                String customer_id = realmCustomer.getCustomer_id();

                                                PreferenceManager
                                                        .getDefaultSharedPreferences(getActivity())
                                                        .edit()
                                                        .putString("RIDE_HISTORY_ID", ride_historyJson.getString("ride_history_id"))
                                                        .putString("PICKUP_LAT", String.valueOf(ride_historyJson.getDouble("pickup_latitude")))
                                                        .putString("PICKUP_LONG", String.valueOf(ride_historyJson.getDouble("pickup_longitude")))
                                                        .putString("DESTINATION_LAT", String.valueOf(ride_historyJson.getDouble("destination_latitude")))
                                                        .putString("DESTINATION_LONG", String.valueOf(ride_historyJson.getDouble("destination_longitude")))
                                                        .putString("PROVIDER_NAME", providerJson.getString("first_name"))
                                                        .putString("PROVIDER_LAT", providerJson.getString("latitude"))
                                                        .putString("PROVIDER_LONG", providerJson.getString("longitude"))
                                                        .putString("PROVIDER_PROFILE_IMAGE_URL", providerJson.getString("profile_image_url"))
                                                        .putString("PICKUP_ADDRESS", ride_historyJson.getString("pickup_address"))
                                                        .putString("DESTINATION_ADDRESS", ride_historyJson.getString("destination_address"))
                                                        .putString("SERVICE_ID", ride_historyJson.getString("service_id"))
                                                        .putString("PROVIDER_PRIMARY_CONTACT", providerJson.getString("primary_contact"))
                                                        .putString("VEHICLE_TYPE", providerJson.getString("vehicle_type"))
                                                        .putString("VEHICLE_REGISTRATION_NUMBER", providerJson.getString("vehicle_registration_number"))
                                                        .putString("PROVIDER_CONFIRMATION_TOKEN", providerJson.getString("confirmation_token"))
                                                        .putString("CUSTOMER_ID", customer_id)
                                                        .putString("PROVIDER_ID", providerJson.getString("provider_id"))
                                                        .apply();


                                                if (ride_historyJson.isNull("start_time") || ride_historyJson.getString("start_time").equals("null")) {
                                                    PreferenceManager
                                                            .getDefaultSharedPreferences(getActivity())
                                                            .edit()
                                                            .putBoolean("DRIVING_TO_PICKUP", true)
                                                            .putBoolean("DRIVING_TO_DESTINATION", false)
                                                            .apply();
                                                }
                                                else {
                                                    PreferenceManager
                                                            .getDefaultSharedPreferences(getActivity())
                                                            .edit()
                                                            .putBoolean("DRIVING_TO_PICKUP", false)
                                                            .putBoolean("DRIVING_TO_DESTINATION", true)
                                                            .apply();
                                                }


                                                if (driverFoundActivity != null) {
                                                    driverFoundActivity.finish();
                                                }
                                                startActivity(new Intent(getActivity(), DriverFoundActivity.class));
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        else {
                                            PreferenceManager
                                                    .getDefaultSharedPreferences(getActivity())
                                                    .edit()
                                                    .putBoolean("DRIVING_TO_PICKUP", false)
                                                    .putBoolean("DRIVING_TO_DESTINATION", false)
                                                    .apply();

                                            startActivityForResult(new Intent(getContext(), MyServiceListActivity.class)
                                                            .putExtra("title", realmServiceCategory.getTitle())
                                                            .putExtra("tag", realmServiceCategory.getTag())
                                                            .putExtra("url", realmServiceCategory.getUrl())
                                                            .putExtra("initiator", "SearchServiceFragment"),
                                                    1914
                                            );
                                        }
                                    });
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
                        Realm.init(getContext());
                        RealmCustomer realmCustomer = Realm.getInstance(RealmUtility.getDefaultConfig(getContext())).where(RealmCustomer.class).findFirst();
                        String customer_id = realmCustomer.getCustomer_id();
                        params.put("customer_id", customer_id);
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
            } else {
                startActivityForResult(new Intent(getContext(), MyServiceListActivity.class)
                                .putExtra("title", realmServiceCategory.getTitle())
                                .putExtra("tag", realmServiceCategory.getTag())
                                .putExtra("url", realmServiceCategory.getUrl())
                                .putExtra("initiator", "SearchServiceFragment"),
                        1914
                );
            }

        }, getActivity(), realmServiceCategoryArrayList, "");

        shimmer_view_container = rootView.findViewById(R.id.shimmer_view_container);
        shimmer_view_container.startShimmerAnimation();
        error_loading = rootView.findViewById(R.id.error_loading);


        recyclerView.setAdapter(listAdapter);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        initSearchServiceFragment();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case 1914:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        if (data != null) {
                            if (data.getStringExtra("tag") != null) {
                                if (data.getStringExtra("tag").contains("radio")) {
                                    String[] split = data.getStringExtra("SERVICE_CATEGORY").split(" >> ");
                                    startActivity(new Intent(getActivity(), RadioActivity.class)
                                            .putExtra("STATION_NAME", split[split.length - 1])
                                            .putExtra("FREQUENCY", data.getStringExtra("tag").split(",")[1])
                                            .putExtra("STREAM_URL", data.getStringExtra("tag").split(",")[2])
                                            .putExtra("ICON_URL", data.getStringExtra("url"))
                                    );
                                } else if (data.getStringExtra("tag").contains("superride")) {
                                    if (checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                        launchSelectLocationActivity(data.getStringExtra("SERVICE_CATEGORY"));
                                    } else {
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
                                                        launchSelectLocationActivity(data.getStringExtra("SERVICE_CATEGORY"));
                                                    } else if (coarseLocationGranted != null && coarseLocationGranted) {
                                                        launchSelectLocationActivity(data.getStringExtra("SERVICE_CATEGORY"));
                                                        // Only approximate location access granted.
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
                            } else {
                                service_category = data.getStringExtra("SERVICE_CATEGORY");
                                if (checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    if (checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                        fusedLocationClient.getLastLocation()
                                                .addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                                                    @Override
                                                    public void onSuccess(Location location) {
                                                        // Got last known location. In some rare situations this can be null.
                                                        if (location != null) {
                                                            // Logic to handle location object
                                                            launchRatedServices(location.getLongitude(), location.getLatitude());
                                                        } else {
                                                            Toast.makeText(getActivity(), "Error retrieving location.", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });
                                    } else {
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
                                                                            // Logic to handle location object
                                                                            launchRatedServices(location.getLongitude(), location.getLatitude());
                                                                        } else {
                                                                            Toast.makeText(getActivity(), "Error retrieving location.", Toast.LENGTH_SHORT).show();
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
                                                                            // Logic to handle location object
                                                                            launchRatedServices(location.getLongitude(), location.getLatitude());
                                                                        } else {
                                                                            Toast.makeText(getActivity(), "Error retrieving location.", Toast.LENGTH_SHORT).show();
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
                                } else {
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
                                                                        // Logic to handle location object
                                                                        launchRatedServices(location.getLongitude(), location.getLatitude());
                                                                    } else {
                                                                        Toast.makeText(getActivity(), "Error retrieving location.", Toast.LENGTH_SHORT).show();
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
                                                                        // Logic to handle location object
                                                                        launchRatedServices(location.getLongitude(), location.getLatitude());
                                                                    } else {
                                                                        Toast.makeText(getActivity(), "Error retrieving location.", Toast.LENGTH_SHORT).show();
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

    private void launchSelectLocationActivity(String service_category) {
        Realm.init(getActivity());
        RealmCustomer realmCustomer = Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).where(RealmCustomer.class).findFirst();
        startActivity(
                new Intent(getActivity(), SelectLocationActivity.class)
                        .putExtra("LONGITUDE", realmCustomer.getLongitude())
                        .putExtra("LATITUDE", realmCustomer.getLatitude())
                        .putExtra("SERVICE_CATEGORY", service_category)
        );
    }

    private void launchRatedServices(double longitude, double lattitude) {
        try {
            ProgressDialog mProgress = new ProgressDialog(getContext());
            mProgress.setMessage("Please wait...");
            mProgress.setCancelable(false);
            mProgress.setIndeterminate(true);
            StringRequest stringRequest = new StringRequest(
                    Request.Method.POST,
                    API_URL + "sub-services",
                    response -> {
                        mProgress.dismiss();
                        if (response != null) {
                            try {
                                JSONArray jsonArray = new JSONArray(response);
                                Realm.init(getContext());
                                Realm.getInstance(RealmUtility.getDefaultConfig(getContext())).executeTransaction(realm -> {
                                    RealmResults<RealmService> realmServices = realm.where(RealmService.class).findAll();
                                    realmServices.deleteAllFromRealm();

                                    realm.createOrUpdateAllFromJson(RealmService.class, jsonArray);
                                });
                                if (jsonArray.length() == 0) {
                                    Toast.makeText(getContext(), "No matching providers available!", Toast.LENGTH_SHORT).show();
                                } else {
                                    startActivity(new Intent(getContext(), ServicesActivity.class)
                                            .putExtra("TITLE", service_category)
                                            .putExtra("LONGITUDE", longitude)
                                            .putExtra("LATITUDE", longitude)
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
                    params.put("service_category", service_category);
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

    public void initSearchServiceFragment() {
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
            realmServiceCategoryArrayList.clear();
            RealmServiceCategory emergencyCategory = realm.where(RealmServiceCategory.class)
                    .equalTo("title", "Emergency")
                    .findFirst();
            realmServiceCategoryArrayList.add(emergencyCategory);
            RealmServiceCategory radioCategory = realm.where(RealmServiceCategory.class)
                    .equalTo("title", "Radio")
                    .findFirst();
            realmServiceCategoryArrayList.add(radioCategory);
            RealmServiceCategory superRideCategory = realm.where(RealmServiceCategory.class)
                    .equalTo("title", "SuperRide")
                    .findFirst();
            //realmServiceCategoryArrayList.add(superRideCategory);

            RealmResults<RealmServiceCategory> realmServiceCategories = realm.where(RealmServiceCategory.class)
                    .notEqualTo("title", "Emergency")
                    .notEqualTo("title", "Radio")
                    .notEqualTo("title", "SuperRide")
                    .equalTo("description", "")
                    .findAll();

            for (RealmServiceCategory realmProviderCategory : realmServiceCategories) {
                realmServiceCategoryArrayList.add(realmProviderCategory);
            }
            if (realmServiceCategoryArrayList.size() > 0) {
                listAdapter.notifyDataSetChanged();
            }
        });
    }
}
