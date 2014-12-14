package com.zte.appopscontrol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.zte.appopscontrol.AppFragment.AppListAdapter;
import com.zte.appopscontrol.AppFragment.AppListLoader;
import com.zte.appopscontrol.AppFragment.PackageIntentReceiver;
import com.zte.appopscontrol.AppOpsState2.OpsTemplate;
import com.zte.appopscontrol.AppOpsUtils.AppInfo;
import com.zte.appopscontrol.AppOpsUtils.PermInfo;
import com.zte.appopscontrol.applications.AppOpsState;
import com.zte.appopscontrol.privacyguard.PrivacyGuardManager;


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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SectionIndexer;
import android.widget.TextView;

public class PermissionFragment extends Fragment implements OnItemClickListener,
		LoaderManager.LoaderCallbacks<List<PermInfo>> {

	private String mCurrentPkgName;
    private ListView mListView;
    private View mContentView;
    private LinearLayout mProgressContainer;
    private PackageManager mPm; 
    private static Activity mActivity;
    private AppOpsManager mAppOps;
    private AppOpsState mState;
    private static List<AppOpsState2> mStates = new ArrayList<AppOpsState2>();

    private PermListAdapter mAdapter; 
    private PermListLoader mAsyncTask;
    
	public PermissionFragment() {
		
	}

	public class PermListAdapter extends BaseAdapter {

	    private LayoutInflater mInflater;
	    private Context mContext;

        private List<PermInfo> mPermList = new ArrayList<PermInfo>();

        public PermListAdapter(Context context) {
        	mContext = context;
        	mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        
        public void setDataAndNotify(List<PermInfo> permList) {
            mPermList = permList;
            notifyDataSetChanged();
        }

	    @Override
	    public int getCount() {
	        return mPermList.size();
	    }

	    @Override
	    public Object getItem(int position) {
	        return mPermList.get(position);
	    }

	    @Override
	    public long getItemId(int position) {
	        return position;
	    }

	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	        AppViewHolder appHolder;

	        if (convertView == null) {
	            convertView = mInflater.inflate(R.layout.perm_fragment_list_item, null);

	            // creates a ViewHolder and children references
	            appHolder = new AppViewHolder();
	            appHolder.mPermLabel = (TextView) convertView.findViewById(R.id.perm_label);
	            appHolder.mPermIcon = (ImageView) convertView.findViewById(R.id.perm_icon);
	            appHolder.mAppCount = (TextView) convertView.findViewById(R.id.perm_count);
	            convertView.setTag(appHolder); 
	        } else {
	            appHolder = (AppViewHolder) convertView.getTag();
	        }

            // Bind the data efficiently with the holder
            PermInfo permInfo = mPermList.get(position);

            appHolder.mPermLabel.setText(permInfo.getLabel());
            appHolder.mPermIcon.setImageDrawable(permInfo.getIcon());
            
            int appCount = permInfo.getAppCount();
            int sysAppCount = permInfo.getSysAppCount();
            String sysCountUnit = sysAppCount > 1 ? getString(R.string.app_count_unit_plural)
                    : getString(R.string.app_count_unit_single);
            String userCountUnit = (appCount-sysAppCount) > 1 ? getString(R.string.app_count_unit_plural)
                    : getString(R.string.app_count_unit_single);
            String appCountStr = getString(R.string.app_system) + " " +sysCountUnit +": " + String.valueOf(sysAppCount) 
            		 + "    " + getString(R.string.app_user) + " " + userCountUnit + ": " + String.valueOf(appCount-sysAppCount);        
			
            appHolder.mAppCount.setText(appCountStr);	        
	        return convertView;
	    }
	}

	/**
	 * Helper class to look for interesting changes to the installed apps
	 * so that the loader can be updated.
	 */
	public static class PackageIntentReceiver extends BroadcastReceiver {
	    final PermListLoader mLoader;
	
	    public PackageIntentReceiver(PermListLoader loader) {
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
    /**
     * An asynchronous task to load .
     */
	public static class PermListLoader extends AsyncTaskLoader<List<PermInfo>> {

	    List<PermInfo> mPerms;
	    PackageIntentReceiver mPackageObserver;
	    
	    public PermListLoader(Context context) {
	        super(context);     
	    }
	    
        @Override
		public List<PermInfo> loadInBackground() {
        	List<PermInfo> permList = new ArrayList<PermInfo>();
        	for(int index = 0; index < AppOpsState2.ALL_TEMPLATES.length; index ++){
        		AppOpsState2 state = mStates.get(index);
        		state.buildState(AppOpsState2.ALL_TEMPLATES[index]);
        		String perm = AppOpsManager.opToPermission(AppOpsState2.ALL_TEMPLATES[index].ops[0]);
        		Drawable drawIcon = mActivity.getResources().getDrawable(AppOpsState2.TEMPLATES_ICON[index]);
        		/*String lastPermGroup = "";
        		Drawable drawIcon = getActivity().getResources().getDrawable(
                        android.R.drawable.sym_def_app_icon);
        		if (perm != null) {
                    try {
                        PermissionInfo pi = mPm.getPermissionInfo(perm, 0);
                        if (pi.group != null && !lastPermGroup.equals(pi.group)) {
                            lastPermGroup = pi.group;
                            PermissionGroupInfo pgi = mPm.getPermissionGroupInfo(pi.group, 0);
                            if (pgi.icon != 0) {
                            	drawIcon = pgi.loadIcon(mPm);
                            }
                        }
                    } catch (NameNotFoundException e) {
                    }
                }*/
        		String label = mActivity.getResources().getString(AppOpsState2.ALL_TEMPLATES[index].resId);
        		PermInfo permItem = new PermInfo(mActivity,label,drawIcon);
        		permItem.setmApps(state.mApps);
        		permItem.setAppCount(state.mApps!=null ? state.mApps.size():0);
        		int sysCount = 0;
        		for(int i=0; i<state.mApps.size(); i++) {
        			if( (state.mApps.get(i).getAppEntry().getApplicationInfo().flags 
        					& ApplicationInfo.FLAG_SYSTEM) != 0) {
        				sysCount++;
        			}
        		}
        		permItem.setSysAppCount(sysCount);
        		permList.add(permItem);
        	}
        	
        	//caculate 
        	
/*        	// get the origin data
        	CharSequence[] originPermArrary = getActivity().getResources().getTextArray(R.array.app_ops_labels);

            // encrypt the data to get view quickly
            List<PermInfo> permList = new ArrayList<PermInfo>();
            List<String> strList = new ArrayList<String>();
            Drawable drawIcon = getActivity().getResources().getDrawable(
                    android.R.drawable.sym_def_app_icon);
            for (int index=0; index < originPermArrary.length; index++) {
            	String str = originPermArrary[index].toString();
            	if(strList.contains(str))continue;
            	else strList.add(str);
            	String lastPermGroup = "";
            	
            	//get each permission (manifest scale)
                String perm = AppOpsManager.opToPermission(index);
                if (perm != null) {
                    try {
                        PermissionInfo pi = mPm.getPermissionInfo(perm, 0);
                        if (pi.group != null && !lastPermGroup.equals(pi.group)) {
                            lastPermGroup = pi.group;
                            PermissionGroupInfo pgi = mPm.getPermissionGroupInfo(pi.group, 0);
                            if (pgi.icon != 0) {
                            	drawIcon = pgi.loadIcon(mPm);
                            }
                        }
                    } catch (NameNotFoundException e) {
                    }
                }
                
                PermInfo permItem = new PermInfo(getActivity(),str,drawIcon);
                permItem.setAppCount(1111);
                
                permList.add(permItem);	            	
            }
            */
            return permList;
        }

	    /**
	     * Called when there is new data to deliver to the client.  The
	     * super class will take care of delivering it; the implementation
	     * here just adds a little more logic.
	     */
	    @Override public void deliverResult(List<PermInfo> perms) {

	        List<PermInfo> oldApps = perms;
	        mPerms = perms;
	
	        if (isStarted()) {
	            // If the Loader is currently started, we can immediately
	            // deliver its results.
	            super.deliverResult(perms);
	        }
	
	    }        

	    /**
	     * Handles a request to start the Loader.
	     */
	    @Override protected void onStartLoading() {
	        // We don't monitor changed when loading is stopped, so need
	        // to always reload at this point.
	        onContentChanged();
	
	        if (mPerms != null) {
	            // If we currently have a result available, deliver it
	            // immediately.
	            deliverResult(mPerms);
	        }

	        // Start watching for changes in the app data.
	        if (mPackageObserver == null) {
	            mPackageObserver = new PackageIntentReceiver(this);
	        }
	        
	        if (takeContentChanged() || mPerms == null) {
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
    }

    private void setListShown(boolean shown) {

        if (shown) {
            mListView.setVisibility(View.VISIBLE);
            mProgressContainer.setVisibility(View.GONE);
        } else {
        	mListView.setVisibility(View.GONE);
        	mProgressContainer.setVisibility(View.VISIBLE);
        
        }
    }	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.perm_fragment, null);
        mProgressContainer = (LinearLayout) mContentView.findViewById(R.id.progressContainer);
        //TextView emptyView = (TextView)mContentView.findViewById(com.android.internal.R.id.empty);
        //emptyView.setText("No Permissions!");
        ListView lv = (ListView) mContentView.findViewById(android.R.id.list);
        //if (emptyView != null) {
        //    lv.setEmptyView(emptyView);
        //}
        lv.setOnItemClickListener(this);
        lv.setSaveEnabled(true);
        lv.setItemsCanFocus(true);
        lv.setTextFilterEnabled(true);
        mListView = lv;

        return mContentView;
    }
    
    //just for parent activity to refresh this fragment
    public void onRefreshUi() {
    	 //load();
    	this.getLoaderManager().getLoader(0).onContentChanged();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        //load();
        onRefreshUi();
    }
    
	@Override public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);

        mActivity = getActivity();
        mPm = mActivity.getPackageManager();
        mAppOps = (AppOpsManager)getActivity().getSystemService(Context.APP_OPS_SERVICE);
        mState = new AppOpsState(getActivity());
        for(int index = 0; index < AppOpsState2.ALL_TEMPLATES.length; index ++){
        	AppOpsState2 state = new AppOpsState2(getActivity());
        	mStates.add(state);
    	}
        mListView.setFastScrollEnabled(true);	    
        mAdapter = new PermListAdapter(getActivity());
        mListView.setAdapter(mAdapter);
	    
	    // We have a menu item to show in action bar.
	    setHasOptionsMenu(true);	
	    		
	    setListShown(false);
	    
	    // Prepare the loader.
	    getLoaderManager().initLoader(0, null, this);
	}

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        ListView l = (ListView) parent;
        PermInfo info = (PermInfo) l.getAdapter().getItem(position);
        String permName = info.getLabel();
        Intent intent = new Intent();
        intent.putExtra("tplIndex", position);
        intent.setClass(getActivity(), PermToAppsActivity.class);
        startActivity(intent);

    }

	@Override
	public Loader<List<PermInfo>> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader with no arguments, so it is simple.
	    return new PermListLoader(getActivity());
	}

	@Override
	public void onLoadFinished(Loader<List<PermInfo>> loader, List<PermInfo> data) {
	    // Set the new data in the adapter.
	    mAdapter.setDataAndNotify(data);
		
	    // The list should now be shown.
	    setListShown(true);
	}
	
	@Override
	public void onLoaderReset(Loader<List<PermInfo>> loader) {
	    // Clear the data in the adapter.
	    mAdapter.setDataAndNotify(null);
		//setApdaterData(null);
		//mAdapter.setDataAndNotify(list)
	}
	
    // View Holder used when displaying views
    static class AppViewHolder {
        ImageView mPermIcon;
        TextView mPermLabel;
        TextView mAppCount;
    }	
}