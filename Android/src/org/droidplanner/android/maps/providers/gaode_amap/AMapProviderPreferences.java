package org.droidplanner.android.maps.providers.gaode_amap;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;

import org.droidplanner.android.R;
import org.droidplanner.android.maps.providers.DPMapProvider;
import org.droidplanner.android.maps.providers.MapProviderPreferences;

/**
 * Created by joe on 2015/9/25.
 */
public class AMapProviderPreferences extends MapProviderPreferences {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_google_maps);
        setupPreferences();
    }

    private void setupPreferences() {
        final Context context = getActivity().getApplicationContext();
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        final String mapTypeKey = getString(R.string.pref_map_type_key);
        final Preference mapTypePref = findPreference(mapTypeKey);
        if (mapTypePref != null) {
            mapTypePref.setSummary(sharedPref.getString(mapTypeKey, ""));
            mapTypePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    mapTypePref.setSummary(newValue.toString());
                    return true;
                }
            });
        }
    }

    @Override
    public DPMapProvider getMapProvider() {
        return DPMapProvider.GAODE_AMAP;
    }
}
