package com.zte.appopscontrol;

import java.util.ArrayList;
import java.util.List;

import com.zte.appopscontrol.AppFragment.PackageIntentReceiver;
import com.zte.appopscontrol.AppOpsState2.AppOpEntry;
import com.zte.appopscontrol.AppOpsState2.OpsTemplate;
import com.zte.appopscontrol.AppOpsUtils.AppInfo;
import com.zte.appopscontrol.applications.AppOpsState;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;


public class PermToAppsActivity extends Activity {

    private static final String TAG = "PermToAppActivity";

    private LayoutInflater mInflater;
    private Context mCxt;
    private LinearLayout mOperationsSection;
    private PackageManager mPm;
	private int mTplIndex;
	private ActionBar actionBar;

    private AppOpsManager mAppOps;
    private AppOpsState2 mState;
    private List<MyPermissionRecord> mPermRecordList = new ArrayList<MyPermissionRecord>();

    private final int batchSettingId = 0;
    private String mPermLabel;
    
    private final int MODE_ALLOWED = 0;
    private final int MODE_IGNORED = 1;
    private final int MODE_ASK     = 2;
    
    private int modeToPosition(int mode) {
        switch (mode) {
        case AppOpsManager.MODE_ALLOWED:
            return MODE_ALLOWED;
        case AppOpsManager.MODE_IGNORED:
            return MODE_IGNORED;
        case AppOpsManager.MODE_ASK:
            return MODE_ASK;
        };

        return MODE_IGNORED;
    }

    private int positionToMode(int position) {
        switch (position) {
        case MODE_ALLOWED:
            return AppOpsManager.MODE_ALLOWED;
        case MODE_IGNORED:
            return AppOpsManager.MODE_IGNORED;
        case MODE_ASK:
            return AppOpsManager.MODE_ASK;
        };

        return AppOpsManager.MODE_IGNORED;
    }
    
    //load all perms & refresh list view
    private void refreshUi(int batchPos) {        
        int sysAppCount = 0;        
        mState.buildState(AppOpsState2.ALL_TEMPLATES[mTplIndex]);

        mOperationsSection.removeAllViews();
        
        //build state info from template index
        for(int i=0; i < mState.mApps.size(); i ++) {
        	final AppOpEntry appEntry = mState.mApps.get(i);        	
        	MyPermissionRecord myPerm = new MyPermissionRecord();
        	
            final int switchOp = AppOpsManager.opToSwitch(AppOpsState2.ALL_TEMPLATES[mTplIndex].ops[0]);
            //batch operation
            if(batchPos != -1) {
            	myPerm.mStatus = positionToMode(batchPos);
            } else {            	
            	myPerm.mStatus = mAppOps.checkOp(switchOp, appEntry.getPackageOps().getUid(),
            		appEntry.getPackageOps().getPackageName());
            }
            
            PackageInfo pkgInfo;
			try {
				pkgInfo = mPm.getPackageInfo(appEntry.getPackageOps().getPackageName(),
											PackageManager.GET_META_DATA);				
				myPerm.mPkgLabel = pkgInfo.applicationInfo.loadLabel(mPm).toString();
				myPerm.mAppIcon = pkgInfo.applicationInfo.loadIcon(mPm);
				myPerm.mAppVer = getString(R.string.version_text,
	                    String.valueOf(pkgInfo.versionName));
				if ( (pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0 ) {
					sysAppCount++;
				}
					
			} catch (PackageManager.NameNotFoundException e) {
				myPerm.mPkgLabel = appEntry.getPackageOps().getPackageName();
	        	myPerm.mAppIcon =  getResources().getDrawable(
	                    android.R.drawable.sym_def_app_icon);
			}
        	mPermRecordList.add(myPerm);
        	
        	// refresh all widgets
            final View convertView = mInflater.inflate(R.layout.perm_app_list_item, 
            		mOperationsSection,false);
            mOperationsSection.addView(convertView);
            ((ImageView)convertView.findViewById(R.id.app_icon))
            	.setImageDrawable(myPerm.mAppIcon);
            ((TextView) convertView.findViewById(R.id.app_name))
            	.setText(myPerm.mPkgLabel);
            
            TextView mAppVer =(TextView) convertView.findViewById(R.id.app_version);
            if(myPerm.mAppVer == null) 
            	mAppVer.setVisibility(View.INVISIBLE);
            else
            	mAppVer.setText(myPerm.mAppVer);
            
            Spinner sw = (Spinner)convertView.findViewById(R.id.spinnerWidget);                              
            sw.setSelection(modeToPosition(myPerm.mStatus));
            
            sw.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    OpsTemplate tpl = AppOpsState2.ALL_TEMPLATES[mTplIndex];
                    for(int k = 0; k < tpl.ops.length; k++) {
                        final int switchOp = AppOpsManager.opToSwitch(tpl.ops[k]);
                    	mAppOps.setMode(switchOp, appEntry.getPackageOps().getUid(),
                    			appEntry.getPackageOps().getPackageName(), positionToMode(position));
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    // Do nothing
                }
            });  
        }

