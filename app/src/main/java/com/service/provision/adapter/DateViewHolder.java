package com.service.provision.adapter;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.service.provision.R;

/**
 * Created by 2CLearning on 2/8/2018.
 */

public class DateViewHolder extends RecyclerView.ViewHolder {
    TextView date;

    public DateViewHolder(View v) {
        super(v);
        date = itemView.findViewById(R.id.date);
    }
}
