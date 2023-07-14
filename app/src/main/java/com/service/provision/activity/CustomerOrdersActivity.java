package com.service.provision.activity;

import static com.service.provision.receiver.NetworkReceiver.activeActivity;

import android.app.ProgressDialog;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.flyco.tablayout.SlidingTabLayout;
import com.service.provision.R;
import com.service.provision.pagerAdapter.OrdersPagerAdapter;
import com.service.provision.receiver.NetworkReceiver;

import java.util.ArrayList;

public class CustomerOrdersActivity extends AppCompatActivity {

    public static ViewPager mViewPager;
    public static SlidingTabLayout mTabLayout;
    public  static ArrayList<String> statuses;
    ImageView backbtn;
    ProgressDialog dialog;
    NetworkReceiver networkReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_orders);

        networkReceiver = new NetworkReceiver();

        backbtn = findViewById(R.id.search);

        backbtn.setOnClickListener(v -> finish());

        statuses = new ArrayList<String>() {{
            add("Unpaid");
            add("Paid");
            add("Delivered");
        }};

        mViewPager = findViewById(R.id.viewPager);
        mTabLayout = findViewById(R.id.tabLayout);
        mViewPager.setAdapter(new OrdersPagerAdapter(getSupportFragmentManager(), statuses));
        mTabLayout.setViewPager(mViewPager);
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
}
