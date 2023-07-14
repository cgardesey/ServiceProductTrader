package com.service.provision.materialDialog;

import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.API_URL;
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
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.makeramen.roundedimageview.RoundedImageView;
import com.service.provision.R;
import com.service.provision.other.InitApplication;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.realm.Realm;

/**
 * Created by Nana on 10/22/2017.
 */

public class RatingMaterialDialog extends DialogFragment {

    private static final String TAG = "SubscriptionMaterialDialog";

    TextView trips_made, rating, name;
    TextView cancel, submit;
    EditText review;
    RoundedImageView profilepic;
    RatingBar ratingbar;
    RatingMaterialDialog ratingMaterialDialog;

    String service_id;
    String trips_made_text;
    String profilepic_text;
    String name_text;


    public String getService_id() {
        return service_id;
    }

    public void setService_id(String service_id) {
        this.service_id = service_id;
    }

    public String getTrips_made_text() {
        return trips_made_text;
    }

    public void setTrips_made_text(String trips_made_text) {
        this.trips_made_text = trips_made_text;
    }

    public String getProfilepic_text() {
        return profilepic_text;
    }

    public void setProfilepic_text(String profilepic_text) {
        this.profilepic_text = profilepic_text;
    }

    public String getName_text() {
        return name_text;
    }

    public void setName_text(String name_text) {
        this.name_text = name_text;
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.list_item_rating, null);

        trips_made = view.findViewById(R.id.trips_made);
        rating = view.findViewById(R.id.total_rating);
        name = view.findViewById(R.id.name);
        cancel = view.findViewById(R.id.cancel);
        submit = view.findViewById(R.id.submit);
        review = view.findViewById(R.id.reviewtext);
        profilepic = view.findViewById(R.id.profilepic);
        ratingbar = view.findViewById(R.id.ratingbar);
        trips_made = view.findViewById(R.id.trips_made);

        trips_made.setText(trips_made_text);
        name.setText(name_text);
        Glide.with(getActivity()).load(profilepic_text).apply( new RequestOptions().centerCrop()).into(profilepic);

        ratingbar.setOnRatingBarChangeListener((ratingBar, myrating, fromUser) -> rating.setText(String.valueOf(myrating)));

        ratingMaterialDialog = RatingMaterialDialog.this;

        cancel.setOnClickListener(v -> dismiss());

        submit.setOnClickListener(v -> {
            if (ratingbar.getRating() > 0) {
                submitRaing();
            } else {
                Toast.makeText(getActivity(), getString(R.string.minimum_rating_allowed_is_onestar), Toast.LENGTH_LONG).show();
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
                            ratingMaterialDialog.dismiss();
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