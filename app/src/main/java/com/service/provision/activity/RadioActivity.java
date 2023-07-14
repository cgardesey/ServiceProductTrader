//      بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ

package com.service.provision.activity;
import static com.service.provision.receiver.NetworkReceiver.activeActivity;

import android.content.IntentFilter;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.greysonparrelli.permiso.PermisoActivity;
import com.makeramen.roundedimageview.RoundedImageView;
import com.service.provision.R;
import com.service.provision.realm.RealmBanner;
import com.service.provision.receiver.NetworkReceiver;
import com.service.provision.util.RealmUtility;
import com.service.provision.util.carousel.ViewPagerCarouselView;

import java.util.ArrayList;

import afriwan.ahda.AudioStreaming;
import io.realm.Realm;
import io.realm.RealmResults;

public class RadioActivity extends PermisoActivity {

    NetworkReceiver networkReceiver;
    private AudioStreaming audioStreamingCustomFont;
    String stream_url = "http://live-hls-web-aje.getaj.net/AJE/06.m3u8";

    TextView stationname, frequency;
    RoundedImageView icon;
    private ArrayList<RealmBanner> realmBanners = new ArrayList<>();
    static ViewPagerCarouselView viewPagerCarouselView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radio);

        audioStreamingCustomFont = findViewById(R.id.playCustomFonts);
        stationname = findViewById(R.id.stationname);
        frequency = findViewById(R.id.frequency);
        icon = findViewById(R.id.icon);
        Typeface iconFont = Typeface.createFromAsset(getAssets(), "audio-player-view-font-custom.ttf");
        audioStreamingCustomFont.setTypeface(iconFont);
        audioStreamingCustomFont.withUrl(getIntent().getStringExtra("STREAM_URL"));


        Glide.with(getApplicationContext()).load(getIntent().getStringExtra("ICON_URL")).apply(new RequestOptions()
                .centerCrop()
                .placeholder(null)
                .error(R.drawable.error))
                .into(icon);

        stationname.setText(getIntent().getStringExtra("STATION_NAME"));
        frequency.setText(getIntent().getStringExtra("FREQUENCY"));


        viewPagerCarouselView = findViewById(R.id.carouselView);

        populateAds();
        viewPagerCarouselView.setData(getSupportFragmentManager(), realmBanners, 3500);

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

    @Override
    protected void onDestroy() {
        audioStreamingCustomFont.destroy();
        super.onDestroy();
    }

    public void populateAds() {
        Realm.init(getApplicationContext());
        Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).executeTransaction(realm -> {
            RealmResults<RealmBanner> realmBannerRealmResults = realm.where(RealmBanner.class).findAll();
            realmBanners.clear();
            for (RealmBanner realmBanner : realmBannerRealmResults) {
                realmBanners.add(realmBanner);
            }
        });
    }
}
