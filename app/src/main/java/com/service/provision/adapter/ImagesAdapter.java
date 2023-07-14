package com.service.provision.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.service.provision.R;
import com.service.provision.activity.PictureActivity;
import com.service.provision.realm.RealmProductImage;
import com.service.provision.realm.RealmServiceImage;

import java.util.ArrayList;

import static com.service.provision.fragment.PersonalProviderAccountFragment1.PICTURE_TYPE;
import static com.service.provision.fragment.PersonalProviderAccountFragment1.TYPE_PROFILE_PIC;
import static com.service.provision.activity.PictureActivity.idPicBitmap;

public class ImagesAdapter extends RecyclerView.Adapter<ImagesAdapter.ViewHolder> implements Filterable {

    ArrayList<Object> objects;
    private Activity activity;
    private String type;
    ImagesAdapterInterface imagesAdapterInterface;

    public ImagesAdapter(ImagesAdapterInterface imagesAdapterInterface, Activity activity, ArrayList<Object> objects, String type) {
        this.imagesAdapterInterface = imagesAdapterInterface;
        this.activity = activity;
        this.objects = objects;
        this.type = type;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycle_image, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, @SuppressLint("RecyclerView") int position) {

        Object object = objects.get(position);
        if (objects.get(position) instanceof RealmServiceImage) {
            RealmServiceImage realmServiceImage = (RealmServiceImage) object;
            Glide.with(activity).load(realmServiceImage.getUrl()).apply(new RequestOptions().centerCrop()).into(holder.image);
            holder.image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    imagesAdapterInterface.onListItemClick(objects, position, holder);
                }
            });

            String role = PreferenceManager.getDefaultSharedPreferences(activity).getString("ROLE", "");
            if (role.equals("CUSTOMER")) {
                holder.more_details.setVisibility(View.GONE);
                holder.featured_layout.setVisibility(View.GONE);
            }
            else {
                if (realmServiceImage.getFeatured_image() == 1) {
                    holder.featured_layout.setVisibility(View.VISIBLE);
                    holder.more_details.setVisibility(View.GONE);
                }
                else {
                    holder.featured_layout.setVisibility(View.GONE);
                    holder.more_details.setVisibility(View.VISIBLE);
                }


                if (objects.size() == 1) {
                    holder.more_details.setVisibility(View.GONE);
                }
                else {
                    holder.more_details.setVisibility(View.VISIBLE);
                }
            }
        }
        else if (objects.get(position) instanceof RealmProductImage) {
            RealmProductImage realmProductImage = (RealmProductImage) object;
            Glide.with(activity).load(realmProductImage.getUrl()).apply(new RequestOptions().centerCrop()).into(holder.image);
            holder.image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    imagesAdapterInterface.onListItemClick(objects, position, holder);
                }
            });

            String role = PreferenceManager.getDefaultSharedPreferences(activity).getString("ROLE", "");
            if (role.equals("CUSTOMER")) {
                holder.more_details.setVisibility(View.GONE);
                holder.featured_layout.setVisibility(View.GONE);
            }
            else {
                if (realmProductImage.getFeatured_image() == 1) {
                    holder.featured_layout.setVisibility(View.VISIBLE);
                    holder.more_details.setVisibility(View.GONE);
                }
                else {
                    holder.featured_layout.setVisibility(View.GONE);
                    holder.more_details.setVisibility(View.VISIBLE);
                }


                if (objects.size() == 1) {
                    holder.more_details.setVisibility(View.GONE);
                }
                else {
                    holder.more_details.setVisibility(View.VISIBLE);
                }
            }
        }

        holder.image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                idPicBitmap = ((BitmapDrawable) holder.image.getDrawable()).getBitmap();
                Intent intent = new Intent(activity, PictureActivity.class);
                intent.putExtra(PICTURE_TYPE, TYPE_PROFILE_PIC);
                activity.startActivity(intent);
            }
        });

        holder.more_details.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imagesAdapterInterface.onListItemClick(objects, position, holder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return objects.size();
    }

    @Override
    public Filter getFilter() {
        return null;
    }

    public void reload(ArrayList<Object> resourceArrayList) {
        this.objects = resourceArrayList;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView image, more_details;
        public FrameLayout featured_layout;

        public ViewHolder(View view) {
            super(view);
            image = view.findViewById(R.id.image);
            more_details = view.findViewById(R.id.more_details);
            featured_layout = view.findViewById(R.id.featured_layout);
        }
    }

    public interface ImagesAdapterInterface {
        void onListItemClick(ArrayList<Object> objects, int position, ViewHolder holder);
    }
}

