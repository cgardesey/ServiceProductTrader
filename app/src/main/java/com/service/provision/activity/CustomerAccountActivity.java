package com.service.provision.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.greysonparrelli.permiso.Permiso;
import com.greysonparrelli.permiso.PermisoActivity;
import com.service.provision.R;
import com.service.provision.constants.Const;
import com.service.provision.fragment.CustomerAccountFragment1;
import com.service.provision.other.MyHttpEntity;
import com.service.provision.pagerAdapter.CustomerAccountPageAdapter;
import com.service.provision.realm.RealmCustomer;
import com.service.provision.receiver.NetworkReceiver;
import com.service.provision.util.RealmUtility;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import io.realm.Realm;

import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.isNetworkAvailable;
import static com.service.provision.fragment.CustomerAccountFragment1.latitude;
import static com.service.provision.fragment.CustomerAccountFragment1.longitude;
import static com.service.provision.fragment.CustomerAccountFragment1.name;
import static com.service.provision.fragment.CustomerAccountFragment1.gender;
import static com.service.provision.fragment.CustomerAccountFragment1.primarycontact;
import static com.service.provision.fragment.CustomerAccountFragment1.auxiliarycontact;
import static com.service.provision.fragment.CustomerAccountFragment1.profile_pic_file;
import static com.service.provision.receiver.NetworkReceiver.activeActivity;

@SuppressWarnings("HardCodedStringLiteral")
public class CustomerAccountActivity extends PermisoActivity {

    public static RealmCustomer realmCustomer = new RealmCustomer();
    static Context context;


    boolean close = false;
    ViewPager mViewPager;
    CustomerAccountPageAdapter customerAccountPageAdapter;
    FloatingActionButton fab;

    RelativeLayout rootview;
    ProgressBar progressBar;
    String tag1 = "android:switcher:" + R.id.pageques + ":" + 0;
//    String tag2 = "android:switcher:" + R.id.pageques + ":" + 1;
    private ProgressDialog mProgress;
    NetworkReceiver networkReceiver;

    String customer_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getApplicationContext();

        networkReceiver = new NetworkReceiver();
        Permiso.getInstance().setActivity(this);

        setContentView(R.layout.activity_customer_account);
        getSupportActionBar().hide();

        mProgress = new ProgressDialog(this);
        mProgress.setTitle(getString(R.string.updating_profile));
        mProgress.setMessage(getString(R.string.please_wait));
        mProgress.setCancelable(false);
        mProgress.setIndeterminate(true);

        fab = findViewById(R.id.movenext);
        fab.setOnClickListener(v -> sendData());

        rootview = findViewById(R.id.root);

