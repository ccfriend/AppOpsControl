package com.zte.appopscontrol;

import java.io.File;
import java.util.List;

import com.zte.appopscontrol.AppOpsState2.AppOpEntry;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;


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
        private int mPermCounts;

        public AppInfo(Context context, ApplicationInfo info, int permCounts) {
        	mContext = context;
        	mPm = context.getPackageManager();
            mInfo = info;
            mApkFile = new File(info.sourceDir);
            mPermCounts = permCounts;
        }

        public ApplicationInfo getApplicationInfo() {
            return mInfo;
        }

        public String getLabel() {
            return mLabel;
        }
        
        public int getPermCounts() {
            return mPermCounts;
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
    public static class PermInfo{
        private final PackageManager mPm;
        private final Context mContext;
        private String mPermLabel;
        private Drawable mPermIcon;
        private int mAppCount;
        private int mSysAppCount;
        private List<AppOpEntry> mApps;
             
        
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

        public int getAppCount() {
            return mAppCount;
        }
        
        public int getSysAppCount() {
            return mSysAppCount;
        }
        
        public int setAppCount(int count) {
            return mAppCount = count;
        }
        
        public int setSysAppCount(int count) {
            return mSysAppCount = count;
        }
        
		public List<AppOpEntry> getmApps() {
			return mApps;
		}

		public void setmApps(List<AppOpEntry> mApps) {
			this.mApps = mApps;
		}
     
    }
}