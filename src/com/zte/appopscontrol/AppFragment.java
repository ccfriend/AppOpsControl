package com.zte.appopscontrol;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.zte.appopscontrol.AppOpsState2.OpsTemplate;
import com.zte.appopscontrol.AppOpsUtils.AppInfo;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.Fragment;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Demonstration of the implementation of a custom Loader.
 */
public class AppFragment extends Fragment implements  OnItemClickListener,
	LoaderManager.LoaderCallbacks<List<AppInfo>> {

	
	// This is the Adapter being used to display the list's data.
	private AppListAdapter mAdapter;	
	private String mCurrentPkgName;
    private ListView mAppsList;
    
    private LayoutInflater mInflater;
    private static TextView mHeaderView;
    private View mContentView;
    private LinearLayout mProgressContainer;
    TextView mEmptyView;
    
	// If non-null, this is the current filter the user has provided.
    private String mCurFilter;
    
    private static SharedPreferences mPreferences;
    private PackageManager mPm;
    private static Activity mActivity;
    private static AppOpsManager mAppOps;
    private static AppOpsState2 mState;
    
	public AppFragment() {
	}
	
    /**
     * Perform alphabetical comparison of application entry objects.
     */
    public static final Comparator<AppInfo> ALPHA_COMPARATOR = new Comparator<AppInfo>() {
        private final Collator sCollator = Collator.getInstance();
        @Override
        public int compare(AppInfo object1, AppInfo object2) {
            return sCollator.compare(object1.getLabel(), object2.getLabel());
        }
    };
    
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
	        
	        // register for refresh ui event (reload sys app or not)
	        IntentFilter refreshFilter = new IntentFilter();
	        refreshFilter.addAction(AppOpsControlActivity.ACTION_APPOPS_REFRESH_UI);
	        mLoader.getContext().registerReceiver(this, refreshFilter);
	        
	    }
	
	    @Override public void onReceive(Context context, Intent intent) {
	        // Tell the loader about the change.
	        mLoader.onContentChanged();
	    }
	}
	
	public static int getPermCounts(OpsTemplate tpl, int uid, String packageName) {
		
		final PackageManager mPm = mActivity.getPackageManager();
		//get ops pkgs 
        List<AppOpsManager.PackageOps> pkgs = new ArrayList<AppOpsManager.PackageOps>();
        if (packageName != null) {
            pkgs = mAppOps.getOpsForPackage(uid, packageName, tpl.ops);
        }
        
        //if has pkg ops, it means we have this tpl's permission
        if( pkgs != null )
        	return 1;
        
        //if no pkg, then we need to check the AnroidManifest.xml using PackageManager
        PackageInfo appInfo = new PackageInfo();
        if (packageName != null) {            
            try {
            	appInfo = mPm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);                
            } catch (NameNotFoundException e) {
            }
        }
        
        for (int i=0; i<tpl.ops.length; i++) {
            if (tpl.showPerms[i]) {
                String perm = AppOpsManager.opToPermission(tpl.ops[i]);
                if (perm != null) {
                    if (appInfo.requestedPermissions != null) {
                        for (int j=0; j<appInfo.requestedPermissions.length; j++) {
                            if (perm.equals(appInfo.requestedPermissions[j])) {
                                return 1;	// found permissions in this tpl
                            }                        
                        }
                    }
                }
            }
        }
        
		return 0;
	}
	
	/**
	 * A custom Loader that loads all of the installed applications.
	 */
	public static class AppListLoader extends AsyncTaskLoader<List<AppInfo>> {
	    final InterestingConfigChanges mLastConfig = new InterestingConfigChanges();
	    private PackageManager mPm;
	
	    List<AppInfo> mApps;
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
	    @Override 
	    public List<AppInfo> loadInBackground() {
            // Retrieve all known applications.
            List<PackageInfo> packages = mPm.getInstalledPackages(
                    PackageManager.GET_PERMISSIONS | 
                    PackageManager.GET_SIGNATURES);
            if (packages == null) {
            	packages = new ArrayList<PackageInfo>();
            }
			boolean showSystemApps = shouldShowSystemApps();
			Signature platformCert;
			
			try {
				PackageInfo sysInfo = mPm.getPackageInfo("android", PackageManager.GET_SIGNATURES);
				platformCert = sysInfo.signatures[0];
			} catch (PackageManager.NameNotFoundException e) {
				platformCert = null;
			}

            final Context context = getContext();
            int pkgPermCounts = 0;
            long time0, time1;
            time0 = System.currentTimeMillis();
            
            // Create corresponding array of entries and load their labels.
            List<AppInfo> entries = new ArrayList<AppInfo>();
	        for (PackageInfo info : packages) {
	            final ApplicationInfo appInfo = info.applicationInfo;
	            pkgPermCounts = 0;
	            // hide apps signed with the platform certificate to avoid the user
	            // shooting himself in the foot
	            if (platformCert != null && info.signatures != null
	                    && platformCert.equals(info.signatures[0])) {
	                continue;
	            }

	            // skip all system apps if they shall not be included
	            if (!showSystemApps && (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
	                continue;
	            }

	            
	            for(int index = 0; index < AppOpsState2.ALL_TEMPLATES.length; index ++){
	            	final AppOpsState2.OpsTemplate tpl = AppOpsState2.ALL_TEMPLATES[index];
//	                List<AppOpsState2.AppOpEntry> appOpEntries = mState.buildState(tpl,
//	                        appInfo.uid, info.packageName);
//	                testCounts += appOpEntries.size();
	            	
	            	pkgPermCounts += getPermCounts(tpl, appInfo.uid, info.packageName);
	            }
	            
	            //temp solution:  not display the zero perm apps ....
	            if(pkgPermCounts == 0)
	            	continue;
	            
                AppInfo entry = new AppInfo(getContext(), appInfo,pkgPermCounts);
                entry.loadLabel(context);
                entries.add(entry);

	        }			

	        time1 = System.currentTimeMillis();
	        
	        //Toast.makeText(mActivity, String.valueOf(time1-time0), Toast.LENGTH_SHORT).show();
	        Log.e("zteappops","delta time: " + (time1-time0));
	        
            // Sort the list.
            Collections.sort(entries, ALPHA_COMPARATOR);

            // Done!
            return entries;
	    }
	
	    /**
	     * Called when there is new data to deliver to the client.  The
	     * super class will take care of delivering it; the implementation
	     * here just adds a little more logic.
	     */
	    @Override public void deliverResult(List<AppInfo> apps) {
	        if (isReset()) {
	            // An async query came in while the loader is stopped.  We
	            // don't need the result.
	            if (apps != null) {
	                onReleaseResources(apps);
	            }
	        }
	        List<AppInfo> oldApps = apps;
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
	        // We don't monitor changed when loading is stopped, so need
	        // to always reload at this point.
	        onContentChanged();
	
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
	    @Override public void onCanceled(List<AppInfo> apps) {
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
	    protected void onReleaseResources(List<AppInfo> apps) {
	        // For a simple List<> there is nothing to do.  For something
	        // like a Cursor, we would close it here.
	    }
	}
	
	public static class AppListAdapter extends BaseAdapter implements SectionIndexer {
	    private final Resources mResources;
	    private final LayoutInflater mInflater;
    	private String[] mSections;
    	private int[] mPositions;
	
	    List<AppInfo> mList;
	
	    public AppListAdapter(Context context) {
	        mResources = context.getResources();
	        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    }
	
	    public void setData(List<AppInfo> data,List<String> sections, List<Integer> positions) {
			
	        mList = data;
			mSections = sections.toArray(new String[sections.size()]);
	        mPositions = new int[positions.size()];
	        for (int i = 0; i < positions.size(); i++) {
	            mPositions[i] = positions.get(i);
	        }
	        notifyDataSetChanged();
	    }
	
	    @Override
	    public int getCount() {
	        return mList != null ? mList.size() : 0;
	    }
	
	    @Override
	    public AppInfo getItem(int position) {
	        return mList.get(position);
	    }
	
	    @Override
	    public long getItemId(int position) {
	        return position;
	    }
	    
	    public int getSysAppCount() {
	    	int counts = getCount();
	    	int sysCounts = 0;
	    	
	    	if ( counts <= 0 )
	    		return 0;
	    	
	    	for(int i=0 ; i<counts; i++) {
	    		if( (mList.get(i).getApplicationInfo().flags & ApplicationInfo.FLAG_SYSTEM) != 0)
	    			sysCounts++;
	    	}
	    	return sysCounts;
	    }
	
	    /**
	     * Populate new items in the list.
	     */
	    @Override public View getView(int position, View convertView, ViewGroup parent) {
	        View view;
	
	        if( mHeaderView != null) {
				int sysAppCount = getSysAppCount();
	            String sysCountUnit = sysAppCount > 1 ? mActivity.getString(R.string.app_count_unit_plural)
	                    : mActivity.getString(R.string.app_count_unit_single);
	            String userCountUnit = (getCount()-sysAppCount) > 1 ? mActivity.getString(R.string.app_count_unit_plural)
	                    : mActivity.getString(R.string.app_count_unit_single);
	            String appCountStr = mActivity.getString(R.string.app_system) + " " +sysCountUnit +": " + String.valueOf(sysAppCount) 
	            		 + "    " + mActivity.getString(R.string.app_user) + " " + userCountUnit + ": " + String.valueOf(getCount()-sysAppCount);        
	            mHeaderView.setText(appCountStr);	        	
	        }
	        
	        if (convertView == null) {
	            view = mInflater.inflate(R.layout.app_list_item, parent, false);
	        } else {
	            view = convertView;
	        }
	
	        AppInfo item = getItem(position);
	        ((ImageView)view.findViewById(R.id.app_icon)).setImageDrawable(
	                item.getIcon());
	        ((TextView)view.findViewById(R.id.app_name)).setText(item.getLabel());
	        
	        String strPerms =  String.valueOf(item.getPermCounts()) + " "
	        			+ mActivity.getString(R.string.perm_count_unit_plural);
	        
	        ((TextView)view.findViewById(R.id.app_perm_details)).setText(strPerms);
	        return view;
	    }

	    @Override
	    public int getPositionForSection(int section) {
	        if (section < 0 || section >= mSections.length) {
	            return -1;
	        }

	        return mPositions[section];
	    }

	    @Override
	    public int getSectionForPosition(int position) {
	        if (position < 0 || position >= getCount()) {
	            return -1;
	        }

	        int index = Arrays.binarySearch(mPositions, position);

	        /*
	         * Consider this example: section positions are 0, 3, 5; the supplied
	         * position is 4. The section corresponding to position 4 starts at
	         * position 3, so the expected return value is 1. Binary search will not
	         * find 4 in the array and thus will return -insertPosition-1, i.e. -3.
	         * To get from that number to the expected value of 1 we need to negate
	         * and subtract 2.
	         */
	        return index >= 0 ? index : -index - 2;
	    }

	    @Override
	    public Object[] getSections() {
	        return mSections;
	    }
			
	}
	
    private static boolean shouldShowSystemApps() {
        return mPreferences.getBoolean("show_system_apps", false);
    }

    private void setListShown(boolean shown) {

        if (shown) {
        	mHeaderView.setVisibility(View.VISIBLE);
        	mAppsList.setVisibility(View.VISIBLE);
            mProgressContainer.setVisibility(View.GONE);
        } else {
        	mEmptyView.setVisibility(View.GONE);
        	mHeaderView.setVisibility(View.GONE);
        	mAppsList.setVisibility(View.GONE);
        	mProgressContainer.setVisibility(View.VISIBLE);
        
        }
    }	    

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);	   
        mState = new AppOpsState2(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.app_list_fragment, null);
        mProgressContainer = (LinearLayout) mContentView.findViewById(R.id.progressContainer);        
        mHeaderView = (TextView) mContentView.findViewById(R.id.header);
        mEmptyView = (TextView)mContentView.findViewById(android.R.id.empty);
        mEmptyView.setText("No Apps!");
        
        ListView lv = (ListView) mContentView.findViewById(android.R.id.list);
        if (mEmptyView != null) {
            lv.setEmptyView(mEmptyView);
        }
        lv.setOnItemClickListener(this);
        lv.setSaveEnabled(true);
        lv.setItemsCanFocus(true);
        lv.setTextFilterEnabled(true);
        lv.setFastScrollEnabled(true);
        mAppsList = lv;

        return mContentView;
	}	
	
    @Override
    public void onResume() {
        super.onResume();
        //load();
        onRefreshUi();
    }
    
    
    public void onRefreshUi() {
    	this.getLoaderManager().getLoader(0).onContentChanged();
    }
    
	@Override public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);

        mActivity = getActivity();
        mPm = mActivity.getPackageManager();
        mAppOps = (AppOpsManager)getActivity().getSystemService(Context.APP_OPS_SERVICE);
	    
	    //get shared preference
        mPreferences = mActivity.getSharedPreferences(AppOpsControlActivity.APP_OPS_SHARED_PREFERENCES_NAME, 
        		Activity.MODE_PRIVATE);	    
	    
	    // Create an empty adapter we will use to display the loaded data.
	    mAdapter = new AppListAdapter(getActivity());
	    mAppsList.setAdapter(mAdapter);
	    
	    // Start out with a progress indicator.
	    setListShown(false);
	    
	    // Prepare the loader.
	    getLoaderManager().initLoader(0, null, this);
	}
	
	// utility method used to start sub activity
	private void startApplicationDetailsActivity() {
	    // start new fragment to display extended information
	    Bundle args = new Bundle();
	    args.putString(AppOpsDetailsActivity.ARG_PACKAGE_NAME, mCurrentPkgName);
	
        Intent intent = new Intent();
        intent.putExtras(args);
        intent.setClass(getActivity(), AppOpsDetailsActivity.class);
        startActivity(intent);	    
	}
		
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
	    AppInfo entry = mAdapter.getItem(position);
	    if (entry != null) {
	        mCurrentPkgName = entry.getApplicationInfo().packageName;
	        startApplicationDetailsActivity();
	    }
    }
	
	@Override
	public Loader<List<AppInfo>> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader with no arguments, so it is simple.
	    return new AppListLoader(getActivity());
	}

	// helper function to set data
	private void setApdaterData(List<AppInfo> data){
		List<AppInfo> apps = data;
        String lastSectionIndex = null;
        ArrayList<String> sections = new ArrayList<String>();
        ArrayList<Integer> positions = new ArrayList<Integer>();
        int count = apps==null ? 0 : apps.size(), offset = 0;

        for (int i = 0; i < count; i++) {
            AppInfo app = apps.get(i);
            String sectionIndex;

			if (app.getLabel().isEmpty()) {
                sectionIndex = "";
            } else {
                sectionIndex = app.getLabel().substring(0, 1).toUpperCase();
            }
            if (lastSectionIndex == null) {
                lastSectionIndex = sectionIndex;
            }

            if (!TextUtils.equals(sectionIndex, lastSectionIndex)) {
                sections.add(sectionIndex);
                positions.add(offset);
                lastSectionIndex = sectionIndex;
            }
            offset++;
        }

		mAdapter.setData(data,sections,positions);
	}

	
	@Override
	public void onLoadFinished(Loader<List<AppInfo>> loader, List<AppInfo> data) {
	    // Set the new data in the adapter.
	    //mAdapter.setData(data);
		setApdaterData(data);
		
	    setListShown(true);
	}
	
	@Override
	public void onLoaderReset(Loader<List<AppInfo>> loader) {
	    // Clear the data in the adapter.
	    //mAdapter.setData(null);
		setApdaterData(null);
	}
	    
}
