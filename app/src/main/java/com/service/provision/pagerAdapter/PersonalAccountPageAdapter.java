package com.service.provision.pagerAdapter;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.service.provision.fragment.PersonalProviderAccountFragment1;
import com.service.provision.fragment.PersonalProviderAccountFragment2;
import com.service.provision.fragment.PersonalProviderAccountFragment3;
import com.service.provision.fragment.PersonalProviderAccountFragment4;

public class PersonalAccountPageAdapter extends FragmentPagerAdapter {

    public PersonalAccountPageAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                PersonalProviderAccountFragment1 personalProviderAccountFragment1 = new PersonalProviderAccountFragment1();
                return personalProviderAccountFragment1;
            case 1:
                PersonalProviderAccountFragment2 personalProviderAccountFragment2 = new PersonalProviderAccountFragment2();
                return personalProviderAccountFragment2;
            case 2:
                PersonalProviderAccountFragment3 personalProviderAccountFragment3 = new PersonalProviderAccountFragment3();
                return personalProviderAccountFragment3;
            case 3:
                PersonalProviderAccountFragment4 personalProviderAccountFragment4 = new PersonalProviderAccountFragment4();
                return personalProviderAccountFragment4;

            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 4;
    }
}