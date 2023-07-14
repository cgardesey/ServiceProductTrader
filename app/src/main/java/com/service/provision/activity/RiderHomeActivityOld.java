package com.service.provision.activity;

import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.receiver.NetworkReceiver.activeActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ApplicationVersionSignature;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.messaging.FirebaseMessaging;
import com.greysonparrelli.permiso.PermisoActivity;
import com.makeramen.roundedimageview.RoundedImageView;
import com.service.provision.R;
import com.service.provision.fragment.AroundDriverMapFragment;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmProvider;
import com.service.provision.receiver.NetworkReceiver;
import com.service.provision.util.RealmUtility;

import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;

public class RiderHomeActivityOld extends PermisoActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private String TAG;
    DrawerLayout drawer;
    TextView rating, name;
    RoundedImageView profile_imgview;
    NetworkReceiver networkReceiver;
    public static Activity riderHomeActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_home_old);
        riderHomeActivity = this;

//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        /*DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();*/

        ImageView menu =  findViewById(R.id.menu);

        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawer.openDrawer(GravityCompat.START);
            }
        });

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.payments);

        View headerView = navigationView.getHeaderView(0);
        profile_imgview = headerView.findViewById(R.id.profile_imgview);
        rating = headerView.findViewById(R.id.rating);
        name = headerView.findViewById(R.id.name);


        headerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(
                        new Intent(getApplicationContext(), RiderProviderAccountActivity.class)
                                .putExtra("MODE", "EDIT")
                );
                drawer.closeDrawer(GravityCompat.START);
            }
        });

        FragmentManager fragmentManager = getSupportFragmentManager();
        AroundDriverMapFragment fragment = new AroundDriverMapFragment();
        fragmentManager.beginTransaction().replace(R.id.frameLayout, fragment).commit();



        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();

                        // Log and toast
                        Log.d("engineer", token);
                        retriev_current_registration_token(getApplicationContext(), token);
                    }
                });
        networkReceiver = new NetworkReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        activeActivity = this;
        registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        Realm.init(getApplicationContext());
        RealmProvider realmProvider = Realm.getInstance(RealmUtility.getDefaultConfig(RiderHomeActivityOld.this)).where(RealmProvider.class).equalTo("provider_id", PreferenceManager.getDefaultSharedPreferences(RiderHomeActivityOld.this).getString("PROVIDER_ID", "")).findFirst();
        if (realmProvider != null) {
            name.setText(realmProvider.getFirst_name());

            String picture = realmProvider.getProfile_image_url();
            boolean pictureExists = picture != null;
            if (pictureExists) {
                Glide.with(RiderHomeActivityOld.this)
                        .load(realmProvider.getProfile_image_url())
                        .apply(new RequestOptions().centerCrop())
                        .apply(RequestOptions.signatureOf(ApplicationVersionSignature.obtain(RiderHomeActivityOld.this)))
                        .into(profile_imgview);
            } else {
                profile_imgview.setImageBitmap(null);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(networkReceiver);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.drawer_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        Fragment fragment = null;
        FragmentManager fragmentManager = getSupportFragmentManager();

        if (id == R.id.payments) {
//           fragment = new ImportFragment();
        } else if (id == R.id.wallet) {
//            fragment = new GalleryFragment();
        } else if (id == R.id.faqs) {
            startActivity(new Intent(getApplicationContext(), HelpActivity.class));
        } else if (id == R.id.logout) {
            PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext())
                    .edit()
                    .putString("ROLE", "")
                    .apply();
            Realm.init(getApplicationContext());
            startActivity(new Intent(getApplicationContext(), SelectRoleActivity.class));
            finish();
        }
//        fragmentManager.beginTransaction().replace(R.id.frameLayout, fragment).commit();
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public static void retriev_current_registration_token(Context context, String confirmation_token) {
        Realm.init(context);
        RealmProvider realmProvider = Realm.getInstance(RealmUtility.getDefaultConfig(context)).where(RealmProvider.class).equalTo("provider_id", PreferenceManager.getDefaultSharedPreferences(context).getString("PROVIDER_ID", "")).findFirst();
        String provider_id = realmProvider.getProvider_id();

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,
                API_URL + "providers/" + provider_id,
                response -> {
                },
                error -> {

                }
        ){
            @Override
            public Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params  = new HashMap<>();
                params.put("confirmation_token", confirmation_token);
                return params;
            }
            /** Passing some request headers* */
            @Override
            public Map getHeaders() throws AuthFailureError {
                HashMap headers = new HashMap();
                headers.put("accept", "application/json");
                headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(NetworkReceiver.activeActivity).getString(APITOKEN, ""));
                return headers;
            }
        };;

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        InitApplication.getInstance().addToRequestQueue(stringRequest);
    }
}
