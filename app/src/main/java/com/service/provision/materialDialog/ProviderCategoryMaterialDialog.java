package com.service.provision.materialDialog;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.service.provision.R;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Timer;
import java.util.TimerTask;

public class ProviderCategoryMaterialDialog extends DialogFragment {
    JSONArray categories = new JSONArray();

    public JSONArray getCategories() {
        return categories;
    }

    public void setCategories(JSONArray categories) {
        this.categories = categories;
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.list_item_provider_category,null);
        RadioGroup rg = view.findViewById(R.id.radio_group);

        rg.setOrientation(RadioGroup.HORIZONTAL);//or RadioGroup.VERTICAL
        for (int i = 0; i < categories.length(); i++) {
            RadioButton rb = new RadioButton(getContext());
            try {
                rb.setText(categories.getJSONObject(i).getString("title"));
                rb.setId(categories.getJSONObject(i).getInt("id"));
                rg.addView(rb);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
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