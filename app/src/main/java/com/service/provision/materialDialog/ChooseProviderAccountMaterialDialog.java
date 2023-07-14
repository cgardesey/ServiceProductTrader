package com.service.provision.materialDialog;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.service.provision.R;
import com.service.provision.adapter.ChooseProviderAccountAdapter;
import com.service.provision.realm.RealmProvider;
import com.service.provision.util.RealmUtility;

import java.util.Timer;
import java.util.TimerTask;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

public class ChooseProviderAccountMaterialDialog extends DialogFragment {

    RealmList<RealmProvider> realmProviders = new RealmList<>();

    public RealmList<RealmProvider> getRealmProviders() {
        return realmProviders;
    }

    public void setRealmProviders(RealmList<RealmProvider> realmProviders) {
        this.realmProviders = realmProviders;
    }


    ProgressDialog mProgress;
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.list_item_choose_provider_account,null);
        
        RecyclerView recyclerView = view.findViewById(R.id.recyclerview);
        Button addaccount = view.findViewById(R.id.addaccount);
        addaccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ChooseProviderAccountTypeMaterialDialog chooseProviderAccountTypeMaterialDialog = new ChooseProviderAccountTypeMaterialDialog();
                if(chooseProviderAccountTypeMaterialDialog != null && chooseProviderAccountTypeMaterialDialog.isAdded()) {

                } else {
                    chooseProviderAccountTypeMaterialDialog.setCancelable(false);
                    chooseProviderAccountTypeMaterialDialog.show(getFragmentManager(), "chooseProviderAccountTypeMaterialDialog");
                    chooseProviderAccountTypeMaterialDialog.setCancelable(true);
                }
                dismiss();
            }
        });

         ChooseProviderAccountAdapter chooseProviderAccountAdapter = new ChooseProviderAccountAdapter((providers, position, holder) -> {

         }, realmProviders);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(chooseProviderAccountAdapter);

        // doneBtn.setOnClickListener(doneAction);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // If you want to modify a view in your Activity
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    }
                });
            }
        }, 5);
        return builder.create();
    }
}