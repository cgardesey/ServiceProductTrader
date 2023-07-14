package com.service.provision.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.greysonparrelli.permiso.Permiso;
import com.greysonparrelli.permiso.PermisoActivity;
import com.service.provision.R;
import com.service.provision.constants.Const;
import com.service.provision.fragment.PersonalProviderAccountFragment1;
import com.service.provision.fragment.PersonalProviderAccountFragment2;
import com.service.provision.fragment.PersonalProviderAccountFragment3;
import com.service.provision.fragment.PersonalProviderAccountFragment4;
import com.service.provision.other.MyHttpEntity;
import com.service.provision.pagerAdapter.PersonalAccountPageAdapter;
import com.service.provision.realm.RealmProvider;
import com.service.provision.receiver.NetworkReceiver;
import com.service.provision.util.NonSwipeableViewPager;
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
import static com.service.provision.activity.ProviderHomeActivity.MYUSERID;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.isNetworkAvailable;
import static com.service.provision.fragment.PersonalProviderAccountFragment1.firstName;
import static com.service.provision.fragment.PersonalProviderAccountFragment1.gender;
import static com.service.provision.fragment.PersonalProviderAccountFragment1.lastName;
import static com.service.provision.fragment.PersonalProviderAccountFragment1.otherNames;
import static com.service.provision.fragment.PersonalProviderAccountFragment1.profile_image_file;
import static com.service.provision.fragment.PersonalProviderAccountFragment3.identification_image_file;
import static com.service.provision.fragment.PersonalProviderAccountFragment1.title;
import static com.service.provision.fragment.PersonalProviderAccountFragment2.auxiliarycontact;
import static com.service.provision.fragment.PersonalProviderAccountFragment2.dob;
import static com.service.provision.fragment.PersonalProviderAccountFragment2.latitude;
import static com.service.provision.fragment.PersonalProviderAccountFragment2.longitude;
import static com.service.provision.fragment.PersonalProviderAccountFragment2.maritalstatus;
import static com.service.provision.fragment.PersonalProviderAccountFragment2.primarycontact;
import static com.service.provision.fragment.PersonalProviderAccountFragment3.identification_type_spinner;
import static com.service.provision.fragment.PersonalProviderAccountFragment3.identification_number;
import static com.service.provision.fragment.PersonalProviderAccountFragment4.association_identification_image_file;
import static com.service.provision.fragment.PersonalProviderAccountFragment4.association_identification_number;
import static com.service.provision.receiver.NetworkReceiver.activeActivity;


@SuppressWarnings("HardCodedStringLiteral")
public class PersonalProviderAccountActivity extends PermisoActivity {

    public static RealmProvider realmProvider = new RealmProvider();
    static Context context;


    boolean close = false;
    NonSwipeableViewPager mViewPager;
    PersonalAccountPageAdapter personalAccountPageAdapter;
    FloatingActionButton moveprevious, movenext, done;

    RelativeLayout rootview;
    ProgressBar progressBar;
    String tag1 = "android:switcher:" + R.id.pageques_personal + ":" + 0;
    String tag2 = "android:switcher:" + R.id.pageques_personal + ":" + 1;
    String tag3 = "android:switcher:" + R.id.pageques_personal + ":" + 2;
    String tag4 = "android:switcher:" + R.id.pageques_personal + ":" + 3;

    PersonalProviderAccountFragment1 tabFrag1;
    PersonalProviderAccountFragment2 tabFrag2;
    PersonalProviderAccountFragment3 tabFrag3;
    PersonalProviderAccountFragment4 tabFrag4;

    String provider_id;
    private ProgressDialog mProgress;
    NetworkReceiver networkReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getApplicationContext();

        networkReceiver = new NetworkReceiver();
        Permiso.getInstance().setActivity(this);

        setContentView(R.layout.activity_personal_provider_account);
        getSupportActionBar().hide();

        mProgress = new ProgressDialog(this);
        mProgress.setTitle(getString(R.string.updating_profile));
        mProgress.setMessage(getString(R.string.please_wait));
        mProgress.setCancelable(false);
        mProgress.setIndeterminate(true);

        rootview = findViewById(R.id.root);

