package com.service.provision.util;

import android.app.Activity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.service.provision.other.InitApplication;

import org.json.JSONException;

import java.util.Map;

public class MyVolleyRequest {

    Activity activity;
    String url;
    String tag;
    OnResponse onResponseInterface;
    OnError onErrorInterface;
    Map headers;
    Map<String, String> params;

    public MyVolleyRequest(Activity activity, String url, String tag, Map headers, Map<String, String> params, OnResponse onResponseInterface, OnError onErrorInterface) {
        this.activity = activity;
        this.url = url;
        this.tag = tag;
        this.headers = headers;
        this.params = params;
        this.onResponseInterface = onResponseInterface;
        this.onErrorInterface = onErrorInterface;
    }

    public void Query() {
        StringRequest stringRequest = new StringRequest(
                com.android.volley.Request.Method.POST,
                url,
                response -> {
                    try {
                        onResponseInterface.onResponse(response);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    onErrorInterface.onError(error);
                }
        ) {
            @Override
            public Map<String, String> getParams() throws AuthFailureError {
                return params;
            }

            /** Passing some request headers* */
            @Override
            public Map getHeaders() throws AuthFailureError {
                return headers;
            }
        };
        stringRequest.setRetryPolicy(new

                DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        InitApplication.getInstance().addToRequestQueue(stringRequest, tag);
    }

    public interface OnResponse {
        void onResponse(String response) throws JSONException;
    }

    public interface OnError {
        void onError(VolleyError error);
    }
}
