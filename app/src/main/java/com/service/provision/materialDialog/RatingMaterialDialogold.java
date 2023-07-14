package com.service.provision.materialDialog;

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
import com.service.provision.realm.RealmInstructorCourse;
import com.service.provision.realm.RealmInstructorCourseRating;
import com.service.provision.util.RealmUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.realm.Realm;

import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.activity.ProviderHomeActivity.MYUSERID;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.myVolleyError;

/**
 * Created by Nana on 10/22/2017.
 */

public class RatingMaterialDialogold extends DialogFragment {

    private static final String TAG = "SubscriptionMaterialDialog";

    TextView course, rating, name;
    TextView cancel, submit;
    EditText reviewtext;
    RoundedImageView profilepic;
    RatingBar ratingbar;
    RatingMaterialDialogold ratingMaterialDialog;

    String instructorcourseid;
    String coursepath;
    String profilepicurl;
    String instructorname;


    public String getInstructorcourseid() {
        return instructorcourseid;
    }

    public void setInstructorcourseid(String instructorcourseid) {
        this.instructorcourseid = instructorcourseid;
    }

    public String getCoursepath() {
        return coursepath;
    }

    public void setCoursepath(String coursepath) {
        this.coursepath = coursepath;
    }

    public String getProfilepicurl() {
        return profilepicurl;
    }

    public void setProfilepicurl(String profilepicurl) {
        this.profilepicurl = profilepicurl;
    }

    public String getInstructorname() {
        return instructorname;
    }

    public void setInstructorname(String instructorname) {
        this.instructorname = instructorname;
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.list_item_ratingold, null);

        course = view.findViewById(R.id.course);
        rating = view.findViewById(R.id.total_rating);
        name = view.findViewById(R.id.provider_name);
        cancel = view.findViewById(R.id.cancel);
        submit = view.findViewById(R.id.submit);
        reviewtext = view.findViewById(R.id.reviewtext);
        profilepic = view.findViewById(R.id.profilepic);
        ratingbar = view.findViewById(R.id.ratingbar);

        course.setText(coursepath);
        name.setText(instructorname);
        Glide.with(getActivity()).load(profilepicurl).apply( new RequestOptions().centerCrop()).into(profilepic);

        ratingbar.setOnRatingBarChangeListener((ratingBar, myrating, fromUser) -> rating.setText(String.valueOf(myrating)));

        ratingMaterialDialog = RatingMaterialDialogold.this;

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
            request.put("studentid", PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(MYUSERID, ""));
            request.put("instructorcourseid", instructorcourseid);
            switch ((int) ratingbar.getRating()) {
                case 1:
                    request.put("onestar", 1);
                    break;
                case 2:
                    request.put("twostar", 1);
                    break;
                case 3:
                    request.put("threestar", 1);
                    break;
                case 4:
                    request.put("fourstar", 1);
                    break;
                case 5:
                    request.put("fivestar", 1);
                    break;
            }
            request.put("review", reviewtext.getText().toString());

            ProgressDialog progressDialog = new ProgressDialog(getActivity());
            progressDialog.setTitle(getString(R.string.processing));
            progressDialog.setMessage(getString(R.string.pls_wait));
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);

            progressDialog.show();

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    API_URL + "instructor-course-ratings",
                    request,
                    response -> {
                        if (response != null) {
                            Realm.init(getActivity());
                            Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).executeTransaction(realm -> {
                                try {
                                    realm.createOrUpdateObjectFromJson(RealmInstructorCourse.class, response.getJSONObject("instructor_course"));
                                    realm.createOrUpdateObjectFromJson(RealmInstructorCourseRating.class, response.getJSONObject("instructor_course_rating"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            });

                            progressDialog.dismiss();
                            ratingMaterialDialog.dismiss();
                            Toast.makeText(getActivity(), getString(R.string.successfully_submitted), Toast.LENGTH_LONG).show();
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