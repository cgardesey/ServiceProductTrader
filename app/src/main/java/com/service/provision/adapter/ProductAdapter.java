package com.service.provision.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.service.provision.R;
import com.service.provision.realm.RealmProduct;
import com.service.provision.realm.RealmProductImage;
import com.service.provision.util.RealmUtility;

import java.util.ArrayList;

import io.realm.Realm;

/**
 * Created by Nana on 9/11/2017.
 */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    ProductAdapterInterface productAdapterInterface;
    ArrayList<RealmProduct> realmProducts;
    private Context mContext;

    public ProductAdapter(ProductAdapterInterface productAdapterInterface, ArrayList<RealmProduct> realmProducts) {
        this.productAdapterInterface = productAdapterInterface;
        this.realmProducts = realmProducts;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        View view = LayoutInflater.from(mContext).inflate(R.layout.recycle_product, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        RealmProduct realmProduct = realmProducts.get(position);

        holder.productcategory.setText(realmProduct.getProduct_category());
        holder.name.setText(realmProduct.getName());
        holder.unit_quantity.setText(String.valueOf(realmProduct.getUnit_quantity()));
        holder.quantity_available.setText(String.valueOf(realmProduct.getQuantity_available()));
        holder.unit_price.setText(String.format("%.2f", realmProduct.getUnit_price()));
        if (realmProduct.getQuantity_available() < realmProduct.getUnit_quantity()) {
            holder.quantity_available.setTextColor(Color.RED);
        }
        else {
            holder.quantity_available.setTextColor(0xFF888888);
        }

        String product_id = realmProduct.getProduct_id();
        final RealmProductImage[] realmProductImage = new RealmProductImage[1];
        Realm.init(mContext);
        Realm.getInstance(RealmUtility.getDefaultConfig(mContext)).executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realmProductImage[0] = realm.where(RealmProductImage.class)
                        .equalTo("product_id", product_id)
                        .equalTo("featured_image", 1)
                        .findFirst();
            }
        });

        if (realmProductImage[0] != null) {
            holder.featured_image.setVisibility(View.VISIBLE);
            Glide.with(mContext).load(realmProductImage[0].getUrl())
                    .into(holder.featured_image);
        }
        else {
            holder.featured_image.setVisibility(View.INVISIBLE);
        }

        mContext = holder.more_details.getContext();

        holder.more_details.setOnClickListener(view -> {
            productAdapterInterface.onListItemClick(realmProducts, position, holder);
        });
    }

    @Override
    public int getItemCount() {
        return realmProducts.size();
    }

    public interface ProductAdapterInterface {
        void onListItemClick(ArrayList<RealmProduct> realmProducts, int position, ViewHolder holder);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView productcategory, name, unit_quantity, quantity_available, unit_price;
        public ImageView more_details, featured_image;

        public ViewHolder(View view) {
            super(view);
            productcategory = view.findViewById(R.id.productcategory);
            name = view.findViewById(R.id.provider_name);
            unit_quantity = view.findViewById(R.id.unit_quantity);
            quantity_available = view.findViewById(R.id.quantity_available);
            unit_price = view.findViewById(R.id.unit_price);
            more_details = view.findViewById(R.id.more_details);
            featured_image = view.findViewById(R.id.featured_image);
        }
    }
}