        progressBar = findViewById(R.id.pbar_pic);
        Realm.init(getApplicationContext());
        realmCustomer = Realm.getInstance(RealmUtility.getDefaultConfig(CustomerAccountActivity.this)).where(RealmCustomer.class).findFirst();
        customer_id = realmCustomer.getCustomer_id();
        longitude = realmCustomer.getLongitude();
        latitude = realmCustomer.getLatitude();
        customerAccountPageAdapter = new CustomerAccountPageAdapter(getSupportFragmentManager());
        mViewPager = findViewById(R.id.pageques);
        mViewPager.setAdapter(customerAccountPageAdapter);
//        mViewPager.setOffscreenPageLimit(1);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Permiso.getInstance().setActivity(this);
        activeActivity = this;
        registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(networkReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void sendData() {

        final CustomerAccountFragment1 tabFrag1 = (CustomerAccountFragment1) getSupportFragmentManager().findFragmentByTag(tag1);
//        final AccountFragment2 tabFrag2 = (AccountFragment2) getSupportFragmentManager().findFragmentByTag(tag2);

        Realm.init(context);
        Realm.getInstance(RealmUtility.getDefaultConfig(CustomerAccountActivity.this)).executeTransaction(realm -> {
            if (tabFrag1 != null) {

                if (tabFrag1.validate()) {
                    if (isNetworkAvailable(CustomerAccountActivity.this)) {
                        new updateStudentaAsync(getApplicationContext()).execute();
                    } else {
                        Toast.makeText(CustomerAccountActivity.this, getString(R.string.internet_connection_is_needed), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(CustomerAccountActivity.this, getString(R.string.pls_correct_the_errors), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showTwoButtonSnackbar() {

        // Create the Snackbar
        LinearLayout.LayoutParams objLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        final Snackbar snackbar = Snackbar.make(rootview, "Exit?", Snackbar.LENGTH_INDEFINITE);

        // Get the Snackbar layout view
        Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbar.getView();

        // Inflate our courseListMaterialDialog viewBitmap bitmap = ((RoundedDrawable)profilePic.getDrawable()).getSourceBitmap();
        View snackView = getLayoutInflater().inflate(R.layout.snackbar, null);


        TextView textViewOne = snackView.findViewById(R.id.first_text_view);
        textViewOne.setText(this.getResources().getString(R.string.yes));
        textViewOne.setOnClickListener(v -> {
            snackbar.dismiss();
            close = true;
            CustomerAccountActivity.this.onBackPressed();

            //  finish();
        });

        final TextView textViewTwo = snackView.findViewById(R.id.second_text_view);

        textViewTwo.setText(this.getResources().getString(R.string.no));
        textViewTwo.setOnClickListener(v -> {
            Log.d("Deny", "showTwoButtonSnackbar() : deny clicked");
            snackbar.dismiss();


        });

        // Add our courseListMaterialDialog view to the Snackbar's layout
        layout.addView(snackView, objLayoutParams);

        // Show the Snackbar
        snackbar.show();
    }

    @Override
    public void onBackPressed() {
        if (close) {
            super.onBackPressed();
            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_CANCELED, returnIntent);
            finish();
        }
        showTwoButtonSnackbar();
    }

    private class updateStudentaAsync extends AsyncTask<Void, Integer, String> {

        HttpClient httpClient = new DefaultHttpClient();
        private Context context;

        private updateStudentaAsync(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(Void... voids) {

            HttpResponse httpResponse = null;
            HttpEntity httpEntity = null;
            String responseString = null;
            String URL = API_URL + "customers/" + customer_id;
            try {
                HttpPost httpPost = new HttpPost(URL);
                MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();

                // Add the file to be uploaded
                if (profile_pic_file != null) {
                    multipartEntityBuilder.addPart("profile_image_file", new FileBody(profile_pic_file));
                }
                multipartEntityBuilder.addTextBody("title", gender.getSelectedItem().toString());
                String firstname = name.getText().toString();
                multipartEntityBuilder.addTextBody("name", name.getText().toString());
                multipartEntityBuilder.addTextBody("gender", gender.getSelectedItem().toString());
                multipartEntityBuilder.addTextBody("primary_contact", primarycontact.getText().toString());
                multipartEntityBuilder.addTextBody("auxiliary_contact", auxiliarycontact.getText().toString());
                multipartEntityBuilder.addTextBody("longitude", String.valueOf(longitude));
                multipartEntityBuilder.addTextBody("latitude", String.valueOf(latitude));

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
                if (result.contains("connect")){
                    Toast.makeText(context, context.getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        Realm.init(getApplicationContext());
                        Realm.getInstance(RealmUtility.getDefaultConfig(CustomerAccountActivity.this)).executeTransaction(realm -> {
                            realm.createOrUpdateObjectFromJson(RealmCustomer.class, jsonObject);
                            Const.showToast(getApplicationContext(), context.getString(R.string.successfully_updated));
                            Intent returnIntent = new Intent();
                            setResult(Activity.RESULT_CANCELED, returnIntent);
                            finish();
                        });
                    } catch (JSONException e) {
                        Toast.makeText(getApplicationContext(), getString(R.string.error_occured), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            }
            else {
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
