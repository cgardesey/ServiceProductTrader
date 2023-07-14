package com.service.provision.materialDialog;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.service.provision.R;
import com.service.provision.activity.OrganisationalProviderAccountActivity;
import com.service.provision.activity.PersonalProviderAccountActivity;
import com.service.provision.realm.RealmProvider;

import java.util.Timer;
import java.util.TimerTask;

import io.realm.RealmList;

public class ChooseProviderAccountTypeMaterialDialog extends DialogFragment {

    ProgressDialog mProgress;
    RadioGroup rg;
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.list_item_choose_provider_account_type,null);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerview);
        rg = (RadioGroup) view.findViewById(R.id.radiogroup);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                RadioButton rb = view.findViewById(i);
                if (rb.getText().toString().trim().equals("Personal account")) {
                    startActivity(
                            new Intent(getContext(), PersonalProviderAccountActivity.class)
                                    .putExtra("MODE", "ADD")
                    );
                } else {
                    startActivity(
                            new Intent(getContext(), OrganisationalProviderAccountActivity.class)
                                    .putExtra("MODE", "ADD")
                    );
                }
                dismiss();
            }
        });


        // doneBtn.setOnClickListener(doneAction);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // If you want to modify a view in your Activity
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    }
                });
            }
        }, 5);
        return builder.create();
    }

    /*public void getIdentificatioTypes() {
        ProgressDialog mProgress;
        mProgress = new ProgressDialog(getActivity());
        mProgress.setMessage("Please wait...");
        mProgress.setCancelable(false);
        mProgress.setIndeterminate(true);
        mProgress.show();
        StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                API_URL + "identification-types",
                response -> {
                    mProgress.dismiss();
                    if (response != null) {
                        try {
                            JSONArray jsonArray = new JSONArray(response);

                            Realm.init(getActivity());
                            Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).executeTransaction(realm -> {
                                realm.createOrUpdateAllFromJson(RealmIdentificationType.class, jsonArray);
                                dismiss();
                                startActivity(new Intent(getContext(), PersonalProviderAccountActivity.class));
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                error -> {
                    mProgress.dismiss();
                    error.printStackTrace();
                    Log.d("Cyrilll", error.toString());
                    myVolleyError(getActivity(), error);
                }
        ){
            @Override
            public Map getHeaders() throws AuthFailureError {
                HashMap headers = new HashMap();
                headers.put("accept", "application/json");
                headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(APITOKEN, ""));
                return headers;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        InitApplication.getInstance().addToRequestQueue(stringRequest);
    }*/

    public void rbclick(View view) {
        int radiobuttonid = rg.getCheckedRadioButtonId();
        Toast.makeText(getActivity(), String.valueOf(radiobuttonid), Toast.LENGTH_SHORT).show();
    }
}