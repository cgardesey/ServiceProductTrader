package com.service.provision.receiver;

import static com.service.provision.activity.ProviderHomeActivity.guidCheck;
import static com.service.provision.activity.ProviderHomeActivity.versionCheck;
import static com.service.provision.constants.Const.isNetworkAvailable;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.service.provision.R;
import com.service.provision.realm.RealmCustomer;
import com.service.provision.realm.RealmProvider;
import com.service.provision.service.LocationUpdateService;
import com.service.provision.util.RealmUtility;

import io.realm.Realm;


public class AutoStartReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcastWithFirebase.
        //throw new UnsupportedOperationException("Not yet implemented");

        Log.d("gfre33", "AutoStartReceiver");

        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
        {
            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("LOCATION_SHARING_ACTIVE", false)) {
                String role = PreferenceManager.getDefaultSharedPreferences(context).getString("ROLE", "");
                Realm.init(context);
                String publisher_id;
                if (role.equals("CUSTOMER")) {
                    publisher_id = Realm.getInstance(RealmUtility.getDefaultConfig(context)).where(RealmCustomer.class).findFirst().getCustomer_id();
                } else {
                    publisher_id = Realm.getInstance(RealmUtility.getDefaultConfig(context)).where(RealmProvider.class).equalTo("provider_id", PreferenceManager.getDefaultSharedPreferences(context).getString("PROVIDER_ID", "")).findFirst().getProvider_id();
                }

                Realm.init(context);
                ContextCompat.startForegroundService(context, new Intent(context, LocationUpdateService.class)
                        .putExtra("PUBLISHER_ID", publisher_id)
                        .putExtra("ROLE", role)
                );
                Log.d("gfre33", publisher_id);
            }

        }
    }
}
