package com.service.provision.activity;

import static com.service.provision.receiver.NetworkReceiver.activeActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.greysonparrelli.permiso.PermisoActivity;
import com.service.provision.R;
import com.service.provision.adapter.ServiceListAdapter;
import com.service.provision.materialDialog.InstitutionLoginMaterialDialog;
import com.service.provision.realm.RealmServiceCategory;
import com.service.provision.receiver.NetworkReceiver;
import com.service.provision.util.RealmUtility;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmResults;

public class ServiceCategoryActivity extends PermisoActivity {

    NetworkReceiver networkReceiver;
    RecyclerView recyclerView;
    ImageView backbtn;
    ArrayList<String> newList = new ArrayList<>();
    ArrayList<String> list = new ArrayList<>();
    ServiceListAdapter listAdapter;
    String service_category_id;
    String title = "";
    Context mContext;

    ArrayList<RealmServiceCategory> realmServiceCategoryArrayList = new ArrayList<>();

    static InstitutionLoginMaterialDialog institutionLoginMaterialDialog = new InstitutionLoginMaterialDialog();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_category);

        mContext = getApplicationContext();
        recyclerView = findViewById(R.id.recyclerView);
        backbtn = findViewById(R.id.backbtn);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));

        backbtn.setOnClickListener(v -> finish());
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getApplicationContext(), 2));
        listAdapter = new ServiceListAdapter((realmProviderCategories, position, holder) -> {
            String textViewText = realmProviderCategories.get(position).getTitle();

            startActivityForResult(new Intent(getApplicationContext(), MyServiceListActivity.class)
                    .putExtra("title", textViewText)
                    .putExtra("initiator", "ServiceCategoryActivity"),
                    1914
            );

        }, ServiceCategoryActivity.this, realmServiceCategoryArrayList, "");
        recyclerView.setAdapter(listAdapter);
        populateServiceCategories();

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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {

            case 1914:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        if (data != null) {
                            setResult(RESULT_OK, new Intent().putExtra("SERVICE_CATEGORY", data.getStringExtra("SERVICE_CATEGORY")));
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

    private void populateServiceCategories() {
        Realm.init(getApplicationContext());
        Realm.getInstance(RealmUtility.getDefaultConfig(getApplicationContext())).executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                RealmResults<RealmServiceCategory> realmServiceCategories = realm.where(RealmServiceCategory.class)
                        .notEqualTo("title", "Emergency")
                        .notEqualTo("title", "Radio")
                        .notEqualTo("title", "SuperRide")
                        .findAll();

                for (RealmServiceCategory realmProviderCategory : realmServiceCategories) {
                    realmServiceCategoryArrayList.add(realmProviderCategory);
                }
                if (realmServiceCategoryArrayList.size() > 0) {
                    listAdapter.notifyDataSetChanged();
                }
            }
        });
    }
}