        //set system or user app info
        TextView permInfoTxt = (TextView) findViewById(R.id.perm_info);
        String sysCountUnit = sysAppCount > 1 ? getString(R.string.app_count_unit_plural)
                : getString(R.string.app_count_unit_single);
        String userCountUnit = (mState.mApps.size()-sysAppCount) > 1 ? getString(R.string.app_count_unit_plural)
                : getString(R.string.app_count_unit_single);
        String appCount = getString(R.string.app_system) + " " + sysCountUnit +": " + String.valueOf(sysAppCount) 
        		 + "    " + getString(R.string.app_user)  + " "+ userCountUnit + ": " + String.valueOf(mState.mApps.size()-sysAppCount);        
        permInfoTxt.setText(appCount);     
                      
        // if app list is empty inform the user
        // else go ahead and construct the list
        if (mPermRecordList == null || mPermRecordList.isEmpty()) {
        	//mAdapter.setDataAndNotify(null);
        } else {
            //mAdapter.setDataAndNotify(mPermRecordList);
            //mListView.setFastScrollEnabled(true);
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mAppOps = (AppOpsManager)getSystemService(Context.APP_OPS_SERVICE);
        mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mCxt = getApplicationContext();
        mPm = getApplicationContext().getPackageManager();

        setContentView(R.layout.perm_apps_list_details);         
        
        //get info
        Bundle bundle = getIntent().getExtras();
        mTplIndex = bundle != null ? bundle.getInt("tplIndex") : null;        
        //...... load....
        mState = new AppOpsState2(this);
       
        //set list view      
//        ListView lv = (ListView) findViewById(android.R.id.list);
//        //lv.setOnItemClickListener(this);
//        lv.setSaveEnabled(true);
//        lv.setItemsCanFocus(true);
//        lv.setTextFilterEnabled(true);
//
//        mListView = lv;
        
        mOperationsSection = (LinearLayout) findViewById(R.id.operations_section);
        
        //set action bar
        actionBar = getActionBar();//初始化ActionBar
        actionBar.setDisplayHomeAsUpEnabled(true);//显示左边的小箭头
        int resId = AppOpsState2.ALL_TEMPLATES[mTplIndex].resId;
        mPermLabel = getResources().getString(resId);
        actionBar.setTitle(mPermLabel);        
    }
    
    @Override
    public void onResume() {
        super.onResume();

        // rebuild the list
        refreshUi(-1);

    }
 
    private void batchSettingPerms(int position) {
    	//for batch operation
    	refreshUi(position);
    }
    
    private class BatchSettingDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(mPermLabel)
                    .setItems(R.array.perm_batch_operation, new DialogInterface.OnClickListener() {
                    	public void onClick(DialogInterface dialog, int which) {

                    		batchSettingPerms(which);
                    	}                    	
                    })
                    .setPositiveButton(null,null)
                    .setNegativeButton(R.string.cancel,null)
                    .create();
        }
    }

    private void showResetDialog() {
    	BatchSettingDialogFragment dialog = new BatchSettingDialogFragment();
        dialog.show(getFragmentManager(), "batch_setting_dialog");
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {    	
        MenuItem menu1 = menu.add(0, batchSettingId, 0, getString(R.string.batch_operation));
        menu1.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);             
         
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case android.R.id.home:
    		this.finish();
    		return true;
    	case batchSettingId:
    		showResetDialog();
    		return true;
    	default:
    		return super.onOptionsItemSelected(item);
    	}    	
    }    
    class MyPermissionRecord {
        String mPkgLabel;
        String mAppVer;
        Drawable mAppIcon;
        int mStatus;
    }    

    // View Holder used when displaying views
    static class AppViewHolder {
        TextView mPkgLabel;
        TextView mAppVer;
        ImageView mAppIcon;
        Spinner mStatus;
    }    
    
}