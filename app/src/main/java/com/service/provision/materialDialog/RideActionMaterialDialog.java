package com.service.provision.materialDialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;

import com.service.provision.R;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Nana on 10/22/2017.
 */

public class RideActionMaterialDialog extends DialogFragment {

    Button map_navigation, end_ride;
    ImageView cancel;

    RideActionMaterialDialog locationTypeMaterialDialog;


    private static final String TAG = "LocationTypeMaterialDialog";


    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.list_item_ride_action, null);

        locationTypeMaterialDialog = RideActionMaterialDialog.this;
        cancel = view.findViewById(R.id.cancel);
        map_navigation = view.findViewById(R.id.map_navigation);
        end_ride = view.findViewById(R.id.end_ride);

        map_navigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RideActionMDInterface activity = (RideActionMDInterface) getActivity();
                activity.onMapNavigationClick();
            }
        });

        end_ride.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RideActionMDInterface activity = (RideActionMDInterface) getActivity();
                activity.onEndRideClick();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        //  builder.setCancelable(false);
        Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // If you want to modify a view in your Activity
                getActivity().runOnUiThread(() -> getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)));
            }
        }, 5);
        return builder.create();
    }

    public interface RideActionMDInterface {
        public void onMapNavigationClick();
        public void onEndRideClick();
    }
}