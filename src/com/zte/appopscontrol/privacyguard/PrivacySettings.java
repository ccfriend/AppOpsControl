/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.zte.appopscontrol.privacyguard;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import com.zte.appopscontrol.R;
import com.zte.appopscontrol.Utils;

import android.app.Fragment;
import android.app.FragmentManager;

/**
 * Privacy settings
 */
public class PrivacySettings extends PreferenceActivity {

    private static final String KEY_BLACKLIST = "blacklist";
    private static final String KEY_PRIVACY_GUARD = "privacy_guard_manager";
    
    private PreferenceScreen mBlacklist;
    private Preference mPrivacyGuard;
    
    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.privacy_settings_cyanogenmod);

        mBlacklist = (PreferenceScreen) findPreference(KEY_BLACKLIST);
        mPrivacyGuard = (Preference) findPreference(KEY_PRIVACY_GUARD);
        
        // Determine options based on device telephony support
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            // WhisperPush
            // Only add if device has telephony support and has WhisperPush installed.
            if (Utils.isPackageInstalled(this, "org.whispersystems.whisperpush")) {
                addPreferencesFromResource(R.xml.security_settings_whisperpush);
            }
        } else {
            // No telephony, remove dependent options
            getPreferenceScreen().removePreference(mBlacklist);
            mBlacklist = null;
        }

        addPreferencesFromResource(R.xml.security_settings_cyanogenmod);
        // Logger
        // Only add if device has Logger installed
        //if (Utils.isPackageInstalled(getActivity(), "com.cyngn.logger")) {
        //    addPreferencesFromResource(R.xml.security_settings_logger);
        //}
    }

    @Override
    public void onResume() {
        super.onResume();
        updateBlacklistSummary();
    }

    private void updateBlacklistSummary() {
        if (mBlacklist != null) {
            //if (BlacklistUtils.isBlacklistEnabled(this)) {
            //    mBlacklist.setSummary(R.string.blacklist_summary);
            //} else {
            //    mBlacklist.setSummary(R.string.blacklist_summary_disabled);
            //}
        }
    }
    
    @SuppressWarnings("deprecation")
	@Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
    	if (mPrivacyGuard ==preference ) {
            //getFragmentManager().beginTransaction().replace(android.R.id.content,
            //        new PrivacyGuardManager()).commit();
    		startActivity(new Intent(this,PrivacyGuardManagerActivity.class));
    		return true;
    	}
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    } 
      
    
}
