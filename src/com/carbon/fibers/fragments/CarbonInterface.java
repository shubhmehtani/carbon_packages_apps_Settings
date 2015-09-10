/*
 * Copyright (C) 2015 Carbon Development
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.carbon.fibers.fragments;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.Resources;
import android.content.Context;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.settings.carbon.*;
import com.android.settings.carbon.gestureanywhere.*;
import com.android.settings.cyanogenmod.*;
import com.android.settings.R;
import com.android.settings.slim.*;
import com.carbon.fibers.preference.SettingsPreferenceFragment;
import com.carbon.fibers.Utils;

import java.lang.Exception;
import java.util.ArrayList;

public class CarbonInterface extends SettingsPreferenceFragment {

    private static final String TAG = "CarbonInterface";

    PagerTabStrip mPagerTabStrip;
    ViewPager mViewPager;

    String titleString[];

    ViewGroup mContainer;

    static Bundle mSavedState;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mContainer = container;

        View view = inflater.inflate(R.layout.tab_fibers, container, false);
        mViewPager = (ViewPager) view.findViewById(R.id.viewPager);
        mPagerTabStrip = (PagerTabStrip) view.findViewById(R.id.pagerTabStrip);
        mPagerTabStrip.setTabIndicatorColorResource(com.android.settings.R.color.color_lollipop);

        StatusBarAdapter StatusBarAdapter = new StatusBarAdapter(getFragmentManager());
        mViewPager.setAdapter(StatusBarAdapter);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // After confirming PreferenceScreen is available, we call super.
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!Utils.isTablet(getActivity())) {
            mContainer.setPadding(0, 0, 0, 0);
        }
    }

    class StatusBarAdapter extends FragmentPagerAdapter {
        String titles[] = getTitles();
        private Fragment frags[] = new Fragment[titles.length];

        public StatusBarAdapter(FragmentManager fm) {
            super(fm);
            frags[0] = new CarbonInterfaceSettings();
            frags[1] = new GestureAnywhereSettings();
            frags[2] = new DisplayAnimationsSettings();
            frags[3] = new AnimationControls();
            frags[4] = new ScrollAnimationInterfaceSettings();
            frags[5] = new OverscrollEffects();
            frags[6] = new AppCircleBar();
            frags[7] = new AppSidebar();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }

        @Override
        public Fragment getItem(int position) {
            return frags[position];
        }

        @Override
        public int getCount() {
            return frags.length;
        }
    }

    private String[] getTitles() {
        String titleString[];
        titleString = new String[] {
                    getString(R.string.interface_settings_title),
                    getString(R.string.gesture_anywhere_title),
                    getString(R.string.disp_anim_settings_title),
                    getString(R.string.aokp_animation_title),
                    getString(R.string.scrolling_title),
                    getString(R.string.ui_overscroll_title),
                    getString(R.string.app_circle_bar_title),
                    getString(R.string.app_sidebar_title)};
        return titleString;
    }
}