        if (getIntent().getStringExtra("MODE").equals("EDIT")) {
            Realm.init(getApplicationContext());
            realmProvider = Realm.getInstance(RealmUtility.getDefaultConfig(PersonalProviderAccountActivity.this)).where(RealmProvider.class).equalTo("provider_id", PreferenceManager.getDefaultSharedPreferences(PersonalProviderAccountActivity.this).getString("PROVIDER_ID", "")).findFirst();
            if (realmProvider != null) {
                provider_id = realmProvider.getProvider_id();
            }
        }
        progressBar = findViewById(R.id.pbar_pic);
        Realm.init(getApplicationContext());
        personalAccountPageAdapter = new PersonalAccountPageAdapter(getSupportFragmentManager());
        mViewPager = findViewById(R.id.pageques_personal);
        mViewPager.setAdapter(personalAccountPageAdapter);
        mViewPager.setOffscreenPageLimit(3); //posible candidate for bug
        mViewPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mViewPager.setCurrentItem(mViewPager.getCurrentItem());
                return true;
            }
        });
        progressBar.setVisibility(View.GONE);

        movenext = findViewById(R.id.movenext);
        moveprevious = findViewById(R.id.moveprevious);
        done = findViewById(R.id.done);
        movenext.setOnClickListener(v -> {
            tabFrag1 = (PersonalProviderAccountFragment1) getSupportFragmentManager().findFragmentByTag(tag1);
            tabFrag2 = (PersonalProviderAccountFragment2) getSupportFragmentManager().findFragmentByTag(tag2);
            tabFrag3 = (PersonalProviderAccountFragment3) getSupportFragmentManager().findFragmentByTag(tag3);
            tabFrag4 = (PersonalProviderAccountFragment4) getSupportFragmentManager().findFragmentByTag(tag4);

            switch (mViewPager.getCurrentItem()) {
                case 0:
                    if (tabFrag1.validate()) {
                        mViewPager.setCurrentItem(1);
                        moveprevious.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(getApplicationContext(), "Please correct the errors.", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 1:
                    if (tabFrag2.validate()) {
                        mViewPager.setCurrentItem(2);
                        moveprevious.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(getApplicationContext(), "Please correct the errors.", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 2:
                    if (tabFrag3.validate()) {
                        mViewPager.setCurrentItem(3);
                        moveprevious.setVisibility(View.VISIBLE);
                        movenext.setVisibility(View.GONE);
                        done.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(getApplicationContext(), "Please correct the errors.", Toast.LENGTH_SHORT).show();
                    }
                    break;
                default:
                    break;
            }
        });
        done.setOnClickListener(v -> {
            if (tabFrag4.validate()) {
                if (isNetworkAvailable(PersonalProviderAccountActivity.this)) {
                    if (provider_id != null && !provider_id.equals("")) {
                        new updateProviderAsync(getApplicationContext()).execute();
                    } else {
                        new addProviderAsync(getApplicationContext()).execute();
                    }
                } else {
                    Toast.makeText(PersonalProviderAccountActivity.this, getString(R.string.internet_connection_is_needed), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(PersonalProviderAccountActivity.this, getString(R.string.pls_correct_the_errors), Toast.LENGTH_LONG).show();
            }
        });
        moveprevious.setOnClickListener(v -> {
            tabFrag1 = (PersonalProviderAccountFragment1) getSupportFragmentManager().findFragmentByTag(tag1);
            tabFrag2 = (PersonalProviderAccountFragment2) getSupportFragmentManager().findFragmentByTag(tag2);
            tabFrag3 = (PersonalProviderAccountFragment3) getSupportFragmentManager().findFragmentByTag(tag3);
            tabFrag4 = (PersonalProviderAccountFragment4) getSupportFragmentManager().findFragmentByTag(tag4);

            switch (mViewPager.getCurrentItem()) {
                case 3:
                    if (tabFrag3.validate()) {
                        mViewPager.setCurrentItem(2);
                        movenext.setVisibility(View.VISIBLE);
                        done.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(getApplicationContext(), "Please correct the errors.", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 2:
                    if (tabFrag2.validate()) {
                        mViewPager.setCurrentItem(1);
                        movenext.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(getApplicationContext(), "Please correct the errors.", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 1:
                    if (tabFrag1.validate()) {
                        mViewPager.setCurrentItem(0);
                        movenext.setVisibility(View.VISIBLE);
                        moveprevious.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(getApplicationContext(), "Please correct the errors.", Toast.LENGTH_SHORT).show();
                    }
                    break;
                default:
                    break;
            }
        });
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
            PersonalProviderAccountActivity.this.onBackPressed();

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

    private boolean canMakeSmores() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    private class addProviderAsync extends AsyncTask<Void, Integer, String> {

        HttpClient httpClient = new DefaultHttpClient();
        private Context context;

        private addProviderAsync(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(Void... voids) {
            HttpResponse httpResponse = null;
            HttpEntity httpEntity = null;
            String responseString = null;
            String URL = API_URL + "providers";
            try {
                HttpPost httpPost = new HttpPost(URL);
                MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();

                // Add the file to be uploaded
                if (profile_image_file != null) {
                    multipartEntityBuilder.addPart("profile_image_file", new FileBody(profile_image_file));
                }
                multipartEntityBuilder.addTextBody("title", title.getSelectedItem().toString());
                multipartEntityBuilder.addTextBody("first_name", firstName.getText().toString());
                multipartEntityBuilder.addTextBody("last_name", lastName.getText().toString());
                multipartEntityBuilder.addTextBody("other_name", otherNames.getText().toString());
                multipartEntityBuilder.addTextBody("gender", gender.getSelectedItem().toString());

                multipartEntityBuilder.addTextBody("primary_contact", primarycontact.getText().toString());
                multipartEntityBuilder.addTextBody("auxiliary_contact", auxiliarycontact.getText().toString());
                multipartEntityBuilder.addTextBody("dob", dob.getText().toString());
//                multipartEntityBuilder.addTextBody("postal_address", postaladdress.getText().toString());
                multipartEntityBuilder.addTextBody("longitude", String.valueOf(longitude));
                multipartEntityBuilder.addTextBody("latitude", String.valueOf(latitude));
                multipartEntityBuilder.addTextBody("marital_status", maritalstatus.getSelectedItem().toString());

                multipartEntityBuilder.addTextBody("identification_type", identification_type_spinner.getSelectedItem().toString());
                multipartEntityBuilder.addTextBody("identification_number", identification_number.getText().toString());
                if (identification_image_file != null) {
                    multipartEntityBuilder.addPart("identification_image_file", new FileBody(identification_image_file));
                }

                multipartEntityBuilder.addTextBody("association_identification_number", association_identification_number.getText().toString());
                if (association_identification_image_file != null) {
                    multipartEntityBuilder.addPart("association_identification_image_file", new FileBody(association_identification_image_file));
                }

                multipartEntityBuilder.addTextBody("category", "Personal");
                multipartEntityBuilder.addTextBody("user_id", PreferenceManager.getDefaultSharedPreferences(context).getString(MYUSERID, ""));





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
            mProgress.setTitle("Creating Profile.");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(String result) {
            mProgress.dismiss();
            if (result != null) {
                if (result.contains("connect")){
                    Toast.makeText(getApplicationContext(), context.getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        Realm.getInstance(RealmUtility.getDefaultConfig(PersonalProviderAccountActivity.this)).executeTransaction(realm -> {
                            realm.createOrUpdateObjectFromJson(RealmProvider.class, jsonObject);
                            Const.showToast(getApplicationContext(), "Successfully saved!");
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

    private class updateProviderAsync extends AsyncTask<Void, Integer, String> {

        HttpClient httpClient = new DefaultHttpClient();
        private Context context;

        private updateProviderAsync(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(Void... voids) {
            HttpResponse httpResponse = null;
            HttpEntity httpEntity = null;
            String responseString = null;
            String URL = API_URL + "providers/" + provider_id;
            try {
                HttpPost httpPost = new HttpPost(URL);
                MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();

                // Add the file to be uploaded
                if (profile_image_file != null) {
                    multipartEntityBuilder.addPart("profile_image_file", new FileBody(profile_image_file));
                }
                multipartEntityBuilder.addTextBody("title", title.getSelectedItem().toString());
                multipartEntityBuilder.addTextBody("first_name", firstName.getText().toString());
                multipartEntityBuilder.addTextBody("last_name", lastName.getText().toString());
                multipartEntityBuilder.addTextBody("other_name", otherNames.getText().toString());
                multipartEntityBuilder.addTextBody("gender", gender.getSelectedItem().toString());

                multipartEntityBuilder.addTextBody("primary_contact", primarycontact.getText().toString());
                multipartEntityBuilder.addTextBody("auxiliary_contact", auxiliarycontact.getText().toString());
                multipartEntityBuilder.addTextBody("dob", dob.getText().toString());
//                multipartEntityBuilder.addTextBody("postal_address", postaladdress.getText().toString());
                multipartEntityBuilder.addTextBody("longitude", String.valueOf(longitude));
                multipartEntityBuilder.addTextBody("latitude", String.valueOf(latitude));
                multipartEntityBuilder.addTextBody("marital_status", maritalstatus.getSelectedItem().toString());

                multipartEntityBuilder.addTextBody("identification_type", identification_type_spinner.getSelectedItem().toString());
                multipartEntityBuilder.addTextBody("identification_number", identification_number.getText().toString());
                if (identification_image_file != null) {
                    multipartEntityBuilder.addPart("identification_image_file", new FileBody(identification_image_file));
                }

                multipartEntityBuilder.addTextBody("association_identification_number", association_identification_number.getText().toString());
                if (association_identification_image_file != null) {
                    multipartEntityBuilder.addPart("association_identification_image_file", new FileBody(association_identification_image_file));
                }

                multipartEntityBuilder.addTextBody("category", "Personal");
                multipartEntityBuilder.addTextBody("user_id", PreferenceManager.getDefaultSharedPreferences(context).getString(MYUSERID, ""));

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
                    Toast.makeText(getApplicationContext(), context.getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        Realm.getInstance(RealmUtility.getDefaultConfig(PersonalProviderAccountActivity.this)).executeTransaction(realm -> {
                            realm.createOrUpdateObjectFromJson(RealmProvider.class, jsonObject);
                            Const.showToast(getApplicationContext(), "Successfully saved!");
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
