package com.service.provision.materialDialog;

import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.base_fare;
import static com.service.provision.constants.Const.myVolleyError;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.service.provision.R;
import com.service.provision.other.InitApplication;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.realm.Realm;

import static com.service.provision.constants.Const.cost_per_min;
import static com.service.provision.constants.Const.cost_per_km;
import static com.service.provision.constants.Const.surge_boost_multiplier;
import static com.service.provision.constants.Const.other_fee;

/**
 * Created by Nana on 10/22/2017.
 */

public class RideEndedMaterialDialog extends DialogFragment {

    TextView rating, price;
    ImageView cancel;
    EditText reviewtext;
    RatingBar ratingbar;
    RideEndedMaterialDialog rideEndedMaterialDialog;

    String service_id;
    int duration;

    Float ride_distance = 20.00F;

    public String getService_id() {
        return service_id;
    }

    public void setService_id(String service_id) {
        this.service_id = service_id;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    private static final String TAG = "RideEndedMaterialDialog";


    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.list_item_ride_ended, null);

        rideEndedMaterialDialog = RideEndedMaterialDialog.this;

        rating = view.findViewById(R.id.total_rating);
        cancel = view.findViewById(R.id.cancel);
        reviewtext = view.findViewById(R.id.reviewtext);
        ratingbar = view.findViewById(R.id.ratingbar);
        price = view.findViewById(R.id.price);

        Float s = Float.valueOf(Math.round(base_fare + ((cost_per_min * duration) + (cost_per_km * ride_distance) * surge_boost_multiplier) + other_fee));
        price.setText("GHC" + String.format("%.2f", s));


        ratingbar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                if (ratingbar.getRating() > 0) {
                    submitRaing();
                } else {
                    Toast.makeText(getActivity(), getString(R.string.minimum_rating_allowed_is_onestar), Toast.LENGTH_LONG).show();
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        //  builder.setCancelable(false);
        Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // If you want to modify a view in your Activity
                getActivity().runOnUiThread(() -> getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)));
            }
        }, 5);
        return builder.create();
    }

    private void submitRaing() {

        try {
            JSONObject request = new JSONObject();
            request.put("service_id", service_id);
            switch ((int) ratingbar.getRating()) {
                case 1:
                    request.put("one_star", 1);
                    break;
                case 2:
                    request.put("two_star", 1);
                    break;
                case 3:
                    request.put("three_star", 1);
                    break;
                case 4:
                    request.put("four_star", 1);
                    break;
                case 5:
                    request.put("five_star", 1);
                    break;
            }

            ProgressDialog progressDialog = new ProgressDialog(getActivity());
            progressDialog.setTitle(getString(R.string.processing));
            progressDialog.setMessage(getString(R.string.pls_wait));
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);

            progressDialog.show();

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    API_URL + "service-ratings",
                    request,
                    response -> {
                        if (response != null) {
                            Realm.init(getActivity());

                            progressDialog.dismiss();
                            rideEndedMaterialDialog.dismiss();
                            Toast.makeText(getActivity(), "Successfully rated!", Toast.LENGTH_LONG).show();
                        }
                    },
                    error -> {
                        progressDialog.dismiss();
                        myVolleyError(getActivity(), error);
                    }
            ) {
                /** Passing some request headers* */
                @Override
                public Map getHeaders() throws AuthFailureError {
                    HashMap headers = new HashMap();
                    headers.put("accept", "application/json");
                    headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(APITOKEN, ""));
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
            Log.e("My error", e.toString());
        }
    }
}