package com.service.provision.fragment;

import static com.service.provision.activity.MapsActivity.RC_CONFIRM_LOCATION;
import static com.service.provision.activity.OrganisationalProviderAccountActivity.realmProvider;
import static com.service.provision.constants.Const.myVolleyError;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.greysonparrelli.permiso.Permiso;
import com.service.provision.R;
import com.service.provision.activity.MapsActivity;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmCustomer;
import com.service.provision.util.RealmUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;

/**
 * Created by 2CLearning on 12/13/2017.
 */

public class OrganisationalProviderAccountFragment2 extends Fragment {
    private static final String TAG = "OrganisationalProviderAccountFragment2";

    public static double longitude = 0.0d;
    public static double latitude = 0.0d;

    public static EditText email, primarycontact, auxiliarycontact, postaladdress;
    public static TextView google_location, street_address, digital_address;
    RelativeLayout google_location_layout;
    public static Spinner years_of_operation_spinner;
    CardView cardView;
    Context mContext;

    public static int PLACE_PICKER_REQUEST = 100;
    private static final LatLngBounds BOUNDS_MOUNTAIN_VIEW = new LatLngBounds(
            new LatLng(37.398160, -122.180831), new LatLng(37.430610, -121.972090));

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_organisational_provider_account2, container, false);

        email = rootView.findViewById(R.id.email);
        primarycontact = rootView.findViewById(R.id.primarycontact);
        auxiliarycontact = rootView.findViewById(R.id.auxiliarycontact);

        mContext = getContext();
        years_of_operation_spinner = rootView.findViewById(R.id.years_of_operation_spinner);
        google_location = rootView.findViewById(R.id.google_location);
        google_location_layout = rootView.findViewById(R.id.google_location_layout);
        street_address = rootView.findViewById(R.id.street_address);
        digital_address = rootView.findViewById(R.id.digital_address);

        postaladdress = rootView.findViewById(R.id.postaladdress);


        google_location_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
                                                             @Override
                                                             public void onPermissionResult(Permiso.ResultSet resultSet) {
                                                                 if (resultSet.isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION) && resultSet.isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                                                                     /*try {
                                                                         PlacePicker.IntentBuilder intentBuilder =
                                                                                 new PlacePicker.IntentBuilder();
                                                                         intentBuilder.setLatLngBounds(BOUNDS_MOUNTAIN_VIEW);
                                                                         Intent intent = intentBuilder.build((Activity) mContext);
                                                                         startActivityForResult(intent, PLACE_PICKER_REQUEST);

                                                                     } catch (GooglePlayServicesRepairableException
                                                                             | GooglePlayServicesNotAvailableException e) {
                                                                         e.printStackTrace();
                                                                     }*/

                                                                     /*Places.initialize(getContext(), getResources().getString(R.string.google_maps_key));
                                                                     List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME);
                                                                     Intent intent = new Autocomplete.IntentBuilder(
                                                                             AutocompleteActivityMode.OVERLAY, fields)
                                                                             .build(getActivity());
                                                                     startActivityForResult(intent, 1101);*/

                                                                     if (realmProvider != null && realmProvider.getProvider_id() != null && !realmProvider.getProvider_id().equals("")) {
                                                                         startActivityForResult(new Intent(getContext(), MapsActivity.class)
                                                                                         .putExtra("LONGITUDE", realmProvider.getLongitude())
                                                                                         .putExtra("LATITUDE", realmProvider.getLatitude())
                                                                                         .putExtra("BUTTON_TEXT", "CONFIRM LOCATION")
                                                                                 , RC_CONFIRM_LOCATION);
                                                                     } else {
                                                                         startActivityForResult(new Intent(getContext(), MapsActivity.class)
                                                                                         .putExtra("BUTTON_TEXT", "CONFIRM LOCATION")
                                                                                 , RC_CONFIRM_LOCATION);
                                                                     }
                                                                 }
                                                             }

                                                             @Override
                                                             public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                                                                 Permiso.getInstance().showRationaleInDialog(getActivity().getString(R.string.permissions), getActivity().getString(R.string.this_permission_is_mandatory_pls_allow_access), null, callback);
                                                             }
                                                         },
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION);
            }
        });
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // txtData = (TextView)view.findViewById(R.id.txtData);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        init();
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
                            if (longitude != 0.0d && latitude != 0.0d) {
                                DecimalFormat formatter = new DecimalFormat("#0.00");
                                google_location.setError(null);
                                google_location.setText(formatter.format(latitude) + ", " + formatter.format(longitude));
                            } else {
                                google_location.setText("");
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


    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(mContext)
                .setMessage(message)
                .setPositiveButton(this.getResources().getString(R.string.ok), okListener)
                .setNegativeButton(this.getResources().getString(R.string.cancel), null)
                .create()
                .show();
    }

    public void init() {
        if (realmProvider != null) {
            years_of_operation_spinner.setSelection(((ArrayAdapter) years_of_operation_spinner.getAdapter()).getPosition(realmProvider.getYears_of_operation()));
            primarycontact.setText(realmProvider.getPrimary_contact());
            auxiliarycontact.setText(realmProvider.getAuxiliary_contact());
            DecimalFormat formatter = new DecimalFormat("#0.00");
            google_location.setText(formatter.format(realmProvider.getLatitude()) + ", " + formatter.format(realmProvider.getLongitude()));
        }
    }

    public boolean validate() {
        boolean validated = true;

        String phonenumber = primarycontact.getText().toString();
        if (!(phonenumber.length() == 10 && phonenumber.charAt(0) == '0')) {
            primarycontact.setError("Invalid number");
            validated = false;
        }

        String auxphonenumber = auxiliarycontact.getText().toString();
        if (!auxphonenumber.equals("") && !(auxphonenumber.length() == 10 && auxphonenumber.charAt(0) == '0')) {
            auxiliarycontact.setError("Invalid number");
            validated = false;
        }
        if (TextUtils.isEmpty(google_location.getText())) {
            google_location.setError(getString(R.string.error_field_required));
            validated = false;
        }
        if (years_of_operation_spinner.getSelectedItemPosition() == 0) {
            TextView errorText = (TextView) years_of_operation_spinner.getSelectedView();
            errorText.setError("");
            errorText.setTextColor(Color.RED);
            validated = false;
        }
        return validated;
    }

    public static boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }
}
