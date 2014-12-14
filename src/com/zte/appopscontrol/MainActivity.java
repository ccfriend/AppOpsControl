package com.zte.appopscontrol;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;

import com.zte.appopscontrol.R;
import com.zte.appopscontrol.firewall.FireWallActivity;
import com.zte.appopscontrol.firewall.MyApi;
import com.zte.appopscontrol.privacyguard.PrivacySettings;

public class MainActivity extends PreferenceActivity  {

    // Preference controls.
    private PreferenceCategory 	mInlineCategory;
    private SwitchPreference 	mMtkActivityPreference;
    private SwitchPreference 	mQcAppOpsPreference;

    private PreferenceCategory 	mPrivacyGuardCategory;
    private SwitchPreference 	mPrivacyGuardPreferece;

    private PreferenceCategory 	mAppOpsCategory;
    private MySwitchPreference 	mAppOpsPreferece;

    private PreferenceCategory 	mFireWallCategory;
    private MySwitchPreference 	mFireWallPreferece;
    
    private boolean mAppOpsEnabled;
    private boolean mFireWallEnabled;
    final public Context ctx = this;
    AppOpsManager mAppOps;
        
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAppOps = (AppOpsManager)this.getSystemService(Context.APP_OPS_SERVICE);
		//addPreferencesFromResource(R.xml.preference);
		setPreferenceScreen(createPreferenceHierarchy());
//		mEnabled = mAppOps.getAppOpsEnable();
//		if(mAppOpsPreferece.getSharedPreferences().getBoolean("app_ops_preference", false) != mEnabled){
//			SharedPreferences.Editor editor = mAppOpsPreferece.getSharedPreferences().edit();
//			editor.putBoolean("app_ops_preference", mEnabled);
//			editor.commit();
//			mAppOpsPreferece.setChecked(mEnabled);
//		}
	}
	

	protected void onResume(){
		super.onResume();	
		mAppOpsEnabled = mAppOps.getAppOpsEnable();
		if(mAppOpsPreferece.getSharedPreferences().getBoolean("app_ops_preference", false) != mAppOpsEnabled){
			SharedPreferences.Editor editor = mAppOpsPreferece.getSharedPreferences().edit();
			editor.putBoolean("app_ops_preference", mAppOpsEnabled);
			editor.commit();
			mAppOpsPreferece.setChecked(mAppOpsEnabled);
		}
		
		mFireWallEnabled = mFireWallPreferece.getSharedPreferences().getBoolean("fire_wall_preference", false);
		mFireWallPreferece.setChecked(mFireWallEnabled);

		return;
	}

    private PreferenceScreen createPreferenceHierarchy() {
        // Root
        @SuppressWarnings("deprecation")
		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
        // Inline preferences

        // PrivacyGuard preferences
//        PreferenceCategory mInlineCategory = new PreferenceCategory(this);
//        mInlineCategory.setTitle(R.string.inline_preferences);
//        root.addPreference(mInlineCategory);
//
//        mMtkActivityPreference = new SwitchPreference(this);
//        mMtkActivityPreference.setKey("switch_mtk_preference");
//        mMtkActivityPreference.setTitle("MTK APP");
//        mMtkActivityPreference.setSummary("launch mtk app demo!!");
//        mInlineCategory.addPreference(mMtkActivityPreference);
//
//        mQcAppOpsPreference = new SwitchPreference(this);
//        mQcAppOpsPreference.setKey("switch_qc_preference");
//        mQcAppOpsPreference.setTitle("QC APP");
//        mQcAppOpsPreference.setSummary("launch qc app demo!!");
//        mInlineCategory.addPreference(mQcAppOpsPreference);        
//
//        // PrivacyGuard preferences
//        PreferenceCategory mPrivacyGuardCategory = new PreferenceCategory(this);
//        mPrivacyGuardCategory.setTitle("PrivacyGuard");
//        root.addPreference(mPrivacyGuardCategory);
//
//        mPrivacyGuardPreferece = new SwitchPreference(this);
//        mPrivacyGuardPreferece.setKey("privacy_guard_preference");
//        mPrivacyGuardPreferece.setTitle("privacy guard");
//        mPrivacyGuardPreferece.setSummary("launch privacy guard!!");
//        mPrivacyGuardCategory.addPreference(mPrivacyGuardPreferece);   
//        
//        // PrivacyGuard preferences

        PreferenceCategory mAppOpsCategory = new PreferenceCategory(this);
        mAppOpsCategory.setTitle("AppOps");
        root.addPreference(mAppOpsCategory);

        mAppOpsPreferece = new MySwitchPreference(this);
        mAppOpsPreferece.setKey("app_ops_preference");
        mAppOpsPreferece.setTitle(R.string.permission_control);
        
        //mAppOpsPreferece.setSummary("launch app ops!!");
        mAppOpsPreferece.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				// TODO Auto-generated method stub
				Boolean bEnabled = (Boolean)newValue;
				if(bEnabled){
					showAlert(R.string.appops_alert_text);
				} else{
					mAppOps.setAppOpsEnable(bEnabled);
					mAppOpsEnabled = bEnabled;
				}
				return true;
			}
        	
        });
        mAppOpsCategory.addPreference(mAppOpsPreferece);  
        
        ///////////////////////////////////////
        
        PreferenceCategory mFireWallCategory = new PreferenceCategory(this);
        mFireWallCategory.setTitle("Firewall");
        root.addPreference(mFireWallCategory);
        
        mFireWallPreferece = new MySwitchPreference(this);
        mFireWallPreferece.setTitle(R.string.firewall);
        mFireWallPreferece.setKey("fire_wall_preference");
        //mFireWallPreferece.setSummary("launch fire wall control!!");
        mFireWallPreferece.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				// TODO Auto-generated method stub
				Boolean bEnabled = (Boolean)newValue;
				if(bEnabled){
					showAlert(R.string.firewall_alert_text);
				} else{
					// below is to init iptables
			        final Handler toaster = new Handler() {
			    		public void handleMessage(Message msg) {
			    			if (msg.arg1 != 0) Toast.makeText(ctx, msg.arg1, Toast.LENGTH_SHORT).show();
			    		}
			    	};
			        new Thread() {
						@Override
						public void run() {
							Log.e("ZQL", "in run");
							if (!com.zte.appopscontrol.firewall.MyApi.cleanIptables(ctx)) {
								// Error enabling firewall on boot
			        			final Message msg = new Message();
			        			msg.arg1 = R.string.toast_error_enabling;
			        			toaster.sendMessage(msg);
								//Api.setEnabled(context, false);
							}
						}
					}.start();
					mFireWallEnabled = bEnabled;
				}
				return true;
			}
        	
        });
        mFireWallCategory.addPreference(mFireWallPreferece); 
        
        return root;
    }
    private class AlertDialogFragment extends DialogFragment {
    	private int mMessageId;
        public AlertDialogFragment(int resId) {
			// TODO Auto-generated constructor stub
        	mMessageId = resId;
		}

		@Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.main_alert_title)
                    .setMessage(mMessageId)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        	if(mMessageId == R.string.appops_alert_text){
	                        	mAppOps.setAppOpsEnable(true);
	                        	mAppOpsEnabled = true;
                        	} else if(mMessageId == R.string.firewall_alert_text){
                        		final Handler toaster = new Handler() {
                            		public void handleMessage(Message msg) {
                            			if (msg.arg1 != 0) Toast.makeText(ctx, msg.arg1, Toast.LENGTH_SHORT).show();
                            		}
                            	};
                                new Thread() {
                        			@Override
                        			public void run() {
                        				Log.e("ZQL", "in run");
                        				if (!MyApi.initIptables(ctx)) {
                        					// Error enabling firewall on boot
                                			final Message msg = new Message();
                                			msg.arg1 = R.string.toast_error_enabling;
                                			toaster.sendMessage(msg);
                        					//Api.setEnabled(context, false);
                        				}
                        				MyApi.applySavedIptablesRules(ctx, true);
                        			}
                        		}.start();
                        		mFireWallEnabled = true;
                        	}
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        	if(mMessageId == R.string.appops_alert_text){
                        		mAppOpsPreferece.setChecked(false);
                        	} else if(mMessageId == R.string.firewall_alert_text){
                        		mFireWallPreferece.setChecked(false);
                        	}
                        }
                    })
                    .create();
        }

    }

    private void showAlert(int resId) {
    	AlertDialogFragment fragment = new AlertDialogFragment(resId);
        fragment.show(getFragmentManager(), "alert_dialog");
    }
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (mMtkActivityPreference == preference) {
        	Toast.makeText(MainActivity.this, "MTK", Toast.LENGTH_SHORT).show();
            return true;
        } else if (mQcAppOpsPreference == preference) {
        	Toast.makeText(MainActivity.this, "QC", Toast.LENGTH_SHORT).show();
            return true;
        } else if (mPrivacyGuardPreferece == preference) {
        	Toast.makeText(MainActivity.this, "Privacy", Toast.LENGTH_SHORT).show();
        	startActivity(new Intent(MainActivity.this,PrivacySettings.class));
        	return true;
        } else if (mAppOpsPreferece == preference) {
        	if(!mAppOpsEnabled){
        		Toast.makeText(MainActivity.this, "Your should enable AppOps first", Toast.LENGTH_SHORT).show();
        		return true;
        	}
        	Toast.makeText(MainActivity.this, "AppOps", Toast.LENGTH_SHORT).show();
        	startActivity(new Intent(MainActivity.this,AppOpsControlActivity.class));
        	return true;
        } else if (mFireWallPreferece == preference) {
        	if(!mFireWallEnabled){
        		Toast.makeText(MainActivity.this, "Your should enable Fire Wall first", Toast.LENGTH_SHORT).show();
        		return true;
        	}
        	Toast.makeText(MainActivity.this, "FireWall", Toast.LENGTH_SHORT).show();
        	startActivity(new Intent(MainActivity.this, FireWallActivity.class));
        }   
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }    


}