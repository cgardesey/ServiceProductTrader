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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.service.provision.R;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmCustomer;
import com.service.provision.realm.RealmPayment;
import com.service.provision.util.RealmUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.realm.Realm;

public class MomonumberMaterialDialog extends DialogFragment {
    public static ProgressDialog dialog1;
    String type, amount, cart_id;

    public String getCart_id() {
        return cart_id;
    }

    public void setCart_id(String cart_id) {
        this.cart_id = cart_id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.list_item_momonumber,null);
        TextView ok = view.findViewById(R.id.ok);
        TextView number = view.findViewById(R.id.number);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String momonumber = number.getText().toString();
                if (!TextUtils.isEmpty(momonumber) && !isValidMtnno(momonumber)){
                    Toast.makeText(getActivity(), getString(R.string.invalid_number), Toast.LENGTH_LONG).show();
                }
                else {
                    ProgressDialog mProgress = new ProgressDialog(getActivity());
                    mProgress.setCancelable(false);
                    mProgress.setIndeterminate(true);

                    mProgress.setTitle("Please wait...");
                    mProgress.show();

                    StringRequest stringRequest = new StringRequest(
                            Request.Method.POST,
                            API_URL + "pay",
                            response -> {
                                mProgress.dismiss();
                                if (response != null) {
                                    try {
                                        JSONObject jsonObject = new JSONObject(response);
                                        if (jsonObject.has("internal_error")) {
                                            new AlertDialog.Builder(getActivity())
                                                    .setTitle("Error.")
//                                            .setMessage(getWaitTimeMsg(response.getInt("wait_time")))
                                                    .setMessage("Error occurred. \n\nPlease try again later.")
                                                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                                        getActivity().finish();
                                                    })
                                                    .setCancelable(false)
                                                    .show();
                                        }
                                        else if (jsonObject.has("wait_time")) {
                                            new AlertDialog.Builder(getActivity())
                                                    .setTitle(getString(R.string.pending_payment))
//                                            .setMessage(getWaitTimeMsg(response.getInt("wait_time")))
                                                    .setMessage(getString(R.string.try_again_later))
                                                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                                        getActivity().finish();
                                                    })
                                                    .setCancelable(false)
                                                    .show();
                                        }
                                        else if (jsonObject.has("payments")) {
                                            Realm.init(getActivity());
                                            Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).executeTransaction(realm -> {
                                                try {
                                                    realm.createOrUpdateAllFromJson(RealmPayment.class, jsonObject.getJSONArray("payments"));
                                                    getActivity().finish();
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            });
                                        }
                                        else {
                                            new AlertDialog.Builder(getActivity())
                                                    .setTitle("Error.")
//                                            .setMessage(getWaitTimeMsg(response.getInt("wait_time")))
                                                    .setMessage("Error occurred. \n\nPlease try again later.")
                                                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                                        getActivity().finish();
                                                    })
                                                    .setCancelable(false)
                                                    .show();
                                        }

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            },
                            error -> {
                                mProgress.dismiss();
                                error.printStackTrace();
                                myVolleyError(getActivity(), error);
                                Log.d("Cyrilll", error.toString());
                            }
                    ) {
                        @Override
                        public Map<String, String> getParams() throws AuthFailureError {

                            Realm.init(getActivity());
                            String customer_id = Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).where(RealmCustomer.class).findFirst().getCustomer_id();

                            Map<String, String> params = new HashMap<>();
                            params.put("msisdn", "233" + momonumber.substring(1));
                            params.put("country_code", "GH");
                            params.put("network", "MTNGHANA");
                            params.put("currency", "GHS");
                            params.put("amount", amount);
                            params.put("cart_id", cart_id);
                            params.put("customer_id", customer_id);
                            return params;
                        }
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
                }
            }
        });
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