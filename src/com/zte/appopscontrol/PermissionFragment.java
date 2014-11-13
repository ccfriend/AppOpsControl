package com.zte.appopscontrol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.zte.appopscontrol.AppFragment.AppListAdapter;
import com.zte.appopscontrol.AppOpsUtils.PermInfo;
import com.zte.appopscontrol.applications.AppOpsState;
import com.zte.appopscontrol.privacyguard.PrivacyGuardManager;


import android.app.Activity;
import android.app.AppOpsManager;
import android.app.Fragment;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

public class PermissionFragment extends Fragment implements OnItemClickListener {

	private String mCurrentPkgName;
    private ListView mListView;
    private View mContentView;    
    private PackageManager mPm; 
    private Activity mActivity;
    private AppOpsManager mAppOps;
    private AppOpsState mState;

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
            appHolder.mAppCount.setText(permInfo.getAppCount());	        
	        return convertView;
	    }
	}

    /**
     * An asynchronous task to load .
     */
    private class PermListLoader extends AsyncTask<Void, Void,List<PermInfo>> {
        @Override
        protected List<PermInfo> doInBackground(Void... params) {

            // get the origin data
        	CharSequence[] originPermArrary = getActivity().getResources().getTextArray(R.array.app_ops_labels);

            // encrypt the data to get view quickly
            List<PermInfo> permList = new ArrayList<PermInfo>();
            Drawable drawIcon = getActivity().getResources().getDrawable(
                    android.R.drawable.sym_def_app_icon);
            for (int index=0; index < originPermArrary.length; index++) {
            	String str = originPermArrary[index].toString();
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
                int count = 1111;
                String countUnit = count > 1 ? getString(R.string.app_count_unit_plural)
                        : getString(R.string.app_count_unit_single);
                String appCount = String.valueOf(count) + " " + countUnit;
                
                permItem.setAppCount(appCount);
                
                permList.add(permItem);	            	
            }
            
            return permList;
        }

        @Override
        protected void onProgressUpdate(Void... progress) {
        	mAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(List<PermInfo> list) {	            
        	mAdapter.setDataAndNotify(list);
     	
        }	        
    }


    private void load() {
        mAsyncTask = (PermListLoader)new PermListLoader().execute();
    }
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.perm_fragment, null);
        TextView emptyView = (TextView)mContentView.findViewById(com.android.internal.R.id.empty);
        emptyView.setText("No Permissions!");
        ListView lv = (ListView) mContentView.findViewById(android.R.id.list);
        if (emptyView != null) {
            lv.setEmptyView(emptyView);
        }
        lv.setOnItemClickListener(this);
        lv.setSaveEnabled(true);
        lv.setItemsCanFocus(true);
        lv.setTextFilterEnabled(true);
        mListView = lv;

        return mContentView;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        load();
               
    }
    
	@Override public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);

        mActivity = getActivity();
        mPm = mActivity.getPackageManager();
        mAppOps = (AppOpsManager)getActivity().getSystemService(Context.APP_OPS_SERVICE);
        mState = new AppOpsState(getActivity());
        
        mListView.setFastScrollEnabled(true);	    
        mAdapter = new PermListAdapter(getActivity());
        mListView.setAdapter(mAdapter);
	    
	    // We have a menu item to show in action bar.
	    setHasOptionsMenu(true);	
	
	}

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        ListView l = (ListView) parent;
        PermInfo info = (PermInfo) l.getAdapter().getItem(position);
        String permName = info.getLabel();
        Intent intent = new Intent();
        intent.putExtra("permName", permName);
        intent.setClass(getActivity(), PermToAppsActivity.class);
        startActivity(intent);

    }
    
    // View Holder used when displaying views
    static class AppViewHolder {
        ImageView mPermIcon;
        TextView mPermLabel;
        TextView mAppCount;
    }	
}