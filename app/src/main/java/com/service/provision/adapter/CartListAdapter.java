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

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

/**
 * Created by Belal on 6/6/2017.
 */

public class CartListAdapter extends RecyclerView.Adapter<CartListAdapter.ViewHolder> {

    private static final String YOUR_DIALOG_TAG = "";
    CartAdapterInterface cartAdapterInterface;
    Activity mActivity;
    private ArrayList<RealmCart> realmCarts;

    public CartListAdapter(CartAdapterInterface cartAdapterInterface, Activity mActivity, ArrayList<RealmCart> realmCarts) {
        this.cartAdapterInterface = cartAdapterInterface;
        this.mActivity = mActivity;
        this.realmCarts = realmCarts;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycle_cart_list, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        RealmCart realmCart = realmCarts.get(position);


        if (realmCart.getProvider_image_url() != null && !realmCart.getProvider_image_url().equals("")) {
            Glide.with(mActivity).
                    load(realmCart.getProvider_image_url())
                    .into(holder.image);
        }
        holder.provider.setText(realmCart.getProvider_name() != null && !realmCart.getProvider_name().equals("") ? realmCart.getProvider_name(): StringUtils.normalizeSpace((realmCart.getProvider_title() + " " + realmCart.getProvider_first_name() + " " + realmCart.getProvider_other_name() + " " + realmCart.getProvider_last_name()).replace("null", "")));
        holder.order_id.setText(realmCart.getOrder_id());
        if (realmCart.getItem_count() > 1) {
            holder.items_in_cart.setText(realmCart.getItem_count() + " items in cart");
        } else {
            holder.items_in_cart.setText(realmCart.getItem_count() + " item in cart");
        }

        if (realmCart.getVerified() == 1) {
            holder.verifiedImage.setVisibility(View.VISIBLE);
        }
        else {
            holder.verifiedImage.setVisibility(View.INVISIBLE);
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

        holder.markasdelivered.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cartAdapterInterface.onMarkAsDeliveredClick(realmCarts, position, holder);
            }
        });

        if (realmCart.getDelivered() == 1) {
            holder.order.setVisibility(View.GONE);
            holder.delivery.setVisibility(View.GONE);
            holder.markasdelivered.setVisibility(View.GONE);
        } else {
            if (realmCart.getStatus() != null && realmCart.getStatus().contains("SUCCESS")) {
                holder.order.setVisibility(View.GONE);
                holder.delivery.setVisibility(View.VISIBLE);
                holder.markasdelivered.setVisibility(View.VISIBLE);
            }
            else {
                holder.order.setVisibility(View.VISIBLE);
                holder.delivery.setVisibility(View.GONE);
                holder.markasdelivered.setVisibility(View.GONE);
            }
        }

        holder.order.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cartAdapterInterface.onOrderClick(realmCarts, position, holder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return realmCarts.size();
    }

    public interface CartAdapterInterface {
        void onViewClick(ArrayList<RealmCart> realmCarts, int position, ViewHolder holder);
        void onContactClick(ArrayList<RealmCart> realmCarts, int position, ViewHolder holder);
        void onOrderClick(ArrayList<RealmCart> realmCarts, int position, ViewHolder holder);
        void onDeliveryClick(ArrayList<RealmCart> realmCarts, int position, ViewHolder holder);
        void onMarkAsDeliveredClick(ArrayList<RealmCart> realmStockCarts, int position, ViewHolder holder);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView items_in_cart, provider, view, contact, delivery, order, order_id, markasdelivered;
        public ImageView image, verifiedImage;

        public ViewHolder(View itemView) {
            super(itemView);
            items_in_cart = itemView.findViewById(R.id.items_in_cart);
            provider = itemView.findViewById(R.id.provider);
            image = itemView.findViewById(R.id.image);
            verifiedImage = itemView.findViewById(R.id.verifiedImage);
            view = itemView.findViewById(R.id.view);
            contact = itemView.findViewById(R.id.contact);
            order = itemView.findViewById(R.id.order);
            order_id = itemView.findViewById(R.id.order_id);
            delivery = itemView.findViewById(R.id.delivery);
            markasdelivered = itemView.findViewById(R.id.markasdelivered);
        }
    }
}
