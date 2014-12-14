/**
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.zte.appopscontrol;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;

import android.util.Log;
import android.util.SparseArray;
import com.zte.appopscontrol.R;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class AppOpsState2 {
    static final String TAG = "AppOpsState";
    static final boolean DEBUG = false;

    final Context mContext;
    final AppOpsManager mAppOps;
    final PackageManager mPm;
    final CharSequence[] mOpSummaries;
    final CharSequence[] mOpLabels;

    List<AppOpEntry> mApps;

    private SharedPreferences mPreferences;

    public AppOpsState2(Context context) {
        mContext = context;
        mAppOps = (AppOpsManager)context.getSystemService(Context.APP_OPS_SERVICE);
        mPm = context.getPackageManager();
        mOpSummaries = context.getResources().getTextArray(R.array.app_ops_summaries_cm);
        mOpLabels = context.getResources().getTextArray(R.array.app_ops_labels_cm);
        mPreferences = context.getSharedPreferences(AppOpsControlActivity.APP_OPS_SHARED_PREFERENCES_NAME,
        					Activity.MODE_PRIVATE);
    }

    public static class OpsTemplate implements Parcelable {
        public final int[] ops;
        public final boolean[] showPerms;
        public final int resId;

        public OpsTemplate(int[] _ops, boolean[] _showPerms, int _resId) {
            ops = _ops;
            showPerms = _showPerms;
            resId = _resId;
        }

        OpsTemplate(Parcel src) {
            ops = src.createIntArray();
            showPerms = src.createBooleanArray();
            resId = 0;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeIntArray(ops);
            dest.writeBooleanArray(showPerms);
            dest.writeInt(resId);
        }

        public static final Creator<OpsTemplate> CREATOR = new Creator<OpsTemplate>() {
            @Override public OpsTemplate createFromParcel(Parcel source) {
                return new OpsTemplate(source);
            }

            @Override public OpsTemplate[] newArray(int size) {
                return new OpsTemplate[size];
            }
        };
    }

    public static final OpsTemplate NOTIFICATION_TEMPLATE = new OpsTemplate(
    		new int[] { AppOpsManager.OP_ACCESS_NOTIFICATIONS,
    				AppOpsManager.OP_POST_NOTIFICATION},
            new boolean[] { true,
    				true},
    		R.string.PUSH_NOTIFICATIONS
    		);
    
    public static final OpsTemplate CALL_PHONE_TEMPLATE = new OpsTemplate(
    		new int[] { AppOpsManager.OP_CALL_PHONE},
            new boolean[] { true}	,
            R.string.MAKE_CALL
    		);
    
    public static final OpsTemplate SEND_MESSAGE_TEMPLATE = new OpsTemplate(
    		new int[] {AppOpsManager.OP_RECEIVE_SMS,
                    AppOpsManager.OP_RECEIVE_EMERGECY_SMS,
                    AppOpsManager.OP_RECEIVE_MMS,
                    AppOpsManager.OP_RECEIVE_WAP_PUSH,
                    AppOpsManager.OP_WRITE_SMS,
                    AppOpsManager.OP_WRITE_MMS,
                    AppOpsManager.OP_SEND_SMS,
                    AppOpsManager.OP_SEND_MMS,
                    AppOpsManager.OP_WRITE_ICC_SMS },
            new boolean[] {true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true },
            R.string.SEND_MSG
    		);
    
    public static final OpsTemplate CONTACT_TEMPLATE = new OpsTemplate(
    		new int[] { AppOpsManager.OP_READ_CONTACTS,
    				AppOpsManager.OP_WRITE_CONTACTS},
            new boolean[] { true,
    				true},
    		R.string.ACCESS_CONTACT
    		);
    
    public static final OpsTemplate MESSAGE_TEMPLATE = new OpsTemplate(
    		new int[] { AppOpsManager.OP_READ_SMS,
    				AppOpsManager.OP_READ_MMS,
    				AppOpsManager.OP_READ_ICC_SMS},
            new boolean[] { true,
    				true,
    				true},
    		R.string.ACCESS_MSG
    		);
    
    public static final OpsTemplate CALL_LOG_TEMPLATE = new OpsTemplate(
    		new int[] { AppOpsManager.OP_READ_CALL_LOG,
    				AppOpsManager.OP_WRITE_CALL_LOG},
            new boolean[] { true,
    				true},
    		R.string.ACCESS_CALLLOG
    		);
    
    public static final OpsTemplate LOCATION_TEMPLATE = new OpsTemplate(
            new int[] { AppOpsManager.OP_COARSE_LOCATION,
                    AppOpsManager.OP_FINE_LOCATION,
                    AppOpsManager.OP_GPS,
                    AppOpsManager.OP_WIFI_SCAN,
                    AppOpsManager.OP_NEIGHBORING_CELLS,
                    AppOpsManager.OP_MONITOR_LOCATION,
                    AppOpsManager.OP_MONITOR_HIGH_POWER_LOCATION},
            new boolean[] { true,
                    true,
                    false,
                    false,
                    false,
                    false,
                    false},
            R.string.ACCESS_LOCATION
            );

/*    public static final OpsTemplate DEVICE_INFO_TEMPLATE = new OpsTemplate(
    		new int[] { AppOpsManager.READ_PHONE_STATE},
            new boolean[] { true}	
    		);*/
    
    public static final OpsTemplate CAMERA_TEMPLATE = new OpsTemplate(
    		new int[] { AppOpsManager.OP_CAMERA},
            new boolean[] { true},
            R.string.ACCESS_CAMERA
    		);
    
    public static final OpsTemplate RECORD_TEMPLATE = new OpsTemplate(
    		new int[] { AppOpsManager.OP_RECORD_AUDIO},
            new boolean[] { true},
            R.string.RECORD_AUDIO
    		);
    
    public static final OpsTemplate MOBILE_TEMPLATE = new OpsTemplate(
    		new int[] { AppOpsManager.OP_DATA_CONNECT_CHANGE},
            new boolean[] { true},
            R.string.CHANGE_NETWORK
    		);
    
    public static final OpsTemplate WIFI_TEMPLATE = new OpsTemplate(
    		new int[] { AppOpsManager.OP_WIFI_CHANGE},
            new boolean[] { true},
            R.string.CHANGE_WIFI
    		);
    
    public static final OpsTemplate BLUETOOTH_TEMPLATE = new OpsTemplate(
    		new int[] { AppOpsManager.OP_BLUETOOTH_CHANGE},
            new boolean[] { true},
            R.string.CHANGE_BLUETOOTH
    		);
    
    public static final OpsTemplate SETTINGS_TEMPLATE = new OpsTemplate(
    		new int[] { AppOpsManager.OP_WRITE_SETTINGS},
            new boolean[] { true},
            R.string.CHANGE_SETTINGS
    		);
    
    public static final OpsTemplate BOOTUP_TEMPLATE = new OpsTemplate(
            new int[] { AppOpsManager.OP_BOOT_COMPLETED },
            new boolean[] { true, },
            R.string.BOOT_UP
            );

    public static final OpsTemplate[] ALL_TEMPLATES = new OpsTemplate[] {
    	NOTIFICATION_TEMPLATE, CALL_PHONE_TEMPLATE,
    	SEND_MESSAGE_TEMPLATE, CONTACT_TEMPLATE, MESSAGE_TEMPLATE, 
    	CALL_LOG_TEMPLATE, LOCATION_TEMPLATE, CAMERA_TEMPLATE,
    	RECORD_TEMPLATE, MOBILE_TEMPLATE, WIFI_TEMPLATE,
        BLUETOOTH_TEMPLATE, SETTINGS_TEMPLATE, BOOTUP_TEMPLATE
    };

    public static final int[] TEMPLATES_NAME = {
    	R.string.CHANGE_SETTINGS,
    	R.string.PUSH_NOTIFICATIONS,
    	R.string.MAKE_CALL,
    	R.string.SEND_MSG,
    	R.string.ACCESS_CONTACT,
    	R.string.ACCESS_MSG,
    	R.string.ACCESS_CALLLOG,
    	R.string.ACCESS_LOCATION,
    	//R.string.ACCESS_DEVICEINFO,
    	R.string.ACCESS_CAMERA,
    	//R.string.RECORD_CALL
    	R.string.RECORD_AUDIO,
    	R.string.CHANGE_NETWORK,
    	R.string.CHANGE_WIFI,
    	R.string.CHANGE_BLUETOOTH,
    	R.string.CHANGE_SETTINGS,
    	R.string.BOOT_UP//,
    	//R.string.ACCESS_PACKAGES
    };
    
    public static final int[] TEMPLATES_ICON = {
    	R.drawable.ic_show_notification,
    	R.drawable.ic_outcall,
    	R.drawable.ic_send_sms,
    	R.drawable.ic_read_contact,
    	R.drawable.ic_read_sms,
    	R.drawable.ic_read_calllogs,
    	R.drawable.ic_location,
    	//R.drawable.ic_read_phonestate,
    	R.drawable.ic_open_camera,
    	//R.drawable.ic_calling_record
    	R.drawable.ic_record,
    	R.drawable.ic_network_mobile,
    	R.drawable.ic_network_wifi,
    	R.drawable.ic_bluetooth,
    	R.drawable.ic_optimize_uninstall,
    	R.drawable.ic_optimize_autostart//,
    	//R.drawable.ACCESS_PACKAGES
    };
    
    /**
     * This class holds the per-item data in our Loader.
     */
    public static class AppEntry {
        private final AppOpsState2 mState;
        private final ApplicationInfo mInfo;
        private final File mApkFile;
        private final SparseArray<AppOpsManager.OpEntry> mOps
                = new SparseArray<AppOpsManager.OpEntry>();
        private final SparseArray<AppOpEntry> mOpSwitches
                = new SparseArray<AppOpEntry>();
        private String mLabel;
        private Drawable mIcon;
        private boolean mMounted;

        public AppEntry(AppOpsState2 state, ApplicationInfo info) {
            mState = state;
            mInfo = info;
            mApkFile = new File(info.sourceDir);
        }

        public void addOp(AppOpEntry entry, AppOpsManager.OpEntry op) {
            mOps.put(op.getOp(), op);
            mOpSwitches.put(AppOpsManager.opToSwitch(op.getOp()), entry);
        }

        public boolean hasOp(int op) {
            return mOps.indexOfKey(op) >= 0;
        }

        public AppOpEntry getOpSwitch(int op) {
            return mOpSwitches.get(AppOpsManager.opToSwitch(op));
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
                    mIcon = mInfo.loadIcon(mState.mPm);
                    return mIcon;
                } else {
                    mMounted = false;
                }
            } else if (!mMounted) {
                // If the app wasn't mounted but is now mounted, reload
                // its icon.
                if (mApkFile.exists()) {
                    mMounted = true;
                    mIcon = mInfo.loadIcon(mState.mPm);
                    return mIcon;
                }
            } else {
                return mIcon;
            }

            return mState.mContext.getResources().getDrawable(
                    android.R.drawable.sym_def_app_icon);
        }

        @Override public String toString() {
            return mLabel;
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
    }

    /**
     * This class holds the per-item data in our Loader.
     */
    public static class AppOpEntry {
        private final AppOpsManager.PackageOps mPkgOps;
        private final ArrayList<AppOpsManager.OpEntry> mOps
                = new ArrayList<AppOpsManager.OpEntry>();
        private final ArrayList<AppOpsManager.OpEntry> mSwitchOps
                = new ArrayList<AppOpsManager.OpEntry>();
        private final AppEntry mApp;
        private final int mSwitchOrder;

        public AppOpEntry(AppOpsManager.PackageOps pkg, AppOpsManager.OpEntry op, AppEntry app,
                int switchOrder) {
            mPkgOps = pkg;
            mApp = app;
            mSwitchOrder = switchOrder;
            mApp.addOp(this, op);
            mOps.add(op);
            mSwitchOps.add(op);
        }

        private static void addOp(ArrayList<AppOpsManager.OpEntry> list, AppOpsManager.OpEntry op) {
            for (int i=0; i<list.size(); i++) {
                AppOpsManager.OpEntry pos = list.get(i);
                if (pos.isRunning() != op.isRunning()) {
                    if (op.isRunning()) {
                        list.add(i, op);
                        return;
                    }
                    continue;
                }
                if (pos.getTime() < op.getTime()) {
                    list.add(i, op);
                    return;
                }
            }
            list.add(op);
        }

        public void addOp(AppOpsManager.OpEntry op) {
            mApp.addOp(this, op);
            addOp(mOps, op);
            if (mApp.getOpSwitch(AppOpsManager.opToSwitch(op.getOp())) == null) {
                addOp(mSwitchOps, op);
            }
        }

        public AppEntry getAppEntry() {
            return mApp;
        }

        public int getSwitchOrder() {
            return mSwitchOrder;
        }

        public AppOpsManager.PackageOps getPackageOps() {
            return mPkgOps;
        }

        public int getNumOpEntry() {
            return mOps.size();
        }

        public AppOpsManager.OpEntry getOpEntry(int pos) {
            return mOps.get(pos);
        }

        private CharSequence getCombinedText(ArrayList<AppOpsManager.OpEntry> ops,
                CharSequence[] items, Resources res, boolean withTerseCounts) {
            StringBuilder builder = new StringBuilder();
            for (int i=0; i<ops.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                AppOpsManager.OpEntry op = ops.get(i);
                //zte test
                //int count = op.getAllowedCount() + op.getIgnoredCount();
                int count = 1111;
                		
                if (withTerseCounts && count > 0) {
                    String quantity = res.getQuantityString(R.plurals.app_ops_count,
                            count, count);
                    builder.append(res.getString(R.string.app_ops_entry_summary,
                            items[op.getOp()], quantity));
                } else {
                    builder.append(items[op.getOp()]);
                }
            }
            return builder.toString();
        }

        public CharSequence getCountsText(Resources res) {
            AppOpsManager.OpEntry op = mOps.get(0);
            //zte test
            //int allowed = op.getAllowedCount();
            //int denied = op.getIgnoredCount();
            int allowed = 1111;
            int denied = 1111;

            if (allowed == 0 && denied == 0) {
                return null;
            }

            CharSequence allowedQuantity = res.getQuantityString(R.plurals.app_ops_count,
                    allowed, allowed);
            CharSequence deniedQuantity = res.getQuantityString(R.plurals.app_ops_count,
                    denied, denied);

            if (denied == 0) {
                return res.getString(R.string.app_ops_allowed_count, allowedQuantity);
            } else if (allowed == 0) {
                return res.getString(R.string.app_ops_ignored_count, deniedQuantity);
            }
            return res.getString(R.string.app_ops_both_count, allowedQuantity, deniedQuantity);
        }

        public CharSequence getSummaryText(AppOpsState2 state) {
            return getCombinedText(mOps, state.mOpSummaries, state.mContext.getResources(), true);
        }

        public CharSequence getSwitchText(AppOpsState2 state) {
            Resources res = state.mContext.getResources();
            if (mSwitchOps.size() > 0) {
                return getCombinedText(mSwitchOps, state.mOpLabels, res, false);
            } else {
                return getCombinedText(mOps, state.mOpLabels, res, false);
            }
        }

        public CharSequence getTimeText(Resources res, boolean showEmptyText) {
            if (isRunning()) {
                return res.getText(R.string.app_ops_running);
            }
            if (getTime() > 0) {
                return DateUtils.getRelativeTimeSpanString(getTime(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE);
            }
            return showEmptyText ? res.getText(R.string.app_ops_never_used) : "";
        }

        public boolean isRunning() {
            return mOps.get(0).isRunning();
        }

        public long getTime() {
            return mOps.get(0).getTime();
        }

        @Override public String toString() {
            return mApp.getLabel();
        }
    }

    /**
     * Perform alphabetical comparison of application entry objects.
     */
    public static final Comparator<AppOpEntry> APP_OP_COMPARATOR = new Comparator<AppOpEntry>() {
        private final Collator sCollator = Collator.getInstance();
        @Override
        public int compare(AppOpEntry object1, AppOpEntry object2) {
            if (object1.getSwitchOrder() != object2.getSwitchOrder()) {
                return object1.getSwitchOrder() < object2.getSwitchOrder() ? -1 : 1;
            }
            if (object1.isRunning() != object2.isRunning()) {
                // Currently running ops go first.
                return object1.isRunning() ? -1 : 1;
            }
            if (object1.getTime() != object2.getTime()) {
                // More recent times go first.
                return object1.getTime() > object2.getTime() ? -1 : 1;
            }
            return sCollator.compare(object1.getAppEntry().getLabel(),
                    object2.getAppEntry().getLabel());
        }
    };

    private void addOp(List<AppOpEntry> entries, AppOpsManager.PackageOps pkgOps,
            AppEntry appEntry, AppOpsManager.OpEntry opEntry, boolean allowMerge, int switchOrder) {
        if (allowMerge && entries.size() > 0) {
            AppOpEntry last = entries.get(entries.size()-1);
            if (last.getAppEntry() == appEntry) {
                boolean lastExe = last.getTime() != 0;
                boolean entryExe = opEntry.getTime() != 0;
                if (lastExe == entryExe) {
                    if (DEBUG) Log.d(TAG, "Add op " + opEntry.getOp() + " to package "
                            + pkgOps.getPackageName() + ": append to " + last);
                    last.addOp(opEntry);
                    return;
                }
            }
        }
        
        // zte 这里可以避免同一个应用显示两次
        for(AppOpEntry entry:entries){
        	if(entry.getAppEntry().getApplicationInfo().packageName.equals(appEntry.getApplicationInfo().packageName)){
        		entry.addOp(opEntry);
        		return;
        	}
        }
        
        AppOpEntry entry = appEntry.getOpSwitch(opEntry.getOp());
        if (entry != null) {
            entry.addOp(opEntry);
            return;
        }
        entry = new AppOpEntry(pkgOps, opEntry, appEntry, switchOrder);
        if (DEBUG) Log.d(TAG, "Add op " + opEntry.getOp() + " to package "
                + pkgOps.getPackageName() + ": making new " + entry);
        entries.add(entry);
    }

    public List<AppOpEntry> buildState(OpsTemplate tpl) {
        return buildState(tpl, 0, null);
    }

    private AppEntry getAppEntry(final Context context, final HashMap<String, AppEntry> appEntries,
            final String packageName, ApplicationInfo appInfo, boolean applyFilters) {

        if (appInfo == null) {
            try {
                appInfo = mPm.getApplicationInfo(packageName,
                        PackageManager.GET_DISABLED_COMPONENTS
                        | PackageManager.GET_UNINSTALLED_PACKAGES);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Unable to find info for package " + packageName);
                return null;
            }
        }
        
        
        if( packageName!=null && packageName.equalsIgnoreCase(context.getPackageName())) {
        	// no need to build state for our package
        	return null;
        }

        if (applyFilters) {
            // Hide user apps if needed
            if (!shouldShowUserApps() &&
                    (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                return null;
            }
            // Hide system apps if needed
            if (!shouldShowSystemApps() &&
                     (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                return null;
            }            
        }

        AppEntry appEntry = appEntries.get(packageName);
        if (appEntry == null) {
            appEntry = new AppEntry(this, appInfo);
            appEntry.loadLabel(context);
            appEntries.put(packageName, appEntry);
        }
        return appEntry;
    }

    private boolean shouldShowUserApps() {
        return mPreferences.getBoolean("show_user_apps", true);
    }

    private boolean shouldShowSystemApps() {
        return mPreferences.getBoolean("show_system_apps", false);
    }

    public List<AppOpEntry> buildState(OpsTemplate tpl, int uid, String packageName) {
        final Context context = mContext;

        if(  packageName!=null && packageName.equalsIgnoreCase(context.getPackageName())) {
        	// no need to build state for our package
        	return null;
        }
        
        final HashMap<String, AppEntry> appEntries = new HashMap<String, AppEntry>();
        final List<AppOpEntry> entries = new ArrayList<AppOpEntry>();

        final ArrayList<String> perms = new ArrayList<String>();
        final ArrayList<Integer> permOps = new ArrayList<Integer>();
        final int[] opToOrder = new int[AppOpsManager._NUM_OP];
        for (int i=0; i<tpl.ops.length; i++) {
            if (tpl.showPerms[i]) {
                String perm = AppOpsManager.opToPermission(tpl.ops[i]);
                if (perm != null && !perms.contains(perm)) {
                    perms.add(perm);  
                    permOps.add(tpl.ops[i]);
                    opToOrder[tpl.ops[i]] = i;
                }
            }
        }

        // Whether to apply hide user / system app filters
        final boolean applyFilters = (packageName == null);


        
        List<AppOpsManager.PackageOps> pkgs;
        if (packageName != null) {
            pkgs = mAppOps.getOpsForPackage(uid, packageName, tpl.ops);
        } else {
            pkgs = mAppOps.getPackagesForOps(tpl.ops);
        }

        if (pkgs != null) {
            for (int i=0; i<pkgs.size(); i++) {
                AppOpsManager.PackageOps pkgOps = pkgs.get(i);
                AppEntry appEntry = getAppEntry(context, appEntries, pkgOps.getPackageName(), null,
                        applyFilters);
                if (appEntry == null) {
                    continue;
                }
                for (int j=0; j<pkgOps.getOps().size(); j++) {
                    AppOpsManager.OpEntry opEntry = pkgOps.getOps().get(j);
                    addOp(entries, pkgOps, appEntry, opEntry, packageName == null,
                            packageName == null ? 0 : opToOrder[opEntry.getOp()]);
                }
            }
        }

        List<PackageInfo> apps;
        if (packageName != null) {
            apps = new ArrayList<PackageInfo>();
            try {
                PackageInfo pi = mPm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
                apps.add(pi);
            } catch (NameNotFoundException e) {
            }
        } else {
            String[] permsArray = new String[perms.size()];
            perms.toArray(permsArray);
            apps = mPm.getPackagesHoldingPermissions(permsArray, 0);
        }
        for (int i=0; i<apps.size(); i++) {
            PackageInfo appInfo = apps.get(i);
            AppEntry appEntry = getAppEntry(context, appEntries, appInfo.packageName,
                    appInfo.applicationInfo, applyFilters);
            if (appEntry == null) {
                continue;
            }
            List<AppOpsManager.OpEntry> dummyOps = null;
            AppOpsManager.PackageOps pkgOps = null;
            if (appInfo.requestedPermissions != null) {
                for (int j=0; j<appInfo.requestedPermissions.length; j++) {
                    if (appInfo.requestedPermissionsFlags != null) {
                        if ((appInfo.requestedPermissionsFlags[j]
                                & PackageInfo.REQUESTED_PERMISSION_GRANTED) == 0) {
                            if (DEBUG) Log.d(TAG, "Pkg " + appInfo.packageName + " perm "
                                    + appInfo.requestedPermissions[j] + " not granted; skipping");
                            continue;
                        }
                    }
                    if (DEBUG) Log.d(TAG, "Pkg " + appInfo.packageName + ": requested perm "
                            + appInfo.requestedPermissions[j]);
                    for (int k=0; k<perms.size(); k++) {
                        if (!perms.get(k).equals(appInfo.requestedPermissions[j])) {
                            continue;
                        }
                        if (DEBUG) Log.d(TAG, "Pkg " + appInfo.packageName + " perm " + perms.get(k)
                                + " has op " + permOps.get(k) + ": " + appEntry.hasOp(permOps.get(k)));
                        if (appEntry.hasOp(permOps.get(k))) {
                            continue;
                        }
                        if (dummyOps == null) {
                            dummyOps = new ArrayList<AppOpsManager.OpEntry>();
                            pkgOps = new AppOpsManager.PackageOps(
                                    appInfo.packageName, appInfo.applicationInfo.uid, dummyOps);

                        }
                        //zte test
                        //AppOpsManager.OpEntry opEntry = new AppOpsManager.OpEntry(
                        //        permOps.get(k), AppOpsManager.MODE_ALLOWED, 0, 0, 0, 0, 0);
                        AppOpsManager.OpEntry opEntry = new AppOpsManager.OpEntry(
                                permOps.get(k), AppOpsManager.MODE_ALLOWED, 0,0,0);
                        dummyOps.add(opEntry);
                        addOp(entries, pkgOps, appEntry, opEntry, packageName == null,
                                packageName == null ? 0 : opToOrder[opEntry.getOp()]);
                    }
                }
            }
        }

        // Sort the list.
        Collections.sort(entries, APP_OP_COMPARATOR);
        mApps = entries;
        // Done!
        return entries;
    }
}
