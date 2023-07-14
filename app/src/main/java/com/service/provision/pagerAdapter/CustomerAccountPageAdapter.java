package com.service.provision.pagerAdapter;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.service.provision.fragment.CustomerAccountFragment1;

public class CustomerAccountPageAdapter extends FragmentPagerAdapter {

    public CustomerAccountPageAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                CustomerAccountFragment1 tab1 = new CustomerAccountFragment1();
                return tab1;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 1;
    }
}