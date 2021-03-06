/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.carbon.fuelgauge;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.PreferenceFragment;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Switch;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import com.android.settings.notification.SettingPref;
import com.android.settings.carbon.SystemSettingSwitchPreference;
import com.android.settings.widget.SwitchBar;

import java.util.List;
import java.util.ArrayList;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import static android.provider.Settings.System.BATTERY_LIGHT_ENABLED;

public class BatteryLightSettings extends SettingsPreferenceFragment implements
        SwitchBar.OnSwitchChangeListener, Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "BatteryLightSettings";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final String LOW_COLOR_PREF = "low_color";
    private static final String MEDIUM_COLOR_PREF = "medium_color";
    private static final String FULL_COLOR_PREF = "full_color";
    private static final String REALLY_FULL_COLOR_PREF = "really_full_color";
    private static final String FAST_COLOR_PREF = "fast_color";
    private static final String FAST_CHARGING_LED_PREF = "fast_charging_led_enabled";
    private static final String BATTERY_PULSE_PREF = "battery_light_pulse";
    private static final String BATTERY_LIGHT_ONLY_FULL_PREF = "battery_light_only_fully_charged";
    private static final String KEY_CATEGORY_FAST_CHARGE = "fast_color_cat";
    private static final String KEY_CATEGORY_CHARGE_COLORS = "colors_list";

    private static final long WAIT_FOR_SWITCH_ANIM = 500;
    private final Handler mHandler = new Handler();

    private boolean mMultiColorLed;
    private SystemSettingSwitchPreference mPulsePref;
    private SystemSettingSwitchPreference mOnlyFullPref;
    private SystemSettingSwitchPreference mFastBatteryLightEnabledPref;
    private PreferenceGroup mColorPrefs;
    private ColorPickerPreference mLowColorPref;
    private ColorPickerPreference mMediumColorPref;
    private ColorPickerPreference mFullColorPref;
    private ColorPickerPreference mReallyFullColorPref;
    private ColorPickerPreference mFastColorPref;
    private static final int MENU_RESET = Menu.FIRST;
    private int mLowBatteryWarningLevel;
    private boolean mBatteryLightEnabled;
    private boolean mFastBatteryLightEnabled;

    private boolean mCreated;
    private boolean mValidListener;
    private Context mContext;
    private SwitchBar mSwitchBar;
    private Switch mSwitch;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.FUELGAUGE_POWER_USAGE_SUMMARY;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mCreated) {
            mSwitchBar.show();
            return;
        }
        mCreated = true;
        addPreferencesFromResource(R.xml.battery_light_settings);
        mFooterPreferenceMixin.createFooterPreference()
                .setTitle(R.string.battery_light_description);
        mContext = getActivity();
        mSwitchBar = ((SettingsActivity) mContext).getSwitchBar();
        mSwitch = mSwitchBar.getSwitch();
        mSwitchBar.show();

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getContentResolver();
        mLowBatteryWarningLevel = getResources().getInteger(
                com.android.internal.R.integer.config_lowBatteryWarningLevel);
        mBatteryLightEnabled = getResources().getBoolean(
                com.android.internal.R.bool.config_intrusiveBatteryLed);

        mPulsePref = (SystemSettingSwitchPreference)prefSet.findPreference(BATTERY_PULSE_PREF);
        mPulsePref.setChecked(Settings.System.getIntForUser(resolver,
                        Settings.System.BATTERY_LIGHT_PULSE, mBatteryLightEnabled ? 1 : 0, UserHandle.USER_CURRENT) != 0);
        mPulsePref.setOnPreferenceChangeListener(this);

        mOnlyFullPref = (SystemSettingSwitchPreference)prefSet.findPreference(BATTERY_LIGHT_ONLY_FULL_PREF);
        mOnlyFullPref.setOnPreferenceChangeListener(this);

        // Does the Device support changing battery LED colors?
        if (getResources().getBoolean(com.android.internal.R.bool.config_multiColorBatteryLed)) {
            setHasOptionsMenu(true);

            // Low, Medium and full color preferences
            mLowColorPref = (ColorPickerPreference) prefSet.findPreference(LOW_COLOR_PREF);
            mLowColorPref.setAlphaSliderEnabled(false);
            mLowColorPref.setOnPreferenceChangeListener(this);

            mMediumColorPref = (ColorPickerPreference) prefSet.findPreference(MEDIUM_COLOR_PREF);
            mMediumColorPref.setAlphaSliderEnabled(false);
            mMediumColorPref.setOnPreferenceChangeListener(this);

            mFullColorPref = (ColorPickerPreference) prefSet.findPreference(FULL_COLOR_PREF);
            mFullColorPref.setAlphaSliderEnabled(false);
            mFullColorPref.setOnPreferenceChangeListener(this);

            mReallyFullColorPref = (ColorPickerPreference) prefSet.findPreference(REALLY_FULL_COLOR_PREF);
            mReallyFullColorPref.setAlphaSliderEnabled(false);
            mReallyFullColorPref.setOnPreferenceChangeListener(this);

            mFastBatteryLightEnabledPref = (SystemSettingSwitchPreference)prefSet.findPreference(FAST_CHARGING_LED_PREF);

            mFastColorPref = (ColorPickerPreference) prefSet.findPreference(FAST_COLOR_PREF);
            mFastColorPref.setAlphaSliderEnabled(false);
            mFastColorPref.setOnPreferenceChangeListener(this);

            // Does the Device support fast charge ?
            if (!getResources().getBoolean(com.android.internal.R.bool.config_FastChargingLedSupported)) {
                prefSet.removePreference(prefSet.findPreference(KEY_CATEGORY_FAST_CHARGE));
            }
        } else {
            prefSet.removePreference(prefSet.findPreference(KEY_CATEGORY_CHARGE_COLORS));
            // not multi color cant have fast charge
            prefSet.removePreference(prefSet.findPreference(KEY_CATEGORY_FAST_CHARGE));
        }
        boolean showOnlyWhenFull = Settings.System.getIntForUser(resolver,
                Settings.System.BATTERY_LIGHT_ONLY_FULLY_CHARGED, 0, UserHandle.USER_CURRENT) != 0;
        updateEnablement(showOnlyWhenFull);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSwitchBar.hide();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mValidListener) {
            mSwitchBar.addOnSwitchChangeListener(this);
            mValidListener = true;
        }
        updateSwitch();
        refreshDefault();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mValidListener) {
            mSwitchBar.removeOnSwitchChangeListener(this);
            mValidListener = false;
        }
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        mHandler.removeCallbacks(mEnableBatteryLight);
        if (isChecked) {
            mHandler.postDelayed(mEnableBatteryLight, WAIT_FOR_SWITCH_ANIM);
        } else {
            if (DEBUG) Log.d(TAG, "Disabling battery light from settings");
            tryEnableBatteryLight(false);
        }
    }

    private void tryEnableBatteryLight(boolean enabled) {
        if (!setBatteryLightEnabled(enabled)) {
            if (DEBUG) Log.d(TAG, "Setting enabled failed, fallback to current value");
            mHandler.post(mUpdateSwitch);
        }
    }

    private void updateSwitch() {
        final boolean enabled = isBatteryLightEnabled();
        if (DEBUG) Log.d(TAG, "updateSwitch: isChecked=" + mSwitch.isChecked() + " enabled=" + enabled);
        if (enabled == mSwitch.isChecked()) return;

        // set listener to null so that that code below doesn't trigger onCheckedChanged()
        if (mValidListener) {
            mSwitchBar.removeOnSwitchChangeListener(this);
        }
        mSwitch.setChecked(enabled);
        if (mValidListener) {
            mSwitchBar.addOnSwitchChangeListener(this);
        }
    }

    private final Runnable mUpdateSwitch = new Runnable() {
        @Override
        public void run() {
            updateSwitch();
        }
    };

    private final Runnable mEnableBatteryLight = new Runnable() {
        @Override
        public void run() {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    if (DEBUG) Log.d(TAG, "Enabling battery light from settings");
                    tryEnableBatteryLight(true);
                }
            });
        }
    };

    public boolean isBatteryLightEnabled() {
        return Settings.System.getIntForUser(mContext.getContentResolver(), BATTERY_LIGHT_ENABLED, mBatteryLightEnabled ? 1 : 0, UserHandle.USER_CURRENT) != 0;
    }
  
    public boolean setBatteryLightEnabled(boolean enabled) {
        return Settings.System.putIntForUser(mContext.getContentResolver(), BATTERY_LIGHT_ENABLED, enabled ? 1 : 0, UserHandle.USER_CURRENT);
    }

    private void refreshDefault() {
        ContentResolver resolver = getContentResolver();
        Resources res = getResources();

        if (mLowColorPref != null) {
            int lowColor = Settings.System.getIntForUser(getContentResolver(),
                    Settings.System.BATTERY_LIGHT_LOW_COLOR, res.getInteger(com.android.internal.R.integer.config_notificationsBatteryLowARGB),
                            UserHandle.USER_CURRENT);
            mLowColorPref.setNewPreviewColor(lowColor);
        }

        if (mMediumColorPref != null) {
            int mediumColor = Settings.System.getIntForUser(getContentResolver(),
                    Settings.System.BATTERY_LIGHT_MEDIUM_COLOR, res.getInteger(com.android.internal.R.integer.config_notificationsBatteryMediumARGB),
                            UserHandle.USER_CURRENT);
            mMediumColorPref.setNewPreviewColor(mediumColor);
        }

        if (mFullColorPref != null) {
            int fullColor = Settings.System.getIntForUser(getContentResolver(),
                    Settings.System.BATTERY_LIGHT_FULL_COLOR, res.getInteger(com.android.internal.R.integer.config_notificationsBatteryFullARGB),
                            UserHandle.USER_CURRENT);
            mFullColorPref.setNewPreviewColor(fullColor);
        }

        if (mReallyFullColorPref != null) {
            int reallyFullColor = Settings.System.getIntForUser(getContentResolver(),
                    Settings.System.BATTERY_LIGHT_REALLY_FULL_COLOR, res.getInteger(com.android.internal.R.integer.config_notificationsBatteryFullARGB),
                            UserHandle.USER_CURRENT);
            mReallyFullColorPref.setNewPreviewColor(reallyFullColor); 
        }

        if (mFastColorPref != null) {
            int fastColor = Settings.System.getIntForUser(getContentResolver(),
                    Settings.System.FAST_BATTERY_LIGHT_COLOR, res.getInteger(com.android.internal.R.integer.config_notificationsFastBatteryARGB),
                            UserHandle.USER_CURRENT);
            mFastColorPref.setNewPreviewColor(fastColor);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_backup_restore)
                .setAlphabeticShortcut('r')
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetToDefaults();
                return true;
        }
        return false;
    }

    protected void resetColors() {
        ContentResolver resolver = getActivity().getContentResolver();
        Resources res = getResources();

        // Reset to the framework default colors
        Settings.System.putIntForUser(resolver, Settings.System.BATTERY_LIGHT_LOW_COLOR, res.getInteger(com.android.internal.R.integer.config_notificationsBatteryLowARGB), UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver, Settings.System.BATTERY_LIGHT_MEDIUM_COLOR, res.getInteger(com.android.internal.R.integer.config_notificationsBatteryMediumARGB), UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver, Settings.System.BATTERY_LIGHT_FULL_COLOR, res.getInteger(com.android.internal.R.integer.config_notificationsBatteryFullARGB), UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver, Settings.System.BATTERY_LIGHT_REALLY_FULL_COLOR, res.getInteger(com.android.internal.R.integer.config_notificationsBatteryFullARGB), UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver, Settings.System.FAST_BATTERY_LIGHT_COLOR, res.getInteger(com.android.internal.R.integer.config_notificationsFastBatteryARGB), UserHandle.USER_CURRENT);
        refreshDefault();
    }

    protected void resetToDefaults() {
        if (mPulsePref != null) mPulsePref.setChecked(false);
        if (mOnlyFullPref != null) mOnlyFullPref.setChecked(false);
        if (mFastBatteryLightEnabledPref != null) mFastBatteryLightEnabledPref.setChecked(false);
        resetColors();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getContentResolver();
        if (preference.equals(mPulsePref)) {
            boolean value = (Boolean) objValue;
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.BATTERY_LIGHT_PULSE, value ? 1:0, UserHandle.USER_CURRENT);
        } else if (preference.equals(mOnlyFullPref)) {
            boolean value = (Boolean) objValue;
            // If enabled, disable all but really full color preference.
            updateEnablement(value);
        } else if (preference.equals(mLowColorPref)) {
            int color = ((Integer) objValue).intValue();
            Settings.System.putIntForUser(resolver, Settings.System.BATTERY_LIGHT_LOW_COLOR, color, UserHandle.USER_CURRENT);
        } else if (preference.equals(mMediumColorPref)) {
            int color = ((Integer) objValue).intValue();
            Settings.System.putIntForUser(resolver, Settings.System.BATTERY_LIGHT_MEDIUM_COLOR, color, UserHandle.USER_CURRENT);
        } else if (preference.equals(mFullColorPref)) {
            int color = ((Integer) objValue).intValue();
            Settings.System.putIntForUser(resolver, Settings.System.BATTERY_LIGHT_FULL_COLOR, color, UserHandle.USER_CURRENT);
        } else if (preference.equals(mReallyFullColorPref)) {
            int color = ((Integer) objValue).intValue();
            Settings.System.putIntForUser(resolver, Settings.System.BATTERY_LIGHT_REALLY_FULL_COLOR, color, UserHandle.USER_CURRENT);
        } else if (preference.equals(mFastColorPref)) {
            int color = ((Integer) objValue).intValue();
            Settings.System.putIntForUser(resolver, Settings.System.FAST_BATTERY_LIGHT_COLOR, color, UserHandle.USER_CURRENT);
        }
        return true;
    }

    private void updateEnablement(boolean showOnlyWhenFull) {
        // If enabled, disable all but really full color preference.
        if (mLowColorPref != null) {
            mLowColorPref.setEnabled(!showOnlyWhenFull && isBatteryLightEnabled());
        }
        if (mMediumColorPref != null) {
            mMediumColorPref.setEnabled(!showOnlyWhenFull && isBatteryLightEnabled());
        }
        if (mFullColorPref != null) {
            mFullColorPref.setEnabled(!showOnlyWhenFull && isBatteryLightEnabled());
        }
        if (mFastColorPref != null) {
            mFastColorPref.setEnabled(!showOnlyWhenFull && isBatteryLightEnabled());
        }
        if (mFastBatteryLightEnabledPref != null) {
            mFastBatteryLightEnabledPref.setEnabled(!showOnlyWhenFull && isBatteryLightEnabled());
        }
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.battery_light_settings;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    final Resources res = context.getResources();
                    boolean intrusiveLED = res.getBoolean(com.android.internal.R.bool.config_intrusiveBatteryLed);
                    if (!intrusiveLED) {
                        result.add(BATTERY_LIGHT_ONLY_FULL_PREF);
                    }
                    if (!intrusiveLED || !res.getBoolean(com.android.internal.R.bool.config_ledCanPulse)) {
                        result.add(BATTERY_PULSE_PREF);
                    }
                    if (!res.getBoolean(com.android.internal.R.bool.config_multiColorBatteryLed)) {
                        result.add(LOW_COLOR_PREF);
                        result.add(MEDIUM_COLOR_PREF);
                        result.add(FULL_COLOR_PREF);
                        result.add(REALLY_FULL_COLOR_PREF);
                    }
                    if (!res.getBoolean(com.android.internal.R.bool.config_FastChargingLedSupported)) {
                        result.add(FAST_CHARGING_LED_PREF);
                        result.add(FAST_COLOR_PREF);
                    }
                    return result;
                }
            };
}
