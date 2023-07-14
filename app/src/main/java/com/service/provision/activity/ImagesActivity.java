package com.service.provision.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kbeanie.multipicker.api.entity.ChosenImage;
import com.kbeanie.multipicker.api.entity.ChosenVideo;
import com.noelchew.multipickerwrapper.library.MultiPickerWrapper;
import com.noelchew.multipickerwrapper.library.ui.MultiPickerWrapperAppCompatActivity;
import com.service.provision.R;
import com.service.provision.adapter.ImagesAdapter;
import com.service.provision.constants.Const;
import com.service.provision.other.InitApplication;
import com.service.provision.other.MyHttpEntity;
import com.service.provision.realm.RealmProductImage;
import com.service.provision.realm.RealmServiceImage;
import com.service.provision.receiver.NetworkReceiver;
import com.service.provision.util.PixelUtil;
import com.service.provision.util.RealmUtility;
import com.yalantis.ucrop.UCrop;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;

import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.myVolleyError;
import static com.service.provision.receiver.NetworkReceiver.activeActivity;


public class ImagesActivity extends MultiPickerWrapperAppCompatActivity {

    protected static Typeface mTfLight;
    private static final int REQUEST_MEDIA = 1002;
    NetworkReceiver networkReceiver;
    Button retrybtn;
    TextView noimages;
    RecyclerView recyclerview;
    ImagesAdapter imagesAdapter;
    GridLayoutManager gridLayoutManager;
    TextView titletextview, activitytitle;
    private ImageView backbtn;
    ArrayList<Object> objects = new ArrayList<>(), newObjects = new ArrayList<>();
    public static Activity recordedConferenceCallActivity;
    FloatingActionButton addImage;
    public static File image_file = null;
    private ProgressDialog mProgress;

