package com.zte.appopscontrol;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;

import com.zte.appopscontrol.R;
import com.zte.appopscontrol.privacyguard.PrivacySettings;

public class MainActivity extends PreferenceActivity  {

    // Preference controls.
    private PreferenceCategory 	mInlineCategory;
    private SwitchPreference 	mMtkActivityPreference;
    private SwitchPreference 	mQcAppOpsPreference;

    private PreferenceCategory 	mPrivacyGuardCategory;
    private SwitchPreference 	mPrivacyGuardPreferece;

    private PreferenceCategory 	mAppOpsCategory;
    private SwitchPreference 	mAppOpsPreferece;
    
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//addPreferencesFromResource(R.xml.preference);
		setPreferenceScreen(createPreferenceHierarchy());
	}

    private PreferenceScreen createPreferenceHierarchy() {
        // Root
        @SuppressWarnings("deprecation")
		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

        // Inline preferences
        PreferenceCategory mInlineCategory = new PreferenceCategory(this);
        mInlineCategory.setTitle(R.string.inline_preferences);
        root.addPreference(mInlineCategory);

        mMtkActivityPreference = new SwitchPreference(this);
        mMtkActivityPreference.setKey("switch_mtk_preference");
        mMtkActivityPreference.setTitle("MTK APP");
        mMtkActivityPreference.setSummary("launch mtk app demo!!");
        mInlineCategory.addPreference(mMtkActivityPreference);

        mQcAppOpsPreference = new SwitchPreference(this);
        mQcAppOpsPreference.setKey("switch_qc_preference");
        mQcAppOpsPreference.setTitle("QC APP");
        mQcAppOpsPreference.setSummary("launch qc app demo!!");
        mInlineCategory.addPreference(mQcAppOpsPreference);        

        // PrivacyGuard preferences
        PreferenceCategory mPrivacyGuardCategory = new PreferenceCategory(this);
        mPrivacyGuardCategory.setTitle("PrivacyGuard");
        root.addPreference(mPrivacyGuardCategory);

        mPrivacyGuardPreferece = new SwitchPreference(this);
        mPrivacyGuardPreferece.setKey("privacy_guard_preference");
        mPrivacyGuardPreferece.setTitle("privacy guard");
        mPrivacyGuardPreferece.setSummary("launch privacy guard!!");
        mPrivacyGuardCategory.addPreference(mPrivacyGuardPreferece);   
        
        // PrivacyGuard preferences
        PreferenceCategory mAppOpsCategory = new PreferenceCategory(this);
        mAppOpsCategory.setTitle("AppOps");
        root.addPreference(mAppOpsCategory);

        mAppOpsPreferece = new SwitchPreference(this);
        mAppOpsPreferece.setKey("app_ops__preference");
        mAppOpsPreferece.setTitle("app ops");
        mAppOpsPreferece.setSummary("launch app ops!!");
        mAppOpsCategory.addPreference(mAppOpsPreferece);  
        
        return root;
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (mMtkActivityPreference == preference) {
        	Toast.makeText(MainActivity.this, "MTK", Toast.LENGTH_SHORT);
            return true;
        } else if (mQcAppOpsPreference == preference) {
        	Toast.makeText(MainActivity.this, "QC", Toast.LENGTH_SHORT);
            return true;
        } else if (mPrivacyGuardPreferece == preference) {
        	Toast.makeText(MainActivity.this, "Privacy", Toast.LENGTH_SHORT);
        	startActivity(new Intent(MainActivity.this,PrivacySettings.class));
        	return true;
        } else if (mAppOpsPreferece == preference) {
        	Toast.makeText(MainActivity.this, "AppOps", Toast.LENGTH_SHORT);
        	startActivity(new Intent(MainActivity.this,AppOpsControlActivity.class));
        	return true;
        }   
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }    


}