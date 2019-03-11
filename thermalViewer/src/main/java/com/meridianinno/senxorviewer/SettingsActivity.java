package com.meridianinno.senxorviewer;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.view.MenuItem;

import com.meridianinno.utility.ConversionHelper;
import com.meridianinno.utility.TemperatureConverter;

import java.util.List;
import java.util.Locale;

import static java.lang.Double.NaN;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity  {

    private static SharedPreferences mSharedPref;
    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPrefListener;

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mSharedPrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                // TODO: Does SettingsActivity do anything when settings changed?
            }
        };
        mSharedPref.registerOnSharedPreferenceChangeListener(mSharedPrefListener);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new GeneralPreferenceFragment())
                .commit();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {

        private ConversionHelper mHelper;

        private void resetAlarmSummary() {
            final EditTextPreference upper = (EditTextPreference)findPreference("pref_over_temp_alarm_value");
            final EditTextPreference lower = (EditTextPreference)findPreference("pref_under_temp_alarm_value");
            final String unitValue = ((ListPreference)findPreference("pref_temp_unit")).getValue();
            String suffix = getTempUnitDisplaySuffix(unitValue);
            upper.setSummary(upper.getText() + suffix);
            lower.setSummary(lower.getText() + suffix);
        }

        String getTempUnitDisplaySuffix(String unitValue) {
            return unitValue.equals(getString(R.string.kelvin_value))? " K" :
                    unitValue.equals(getString(R.string.degree_c_value))? " \u00B0C" : " \u00B0F";
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            PreferenceManager.setDefaultValues(getActivity(), R.xml.pref_general, false);

            resetAlarmSummary();

            // Reduce lookup times by using class member variables
            mHelper = new ConversionHelper(getString(R.string.degree_c_value), getString(R.string.degree_f_value), getString(R.string.kelvin_value));

            findPreference("pref_temp_unit").setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                        final EditTextPreference upper = (EditTextPreference)findPreference("pref_over_temp_alarm_value");
                        final EditTextPreference lower = (EditTextPreference)findPreference("pref_under_temp_alarm_value");
                        final double upperOldTemp = Double.valueOf(upper.getText());
                        final double lowerOldTemp = Double.valueOf(lower.getText());
                        final String oldUnit = ((ListPreference)preference).getValue();
                        final String newUnit = newValue.toString();
                        final double upperNewTemp = mHelper.convertTemperatureByUnitString(upperOldTemp, oldUnit, newUnit);
                        final double lowerNewTemp = mHelper.convertTemperatureByUnitString(lowerOldTemp, oldUnit, newUnit);
                        final String upperNewText = String.format(Locale.getDefault(), "%2.1f", upperNewTemp);
                        final String lowerNewText = String.format(Locale.getDefault(), "%2.1f", lowerNewTemp);
                        // TODO: handle NaN in upper and lower NewTemps
                        upper.setText(upperNewText);
                        lower.setText(lowerNewText);
                        String suffix = getTempUnitDisplaySuffix(newUnit);
                        upper.setSummary(upperNewText + suffix);
                        lower.setSummary(lowerNewText + suffix);
                        return true;
                        }
                    }
            );

            Preference.OnPreferenceChangeListener alarmTempChangeListener =
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        final String unitValue = ((ListPreference)findPreference("pref_temp_unit")).getValue();
                        String suffix = getTempUnitDisplaySuffix(unitValue);
                        preference.setSummary(newValue.toString() + suffix);
                        return true;
                    }
                };

            findPreference("pref_over_temp_alarm_value")
                    .setOnPreferenceChangeListener(alarmTempChangeListener);
            findPreference("pref_under_temp_alarm_value")
                    .setOnPreferenceChangeListener(alarmTempChangeListener);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}