package com.service.provision.activity;

import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.activity.ProviderHomeActivity.MYUSERID;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.myVolleyError;
import static com.service.provision.receiver.NetworkReceiver.activeActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.greysonparrelli.permiso.PermisoActivity;
import com.service.provision.R;
import com.service.provision.adapter.ProductListAdapter;
import com.service.provision.materialDialog.InstitutionLoginMaterialDialog;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmProductCategory;
import com.service.provision.receiver.NetworkReceiver;
import com.service.provision.util.RealmUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;

public class MyProductListActivity extends PermisoActivity {

    RecyclerView recyclerView;
    ImageView loadinggif;
    ImageView backbtn;
    Button retrybtn;
    LinearLayout retry_layout;
    TextView titleTextView, text;
    ArrayList<String> newList = new ArrayList<>();
    ArrayList<String> list = new ArrayList<>();
    ProductListAdapter listAdapter;
    String title = "";
    String tag = "";
    String url = "";
    Context mContext;
    NetworkReceiver networkReceiver;

    ArrayList<RealmProductCategory> realmProductCategoryArrayList = new ArrayList<>();

    static InstitutionLoginMaterialDialog institutionLoginMaterialDialog = new InstitutionLoginMaterialDialog();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_list);

        mContext = getApplicationContext();
        loadinggif = findViewById(R.id.loadinggif);
        retry_layout = findViewById(R.id.retry_layout);
        retrybtn = findViewById(R.id.retrybtn);
        Glide.with(getApplicationContext()).asGif().load(R.drawable.spinner).apply(new RequestOptions()
                .centerCrop()
                .placeholder(R.drawable.spinner)
                .error(R.drawable.error)).into(loadinggif);
        text = findViewById(R.id.text);
        recyclerView = findViewById(R.id.recyclerView);
        titleTextView = findViewById(R.id.title);
        backbtn = findViewById(R.id.search);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        title = getIntent().getStringExtra("title");
        tag = getIntent().getStringExtra("tag");
        url = getIntent().getStringExtra("url");
        titleTextView.setText(title);

        populateNames(title.concat(" ").concat(">>").concat(" "));
        backbtn.setOnClickListener(v -> finish());
        retrybtn.setOnClickListener(v -> populateNames(title.concat(" ").concat(">>").concat(" ")));

        networkReceiver = new NetworkReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        activeActivity = this;
        registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(networkReceiver);
    }

    private void populateNames(String search) {

        try {
            loadinggif.setVisibility(View.VISIBLE);
            StringRequest stringRequest = new StringRequest(
                    Request.Method.POST,
                    API_URL + "sub-product-categories",
                    response -> {
                        loadinggif.setVisibility(View.GONE);
                        retry_layout.setVisibility(View.GONE);
                        if (response != null) {
                            try {
                                JSONArray jsonArray = new JSONArray(response);
                                int length = jsonArray.length();
                                if (length == 0) {
                                    setResult(RESULT_OK, new Intent()
                                            .putExtra("PRODUCT_CATEGORY", title)
                                            .putExtra("url", url)
                                            .putExtra("tag", tag));
                                    finish();
                                }
                                realmProductCategoryArrayList.clear();
                                for (int i = 0; i < length; i++) {

                                    Realm.init(getApplicationContext());
                                    int finalI = i;
                                    Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).executeTransaction(new Realm.Transaction() {
                                        @Override
                                        public void execute(Realm realm) {
                                            RealmProductCategory realmProductCategory = null;
                                            try {
                                                realmProductCategory = realm.createOrUpdateObjectFromJson(RealmProductCategory.class, jsonArray.getJSONObject(finalI));
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            realmProductCategoryArrayList.add(realmProductCategory);
                                        }
                                    });
                                }
                                listAdapter = new ProductListAdapter((realmProductCategories, position, holder) -> {
                                    RealmProductCategory realmProductCategory = realmProductCategories.get(position);
                                    String textViewText = realmProductCategory.getTitle();
                                    startActivityForResult(new Intent(getApplicationContext(), MyProductListActivity.class)
                                                    .putExtra("title", realmProductCategory.getTitle())
                                                    .putExtra("tag", realmProductCategory.getTag())
                                                    .putExtra("url", realmProductCategory.getUrl())
                                                    .putExtra("initiator", getIntent().getStringExtra("initiator")),
                                            1915
                                    );

                                }, this, realmProductCategoryArrayList, "");
                                recyclerView.setAdapter(listAdapter);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    },
                    error -> {
                        loadinggif.setVisibility(View.GONE);
                        retry_layout.setVisibility(View.VISIBLE);
                        myVolleyError(mContext, error);
                    }
            ) {
                @Override
                public Map<String, String> getParams() throws AuthFailureError {

                    Map<String, String> params = new HashMap<>();
                    params.put("userid", PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(MYUSERID, ""));
                    params.put("search", search);
                    return params;
                }
                @Override
                public Map getHeaders() throws AuthFailureError {
                    HashMap headers = new HashMap();
                    headers.put("accept", "application/json");
                    headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(mContext).getString(APITOKEN, ""));
                    return headers;
                }
            };
            stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                    0,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            InitApplication.getInstance().addToRequestQueue(stringRequest);

        } catch (Exception e) {
            Log.e("My error", e.toString());
        }
    }

    public static void institutionLogin(Activity activity, String studentidno, String password, String institutionid, String title, ArrayList<String> courses, JSONArray jsonArray, RecyclerView recyclerView) {
        try {


            ProgressDialog mProgress = new ProgressDialog(activity);
            mProgress.setTitle("Signig in...");
            mProgress.setMessage("Please wait...");
            mProgress.setCancelable(false);
            mProgress.setIndeterminate(true);

            mProgress.show();

            JSONObject request = new JSONObject();
            request.put("studentid", PreferenceManager.getDefaultSharedPreferences(activity).getString(MYUSERID, ""));
            request.put("studentno", studentidno);
            request.put("password", password);
            request.put("institutionid", institutionid);

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    API_URL + "student-institution-login",
                    request,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            mProgress.dismiss();
                            if (response != null) {

                                try {
                                    if (response.has("user_not_found")) {
                                        mProgress.dismiss();
                                        Toast.makeText(activity, "Student id number not found for your student account.", Toast.LENGTH_SHORT).show();
                                    } else if (response.has("incorrect_password")) {
                                        mProgress.dismiss();
                                        Toast.makeText(activity, "Incorrect password!", Toast.LENGTH_SHORT).show();
                                    } else if (response.has("login_successful")) {
                                        institutionLoginMaterialDialog.dismiss();

                                        int length = jsonArray.length();
                                        courses.clear();
                                        for (int i = 0; i < length; i++) {
                                            try {
                                                courses.add(jsonArray.getString(i));
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                } catch (Throwable t) {
                                    Log.d("My App", "Could not parse malformed JSON: " + t.toString());
                                }
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d("hmmm eii", "error");
                            mProgress.dismiss();
                            myVolleyError(activity, error);
                        }
                    }
            ) {
                @Override
                public Map getHeaders() throws AuthFailureError {
                    HashMap headers = new HashMap();
                    headers.put("accept", "application/json");
                    headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(activity).getString(APITOKEN, ""));
                    return headers;
                }
            };
            ;
            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                    0,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            InitApplication.getInstance().addToRequestQueue(jsonObjectRequest);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1915:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        if (data != null) {
                            setResult(RESULT_OK, new Intent()
                                    .putExtra("PRODUCT_CATEGORY", data.getStringExtra("PRODUCT_CATEGORY"))
                                    .putExtra("tag", data.getStringExtra("tag"))
                                    .putExtra("url", data.getStringExtra("url"))
                            );
                            finish();
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
}
