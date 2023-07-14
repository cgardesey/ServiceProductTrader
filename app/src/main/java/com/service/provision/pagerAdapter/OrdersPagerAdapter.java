package com.service.provision.pagerAdapter;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.service.provision.fragment.ConsumerOrderFragment;

import java.util.ArrayList;

public class OrdersPagerAdapter extends FragmentPagerAdapter {
    ArrayList<String> statuses;

    public OrdersPagerAdapter(FragmentManager fm, ArrayList<String> statuses) {
        super(fm);
        this.statuses = statuses;
    }

    @Override
    public Fragment getItem(int position) {
        ConsumerOrderFragment consumerOrderFragment = new ConsumerOrderFragment();
        Bundle bundle = new Bundle(3);
        bundle.putString("status", getPageTitle(position).toString());
        consumerOrderFragment.setArguments(bundle);

        return consumerOrderFragment;
    }

    @Override
    public int getCount() {
        return statuses.size();
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return statuses.get(position);
    }
}