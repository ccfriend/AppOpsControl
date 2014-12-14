package com.zte.appopscontrol;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.zte.appopscontrol.R;
import com.zte.appopscontrol.Utils;

import java.util.List;

public class AppOpsDetailsActivity extends Activity {
    static final String TAG = "AppOpsDetails";

    public static final String ARG_PACKAGE_NAME = "package";

    private AppOpsState2 mState;
    private PackageManager mPm;
    private AppOpsManager mAppOps;
    private PackageInfo mPackageInfo;
    private LayoutInflater mInflater;
    private View mRootView;
    private TextView mAppVersion;
    private LinearLayout mOperationsSection;
    private String mAppLabel;

    private final int batchSettingId = 0;

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

    // Utility method to set application label and icon.
    private void setAppLabelAndIcon(PackageInfo pkgInfo) {
        final View appSnippet = findViewById(R.id.app_snippet);
        appSnippet.setPaddingRelative(0, appSnippet.getPaddingTop(), 0, appSnippet.getPaddingBottom());

        ImageView icon = (ImageView) appSnippet.findViewById(R.id.app_icon);
        icon.setImageDrawable(mPm.getApplicationIcon(pkgInfo.applicationInfo));
        // Set application name.
        TextView label = (TextView) appSnippet.findViewById(R.id.app_name);
        label.setText(mPm.getApplicationLabel(pkgInfo.applicationInfo));
        // Version number of application
        mAppVersion = (TextView) appSnippet.findViewById(R.id.app_size);

        if (pkgInfo.versionName != null) {
            mAppVersion.setVisibility(View.VISIBLE);
            mAppVersion.setText(getString(R.string.version_text,
                    String.valueOf(pkgInfo.versionName)));
        } else {
            mAppVersion.setVisibility(View.INVISIBLE);
        }
    }

    private String retrieveAppEntry() {
        final Bundle args = getIntent().getExtras();
        String packageName = (args != null) ? args.getString(ARG_PACKAGE_NAME) : null;
        if (packageName == null) {
            Intent intent = (args == null) ?
                    this.getIntent() : (Intent) args.getParcelable("intent");
            if (intent != null) {
                packageName = intent.getData().getSchemeSpecificPart();
            }
        }
        try {
            mPackageInfo = mPm.getPackageInfo(packageName,
                    PackageManager.GET_DISABLED_COMPONENTS |
                    PackageManager.GET_UNINSTALLED_PACKAGES);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Exception when retrieving package:" + packageName, e);
            ;
        }
        
        if(mPackageInfo != null) {
        	ApplicationInfo mInfo = mPackageInfo.applicationInfo;
        	packageName = mInfo.loadLabel(mPm).toString();
        }

        return packageName;
    }

    private boolean refreshUi(int batchPos) {
        if (mPackageInfo == null) {
            return false;
        }

        setAppLabelAndIcon(mPackageInfo);

        Resources res = this.getResources();

        mOperationsSection.removeAllViews();
        String lastPermGroup = "";
        for(int index = 0; index < AppOpsState2.ALL_TEMPLATES.length; index ++){
        	final AppOpsState2.OpsTemplate tpl = AppOpsState2.ALL_TEMPLATES[index];
            List<AppOpsState2.AppOpEntry> entries = mState.buildState(tpl,
                    mPackageInfo.applicationInfo.uid, mPackageInfo.packageName);
            for (final AppOpsState2.AppOpEntry entry : entries) {
                final AppOpsManager.OpEntry firstOp = entry.getOpEntry(0);
                final View view = mInflater.inflate(R.layout.app_ops_details_item,
                        mOperationsSection, false);
                mOperationsSection.addView(view);
                String perm = AppOpsManager.opToPermission(firstOp.getOp());
                Drawable drawIcon = this.getResources().getDrawable(AppOpsState2.TEMPLATES_ICON[index]);
                ((ImageView)view.findViewById(R.id.op_icon)).setImageDrawable(drawIcon);
                ((TextView)view.findViewById(R.id.op_name)).setText(
                		tpl.resId);
                       // entry.getSwitchText(mState));
                ((TextView)view.findViewById(R.id.op_counts)).setText(
                        entry.getCountsText(res));
                ((TextView)view.findViewById(R.id.op_time)).setText(
                        entry.getTimeText(res, true));
                Spinner sw = (Spinner)view.findViewById(R.id.spinnerWidget);
                final int switchOp = AppOpsManager.opToSwitch(firstOp.getOp());
                int mode = 0;
                
                //batch operation
                if(batchPos != -1) {
                	mode = positionToMode(batchPos);
                    for(int op : tpl.ops){
                    	final int swOp = AppOpsManager.opToSwitch(op);
                        mAppOps.setMode(swOp, entry.getPackageOps().getUid(),
                                entry.getPackageOps().getPackageName(), positionToMode(batchPos));
                    }                	
                } else {
                	mode = mAppOps.checkOp(switchOp, entry.getPackageOps().getUid(),
                        entry.getPackageOps().getPackageName());                	                	
                }
                sw.setSelection(modeToPosition(mode));
                
                sw.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
                    boolean firstMode = true;

                    @Override
                    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                        if(firstMode) {
                            firstMode = false;
                            return;
                         }
                        for(int op : tpl.ops){
                        	final int switchOp = AppOpsManager.opToSwitch(op);
	                        mAppOps.setMode(switchOp, entry.getPackageOps().getUid(),
	                                entry.getPackageOps().getPackageName(), positionToMode(position));
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parentView) {
                        // Do nothing
                    }
                });
            }
        }

        return true;
    }

    private void setIntentAndFinish(boolean finish, boolean appChanged) {
        Intent intent = new Intent();
        //intent.putExtra(ManageApplications.APP_CHG, appChanged);
        //PreferenceActivity pa = (PreferenceActivity)getActivity();
        //pa.finishPreferencePanel(this, Activity.RESULT_OK, intent);
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        mState = new AppOpsState2(this);
        mPm = getPackageManager();
        mInflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mAppOps = (AppOpsManager)this.getSystemService(Context.APP_OPS_SERVICE);
        
        setContentView(R.layout.app_ops_details);
        mOperationsSection = (LinearLayout) findViewById(R.id.operations_section);
        
        mAppLabel = retrieveAppEntry();
        //set action bar
        ActionBar actionBar = getActionBar();//初始化ActionBar
        actionBar.setDisplayHomeAsUpEnabled(true);//显示左边的小箭头
        actionBar.setTitle(mAppLabel);        
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!refreshUi(-1)) {
            setIntentAndFinish(true, true);
        }
    }
    
    private void batchSettingPerms(int position) {
    	//for batch operation
    	refreshUi(position);
    }
    
    private class BatchSettingDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(mAppLabel)
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
    
}
