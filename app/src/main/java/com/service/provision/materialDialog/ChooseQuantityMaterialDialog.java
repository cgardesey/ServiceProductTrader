package com.service.provision.materialDialog;

import static com.service.provision.activity.ProviderHomeActivity.APITOKEN;
import static com.service.provision.constants.keyConst.API_URL;
import static com.service.provision.constants.Const.myVolleyError;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.jama.carouselview.CarouselView;
import com.jama.carouselview.CarouselViewListener;
import com.jama.carouselview.enums.IndicatorAnimationType;
import com.jama.carouselview.enums.OffsetType;
import com.service.provision.R;
import com.service.provision.other.InitApplication;
import com.service.provision.realm.RealmCustomer;
import com.service.provision.realm.RealmProductImage;
import com.service.provision.util.RealmUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.realm.Realm;
import io.realm.RealmResults;
import it.sephiroth.android.library.numberpicker.NumberPicker;

public class ChooseQuantityMaterialDialog extends DialogFragment {

    private ArrayList<String> images = new ArrayList<>();

    private String provider_id;
    private String product_id;
    private int quantity_available;
    private int unit_quantity;
    private double unit_price;

    public String getProvider_id() {
        return provider_id;
    }

    public void setProvider_id(String provider_id) {
        this.provider_id = provider_id;
    }

    public String getProduct_id() {
        return product_id;
    }

    public void setProduct_id(String product_id) {
        this.product_id = product_id;
    }

    public int getQuantity_available() {
        return quantity_available;
    }

    public void setQuantity_available(int quantity_available) {
        this.quantity_available = quantity_available;
    }

    public int getUnit_quantity() {
        return unit_quantity;
    }

    public double getUnit_price() {
        return unit_price;
    }

    public void setUnit_price(double unit_price) {
        this.unit_price = unit_price;
    }

    public void setUnit_quantity(int unit_quantity) {
        this.unit_quantity = unit_quantity;
    }

    NumberPicker numberPicker;
    TextView price, currency, cancel, ok;


    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.list_item_choose_quantity, null);
        cancel = view.findViewById(R.id.cancel);
        ok = view.findViewById(R.id.ok);
        price = view.findViewById(R.id.price);
        currency = view.findViewById(R.id.currency);
        numberPicker = view.findViewById(R.id.numberPicker);
        CarouselView carouselView = view.findViewById(R.id.carouselView);

        numberPicker.setProgress(unit_quantity);
        price.setText(String.valueOf(unit_quantity * unit_price));
        numberPicker.setMinValue(unit_quantity);
        numberPicker.setMaxValue(quantity_available);
        numberPicker.setStepSize(unit_quantity);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Realm.init(getActivity());
                String customer_id = Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).where(RealmCustomer.class).findFirst().getCustomer_id();

                ProgressDialog progressDialog = new ProgressDialog(getActivity());
                progressDialog.setMessage(getActivity().getString(R.string.please_wait));
                progressDialog.setCancelable(false);
                progressDialog.setIndeterminate(true);
                progressDialog.show();

                StringRequest stringRequest = new StringRequest(
                        Request.Method.POST,
                        API_URL + "carts",
                        response -> {
                            progressDialog.dismiss();
                            if (response != null) {
                                progressDialog.dismiss();
                                try {
                                    JSONObject jsonObject = new JSONObject(response);

                                    ChooseQuantityMDInterface activity = (ChooseQuantityMDInterface) getActivity();
                                    if (jsonObject.getBoolean("success")) {
                                        activity.onViewClick("Successfully added to cart!", jsonObject.getJSONObject("cart"));
                                    } else {
                                        activity.onViewClick("Item already in cart", jsonObject.getJSONObject("cart"));
                                    }
                                    dismiss();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        error -> {
                            error.printStackTrace();
                            progressDialog.dismiss();
                            myVolleyError(getActivity(), error);
                        }
                ) {
                    @Override
                    public Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> params = new HashMap<>();
                        params.put("provider_id", provider_id);
                        params.put("customer_id", customer_id);
                        params.put("product_id", product_id);
                        params.put("quantity", String.valueOf(numberPicker.getProgress()));
                        params.put("price", price.getText().toString());
                        return params;
                    }

                    /** Passing some request headers* */
                    @Override
                    public Map getHeaders() throws AuthFailureError {
                        HashMap headers = new HashMap();
                        headers.put("accept", "application/json");
                        headers.put("Authorization", "Bearer " + PreferenceManager.getDefaultSharedPreferences(getContext()).getString(APITOKEN, ""));
                        return headers;
                    }
                };
                ;
                stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                        0,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                InitApplication.getInstance().addToRequestQueue(stringRequest);
            }
        });
        numberPicker.setNumberPickerChangeListener(new NumberPicker.OnNumberPickerChangeListener() {
            @Override
            public void onProgressChanged(@NonNull NumberPicker numberPicker, int i, boolean b) {
                price.setText(String.format("%.2f", i * unit_price));
            }

            @Override
            public void onStartTrackingTouch(@NonNull NumberPicker numberPicker) {

            }

            @Override
            public void onStopTrackingTouch(@NonNull NumberPicker numberPicker) {

            }
        });

        Realm.init(getContext());
        Realm.getInstance(RealmUtility.getDefaultConfig(getActivity())).executeTransaction(realm -> {
            RealmResults<RealmProductImage> realmProductImages = realm.where(RealmProductImage.class)
                    .equalTo("product_id", product_id)
                    .findAll();
            for (RealmProductImage realmProductImage : realmProductImages) {
                images.add(realmProductImage.getUrl());
            }
        });
        carouselView.setSize(images.size());
        carouselView.setAutoPlay(true);
        carouselView.setIndicatorAnimationType(IndicatorAnimationType.THIN_WORM);
        carouselView.setCarouselOffset(OffsetType.START);
        carouselView.setCarouselViewListener(new CarouselViewListener() {
            @Override
            public void onBindView(View view, int position) {
                // Example here is setting up a full image carousel
                ImageView imageView = view.findViewById(R.id.image);
                Glide.with(getActivity()).load(images.get(position)).apply(new RequestOptions().centerCrop()).into(imageView);
            }
        });
        // After you finish setting up, show the CarouselView
        carouselView.show();



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

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

    }

    public interface ChooseQuantityMDInterface {
        public void onViewClick(String message, JSONObject jsonObject);
        public void onStockCartViewClick(String message, JSONObject jsonObject);
    }
}