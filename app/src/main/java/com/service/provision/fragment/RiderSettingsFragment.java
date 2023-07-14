package com.service.provision.fragment;

import static com.service.provision.activity.MapsActivity.RC_CONFIRM_LOCATION;
import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.activity.ProviderHomeActivity.MYUSERID;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.myVolleyError;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.greysonparrelli.permiso.Permiso;
import com.service.provision.R;
import com.service.provision.activity.ChangePhonenumberActivity;
import com.service.provision.activity.HelpActivity;
import com.service.provision.activity.OrganisationalProviderAccountActivity;
import com.service.provision.activity.PaymentActivity;
import com.service.provision.activity.PersonalProviderAccountActivity;
import com.service.provision.activity.ProviderOrdersActivity;
import com.service.provision.activity.RiderProviderAccountActivity;
import com.service.provision.activity.SelectRoleActivity;
import com.service.provision.constants.Const;
import com.service.provision.materialDialog.PhonenumberMaterialDialog;
import com.service.provision.materialDialog.WebPortalMaterialDialog;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmCustomer;
import com.service.provision.realm.RealmProvider;
import com.service.provision.realm.RealmStudent;
import com.service.provision.receiver.NetworkReceiver;
import com.service.provision.util.RealmUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import it.beppi.tristatetogglebutton_library.TriStateToggleButton;


/**
 * Created by Nana on 11/26/2017.
 */

public class RiderSettingsFragment extends Fragment {

    public static final String ISNIGHTMODE = "ISNIGHTMODE";
    private static final int PICKFILE_REQUEST_CODE = 327;

    CardView attendancebtn, homeworkbtn, paymentsbtn, walletbtn, website, profilebtn, displaybtn, availabilitybtn, permission, apppermission, logout, groups, faqs, webportal, resetpassword, conferencecallno, backup, restore, addvirtualaccount, change_number;
    LinearLayout detailnightmode, detailavailability;
    ImageView displayright, displayright_availability;
    Switch aSwitch;
    TriStateToggleButton availabilityswitch;
    ProgressDialog dialog;
    TextView availabilitytextview;

    double longitude = 0.0d;
    double latitude = 0.0d;

    public static Callbacks mCallbacks;
    Activity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.settingsfrag, container, false);
        homeworkbtn = rootView.findViewById(R.id.homeworkbtn);
        groups = rootView.findViewById(R.id.groups);
        permission = rootView.findViewById(R.id.permission);
        apppermission = rootView.findViewById(R.id.apppermission);
