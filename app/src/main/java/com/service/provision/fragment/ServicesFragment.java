package com.service.provision.fragment;

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
import com.service.provision.adapter.ServiceAdapter;
import com.service.provision.materialDialog.ServiceMaterialDialog;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmService;
import com.service.provision.realm.RealmServiceImage;
import com.service.provision.util.RealmUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;

import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.myVolleyError;

/**
 * Created by 2CLearning on 12/13/2017.
 */

public class ServicesFragment extends Fragment {
    private static final String TAG = "PersonalProviderAccountFragment6";
    Button add, remove;
    public static RecyclerView recyclerView;
    Context mContext;
    LinearLayout clickToAdd;
    CardView cardView;
    public static ArrayList<RealmService> serviceArrayList;
    public static ServiceAdapter serviceAdapter;
    public static RecyclerView.LayoutManager layoutManager;
    public static ServiceMaterialDialog serviceMaterialDialog = new ServiceMaterialDialog();
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_services, container, false);


        serviceArrayList = new ArrayList<>();
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
                if(serviceMaterialDialog != null && serviceMaterialDialog.isAdded()) {

                } else {
                    serviceMaterialDialog.setCancelable(false);
                    serviceMaterialDialog.show(getFragmentManager(), "addServiceMaterialDialog");
                    serviceMaterialDialog.setCancelable(true);
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

        serviceAdapter.notifyDataSetChanged();

        final boolean[] imageSet = new boolean[1];
        imageSet[0] = true;
        final String[] service_category = new String[1];
        final String[] service_id = new String[1];
        Realm.init(getContext());
        Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).executeTransaction(realm -> {

            RealmResults<RealmService> realmServices = realm.where(RealmService.class).equalTo("provider_id", PreferenceManager.getDefaultSharedPreferences(mContext).getString("PROVIDER_ID", "")).findAll();
            for (RealmService realmService : realmServices) {
                imageSet[0] = realm.where(RealmServiceImage.class).equalTo("service_id", realmService.getService_id()).findAll().size() > 0;
                if (!imageSet[0]) {
                    service_category[0] = realmService.getService_category();
                    service_id[0] = realmService.getService_id();
                    break;
                }
            }
        });
        if (!imageSet[0]) {
            startActivity(new Intent(getContext(), ImagesActivity.class)
                    .putExtra("SERVICEID", service_id[0])
                    .putExtra("TITLE", service_category[0])
            );
        }
    }

    public void init() {
        Realm.init(getContext());
        Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).executeTransaction(realm -> {

            RealmResults<RealmService> realmServices = realm.where(RealmService.class).equalTo("provider_id", PreferenceManager.getDefaultSharedPreferences(mContext).getString("PROVIDER_ID", "")).findAll();
            serviceArrayList.clear();
            for (RealmService realmService : realmServices) {
               serviceArrayList.add(realmService);
            }
        });

        serviceAdapter = new ServiceAdapter(new ServiceAdapter.ServiceAdapterInterface() {
            @Override
            public void onListItemClick(ArrayList<RealmService> realmServices, int position, ServiceAdapter.ViewHolder holder) {
                RealmService realmService = realmServices.get(position);

                PopupMenu popup = new PopupMenu(mContext, holder.more_details);

                popup.inflate(R.menu.service_menu);

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
                                                                             String service_id = realmService.getService_id();
                                                                             StringRequest stringRequest = new StringRequest(
                                                                                     Request.Method.POST,
                                                                                     API_URL + "scoped-service-images",
                                                                                     response -> {
                                                                                         mProgress.dismiss();
                                                                                         if (response != null) {
                                                                                             try {
                                                                                                 JSONArray jsonArray = new JSONArray(response);
                                                                                                 Realm.init(getContext());
                                                                                                 Realm.getInstance(RealmUtility.getDefaultConfig(getContext())).executeTransaction(realm -> {
                                                                                                     RealmResults<RealmServiceImage> serviceImages = realm.where(RealmServiceImage.class).equalTo("service_id", service_id).findAll();
                                                                                                     serviceImages.deleteAllFromRealm();
                                                                                                     realm.createOrUpdateAllFromJson(RealmServiceImage.class, jsonArray);
                                                                                                 });
                                                                                                 try {
                                                                                                     startActivity(new Intent(getContext(), ImagesActivity.class)
                                                                                                             .putExtra("SERVICEID", service_id)
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
                                                                         Permiso.getInstance().showRationaleInDialog(getActivity().getString(R.string.permissions), getActivity().getString(R.string.this_permission_is_mandatory_pls_allow_access), null, callback);
                                                                     }
                                                                 },
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        return true;
                    } else if (itemId == R.id.edit) {
                        ServiceMaterialDialog serviceMaterialDialog = new ServiceMaterialDialog();
                        if (serviceMaterialDialog != null && serviceMaterialDialog.isAdded()) {

                        } else {
                            serviceMaterialDialog.setService_id(realmService.getService_id());
                            serviceMaterialDialog.setServicecategorytext(realmService.getService_category());
                            serviceMaterialDialog.setServicenametext(realmService.getName());
                            serviceMaterialDialog.setDescriptiontext(realmService.getDescription());
                            serviceMaterialDialog.setMin_charge_amounttext(String.format("%.2f", realmService.getMin_charge_amount()));
                            serviceMaterialDialog.setMax_charge_amounttext(String.format("%.2f", realmService.getMax_charge_amount()));

                            serviceMaterialDialog.setCancelable(false);
                            serviceMaterialDialog.show(getFragmentManager(), "editServiceMaterialDialog");
                            serviceMaterialDialog.setCancelable(true);
                        }
                        return true;
                    } else if (itemId == R.id.remove) {
                        String service_id = realmService.getService_id();
                        StringRequest stringRequestDelete = new StringRequest(
                                Request.Method.DELETE,
                                API_URL + "services/" + realmService.getService_id(),
                                response -> {
                                    if (response != null) {
                                        try {
                                            JSONObject jsonObject = new JSONObject(response);
                                            if (jsonObject.getBoolean("status")) {
                                                Realm.init(getActivity());
                                                Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).executeTransaction(realm -> {
                                                    realmServices.get(position).deleteFromRealm();
                                                });
                                                realmServices.remove(position);
                                                serviceAdapter.notifyDataSetChanged();
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
                                params.put("service_id", service_id);
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
        }, serviceArrayList);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        //  myrecyclerview.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(serviceAdapter);
    }

    public boolean validate (){
        boolean validated = true;
        return validated;
    }
}