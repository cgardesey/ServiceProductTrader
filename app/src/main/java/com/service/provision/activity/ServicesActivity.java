package com.service.provision.activity;

import static com.service.provision.receiver.NetworkReceiver.activeActivity;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.greysonparrelli.permiso.PermisoActivity;
import com.service.provision.R;
import com.service.provision.adapter.RatedServiceAdapter;
import com.service.provision.materialDialog.ChooseServiceContactMethodMaterialDialog;
import com.service.provision.realm.RealmCustomer;
import com.service.provision.realm.RealmService;
import com.service.provision.receiver.NetworkReceiver;
import com.service.provision.util.PixelUtil;
import com.service.provision.util.RealmUtility;
import com.yalantis.ucrop.UCrop;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmResults;


public class ServicesActivity extends PermisoActivity {

    NetworkReceiver networkReceiver;
    RecyclerView recyclerview;
    RatedServiceAdapter ratedServiceAdapter;
    private ImageView backbtn;
    ArrayList<RealmService> realmServices = new ArrayList<>(), newRealmServices = new ArrayList<>();
    public static Activity serviceActivity;
    TextView title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_services);

        recyclerview = findViewById(R.id.recyclerview);
        serviceActivity = this;
        backbtn = findViewById(R.id.backbtn1);
        title = findViewById(R.id.title);

        title.setText(getIntent().getStringExtra("TITLE"));

        backbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        ratedServiceAdapter = new RatedServiceAdapter(new RatedServiceAdapter.RatedServiceAdapterInterface() {
            @Override
            public void onListItemClick(ArrayList<RealmService> realmServices, int position, RatedServiceAdapter.ViewHolder holder) {
                RealmService realmService = realmServices.get(position);
                ChooseServiceContactMethodMaterialDialog chooseServiceContactMethodMaterialDialog = new ChooseServiceContactMethodMaterialDialog();
                if(chooseServiceContactMethodMaterialDialog != null && chooseServiceContactMethodMaterialDialog.isAdded()) {

                } else {
                    chooseServiceContactMethodMaterialDialog.setProvider_id(realmService.getProvider_id());
                    Realm.init(ServicesActivity.this);
                    String customer_id = Realm.getInstance(RealmUtility.getDefaultConfig(ServicesActivity.this)).where(RealmCustomer.class).findFirst().getCustomer_id();
                    chooseServiceContactMethodMaterialDialog.setCustomer_id(customer_id);
                    chooseServiceContactMethodMaterialDialog.show(getSupportFragmentManager(), "chooseContactMethodMaterialDialog");
                    chooseServiceContactMethodMaterialDialog.setCancelable(true);
                }
            }
        }, serviceActivity, realmServices);

        recyclerview.setLayoutManager(new LinearLayoutManager(this));
        recyclerview.setNestedScrollingEnabled(false);
        recyclerview.setItemAnimator(new DefaultItemAnimator());
        recyclerview.addItemDecoration(new DividerItemDecoration(getApplicationContext(), DividerItemDecoration.VERTICAL));
        recyclerview.setAdapter(ratedServiceAdapter);

        populateServices(getApplicationContext());
        ratedServiceAdapter.notifyDataSetChanged();

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

    void populateServices(final Context context) {
        newRealmServices.clear();

        Realm.init(context);
        Realm.getInstance(RealmUtility.getDefaultConfig(context)).executeTransaction(realm -> {
            String stringExtra;
            String fieldName;
            RealmResults<RealmService> realmServices = realm.where(RealmService.class)
                    .findAll();


            for (RealmService realmService : realmServices) {
                newRealmServices.add(realmService);
            }

            this.realmServices.clear();
            this.realmServices.addAll(newRealmServices);

            ratedServiceAdapter.notifyDataSetChanged();
            recyclerview.setVisibility(View.VISIBLE);
        });
    }
}
