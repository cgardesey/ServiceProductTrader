package com.service.provision.fragment;

import static com.service.provision.activity.OrganisationalProviderAccountActivity.realmProvider;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.text.SimpleDateFormat;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.service.provision.R;
import com.service.provision.util.PixelUtil;
import com.tsongkha.spinnerdatepicker.DatePickerDialog;
import com.tsongkha.spinnerdatepicker.SpinnerDatePickerDialogBuilder;
import com.yalantis.ucrop.UCrop;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Created by 2CLearning on 12/13/2017.
 */

public class OrganisationalProviderAccountFragment3 extends Fragment implements DatePickerDialog.OnDateSetListener {
    Context mContext;
    public static EditText tin_number;
    public static TextView date_registered;
    SimpleDateFormat simpleDateFormat;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getContext();

        final View rootView = inflater.inflate(R.layout.fragment_organisational_provider_account3, container, false);
        tin_number = rootView.findViewById(R.id.tin_number);
        date_registered = rootView.findViewById(R.id.date_registered);
        simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        date_registered.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDate(1980, 0, 1, R.style.DatePickerSpinner);
            }

        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        init();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // txtData = (TextView)view.findViewById(R.id.txtData);
    }

    public void init() {
        if (realmProvider != null) {
            tin_number.setText(realmProvider.getTin_number());
            date_registered.setText(realmProvider.getDate_registered());
        }
    }

    public boolean validate() {
        boolean validated = true;
        if (TextUtils.isEmpty(tin_number.getText())) {
            tin_number.setError(getString(R.string.error_field_required));
            validated = false;
        }
        if (TextUtils.isEmpty(date_registered.getText())) {
            date_registered.setError(getString(R.string.error_field_required));
            validated = false;
        }
        return validated;
    }

    public static boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }

    private UCrop.Options imgOptions() {
        UCrop.Options options = new UCrop.Options();
        options.setStatusBarColor(ContextCompat.getColor(getContext(), R.color.colorPrimaryDark));
        options.setToolbarColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        options.setCropFrameColor(ContextCompat.getColor(getContext(), R.color.colorPrimaryDark));
        options.setCropFrameStrokeWidth(PixelUtil.dpToPx(getContext(), 4));
        options.setCropGridColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        options.setCropGridStrokeWidth(PixelUtil.dpToPx(getContext(), 2));
        options.setActiveWidgetColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        options.setToolbarTitle(getString(R.string.crop_image));

        // set rounded cropping guide
        options.setCircleDimmedLayer(true);
        return options;
    }

    @Override
    public void onDateSet(com.tsongkha.spinnerdatepicker.DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        Calendar calendar = new GregorianCalendar(year, monthOfYear, dayOfMonth);
        date_registered.setError(null);
        date_registered.setText(simpleDateFormat.format(calendar.getTime()));
    }

    @VisibleForTesting
    void showDate(int year, int monthOfYear, int dayOfMonth, int spinnerTheme) {
        new SpinnerDatePickerDialogBuilder()
                .context(getContext())
                .callback(this)
                .spinnerTheme(R.style.NumberPickerStyle)
                .defaultDate(year, monthOfYear, dayOfMonth)
                .build()
                .show();
    }
}