//        subscriptiontime = rootView.findViewById(R.id.subscriptiontime);
        paymentsbtn = rootView.findViewById(R.id.paymentsbtn);
        walletbtn = rootView.findViewById(R.id.walletbtn);
        website = rootView.findViewById(R.id.website);
        profilebtn = rootView.findViewById(R.id.profilebtn);
        attendancebtn = rootView.findViewById(R.id.attendancebtn);
        attendancebtn.setVisibility(View.GONE);
        displaybtn = rootView.findViewById(R.id.displaybtn);
        availabilitybtn = rootView.findViewById(R.id.availabilitybtn);
        detailnightmode = rootView.findViewById(R.id.detailnightmode);
        detailavailability = rootView.findViewById(R.id.detailavailability);
        displayright = rootView.findViewById(R.id.displayright);
        displayright_availability = rootView.findViewById(R.id.displayright_availability);
        faqs = rootView.findViewById(R.id.faqs);
        aSwitch = rootView.findViewById(R.id.day_night_switch);
        availabilityswitch = rootView.findViewById(R.id.availabilityswitch);
        logout = rootView.findViewById(R.id.logout);
        webportal = rootView.findViewById(R.id.webportal);
        resetpassword = rootView.findViewById(R.id.resetpassword);
        conferencecallno = rootView.findViewById(R.id.conferencecallno);
        backup = rootView.findViewById(R.id.backup);
        restore = rootView.findViewById(R.id.restore);
        availabilitytextview = rootView.findViewById(R.id.availabilitytextview);
        addvirtualaccount = rootView.findViewById(R.id.addvirtualaccount);
        change_number = rootView.findViewById(R.id.change_number);

        addvirtualaccount.setVisibility(View.GONE);
        homeworkbtn.setVisibility(View.GONE);
        availabilitybtn.setVisibility(View.GONE);

        change_number.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), ChangePhonenumberActivity.class));
            }
        });

        homeworkbtn.setOnClickListener(view -> {
            clickview(view);
            startActivity(new Intent(activity, ProviderOrdersActivity.class));
        });

        webportal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri webpage = Uri.parse("http://41.189.178.40:55554");
                Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
                if (intent.resolveActivity(activity.getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(getContext(), getString(R.string.page_not_found), Toast.LENGTH_LONG).show();
                }
            }
        });


        profilebtn.setOnClickListener(view -> {
            clickview(view);

            final String[] category = {""};
            Realm.init(activity);
            Realm.getInstance(RealmUtility.getDefaultConfig(activity)).executeTransaction(realm -> {
                category[0] = realm.where(RealmProvider.class).equalTo("provider_id", PreferenceManager.getDefaultSharedPreferences(activity).getString("PROVIDER_ID", "")).findFirst().getCategory();
            });

            if (category[0].toLowerCase().equals("rider")) {
                startActivity(
                        new Intent(activity, RiderProviderAccountActivity.class)
                                .putExtra("MODE", "EDIT")
                );
            }
            else if (category[0].toLowerCase().equals("personal")) {
                startActivity(
                        new Intent(activity, PersonalProviderAccountActivity.class)
                        .putExtra("MODE", "EDIT")
                );
            } else {
                startActivity(
                        new Intent(activity, OrganisationalProviderAccountActivity.class)
                        .putExtra("MODE", "EDIT")
                );
            }
        });

        conferencecallno.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String[] phonenumber = {""};
                Realm.init(activity);
                Realm.getInstance(RealmUtility.getDefaultConfig(activity)).executeTransaction(realm -> {
                    phonenumber[0] = realm.where(RealmStudent.class).equalTo("infoid", PreferenceManager.getDefaultSharedPreferences(activity).getString(MYUSERID, "")).findFirst().getPrimarycontact();
                });


                PhonenumberMaterialDialog phonenumberMaterialDialog = new PhonenumberMaterialDialog();
                if (phonenumberMaterialDialog != null && phonenumberMaterialDialog.isAdded()) {

                } else {

                    phonenumberMaterialDialog.setType("");
                    phonenumberMaterialDialog.setPhonenumber(phonenumber[0]);
                    phonenumberMaterialDialog.show(getFragmentManager(), "");
                }
            }
        });

        faqs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(activity, HelpActivity.class));
            }
        });


        logout.setOnClickListener(view -> {
            PreferenceManager
                    .getDefaultSharedPreferences(activity)
                    .edit()
                    .putString("ROLE", "")
                    .apply();
            Realm.init(getContext());
            startActivity(new Intent(getContext(), SelectRoleActivity.class));
            activity.finish();
        });


        paymentsbtn.setOnClickListener(view -> {
            clickview(view);
            startActivity(new Intent(activity, PaymentActivity.class));
        });

        walletbtn.setOnClickListener(view -> {

        });

        website.setOnClickListener(view -> {
            clickview(view);
            Uri webpage = Uri.parse("https://www.univirtualschools.com/");
            Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), getString(R.string.page_not_found), Toast.LENGTH_LONG).show();
            }
        });

        displaybtn.setOnClickListener(view -> {
            clickview(view);
            aSwitch.setChecked(PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(ISNIGHTMODE, false));
            if (detailnightmode.getVisibility() == View.VISIBLE) {
                detailnightmode.setVisibility(View.GONE);
                displayright.setImageResource(R.drawable.right);
            } else {
                detailnightmode.setVisibility(View.VISIBLE);
                displayright.setImageResource(R.drawable.arrowdown);
            }
        });
        aSwitch.setOnClickListener(v -> {
            PreferenceManager
                    .getDefaultSharedPreferences(getContext())
                    .edit()
                    .putBoolean(ISNIGHTMODE, !PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(ISNIGHTMODE, false))
                    .apply();

            mCallbacks.onChangeNightMOde();
        });


        availabilitybtn.setOnClickListener(view -> {
            clickview(view);
            Realm.init(getContext());
            String availability = Realm.getInstance(RealmUtility.getDefaultConfig(getContext())).where(RealmProvider.class).equalTo("provider_id", PreferenceManager.getDefaultSharedPreferences(getContext()).getString("PROVIDER_ID", "")).findFirst().getAvailability();

            availabilitytextview.setText(availability);
            switch (availability) {
                case "Closed":
                    availabilityswitch.setToggleStatus(0, true);
                    break;
                case "Busy":
                    availabilityswitch.setToggleStatus(1, true);
                    break;
                case "Available":
                    availabilityswitch.setToggleStatus(2, true);
                    break;
            }



            if (detailavailability.getVisibility() == View.VISIBLE) {
                detailavailability.setVisibility(View.GONE);
                displayright_availability.setImageResource(R.drawable.collapse);
            } else {
                detailavailability.setVisibility(View.VISIBLE);
                displayright_availability.setImageResource(R.drawable.expand);
            }
        });
        availabilityswitch.setOnToggleChanged(new TriStateToggleButton.OnToggleChanged() {
            @Override
            public void onToggle(TriStateToggleButton.ToggleStatus toggleStatus, boolean booleanToggleStatus, int toggleIntValue) {
                Realm.init(getContext());
                RealmProvider realmProvider = Realm.getInstance(RealmUtility.getDefaultConfig(getContext())).where(RealmProvider.class).equalTo("provider_id", PreferenceManager.getDefaultSharedPreferences(getContext()).getString("PROVIDER_ID", "")).findFirst();
                String provider_id = realmProvider.getProvider_id();

                ProgressDialog dialog = new ProgressDialog(activity);
                dialog.setMessage("Please wait...");
                dialog.setCancelable(false);
                dialog.setIndeterminate(true);
                dialog.show();

                StringRequest stringRequest = new StringRequest(
                        Request.Method.POST,
                        API_URL + "providers/" + provider_id,
                        response -> {
                            dialog.dismiss();
                            if (response != null) {
                                JSONObject jsonObjectResponse = null;
                                try {
                                    jsonObjectResponse = new JSONObject(response);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                Realm.init(activity);
                                JSONObject finalJsonObjectResponse = jsonObjectResponse;
                                Realm.getInstance(RealmUtility.getDefaultConfig(NetworkReceiver.activeActivity)).executeTransaction(realm -> {
                                    realm.createOrUpdateObjectFromJson(RealmProvider.class, finalJsonObjectResponse);

                                    switch (toggleIntValue) {
                                        case 0:
                                            availabilitytextview.setText("Closed");
                                            break;
                                        case 1:
                                            availabilitytextview.setText("Busy");
                                            break;
                                        case 2:
                                            availabilitytextview.setText("Available");
                                            break;
                                    }
                                    Toast.makeText(NetworkReceiver.activeActivity, "Availability status successfully updated!", Toast.LENGTH_SHORT).show();
                                });
                            }
                        },
                        error -> {
                            dialog.dismiss();
                            error.printStackTrace();
                            Log.d("Cyrilll", error.toString());
                            myVolleyError(NetworkReceiver.activeActivity, error);
                        }
                ){
                    @Override
                    public Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> params  = new HashMap<>();
                        switch (toggleIntValue) {
                            case 0:
                                params.put("availability", "Closed");
                                break;
                            case 1:
                                params.put("availability", "Busy");
                                break;
                            case 2:
                                params.put("availability", "Available");
                                break;
                        }
                        return params;
                    }
                    /** Passing some request headers* */
                    @Override
                    public Map getHeaders() throws AuthFailureError {
                        HashMap headers = new HashMap();
                        headers.put("accept", "application/json");
                        headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(NetworkReceiver.activeActivity).getString(APITOKEN, ""));
                        return headers;
                    }
                };;

                stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                        0,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                InitApplication.getInstance().addToRequestQueue(stringRequest);
            }
        });

        backup.setOnClickListener(v -> {
            Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
                                                         @Override
                                                         public void onPermissionResult(Permiso.ResultSet resultSet) {
                                                             if (resultSet.isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE) && resultSet.isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                                                 // create a backup file
                                                                 String timestamp = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss.SSS").format(Calendar.getInstance().getTime());
                                                                 File exportRealmFile = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/SchoolDirectStudent/Backups/" + timestamp + ".realm");
                                                                 if (!exportRealmFile.getParentFile().exists()) {
                                                                     exportRealmFile.getParentFile().mkdirs();
                                                                 }
                                                                 // if backup file already exists, delete it
                                                                 if (exportRealmFile.exists()) {
                                                                     exportRealmFile.delete();
                                                                 }

                                                                 // copy current realm to backup file
                                                                 Realm.init(activity);
                                                                 Realm.getInstance(RealmUtility.getDefaultConfig(activity)).executeTransaction(realm -> {
                                                                     realm.writeEncryptedCopyTo(exportRealmFile, new byte[]{42, -17, -94, 54, 44, 122, -71, 110, -80, 23, 53, 6, 102, 67, -24, -63, -93, -45, 64, -25, 90, -6, 125, -64, 74, 53, -41, -81, -12, 7, -27, 34, 84, 86, 108, -46, -126, -6, 59, 64, -23, -56, 2, -89, 28, -47, -43, -70, 94, -1, -110, 126, -14, -31, 51, -23, -120, -50, -70, -104, -100, -39, 52, 77});
                                                                 });
                                                                 File destinationFile = new File(Environment.getExternalStorageDirectory() + "/SchoolDirectStudent/Backups/" + timestamp + ".realm");
                                                                 try {
                                                                     if (destinationFile.exists()) {
                                                                         destinationFile.delete();
                                                                     }
                                                                     if (destinationFile.getParentFile().exists()) {
                                                                         destinationFile.getParentFile().mkdirs();
                                                                     }
                                                                     Const.copy(exportRealmFile, destinationFile);
                                                                     exportRealmFile.delete();

                                                                     AlertDialog.Builder builder = new AlertDialog.Builder(NetworkReceiver.activeActivity);
                                                                     builder.setTitle("Backup Successful!");
                                                                     builder.setMessage("Backup file saved to /storage/SchoolDirectStudent/Backups/" + timestamp + ".realm");
                                                                     builder.setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                                                         dialog.dismiss();
                                                                     });
                                                                     builder.show();
                                                                 } catch (IOException e) {
                                                                     e.printStackTrace();
                                                                 }
                                                             }
                                                         }

                                                         @Override
                                                         public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                                                             Permiso.getInstance().showRationaleInDialog(activity.getString(R.string.permissions), activity.getString(R.string.this_permission_is_mandatory_pls_allow_access), null, callback);
                                                         }
                                                     },
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
        });
        restore.setOnClickListener(v -> {
            Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
                                                         @Override
                                                         public void onPermissionResult(Permiso.ResultSet resultSet) {
                                                             if (resultSet.isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE) && resultSet.isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                                                 Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                                                                 intent.setType("*/*");
                                                                 startActivityForResult(intent, PICKFILE_REQUEST_CODE);
                                                             }
                                                         }

                                                         @Override
                                                         public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                                                             Permiso.getInstance().showRationaleInDialog(activity.getString(R.string.permissions), activity.getString(R.string.this_permission_is_mandatory_pls_allow_access), null, callback);
                                                         }
                                                     },
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
        });
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
        // Activities containing this fragment must implement its callbacks
