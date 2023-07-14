package com.service.provision.receiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.material.snackbar.Snackbar;

import static com.service.provision.activity.ProviderHomeActivity.guidCheck;
import static com.service.provision.activity.ProviderHomeActivity.versionCheck;
import static com.service.provision.constants.Const.isNetworkAvailable;


public class NetworkReceiver extends BroadcastReceiver {

    public static String CONNECTEDTONETWORK = "CONNECTEDTONETWORK";
    public static Activity activeActivity;

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcastWithFirebase.
        //throw new UnsupportedOperationException("Not yet implemented");

        boolean networkAvailable = isNetworkAvailable(context);
        Log.d("09876", Boolean.toString(networkAvailable));

        if (networkAvailable) {
            if (activeActivity != null) {
//                versionCheck(activeActivity);
//                guidCheck(activeActivity);

                if (networkAvailable != PreferenceManager.getDefaultSharedPreferences(context).getBoolean(CONNECTEDTONETWORK, false)) {
                    Snackbar.make(activeActivity.findViewById(android.R.id.content), "Connected to internet", Snackbar.LENGTH_LONG)
                            .setBackgroundTint(activeActivity.getResources().getColor(android.R.color.holo_green_dark ))
                            .show();
                }
            }
        }
        else {
            if (activeActivity != null) {
                if (networkAvailable != PreferenceManager.getDefaultSharedPreferences(context).getBoolean(CONNECTEDTONETWORK, false)) {
                    Snackbar.make(activeActivity.findViewById(android.R.id.content), "Disconnected from internet", Snackbar.LENGTH_LONG)
                            .setBackgroundTint(activeActivity.getResources().getColor(android.R.color.holo_red_dark ))
                            .show();
                }
            }
        }

        PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(CONNECTEDTONETWORK, networkAvailable)
                .apply();
    }
}
