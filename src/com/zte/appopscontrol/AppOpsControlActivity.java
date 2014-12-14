package com.zte.appopscontrol;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import com.zte.appopscontrol.privacyguard.PrivacyGuardManager;
import com.zte.appopscontrol.applications.AppOpsCategory;
import com.zte.appopscontrol.applications.AppOpsSummary;

/**
 * This demonstrates the use of action bar tabs and how they interact
 * with other action bar features.
 */
public class AppOpsControlActivity extends Activity implements ActionBar.TabListener {
		      
	private static final int TAB_INDEX_APPS = 0;  
	private static final int TAB_INDEX_PERMISSIONS = 1;  
	private static final int TAB_INDEX_COUNT = 2;
	
    int mCurPos;
    private ViewPager mViewPager;
    private MyPagerAdapter mAdapter;
    
    private static SharedPreferences mPreferences;
    private AppOpsManager mAppOps;

    public static final String APP_OPS_SHARED_PREFERENCES_NAME = "app_ops_control";    
    public static final String ACTION_APPOPS_REFRESH_UI = "com.zte.appops_refreshui";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		mAppOps = (AppOpsManager)this.getSystemService(Context.APP_OPS_SERVICE);

        //int flags = ActionBar.DISPLAY_SHOW_TITLE;//ActionBar.DISPLAY_SHOW_HOME;
        //int change = bar.getDisplayOptions() ^ flags;
        //bar.setDisplayOptions(change, flags);
        setContentView(R.layout.app_ops_control_activity);
        
        //setActionBar();
        //setViewPager();
        //setTabs();
        final ActionBar bar = getActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setTitle(R.string.permission_control);
        
        
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mAdapter = new MyPagerAdapter(getFragmentManager());
        mViewPager.setAdapter(mAdapter);
        mViewPager.setOnPageChangeListener(mAdapter);
        
        for (int i = 0; i < mAdapter.getCount(); ++i) {  
        	bar.addTab(bar.newTab()  
        	.setText(mAdapter.getPageTitle(i))  
        	.setTabListener(this));  
        }
        
        if (savedInstanceState != null) {
            bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
        }
        
        // get shared preference
        mPreferences = getSharedPreferences(APP_OPS_SHARED_PREFERENCES_NAME, Activity.MODE_PRIVATE);
        if (!mPreferences.getBoolean("first_help_shown", false)) {
            showHelp();
        }	        
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
    }

    private class HelpDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.privacy_guard_help_title)
                    .setMessage(R.string.privacy_guard_help_text)
                    .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            mPreferences.edit().putBoolean("first_help_shown", true).commit();
        }
    }

    private void showHelp() {
        HelpDialogFragment fragment = new HelpDialogFragment();
        fragment.show(getFragmentManager(), "help_dialog");
    }

    private class ResetDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.privacy_guard_reset_title)
                    .setMessage(R.string.privacy_guard_reset_text)
                    .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // turn off privacy guard for all apps shown in the current list
                                mAppOps.resetAllModes();
                                //mAdapter.notifyDataSetChanged();
                                refreshUi();
                        }
                    })
                    .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Do nothing
                        }
                    })
                    .create();
        }
    }

    private void showResetDialog() {
        ResetDialogFragment dialog = new ResetDialogFragment();
        dialog.show(getFragmentManager(), "reset_dialog");
    }

    private static boolean shouldShowSystemApps() {
        return mPreferences.getBoolean("show_system_apps", false);
    }
    
    private void refreshUi() {    	
        Intent intent = new Intent();
        intent.setAction(AppOpsControlActivity.ACTION_APPOPS_REFRESH_UI);
        sendBroadcast(intent);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.app_fragment_menu, menu);
        menu.findItem(R.id.show_system_apps).setChecked(shouldShowSystemApps());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.help:
                showHelp();
                return true;
            case R.id.reset:
            	showResetDialog();
                return true;                
            case R.id.show_system_apps:
                final String prefName = "show_system_apps";
                // set the menu checkbox and save it in
                // shared preference and rebuild the list
                item.setChecked(!item.isChecked());
                mPreferences.edit().putBoolean(prefName, item.isChecked()).commit();
                // zte test ..... we should broadcast the change intent to reload
                refreshUi();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
    
    class MyPagerAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener {

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
        	switch(position) {
        	case TAB_INDEX_APPS:
        		return new AppFragment();
        	case TAB_INDEX_PERMISSIONS:
        		return new PermissionFragment();         		
        	}
        	throw new IllegalStateException("No fragment at position " + position);  
        }

        @Override
        public int getCount() {
            return TAB_INDEX_COUNT;
        }

        @Override
        public CharSequence getPageTitle(int position) {
        	switch(position) {
        	case TAB_INDEX_APPS:
        		return getString(R.string.apps_tab);
        	case TAB_INDEX_PERMISSIONS:
        		return getString(R.string.permissions_tab);         		
        	}
        	throw new IllegalStateException("No fragment at position " + position);  
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            mCurPos = position;
            
            //update actionbar state
            final ActionBar actionBar = getActionBar();  
            actionBar.setSelectedNavigationItem(position); 
        }

        public int getCurrentPage() {
            return mCurPos;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == ViewPager.SCROLL_STATE_IDLE) {
                //updateCurrentTab(mCurPos);
            }
        }
    }
    
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
    	// notify view pager to change fragment
    	mViewPager.setCurrentItem(tab.getPosition());
    }

    public void onTabUnselected(Tab tab, FragmentTransaction ft) {

    }

    public void onTabReselected(Tab tab, FragmentTransaction ft) {
        //Toast.makeText(this, "Reselected!", Toast.LENGTH_SHORT).show();
    }    
    
}
