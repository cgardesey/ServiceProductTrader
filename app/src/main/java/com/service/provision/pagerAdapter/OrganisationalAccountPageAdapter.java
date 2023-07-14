package com.service.provision.pagerAdapter;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.service.provision.fragment.OrganisationalProviderAccountFragment1;
import com.service.provision.fragment.OrganisationalProviderAccountFragment2;
import com.service.provision.fragment.OrganisationalProviderAccountFragment3;
import com.service.provision.fragment.OrganisationalProviderAccountFragment4;

public class OrganisationalAccountPageAdapter extends FragmentPagerAdapter {

    public OrganisationalAccountPageAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                OrganisationalProviderAccountFragment1 organisationalProviderAccountFragment1 = new OrganisationalProviderAccountFragment1();
                return organisationalProviderAccountFragment1;
            case 1:
                OrganisationalProviderAccountFragment2 organisationalProviderAccountFragment2 = new OrganisationalProviderAccountFragment2();
                return organisationalProviderAccountFragment2;
            case 2:
                OrganisationalProviderAccountFragment3 organisationalProviderAccountFragment3 = new OrganisationalProviderAccountFragment3();
                return organisationalProviderAccountFragment3;
            case 3:
                OrganisationalProviderAccountFragment4 organisationalProviderAccountFragment4 = new OrganisationalProviderAccountFragment4();
                return organisationalProviderAccountFragment4;

            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 4;
    }
}