//        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_CONFIRM_LOCATION:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        if (data != null) {
                            longitude = data.getDoubleExtra("LONGITUDE", 0.0d);
                            latitude = data.getDoubleExtra("LATITUDE", 0.0d);
                            Log.d("sdffds0990xc", String.valueOf(longitude) + "  " + String.valueOf(latitude));
                            final Map<String, String>[] params = new Map[]{new HashMap<>()};
                            final String[] customer_id = new String[1];
                            Realm.init(activity);
                            Realm.getInstance(RealmUtility.getDefaultConfig(activity)).executeTransaction(realm -> {
                                RealmCustomer realmCustomer = realm.where(RealmCustomer.class).findFirst();
                                customer_id[0] = realmCustomer.getCustomer_id();
                                realmCustomer.setLongitude(longitude);
                                realmCustomer.setLatitude(latitude);

                                params[0] = new HashMap<>();
                                params[0].put("name", realmCustomer.getName() == null ? "" : realmCustomer.getName());
                                params[0].put("gender", realmCustomer.getGender() == null ? "" : realmCustomer.getGender());
                                params[0].put("primary_contact", realmCustomer.getPrimary_contact() == null ? "" : realmCustomer.getPrimary_contact());
                                params[0].put("auxiliary_contact", realmCustomer.getAuxiliary_contact() == null ? "" : realmCustomer.getAuxiliary_contact());
                                params[0].put("location", realmCustomer.getStreet_address() == null ? "" : realmCustomer.getStreet_address());
                                params[0].put("longitude", String.valueOf(longitude));
                                params[0].put("latitude", String.valueOf(latitude));
                            });
                            ProgressDialog dialog = new ProgressDialog(activity);
                            dialog.setMessage("Updating location...");
                            dialog.setMessage("Please wait...");
                            dialog.setCancelable(false);
                            dialog.setIndeterminate(true);
                            dialog.show();



                            StringRequest stringRequest = new StringRequest(
                                    Request.Method.POST,
                                    API_URL + "customers/" + customer_id[0],
                                    response -> {
                                        dialog.dismiss();
                                        if (response != null) {
                                            JSONObject jsonObjectResponse = null;
                                            try {
                                                jsonObjectResponse = new JSONObject(response);
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            Realm.init(activity);
                                            JSONObject finalJsonObjectResponse = jsonObjectResponse;
                                            Realm.getInstance(RealmUtility.getDefaultConfig(NetworkReceiver.activeActivity)).executeTransaction(realm -> {
                                                realm.createOrUpdateObjectFromJson(RealmCustomer.class, finalJsonObjectResponse);
                                                Toast.makeText(NetworkReceiver.activeActivity, "Location successfully set!", Toast.LENGTH_SHORT).show();
                                            });
                                        }
                                    },
                                    error -> {
                                        dialog.dismiss();
                                        error.printStackTrace();
                                        Log.d("Cyrilll", error.toString());
                                        myVolleyError(NetworkReceiver.activeActivity, error);
                                    }
                            ){
                                @Override
                                public Map<String, String> getParams() throws AuthFailureError {
                                    return params[0];
                                }
                                /** Passing some request headers* */
                                @Override
                                public Map getHeaders() throws AuthFailureError {
                                    HashMap headers = new HashMap();
                                    headers.put("accept", "application/json");
                                    headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(NetworkReceiver.activeActivity).getString(APITOKEN, ""));
                                    return headers;
                                }
                            };;

                            stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                                    0,
                                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                            InitApplication.getInstance().addToRequestQueue(stringRequest);
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        // some stuff that will happen if there's no result
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + resultCode);
                }
                break;

            default:
                break;

        }
    }

    private void clickview(View v) {
        Animation animation1 = AnimationUtils.loadAnimation(v.getContext(), R.anim.click);
        v.startAnimation(animation1);

    }

    public interface Callbacks {
        //Callback for when button clicked.
        void onChangeNightMOde();
    }

    public void generatePin() {
        String URL = null;
        dialog = ProgressDialog.show(getContext(), null, activity.getResources().getString(R.string.pls_wait), true);

        try {
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                    Request.Method.GET,
                    API_URL + "generate-pin",
                    null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    if (response == null) {
                        return;
                    }
                    dialog.dismiss();
                    try {
                        WebPortalMaterialDialog webPortalMaterialDialog = new WebPortalMaterialDialog();
                        webPortalMaterialDialog.setAccesscode(response.getString("pin"));
                        webPortalMaterialDialog.show(getFragmentManager(), "");
                    } catch (JSONException e) {
                        e.printStackTrace();
//                        webcodeView.setText(e.getMessage());
                    }
//                    webcodeView.setText(message);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    dialog.dismiss();
//                       loadimg.setVisibility(View.GONE);
//                    webcodeView.setText(activity.getResources().getString(R.string.error_occured));
                    myVolleyError(getContext(), error);
                    Log.d("Obeng", error.toString());

                }
            }) {
                @Override
                public Map getHeaders() throws AuthFailureError {
                    HashMap headers = new HashMap();
                    headers.put("accept", "application/json");
                    headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(getContext()).getString(APITOKEN, ""));
                    return headers;
                }
            };
            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                    0,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            InitApplication.getInstance().addToRequestQueue(jsonObjectRequest);

        } catch (Exception e) {
            e.printStackTrace();
        }
        // Toast.makeText(getApplicationContext(), "done", Toast.LENGTH_LONG).show();


    }
}
