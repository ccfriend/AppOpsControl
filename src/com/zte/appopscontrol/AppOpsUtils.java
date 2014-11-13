package com.zte.appopscontrol;

import java.io.File;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;


public class AppOpsUtils {
	
	
    /**
     * This class holds the per-item data in our Loader.
     */
    public static class AppInfo {
        private final PackageManager mPm;
        private final Context mContext;
        private final ApplicationInfo mInfo;
        private final File mApkFile;
        private String mLabel;
        private Drawable mIcon;
        private boolean mMounted;

        public AppInfo(Context context, ApplicationInfo info) {
        	mContext = context;
        	mPm = context.getPackageManager();
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
                    mIcon = mInfo.loadIcon(mPm);
                    return mIcon;
                } else {
                    mMounted = false;
                }
            } else if (!mMounted) {
                // If the app wasn't mounted but is now mounted, reload
                // its icon.
                if (mApkFile.exists()) {
                    mMounted = true;
                    mIcon = mInfo.loadIcon(mPm);
                    return mIcon;
                }
            } else {
                return mIcon;
            }

            return mContext.getResources().getDrawable(
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
    public static class PermInfo {
        private final PackageManager mPm;
        private final Context mContext;
        private String mPermLabel;
        private Drawable mPermIcon;
        private String mAppCount;
             
        
        public PermInfo(Context context,String label, Drawable icon) {
        	mContext = context;
        	mPm = context.getPackageManager();
        	mPermLabel=label;
        	mPermIcon = icon;
        }
        
        public String getLabel() {
            return mPermLabel;
        }

        public Drawable getIcon() {
            return mPermIcon;
        }

        public String getAppCount() {
            return mAppCount;
        }

        public String setAppCount(String count) {
            return mAppCount=count;
        }
     
    }
}