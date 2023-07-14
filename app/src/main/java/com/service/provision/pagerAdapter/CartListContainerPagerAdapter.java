package com.service.provision.pagerAdapter;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.service.provision.fragment.CartListFragment;

import java.util.ArrayList;

public class CartListContainerPagerAdapter extends FragmentPagerAdapter {
    ArrayList<String> titles;

    public CartListContainerPagerAdapter(FragmentManager fm, ArrayList<String> titles) {
        super(fm);
        this.titles = titles;
    }

    @Override
    public Fragment getItem(int position) {
        CartListFragment cartListFragment = new CartListFragment();
        Bundle bundle = new Bundle(3);
        bundle.putString("title", getPageTitle(position).toString());
        cartListFragment.setArguments(bundle);

        return cartListFragment;
    }

    @Override
    public int getCount() {
        return titles.size();
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        String title = titles.get(position);
        return title;
    }
}