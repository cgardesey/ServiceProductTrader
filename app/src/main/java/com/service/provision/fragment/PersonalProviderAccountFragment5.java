package com.service.provision.fragment;

import static com.service.provision.activity.PersonalProviderAccountActivity.realmProvider;
import static com.service.provision.activity.PictureActivity.idPicBitmap;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ApplicationVersionSignature;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kbeanie.multipicker.api.entity.ChosenImage;
import com.kbeanie.multipicker.api.entity.ChosenVideo;
import com.makeramen.roundedimageview.RoundedDrawable;
import com.makeramen.roundedimageview.RoundedImageView;
import com.noelchew.multipickerwrapper.library.MultiPickerWrapper;
import com.noelchew.multipickerwrapper.library.ui.MultiPickerWrapperSupportFragment;
import com.service.provision.R;
import com.service.provision.activity.PictureActivity;
import com.service.provision.constants.Const;
import com.service.provision.realm.RealmIdentificationType;
import com.service.provision.util.PixelUtil;
import com.service.provision.util.RealmUtility;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by 2CLearning on 12/13/2017.
 */

public class PersonalProviderAccountFragment5 extends MultiPickerWrapperSupportFragment {
    public static final String PICTURE_TYPE = "PICTURE_TYPE";
    public static final String TYPE_ASSOCIATION_ID_PIC = "TYPE_ASSOCIATION_ID_PIC";
    private static final String TAG = "PersonalProviderAccountFragment4";
    public RoundedImageView association_identification_image;
    Context mContext;
    LinearLayout controls;
    public static File association_identification_image_file = null;
    MultiPickerWrapper.PickerUtilListener multiPickerWrapperListener = new MultiPickerWrapper.PickerUtilListener() {
        @Override
        public void onPermissionDenied() {
            // do something here
        }

        @Override
        public void onImagesChosen(List<ChosenImage> list) {
            controls.setVisibility(View.GONE);
            String imagePath = list.get(0).getOriginalPath();
            association_identification_image.setImageBitmap(BitmapFactory.decodeFile(imagePath));

            association_identification_image_file = new File(list.get(0).getOriginalPath());
        }

        @Override
        public void onVideosChosen(List<ChosenVideo> list) {
            Const.showToast(getContext(), mContext.getString(R.string.unsupported_file_format));
        }

        @Override
        public void onError(String s) {
            Toast.makeText(getContext(), getString(R.string.error_choosing_image), Toast.LENGTH_SHORT).show();
        }
    };
    public static Spinner association_account_type_spinner;
    public static EditText association_identification_number;
    private FloatingActionButton addimage, gal, cam;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getContext();

        final View rootView = inflater.inflate(R.layout.fragment_personal_provider_account4, container, false);
        association_identification_image = rootView.findViewById(R.id.association_identification_image);
        association_account_type_spinner = rootView.findViewById(R.id.association_account_type_spinner);
        association_identification_number = rootView.findViewById(R.id.association_identification_number);
        addimage = rootView.findViewById(R.id.addimage);
        controls = rootView.findViewById(R.id.add);
        gal = rootView.findViewById(R.id.gal);
        cam = rootView.findViewById(R.id.cam);


        addimage.setOnClickListener(v -> {
            if (controls.getVisibility() == View.VISIBLE) {
                controls.setVisibility(View.GONE);

            } else {
                controls.setVisibility(View.VISIBLE);
            }
        });
        gal.setOnClickListener(v -> multiPickerWrapper.getPermissionAndPickSingleImageAndCrop(imgOptions(), 3, 2));
        cam.setOnClickListener(v -> multiPickerWrapper.getPermissionAndTakePictureAndCrop(imgOptions(), 3, 2));

        association_identification_image.setOnClickListener(view -> {
            if (association_identification_image.getDrawable() == null) {
                //Toast.makeText(mContext, getString(R.string.image_not_set), Toast.LENGTH_SHORT).show();
            } else {
                idPicBitmap = ((RoundedDrawable) association_identification_image.getDrawable()).getSourceBitmap();
                Intent intent = new Intent(getActivity(), PictureActivity.class);
                intent.putExtra(PICTURE_TYPE, TYPE_ASSOCIATION_ID_PIC);
                getActivity().startActivity(intent);
            }
        });

        Realm.init(getContext());
        Realm.getInstance(RealmUtility.getDefaultConfig(getContext())).executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                RealmResults<RealmIdentificationType> realmIdentificationTypes = realm.where(RealmIdentificationType.class).findAll();
                ArrayList<String> spinnerArray = new ArrayList<String>();
                spinnerArray.add("None selected");
                for (RealmIdentificationType realmIdentificationType : realmIdentificationTypes) {
                    spinnerArray.add(realmIdentificationType.getName());
                }
                ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item, spinnerArray);
                association_account_type_spinner.setAdapter(spinnerArrayAdapter);
            }
        });
        association_account_type_spinner.setSelection(((ArrayAdapter) association_account_type_spinner.getAdapter()).getPosition(0));

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

    @Override
    protected MultiPickerWrapper.PickerUtilListener getMultiPickerWrapperListener() {
        return multiPickerWrapperListener;
    }

    public void init() {
        if (realmProvider != null) {
            String association_picture = realmProvider.getAssociation_identification_image_url();
            boolean associationPictureExists = association_picture != null;
            if (associationPictureExists) {
                Glide.with(getContext())
                        .load(realmProvider.getAssociation_identification_image_url())
                        .apply(new RequestOptions().centerCrop())
                        .apply(RequestOptions.signatureOf(ApplicationVersionSignature.obtain(getContext())))
                        .into(association_identification_image);
            } else {
                association_identification_image.setImageBitmap(null);
            }

            association_identification_number.setText(realmProvider.getAssociation_identification_number());
        }
    }

    public boolean validate() {
        boolean validated = true;
        /*if (TextUtils.isEmpty(association_identification_number.getText())) {
            association_identification_number.setError(getString(R.string.error_field_required));
            validated = false;
        }*/
        /*if (association_account_type_spinner.getSelectedItemPosition() == 0) {
            TextView errorText = (TextView) association_account_type_spinner.getSelectedView();
            errorText.setError("");
            errorText.setTextColor(Color.RED);
            validated = false;
        }*/
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
}