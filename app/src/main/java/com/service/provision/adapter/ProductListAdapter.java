package com.service.provision.adapter;

/**
 * Created by Nana on 11/10/2017.
 */

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.service.provision.R;
import com.service.provision.realm.RealmProductCategory;

import java.util.ArrayList;

/**
 * Created by Belal on 6/6/2017.
 */

public class ProductListAdapter extends RecyclerView.Adapter<ProductListAdapter.ViewHolder> {

    private static final String YOUR_DIALOG_TAG = "";
    ListAdapterInterface listAdapterInterface;
    Activity mActivity;
    String title = "";
    private ArrayList<RealmProductCategory> realmProviderCategories;

    public ProductListAdapter(ListAdapterInterface listAdapterInterface, Activity mActivity, ArrayList<RealmProductCategory> realmProviderCategories, String title) {
        this.listAdapterInterface = listAdapterInterface;
        this.mActivity = mActivity;
        this.realmProviderCategories = realmProviderCategories;
        this.title = title;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycle_list, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        RealmProductCategory RealmProductCategory = realmProviderCategories.get(position);

        if (RealmProductCategory.getUrl() != null && !RealmProductCategory.getUrl().equals("")) {
            holder.categoryicon.setVisibility(View.VISIBLE);
            Glide.with(mActivity).
                    load(RealmProductCategory.getUrl())
                    .into(holder.categoryicon);
        }
        else {
            holder.categoryicon.setVisibility(View.GONE);
        }

        String[] split = RealmProductCategory.getTitle().split((" >> "));
        holder.textViewName.setText(split[split.length - 1]);
        holder.parent.setOnClickListener(view -> listAdapterInterface.onListItemClick(realmProviderCategories, position, holder));
    }

    @Override
    public int getItemCount() {
//        Toast.makeText(mActivity, String.valueOf(realmProviderCategories.size()), Toast.LENGTH_SHORT).show();
        return realmProviderCategories.size();
    }

    public interface ListAdapterInterface {
        void onListItemClick(ArrayList<RealmProductCategory> names, int position, ViewHolder holder);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView textViewName;
        public ImageView categoryicon;
        public LinearLayout parent;

        public ViewHolder(View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewName);
            parent = itemView.findViewById(R.id.parent);
            categoryicon = itemView.findViewById(R.id.categoryicon);
        }
    }
}
