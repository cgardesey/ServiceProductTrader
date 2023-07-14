package com.service.provision.adapter;

/**
 * Created by Nana on 11/10/2017.
 */

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.service.provision.R;
import com.service.provision.realm.RealmCart;

import java.util.ArrayList;

/**
 * Created by Belal on 6/6/2017.
 */

public class ProviderCartListAdapter extends RecyclerView.Adapter<ProviderCartListAdapter.ViewHolder> {

    private static final String YOUR_DIALOG_TAG = "";
    ProviderCartAdapterInterface cartAdapterInterface;
    Activity mActivity;
    private ArrayList<RealmCart> realmCarts;

    public ProviderCartListAdapter(ProviderCartAdapterInterface cartAdapterInterface, Activity mActivity, ArrayList<RealmCart> realmCarts) {
        this.cartAdapterInterface = cartAdapterInterface;
        this.mActivity = mActivity;
        this.realmCarts = realmCarts;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycle_provider_cart_list, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        RealmCart realmCart = realmCarts.get(position);

        if (realmCart.getCustomer_image_url() != null && !realmCart.getCustomer_image_url().equals("")) {
            Glide.with(mActivity).
                    load(realmCart.getCustomer_image_url())
                    .into(holder.image);
        }
        holder.customer.setText(realmCart.getCustomer_name());
        holder.order_id.setText(realmCart.getOrder_id());
        if (realmCart.getItem_count() > 1) {
            holder.items_in_cart.setText(realmCart.getItem_count() + " items in cart");
        } else {
            holder.items_in_cart.setText(realmCart.getItem_count() + " item in cart");
        }

        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cartAdapterInterface.onViewClick(realmCarts, position, holder);
            }
        });

        holder.contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cartAdapterInterface.onContactClick(realmCarts, position, holder);
            }
        });

        holder.delivery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cartAdapterInterface.onDeliveryClick(realmCarts, position, holder);
            }
        });

        if (realmCart.getDelivered() == 1) {
            holder.delivery.setVisibility(View.GONE);
        } else {
            holder.delivery.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return realmCarts.size();
    }

    public interface ProviderCartAdapterInterface {
        void onViewClick(ArrayList<RealmCart> realmCarts, int position, ViewHolder holder);
        void onContactClick(ArrayList<RealmCart> realmCarts, int position, ViewHolder holder);
        void onOrderClick(ArrayList<RealmCart> realmCarts, int position, ViewHolder holder);
        void onDeliveryClick(ArrayList<RealmCart> realmCarts, int position, ViewHolder holder);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView items_in_cart, customer, view, contact, delivery, order_id;
        public ImageView image;

        public ViewHolder(View itemView) {
            super(itemView);
            items_in_cart = itemView.findViewById(R.id.items_in_cart);
            customer = itemView.findViewById(R.id.provider);
            image = itemView.findViewById(R.id.image);
            view = itemView.findViewById(R.id.view);
            contact = itemView.findViewById(R.id.contact);
            order_id = itemView.findViewById(R.id.order_id);
            delivery = itemView.findViewById(R.id.delivery);
        }
    }
}
