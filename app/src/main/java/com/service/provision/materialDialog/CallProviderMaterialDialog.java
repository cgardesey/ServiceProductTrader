package com.service.provision.materialDialog;

import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.isValidMtnno;
import static com.service.provision.constants.Const.myVolleyError;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.service.provision.R;
import com.service.provision.other.InitApplication;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class CallProviderMaterialDialog extends DialogFragment {
    String phone_number, customer_id, provider_id;

    public String getPhone_number() {
        return phone_number;
    }

    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }

    public String getCustomer_id() {
        return customer_id;
    }

    public void setCustomer_id(String customer_id) {
        this.customer_id = customer_id;
    }

    public String getProvider_id() {
        return provider_id;
    }

    public void setProvider_id(String provider_id) {
        this.provider_id = provider_id;
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.list_item_phonenumber,null);
        TextView ok = view.findViewById(R.id.ok);
        TextView number = view.findViewById(R.id.number);
        number.setText(phone_number);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mobileno = number.getText().toString();
                if (!TextUtils.isEmpty(mobileno) && !isValidMtnno(mobileno)){
                    Toast.makeText(getActivity(), getString(R.string.invalid_number), Toast.LENGTH_LONG).show();
                }
                else {
                    ProgressDialog mProgress = new ProgressDialog(getActivity());
                    mProgress.setTitle("Processing...");
                    mProgress.setMessage("Please wait...");
                    mProgress.setCancelable(false);
                    mProgress.setIndeterminate(true);
                    mProgress.show();
                    StringRequest stringRequest = new StringRequest(
                            Request.Method.POST,
                            API_URL + "group-call",
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    mProgress.dismiss();
                                    if (response != null) {
                                        try {
                                            JSONObject jsonObject = new JSONObject(response);
                                            dismiss();
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    mProgress.dismiss();
                                    myVolleyError(getActivity(), error);
                                }
                            }
                    )
                    {
                        /** Passing some request headers* */
                        @Override
                        public Map getHeaders() throws AuthFailureError {
                            HashMap headers = new HashMap();
                            headers.put("accept", "application/json");
                            headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(APITOKEN, ""));
                            return headers;
                        }

                        @Override
                        public Map<String, String> getParams() throws AuthFailureError {

                            Map<String, String> params = new HashMap<>();

                            String role = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("ROLE", "");
                            if (role.equals("CUSTOMER")) {
                                params.put("provider_id", provider_id);
                            }
                            else {
                                params.put("customer_id", customer_id);
                            }
                            params.put("phone_number", mobileno);
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
}