    MultiPickerWrapper.PickerUtilListener multiPickerWrapperListener = new MultiPickerWrapper.PickerUtilListener() {
        @Override
        public void onPermissionDenied() {
            // do something here
        }

        @Override
        public void onImagesChosen(List<ChosenImage> list) {
            image_file = new File(list.get(0).getOriginalPath());
            if (getIntent().hasExtra("SERVICEID")) {
                new uploadServiceImageAsync(getApplicationContext(), getIntent().getStringExtra("SERVICEID")).execute();
            } else {
                new uploadProductImageAsync(getApplicationContext(), getIntent().getStringExtra("PRODUCTID")).execute();
            }
        }

        @Override
        public void onVideosChosen(List<ChosenVideo> list) {
            Const.showToast(getApplicationContext(), getString(R.string.unsupported_file_format));
        }

        @Override
        public void onError(String s) {
            Toast.makeText(getApplicationContext(), getString(R.string.error_choosing_image), Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected MultiPickerWrapper.PickerUtilListener getMultiPickerWrapperListener() {
        return multiPickerWrapperListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_images);

        mProgress = new ProgressDialog(this);
        mProgress.setTitle("Uploading image...");
        mProgress.setMessage(getString(R.string.please_wait));
        mProgress.setCancelable(false);
        mProgress.setIndeterminate(true);

        recyclerview = findViewById(R.id.recyclerview);
        recordedConferenceCallActivity = this;
        noimages = findViewById(R.id.noimages);
        backbtn = findViewById(R.id.backbtn1);
        titletextview = findViewById(R.id.title);
        activitytitle = findViewById(R.id.activitytitle);
        titletextview.setText(getIntent().getStringExtra("TITLE"));

        if (getIntent().hasExtra("SERVICEID")) {
            activitytitle.setText("Service Images");
        } else {
            activitytitle.setText("Product Images");
        }

        addImage = findViewById(R.id.addImage);
        addImage.setOnClickListener(v -> {
            multiPickerWrapper.getPermissionAndPickSingleImageAndCrop(imgOptions(), 1, 1);
        });

        backbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        imagesAdapter = new ImagesAdapter((objects, position, holder) -> {
            Object object = objects.get(position);
            PopupMenu popup = new PopupMenu(ImagesActivity.this, holder.more_details);

            popup.inflate(R.menu.image_menu);

            popup.setOnMenuItemClickListener(item -> {

                if (object instanceof RealmServiceImage) {
                    RealmServiceImage realmServiceImage = (RealmServiceImage) object;
                    final String service_image_id = realmServiceImage.getService_image_id();
                    int itemId = item.getItemId();
                    if (itemId == R.id.delete) {
                        StringRequest stringRequest = new StringRequest(
                                Request.Method.DELETE,
                                API_URL + "service-images/" + service_image_id,
                                response -> {
                                    if (response != null) {
                                        try {
                                            JSONObject jsonObject = new JSONObject(response);
                                            if (jsonObject.getBoolean("status")) {
                                                Realm.init(getApplicationContext());
                                                Realm.getInstance(RealmUtility.getDefaultConfig(ImagesActivity.this)).executeTransaction(realm -> {
                                                    realm.where(RealmServiceImage.class).equalTo("service_image_id", service_image_id).findFirst().deleteFromRealm();
                                                });
                                                objects.remove(position);
                                                imagesAdapter.notifyDataSetChanged();

                                                if (objects.size() > 0) {
                                                    noimages.setVisibility(View.GONE);
                                                    recyclerview.setVisibility(View.VISIBLE);
                                                } else {
                                                    noimages.setVisibility(View.VISIBLE);
                                                    recyclerview.setVisibility(View.GONE);
                                                }
                                                Toast.makeText(ImagesActivity.this, "Successfully deleted.", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(ImagesActivity.this, "Error deleting.", Toast.LENGTH_SHORT).show();
                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                },
                                error -> {
                                    error.printStackTrace();
                                    myVolleyError(ImagesActivity.this, error);
                                    Log.d("Cyrilll", error.toString());
                                }
                        ) {
                            @Override
                            public Map getHeaders() throws AuthFailureError {
                                HashMap headers = new HashMap();
                                headers.put("accept", "application/json");
                                headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(APITOKEN, ""));
                                return headers;
                            }
                        };
                        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                                0,
                                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                        InitApplication.getInstance().addToRequestQueue(stringRequest);
                        return true;
                    } else if (itemId == R.id.featured_layout) {
                        try {
                            mProgress = new ProgressDialog(ImagesActivity.this);
                            mProgress.setMessage(ImagesActivity.this.getString(R.string.pls_wait));
                            mProgress.setCancelable(false);
                            mProgress.setIndeterminate(true);
                            mProgress.show();
                            StringRequest stringRequest1 = new StringRequest(
                                    Request.Method.PATCH,
                                    API_URL + "service-images/" + service_image_id,
                                    response -> {
                                        mProgress.dismiss();
                                        if (response != null) {
                                            Toast.makeText(ImagesActivity.this, "Featured image successfully set.", Toast.LENGTH_LONG).show();
                                            try {
                                                JSONArray jsonArray = new JSONArray(response);
                                                Realm.init(getApplicationContext());
                                                Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).executeTransaction(realm -> {
                                                    realm.createOrUpdateAllFromJson(RealmServiceImage.class, jsonArray);
                                                });
                                                populateImages(getApplicationContext());
                                                imagesAdapter.notifyDataSetChanged();
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    },
                                    error -> {
                                        mProgress.dismiss();
                                        error.printStackTrace();
                                        myVolleyError(ImagesActivity.this, error);
                                        Log.d("Cyrilll", error.toString());
                                    }
                            ) {
                                @Override
                                public Map getHeaders() throws AuthFailureError {
                                    HashMap headers = new HashMap();
                                    headers.put("accept", "application/json");
                                    headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(APITOKEN, ""));
                                    return headers;
                                }
                            };
                            stringRequest1.setRetryPolicy(new DefaultRetryPolicy(
                                    0,
                                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                            InitApplication.getInstance().addToRequestQueue(stringRequest1);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return true;
                    }
                }
                else if (object instanceof RealmProductImage) {
                    RealmProductImage realmProductImage = (RealmProductImage) object;
                    final String product_image_id = realmProductImage.getProduct_image_id();
                    int itemId = item.getItemId();
                    if (itemId == R.id.delete) {
                        StringRequest stringRequest = new StringRequest(
                                Request.Method.DELETE,
                                API_URL + "product-images/" + product_image_id,
                                response -> {
                                    if (response != null) {
                                        try {
                                            JSONObject jsonObject = new JSONObject(response);
                                            if (jsonObject.getBoolean("status")) {
                                                Realm.init(getApplicationContext());
                                                Realm.getInstance(RealmUtility.getDefaultConfig(ImagesActivity.this)).executeTransaction(realm -> {
                                                    realm.where(RealmProductImage.class).equalTo("product_image_id", product_image_id).findFirst().deleteFromRealm();
                                                });
                                                objects.remove(position);
                                                imagesAdapter.notifyDataSetChanged();

                                                if (objects.size() > 0) {
                                                    noimages.setVisibility(View.GONE);
                                                    recyclerview.setVisibility(View.VISIBLE);
                                                } else {
                                                    noimages.setVisibility(View.VISIBLE);
                                                    recyclerview.setVisibility(View.GONE);
                                                }
                                                Toast.makeText(ImagesActivity.this, "Successfully deleted.", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(ImagesActivity.this, "Error deleting.", Toast.LENGTH_SHORT).show();
                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                },
                                error -> {
                                    error.printStackTrace();
                                    myVolleyError(ImagesActivity.this, error);
                                    Log.d("Cyrilll", error.toString());
                                }
                        ) {
                            @Override
                            public Map getHeaders() throws AuthFailureError {
                                HashMap headers = new HashMap();
                                headers.put("accept", "application/json");
                                headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(APITOKEN, ""));
                                return headers;
                            }
                        };
                        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                                0,
                                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                        InitApplication.getInstance().addToRequestQueue(stringRequest);
                        return true;
                    } else if (itemId == R.id.featured_layout) {
                        try {
                            mProgress = new ProgressDialog(ImagesActivity.this);
                            mProgress.setMessage(ImagesActivity.this.getString(R.string.pls_wait));
                            mProgress.setCancelable(false);
                            mProgress.setIndeterminate(true);
                            mProgress.show();
                            StringRequest stringRequest1 = new StringRequest(
                                    Request.Method.PATCH,
                                    API_URL + "product-images/" + product_image_id,
                                    response -> {
                                        mProgress.dismiss();
                                        if (response != null) {
                                            Toast.makeText(ImagesActivity.this, "Featured image successfully set.", Toast.LENGTH_LONG).show();
                                            try {
                                                JSONArray jsonArray = new JSONArray(response);
                                                Realm.init(getApplicationContext());
                                                Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).executeTransaction(realm -> {
                                                    realm.createOrUpdateAllFromJson(RealmProductImage.class, jsonArray);
                                                });
                                                populateImages(getApplicationContext());
                                                imagesAdapter.notifyDataSetChanged();
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    },
                                    error -> {
                                        mProgress.dismiss();
                                        error.printStackTrace();
                                        myVolleyError(ImagesActivity.this, error);
                                        Log.d("Cyrilll", error.toString());
                                    }
                            ) {
                                @Override
                                public Map getHeaders() throws AuthFailureError {
                                    HashMap headers = new HashMap();
                                    headers.put("accept", "application/json");
                                    headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(APITOKEN, ""));
                                    return headers;
                                }
                            };
                            stringRequest1.setRetryPolicy(new DefaultRetryPolicy(
                                    0,
                                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                            InitApplication.getInstance().addToRequestQueue(stringRequest1);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return true;
                    }
                }
                return false;
            });
            popup.show();
        }, ImagesActivity.this, objects, "all");
        if (isTablet(getApplicationContext())) {
            gridLayoutManager = new GridLayoutManager(getApplicationContext(), 4);
        } else {
            gridLayoutManager = new GridLayoutManager(getApplicationContext(), 3);
        }
        recyclerview.setLayoutManager(gridLayoutManager);
        recyclerview.setNestedScrollingEnabled(false);
        recyclerview.setItemAnimator(new DefaultItemAnimator());
        recyclerview.setAdapter(imagesAdapter);

        populateImages(getApplicationContext());
        imagesAdapter.notifyDataSetChanged();

        String role = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("ROLE", "");
        if (role.equals("CUSTOMER")) {
            addImage.setVisibility(View.GONE);
        }
        else {
            addImage.setVisibility(View.VISIBLE);
        }

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

    private UCrop.Options imgOptions() {
        UCrop.Options options = new UCrop.Options();
        options.setStatusBarColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
        options.setToolbarColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary));
        options.setCropFrameColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
        options.setCropFrameStrokeWidth(PixelUtil.dpToPx(getApplicationContext(), 4));
        options.setCropGridColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary));
        options.setCropGridStrokeWidth(PixelUtil.dpToPx(getApplicationContext(), 2));
        options.setActiveWidgetColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary));
        options.setToolbarTitle(getString(R.string.crop_image));

        // set rounded cropping guide
        options.setCircleDimmedLayer(true);
        return options;
    }

    void populateImages(final Context context) {
        newObjects.clear();

        Realm.init(context);
        Realm.getInstance(RealmUtility.getDefaultConfig(context)).executeTransaction(realm -> {
            String stringExtra;
            String fieldName;
            if (getIntent().hasExtra("SERVICEID")) {
                fieldName = "service_id";
                stringExtra = getIntent().getStringExtra("SERVICEID");

                RealmResults<RealmServiceImage> realmServiceImages = realm.where(RealmServiceImage.class)
                        .equalTo(fieldName, stringExtra)
                        .findAll();


                for (RealmServiceImage realmServiceImage : realmServiceImages) {
                    newObjects.add(realmServiceImage);
                }
            } else {
                stringExtra = getIntent().getStringExtra("PRODUCTID");
                fieldName = "product_id";

                RealmResults<RealmProductImage> realmProductImages = realm.where(RealmProductImage.class)
                        .equalTo(fieldName, stringExtra)
                        .findAll();


                for (RealmProductImage realmProductImage : realmProductImages) {
                    newObjects.add(realmProductImage);
                }
            }

            objects.clear();
            objects.addAll(newObjects);

            imagesAdapter.notifyDataSetChanged();
            if (objects.size() > 0) {
                noimages.setVisibility(View.GONE);
                recyclerview.setVisibility(View.VISIBLE);
            } else {
                noimages.setVisibility(View.VISIBLE);
                recyclerview.setVisibility(View.GONE);
            }
        });
    }

    public boolean isTablet(Context context) {
        boolean xlarge = ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4);
        boolean large = ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE);
        return (xlarge || large);
    }

    private class uploadServiceImageAsync extends AsyncTask<Void, Integer, String> {

        HttpClient httpClient = new DefaultHttpClient();
        private Context context;
        private String service_id;

        private uploadServiceImageAsync(Context context, String service_id) {
            this.context = context;
            this.service_id = service_id;
        }

        @Override
        protected String doInBackground(Void... voids) {
            HttpResponse httpResponse = null;
            HttpEntity httpEntity = null;
            String responseString = null;
            String URL = API_URL + "service-images";
            try {
                HttpPost httpPost = new HttpPost(URL);
                MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
                multipartEntityBuilder.addTextBody("service_id", service_id);
                multipartEntityBuilder.addTextBody("featured_image", objects.size() == 0 ? "1" : "0");
                if (image_file != null) {
                    multipartEntityBuilder.addPart("image_file", new FileBody(image_file));
                }
                // Progress listener - updates task's progress
                MyHttpEntity.ProgressListener progressListener =
                        progress -> publishProgress((int) progress);

                // POST
                httpPost.setEntity(new MyHttpEntity(multipartEntityBuilder.build(),
                        progressListener));
                httpPost.setHeader("accept", "application/json");
                httpPost.setHeader("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(APITOKEN, ""));


                httpResponse = httpClient.execute(httpPost);
                httpEntity = httpResponse.getEntity();

                int statusCode = httpResponse.getStatusLine().getStatusCode();

                if (statusCode == 200 || statusCode == 201) {
                    // Server response
                    responseString = EntityUtils.toString(httpEntity);
                }
            } catch (UnsupportedEncodingException | ClientProtocolException e) {
                responseString = e.getMessage();
                e.printStackTrace();
                Log.e("UPLOAD", e.getMessage());
            } catch (IOException e) {
                responseString = e.getMessage();
                Log.e("gardes", e.toString());
//                e.printStackTrace();
            }

            return responseString;
        }

        @Override
        protected void onPreExecute() {
            mProgress.show();
        }

        @Override
        protected void onPostExecute(String result) {
            mProgress.dismiss();
            if (result != null) {
                if (result.contains("connect")) {
                    Toast.makeText(getApplicationContext(), context.getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        Realm.init(ImagesActivity.this);
                        Realm.getInstance(RealmUtility.getDefaultConfig(ImagesActivity.this)).executeTransaction(realm -> {
                            realm.createOrUpdateObjectFromJson(RealmServiceImage.class, jsonObject);
                        });
                        populateImages(ImagesActivity.this);
                        imagesAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        Toast.makeText(getApplicationContext(), getString(R.string.error_occured), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.error_occured), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            // Update process
            /*progressbar.setProgress(progress[0]);
            statustext.setText(progress[0].toString() + "%  complete");*/
        }
    }

    private class uploadProductImageAsync extends AsyncTask<Void, Integer, String> {

        HttpClient httpClient = new DefaultHttpClient();
        private Context context;
        private String product_id;

        private uploadProductImageAsync(Context context, String product_id) {
            this.context = context;
            this.product_id = product_id;
        }

        @Override
        protected String doInBackground(Void... voids) {
            HttpResponse httpResponse = null;
            HttpEntity httpEntity = null;
            String responseString = null;
            String URL = API_URL + "product-images";
            try {
                HttpPost httpPost = new HttpPost(URL);
                MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
                multipartEntityBuilder.addTextBody("product_id", product_id);
                multipartEntityBuilder.addTextBody("featured_image", objects.size() == 0 ? "1" : "0");
                if (image_file != null) {
                    multipartEntityBuilder.addPart("image_file", new FileBody(image_file));
                }
                // Progress listener - updates task's progress
                MyHttpEntity.ProgressListener progressListener =
                        progress -> publishProgress((int) progress);

                // POST
                httpPost.setEntity(new MyHttpEntity(multipartEntityBuilder.build(),
                        progressListener));
                httpPost.setHeader("accept", "application/json");
                httpPost.setHeader("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(APITOKEN, ""));


                httpResponse = httpClient.execute(httpPost);
                httpEntity = httpResponse.getEntity();

                int statusCode = httpResponse.getStatusLine().getStatusCode();

                if (statusCode == 200 || statusCode == 201) {
                    // Server response
                    responseString = EntityUtils.toString(httpEntity);
                }
            } catch (UnsupportedEncodingException | ClientProtocolException e) {
                responseString = e.getMessage();
                e.printStackTrace();
                Log.e("UPLOAD", e.getMessage());
            } catch (IOException e) {
                responseString = e.getMessage();
                Log.e("gardes", e.toString());
//                e.printStackTrace();
            }

            return responseString;
        }

        @Override
        protected void onPreExecute() {
            mProgress.show();
        }

        @Override
        protected void onPostExecute(String result) {
            mProgress.dismiss();
            if (result != null) {
                if (result.contains("connect")) {
                    Toast.makeText(getApplicationContext(), context.getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        Realm.init(ImagesActivity.this);
                        Realm.getInstance(RealmUtility.getDefaultConfig(ImagesActivity.this)).executeTransaction(realm -> {
                            realm.createOrUpdateObjectFromJson(RealmProductImage.class, jsonObject);
                        });
                        populateImages(ImagesActivity.this);
                        imagesAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        Toast.makeText(getApplicationContext(), getString(R.string.error_occured), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.error_occured), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            // Update process
            /*progressbar.setProgress(progress[0]);
            statustext.setText(progress[0].toString() + "%  complete");*/
        }
    }
}
