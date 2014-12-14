package com.zte.appopscontrol.firewall;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.zte.appopscontrol.R;
import com.zte.appopscontrol.Utils;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.Toast;

public class FireWallFragment extends ListFragment implements
		OnQueryTextListener, LoaderManager.LoaderCallbacks<List<FireWallFragment.AppEntry>> {

	// This is the Adapter being used to display the list's data.
	AppListAdapter mAdapter;

	// If non-null, this is the current filter the user has provided.
	String mCurFilter;
	
	TextView mFirewallInfo;
	
	static int wifiRes[] = {R.drawable.ic_firewall_wifi_allow, R.drawable.ic_firewall_wifi_deny};
	static int mobileRes[] = {R.drawable.ic_firewall_mobile_allow, R.drawable.ic_firewall_mobile_deny};
	private static SharedPreferences mMobilePreferences;
	private static SharedPreferences mWifiPreferences;
    private static SharedPreferences mPreferences;
    
    public static final String FIREWALL_SHARED_PREFERENCES_NAME = "firewall_control"; 
    public static final String ACTION_REFRESH_UI = "com.zte.appops.firewall_refreshui";
    public static final String EXTRA_FIREWALL = "firewall"; 
    public static final String EXTRA_ENABLE = "enable";
    
	private static Context ctx;
	private static BatchSetting mSetting;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Give some text to display if there is no data. In a real
		// application this would come from a resource.
		setEmptyText("No applications");

		// We have a menu item to show in action bar.
		setHasOptionsMenu(true);

		// Create an empty adapter we will use to display the loaded data.
		mAdapter = new AppListAdapter(getActivity());
		setListAdapter(mAdapter);

		// Start out with a progress indicator.
		setListShown(false);
		
		//get shared preference
		mMobilePreferences = getActivity().getSharedPreferences("mobile", Activity.MODE_PRIVATE);
		mWifiPreferences = getActivity().getSharedPreferences("wifi", Activity.MODE_PRIVATE);
        mPreferences = getActivity().getSharedPreferences(FIREWALL_SHARED_PREFERENCES_NAME, Activity.MODE_PRIVATE);

        mSetting = new BatchSetting();
        mSetting.which = null;
        mSetting.enable = false;
        
		ctx = getActivity();
		// Prepare the loader. Either re-connect with an existing one,
		// or start a new one.
		getLoaderManager().initLoader(0, null, this);
		
		// it is too slow here
		//MyApi.initIptables(getActivity());
		
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
 
	}

    private void refreshUi() {    	
        Intent intent = new Intent();
        intent.setAction(ACTION_REFRESH_UI);
        getActivity().sendBroadcast(intent);
    }
    
    private static boolean shouldShowSystemApps() {
        return mPreferences.getBoolean("show_system_apps", false);
    }
    
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// Place an action bar item for searching.
		MenuItem item = menu.add("Search");
		item.setIcon(android.R.drawable.ic_menu_search);
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
				| MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		SearchView sv = new SearchView(getActivity());
		sv.setOnQueryTextListener(this);
		item.setActionView(sv);
		
		inflater.inflate(R.menu.firewall_control, menu);
        menu.findItem(R.id.show_system_apps).setChecked(shouldShowSystemApps());
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {              
            case R.id.show_system_apps:
                final String prefName = "show_system_apps";
                item.setChecked(!item.isChecked());
                mPreferences.edit().putBoolean(prefName, item.isChecked()).commit();
                refreshUi();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }	
	
	@Override
	public boolean onQueryTextChange(String newText) {
		// Called when the action bar search text has changed. Since this
		// is a simple array adapter, we can just have it do the filtering.
		mCurFilter = !TextUtils.isEmpty(newText) ? newText : null;
		mAdapter.getFilter().filter(mCurFilter);
		return true;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		// Don't care about this.
		return true;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// Insert desired behavior here.
		Log.i("LoaderCustom", "Item clicked: " + id);
	}

	@Override
	public Loader<List<AppEntry>> onCreateLoader(int id, Bundle args) {
		// This is called when a new Loader needs to be created. This
		// sample only has one Loader with no arguments, so it is simple.
		return new AppListLoader(getActivity());
	}

	@Override
	public void onLoadFinished(Loader<List<AppEntry>> loader,
			List<AppEntry> data) {
		// Set the new data in the adapter.
		mAdapter.setData(data);

		// The list should now be shown.
		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}
	}

	@Override
	public void onLoaderReset(Loader<List<AppEntry>> loader) {
		// Clear the data in the adapter.
		mAdapter.setData(null);
	}
	
	private static int getWifiRes(boolean enable){
		if(enable) return wifiRes[0];
		else return wifiRes[1];
	}
	
	private static int getMobileRes(boolean enable){
		if(enable) return mobileRes[0];
		else return mobileRes[1];
	}
	
    /**
     * This class holds the per-item data in our Loader.
     */
    public static class AppEntry {
        public AppEntry(AppListLoader loader, ApplicationInfo info) {
            mLoader = loader;
            mInfo = info;
            mApkFile = new File(info.sourceDir);
        }

        public ApplicationInfo getApplicationInfo() {
            return mInfo;
        }

        public String getLabel() {
            return mLabel;
        }

        public Drawable getIcon() {
            if (mIcon == null) {
                if (mApkFile.exists()) {
                    mIcon = mInfo.loadIcon(mLoader.mPm);
                    return mIcon;
                } else {
                    mMounted = false;
                }
            } else if (!mMounted) {
                // If the app wasn't mounted but is now mounted, reload
                // its icon.
                if (mApkFile.exists()) {
                    mMounted = true;
                    mIcon = mInfo.loadIcon(mLoader.mPm);
                    return mIcon;
                }
            } else {
                return mIcon;
            }

            return mLoader.getContext().getResources().getDrawable(
                    android.R.drawable.sym_def_app_icon);
        }

        @Override public String toString() {
            return mLabel;
        }

        public boolean isSystemApp() {
        	if(mInfo != null) {
        		if((mInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0)
        			return true;
        	}
            return false;
        }
        
        void loadLabel(Context context) {
            if (mLabel == null || !mMounted) {
                if (!mApkFile.exists()) {
                    mMounted = false;
                    mLabel = mInfo.packageName;
                } else {
                    mMounted = true;
                    CharSequence label = mInfo.loadLabel(context.getPackageManager());
                    mLabel = label != null ? label.toString() : mInfo.packageName;
                }
            }
        }
        
        void loadSettings(Context context){
        	mMobileEnable = mMobilePreferences.getBoolean(mInfo.uid+"", true);
        	mWifiEnable = mWifiPreferences.getBoolean(mInfo.uid+"", true);
        }

        //boolean isSystemApp()
        
        public boolean isMobileEnable() {
			return mMobileEnable;
		}

		public void setMobileEnable(boolean MobileEnable) {
			Editor editor = mMobilePreferences.edit();
			editor.putBoolean(mInfo.uid+"", MobileEnable);
			editor.commit();
			this.mMobileEnable = MobileEnable;
		}

		public boolean isWifiEnable() {
			return mWifiEnable;
		}

		public void setWifiEnable(boolean WifiEnable) {
			Editor editor = mWifiPreferences.edit();
			editor.putBoolean(mInfo.uid+"", WifiEnable);
			editor.commit();
			this.mWifiEnable = WifiEnable;
		}

		private final AppListLoader mLoader;
        private final ApplicationInfo mInfo;
        private final File mApkFile;
        private String mLabel;
        private Drawable mIcon;
        private boolean mMounted;
        private boolean mMobileEnable;
        private boolean mWifiEnable;   
    }
    
    public static class AppListAdapter extends ArrayAdapter<AppEntry> {
        private final LayoutInflater mInflater;

        public AppListAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_2);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setData(List<AppEntry> data) {
            clear();
            if (data != null) {
                addAll(data);
            }
        }

        /**
         * Populate new items in the list.
         */
        @Override public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            final ImageButton mobileBtn;
            final ImageButton wifiBtn;

            if (convertView == null) {
                view = mInflater.inflate(R.layout.list_item_icon_text, parent, false);
            } else {
                view = convertView;
            }

            final AppEntry item = getItem(position);
            mobileBtn = (ImageButton)view.findViewById(R.id.button_mobile);
        	wifiBtn = (ImageButton)view.findViewById(R.id.button_wifi);
            
        	mobileBtn.setImageResource(getMobileRes(item.isMobileEnable()));
        	wifiBtn.setImageResource(getWifiRes(item.isWifiEnable()));
        	
            mobileBtn.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View arg0) {
					// TODO Auto-generated method stub
					item.setMobileEnable(!item.isMobileEnable());
					MyApi.modifyByUID(ctx, item.getApplicationInfo().uid, false, !item.isMobileEnable());
					mobileBtn.setImageResource(getMobileRes(item.isMobileEnable()));
				}
            	
            });
            
            wifiBtn.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View arg0) {
					// TODO Auto-generated method stub
					item.setWifiEnable(!item.isWifiEnable());
//					try {
//						testScript.testRun(ctx);
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
					MyApi.modifyByUID(ctx, item.getApplicationInfo().uid, true, !item.isWifiEnable());
					wifiBtn.setImageResource(getWifiRes(item.isWifiEnable()));
				}
            	
            });
            

            //Bitmap photo = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.arrow_down);
            
            // set a flag to display a "sys "  icon
            if( item.isSystemApp()) {
	            ImageView iv = (ImageView)view.findViewById(R.id.icon);
	            
	            LayoutParams lp = iv.getLayoutParams();
	            Rect dst = new Rect(0,0, lp.width, lp.height);
	            
	            Bitmap photo = Utils.drawableToBitmap(item.getIcon(),dst);
	            Bitmap mark = BitmapFactory.decodeResource(ctx.getResources(),  R.drawable.tag_system); 
	            
	            dst = new Rect(lp.width/3, lp.height-lp.height/3, lp.width, lp.height);
	            Bitmap newbm = Utils.createBitmap(photo, dst,mark,ctx.getString(R.string.app_system));       
	            
	            ((ImageView)view.findViewById(R.id.icon)).setImageBitmap(newbm);
            } else {
                ((ImageView)view.findViewById(R.id.icon)).setImageDrawable(item.getIcon());            	
            }            	

            ((TextView)view.findViewById(R.id.text)).setText(item.getLabel());

            return view;
        }
    }
    
    /**
     * A custom Loader that loads all of the installed applications.
     */
    public static class AppListLoader extends AsyncTaskLoader<List<AppEntry>> {
        final InterestingConfigChanges mLastConfig = new InterestingConfigChanges();
        final PackageManager mPm;

        List<AppEntry> mApps;
        PackageIntentReceiver mPackageObserver;

        public AppListLoader(Context context) {
            super(context);

            // Retrieve the package manager for later use; note we don't
            // use 'context' directly but instead the save global application
            // context returned by getContext().
            mPm = getContext().getPackageManager();
        }

        /**
         * This is where the bulk of our work is done.  This function is
         * called in a background thread and should generate a new set of
         * data to be published by the loader.
         */
        @Override public List<AppEntry> loadInBackground() {
            // Retrieve all known applications.
            List<ApplicationInfo> apps = mPm.getInstalledApplications(
                    PackageManager.GET_UNINSTALLED_PACKAGES |
                    PackageManager.GET_DISABLED_COMPONENTS);
            if (apps == null) {
                apps = new ArrayList<ApplicationInfo>();
            }

            final Context context = getContext();
            boolean showSystemApps = shouldShowSystemApps();
            // Create corresponding array of entries and load their labels.
            final List<AppEntry> entries = new ArrayList<AppEntry>(apps.size());
            for (int i=0; i<apps.size(); i++) {
            	if(apps.get(i).uid < 10000 && apps.get(i).uid != 1000)
            		continue;

	            // skip all system apps if they shall not be included
	            if (!showSystemApps && (apps.get(i).flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
	                continue;
	            }
	            
            	/*                
            	ApplicationInfo appInfo = apps.get(i);
            	try {
            		PackageInfo pkgInfo = mPm.getPackageInfo(appInfo.packageName, 0);
            		String shareUid = pkgInfo.sharedUserId;
				} catch (NameNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	*/
            	AppEntry entry = new AppEntry(this, apps.get(i));
                String name = mPm.getNameForUid(apps.get(i).uid);
                entry.loadLabel(context);
                entry.loadSettings(context);                                   
                
                entries.add(entry);
            }

            new Thread() {
    			@Override
    			public void run() {
    				Log.e("ZQL", "in run");
	                	if(mSetting.which != null) {
	                		for(AppEntry app: entries) {
	                		if( mSetting.which.equals("mobile")) {
	                			app.setMobileEnable(mSetting.enable);
	                			MyApi.modifyByUID(ctx, app.getApplicationInfo().uid, false, mSetting.enable);
	                		} else {
	                			app.setWifiEnable(mSetting.enable);
	                			MyApi.modifyByUID(ctx, app.getApplicationInfo().uid, true, mSetting.enable);
	                		}
	                	}
                	};
    			}
    		}.start();
            
            // Sort the list.
            Collections.sort(entries, ALPHA_COMPARATOR);
            //MyApi.initIptables(context);
            // Done!
            return entries;
        }

        /**
         * Called when there is new data to deliver to the client.  The
         * super class will take care of delivering it; the implementation
         * here just adds a little more logic.
         */
        @Override public void deliverResult(List<AppEntry> apps) {
            if (isReset()) {
                // An async query came in while the loader is stopped.  We
                // don't need the result.
                if (apps != null) {
                    onReleaseResources(apps);
                }
            }
            List<AppEntry> oldApps = apps;
            mApps = apps;

            if (isStarted()) {
                // If the Loader is currently started, we can immediately
                // deliver its results.
                super.deliverResult(apps);
            }

            // At this point we can release the resources associated with
            // 'oldApps' if needed; now that the new result is delivered we
            // know that it is no longer in use.
            if (oldApps != null) {
                onReleaseResources(oldApps);
            }
        }

        /**
         * Handles a request to start the Loader.
         */
        @Override protected void onStartLoading() {
            if (mApps != null) {
                // If we currently have a result available, deliver it
                // immediately.
                deliverResult(mApps);
            }

            // Start watching for changes in the app data.
            if (mPackageObserver == null) {
                mPackageObserver = new PackageIntentReceiver(this);
            }

            // Has something interesting in the configuration changed since we
            // last built the app list?
            boolean configChange = mLastConfig.applyNewConfig(getContext().getResources());

            if (takeContentChanged() || mApps == null || configChange) {
                // If the data has changed since the last time it was loaded
                // or is not currently available, start a load.
                forceLoad();
            }
        }

        /**
         * Handles a request to stop the Loader.
         */
        @Override protected void onStopLoading() {
            // Attempt to cancel the current load task if possible.
            cancelLoad();
        }

        /**
         * Handles a request to cancel a load.
         */
        @Override public void onCanceled(List<AppEntry> apps) {
            super.onCanceled(apps);

            // At this point we can release the resources associated with 'apps'
            // if needed.
            onReleaseResources(apps);
        }

        /**
         * Handles a request to completely reset the Loader.
         */
        @Override protected void onReset() {
            super.onReset();

            // Ensure the loader is stopped
            onStopLoading();

            // At this point we can release the resources associated with 'apps'
            // if needed.
            if (mApps != null) {
                onReleaseResources(mApps);
                mApps = null;
            }

            // Stop monitoring for changes.
            if (mPackageObserver != null) {
                getContext().unregisterReceiver(mPackageObserver);
                mPackageObserver = null;
            }
        }

        /**
         * Helper function to take care of releasing resources associated
         * with an actively loaded data set.
         */
        protected void onReleaseResources(List<AppEntry> apps) {
            // For a simple List<> there is nothing to do.  For something
            // like a Cursor, we would close it here.
        }
    }
    
    /**
     * Helper class to look for interesting changes to the installed apps
     * so that the loader can be updated.
     */
    public static class PackageIntentReceiver extends BroadcastReceiver {
        final AppListLoader mLoader;

        public PackageIntentReceiver(AppListLoader loader) {
            mLoader = loader;
            IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addDataScheme("package");
            mLoader.getContext().registerReceiver(this, filter);
            // Register for events related to sdcard installation.
            IntentFilter sdFilter = new IntentFilter();
            sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
            sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
            mLoader.getContext().registerReceiver(this, sdFilter);
            
            IntentFilter refreshFilter = new IntentFilter();
            refreshFilter.addAction(ACTION_REFRESH_UI);
            mLoader.getContext().registerReceiver(this, refreshFilter);
        }

        @Override public void onReceive(Context context, Intent intent) {
            // Tell the loader about the change.
        	if(intent.getAction().equals(ACTION_REFRESH_UI)) {
        		String which = intent.getStringExtra(EXTRA_FIREWALL);
        		boolean enable = intent.getBooleanExtra(EXTRA_ENABLE , false);
        		if(which!=null) {
        			mSetting.enable = enable;
        			mSetting.which = which;
        		} else
        			mSetting.which = null;
        	}
            mLoader.onContentChanged();
        }
    }
    
    /**
     * Helper for determining if the configuration has changed in an interesting
     * way so we need to rebuild the app list.
     */
    public static class InterestingConfigChanges {
        final Configuration mLastConfiguration = new Configuration();
        int mLastDensity;

        boolean applyNewConfig(Resources res) {
            int configChanges = mLastConfiguration.updateFrom(res.getConfiguration());
            boolean densityChanged = mLastDensity != res.getDisplayMetrics().densityDpi;
            if (densityChanged || (configChanges&(ActivityInfo.CONFIG_LOCALE
                    |ActivityInfo.CONFIG_UI_MODE|ActivityInfo.CONFIG_SCREEN_LAYOUT)) != 0) {
                mLastDensity = res.getDisplayMetrics().densityDpi;
                return true;
            }
            return false;
        }
    }
    
    /**
     * Perform alphabetical comparison of application entry objects.
     */
    public static final Comparator<AppEntry> ALPHA_COMPARATOR = new Comparator<AppEntry>() {
        private final Collator sCollator = Collator.getInstance();
        @Override
        public int compare(AppEntry object1, AppEntry object2) {
            return sCollator.compare(object1.getLabel(), object2.getLabel());
        }
    };
    
    public static class testScript{    	
    	public static void testRun(Context context) throws IOException{
    		int code;
    		final StringBuilder script = new StringBuilder();
    		StringBuilder res = new StringBuilder();
    		script.append("iptables -L");
    		code = MyApi.runScriptAsRoot(context, script.toString(), res);
    		String msg = res.toString();
			Log.e("ZQL", msg);
    	}
    }
    
    class BatchSetting {
    	String which;
    	boolean enable;
    }
}
