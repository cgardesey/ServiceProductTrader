package com.service.provision.activity;

import static com.service.provision.activity.ProviderHomeActivity.MYUSERID;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.service.provision.R;
import com.service.provision.realm.RealmProvider;
import com.service.provision.util.FCMAsyncTask;
import com.service.provision.util.RealmUtility;

import java.util.Timer;
import java.util.TimerTask;

import io.realm.Realm;


public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        ImageView imageView = findViewById(R.id.logotext);
        ImageView logo = findViewById(R.id.logo);

        Glide.with(this)
                .asGif()
                .load(R.drawable.superfixlogogif)

                .listener(new RequestListener<GifDrawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<GifDrawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GifDrawable resource, Object model, Target<GifDrawable> target, DataSource dataSource, boolean isFirstResource) {
                        SplashScreenActivity.this.runOnUiThread(() -> {

                            Timer myTimer = new Timer();

                            myTimer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    // If you want to modify a view in your Activity
                                    SplashScreenActivity.this.runOnUiThread(() -> {
                                        logo.setImageDrawable(null);
/*
                                        Glide.with(SplashScreenActivity.this)
                                                .asGif()
                                                .load(R.drawable.solidlogoanimation)

                                                .listener(new RequestListener<GifDrawable>() {
                                                    @Override
                                                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<GifDrawable> target, boolean isFirstResource) {
                                                        return false;
                                                    }

                                                    @Override
                                                    public boolean onResourceReady(GifDrawable resource, Object model, Target<GifDrawable> target, DataSource dataSource, boolean isFirstResource) {

                                                        return false;
                                                    }
                                                })
                                                .into(logo);
*/
                                        Glide.with(SplashScreenActivity.this)
                                                .asGif()
                                                .load(R.drawable.staticsuperfixlogogif)

                                                .listener(new RequestListener<GifDrawable>() {
                                                    @Override
                                                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<GifDrawable> target, boolean isFirstResource) {
                                                        return false;
                                                    }

                                                    @Override
                                                    public boolean onResourceReady(GifDrawable resource, Object model, Target<GifDrawable> target, DataSource dataSource, boolean isFirstResource) {
                                                        Timer myTimer = new Timer();

                                                        myTimer.schedule(new TimerTask() {
                                                            @Override
                                                            public void run() {
                                                                // If you want to modify a view in your Activity
                                                                boolean signedIn = !PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(MYUSERID, "").equals("");
                                                                Realm.init(getApplicationContext());
                                                                Realm.getInstance(RealmUtility.getDefaultConfig(SplashScreenActivity.this)).executeTransaction(realm -> {
                                                                    if (signedIn) {
                                                                        String role = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("ROLE", "");
                                                                        if (role.equals("CUSTOMER")) {
                                                                            startActivity(new Intent(SplashScreenActivity.this, CustomerHomeActivity.class));
                                                                        } else if (role.equals("PROVIDER")) {
                                                                            String provider_id = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("PROVIDER_ID", "");
                                                                            RealmProvider realmProvider = realm.where(RealmProvider.class).equalTo("provider_id", provider_id).findFirst();
                                                                            if (realmProvider.getVehicle_type() != null && !realmProvider.getVehicle_type().equals("")) {
                                                                                startActivity(new Intent(SplashScreenActivity.this, RiderHomeActivity.class));
                                                                            } else {
                                                                                startActivity(new Intent(SplashScreenActivity.this, ProviderHomeActivity.class));
                                                                            }
                                                                        } else {
                                                                            startActivity(new Intent(SplashScreenActivity.this, SelectRoleActivity.class));
                                                                        }
                                                                    } else {
                                                                        startActivity(new Intent(SplashScreenActivity.this, GetPhoneNumberActivity.class));
                                                                    }
                                                                    finish();

                                                                });
                                                            }
                                                        }, 1500);
                                                        return false;
                                                    }
                                                })
                                                .into(imageView);

                                    });
                                }
                            }, 2000);
                        });
                        return false;
                    }
                })
                .into(imageView);

    }
}
