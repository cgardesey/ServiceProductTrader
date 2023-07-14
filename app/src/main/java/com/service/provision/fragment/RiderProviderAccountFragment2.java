package com.service.provision.fragment;

import static com.service.provision.activity.MapsActivity.RC_CONFIRM_LOCATION;
import static com.service.provision.activity.RiderProviderAccountActivity.realmProvider;
import static com.service.provision.constants.Const.myVolleyError;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
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
import androidx.annotation.VisibleForTesting;
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
import com.tsongkha.spinnerdatepicker.DatePickerDialog;
import com.tsongkha.spinnerdatepicker.SpinnerDatePickerDialogBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.realm.Realm;

/**
 * Created by 2CLearning on 12/13/2017.
 */

public class RiderProviderAccountFragment2 extends Fragment implements DatePickerDialog.OnDateSetListener {
    private static final String TAG = "PersonalProviderAccountFragment2";

    public static double longitude = 0.0d;
    public static double latitude = 0.0d;

    public static EditText email, primarycontact, auxiliarycontact, postaladdress;
    public static TextView google_location, street_address, digital_address, dob;
    RelativeLayout google_location_layout, date_select_layout;
    public static Spinner maritalstatus;
    CardView cardView;
    Context mContext;
    SimpleDateFormat simpleDateFormat;
    public static int PLACE_PICKER_REQUEST = 100;
    private static final LatLngBounds BOUNDS_MOUNTAIN_VIEW = new LatLngBounds(
            new LatLng(37.398160, -122.180831), new LatLng(37.430610, -121.972090));

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_rider_provider_account2, container, false);

        email = rootView.findViewById(R.id.email);
        primarycontact = rootView.findViewById(R.id.primarycontact);
        auxiliarycontact = rootView.findViewById(R.id.auxiliarycontact);
        dob = rootView.findViewById(R.id.dob);

        mContext = getContext();
        maritalstatus = rootView.findViewById(R.id.maritalstatus_spinner);
        google_location = rootView.findViewById(R.id.google_location);
        street_address = rootView.findViewById(R.id.street_address);
        digital_address = rootView.findViewById(R.id.digital_address);
        google_location_layout = rootView.findViewById(R.id.google_location_layout);
        date_select_layout = rootView.findViewById(R.id.date_select_layout);
        postaladdress = rootView.findViewById(R.id.postaladdress);
        simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US);


        date_select_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDate(1980, 0, 1, R.style.NumberPickerStyle);
            }

        });

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

    private boolean canMakeSmores() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    @Override
    public void onDateSet(com.tsongkha.spinnerdatepicker.DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        Calendar calendar = new GregorianCalendar(year, monthOfYear, dayOfMonth);
        dob.setError(null);
        dob.setText(simpleDateFormat.format(calendar.getTime()));
    }

    @VisibleForTesting
    void showDate(int year, int monthOfYear, int dayOfMonth, int spinnerTheme) {
        new SpinnerDatePickerDialogBuilder()
                .context(getContext())
                .callback(this)
                .spinnerTheme(R.style.NumberPickerStyle)
                .defaultDate(year, monthOfYear, dayOfMonth)
                .build()
                .show();
    }

    public void init() {
        if (realmProvider != null) {
            maritalstatus.setSelection(((ArrayAdapter) maritalstatus.getAdapter()).getPosition(realmProvider.getMarital_status()));

            /*Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).executeTransaction(realm -> {
                String emailaddress = Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).where(RealmUser.class).findFirst().getEmail();
                email.setText(emailaddress);
            });*/
            primarycontact.setText(realmProvider.getPrimary_contact());
            auxiliarycontact.setText(realmProvider.getAuxiliary_contact());
//            postaladdress.setText(realmProvider.getPostal_address());
            DecimalFormat formatter = new DecimalFormat("#0.00");
            google_location.setText(formatter.format(realmProvider.getLatitude()) + ", " + formatter.format(realmProvider.getLongitude()));
            dob.setText(realmProvider.getDob());
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
        if (TextUtils.isEmpty(dob.getText())) {
            dob.setError(getString(R.string.error_field_required));
            validated = false;
        }
        if (maritalstatus.getSelectedItemPosition() == 0) {
            TextView errorText = (TextView) maritalstatus.getSelectedView();
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
