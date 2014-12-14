package com.zte.appopscontrol.firewall;


import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.zte.appopscontrol.R;


/**
 * This demonstrates the use of action bar tabs and how they interact
 * with other action bar features.
 */
public class FireWallActivity extends Activity implements OnClickListener{
		      
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.firewall_activity);        
        final ActionBar bar = getActionBar();        
        bar.setTitle(R.string.firewall);
        
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        FireWallFragment newFragment = new FireWallFragment();
        ft.add(R.id.embedded, newFragment);
        ft.commit();

        TextView tv = (TextView) findViewById(R.id.firewall_info);
        int mCount = 1000000;
        String mCountUnit = mCount > 1 ? getString(R.string.app_count_unit_plural)
                : getString(R.string.app_count_unit_single);
        String appCountStr = String.valueOf(mCount) + mCountUnit;        

        tv.setText(appCountStr);
        
        View br = (View)findViewById(R.id.mobile);
        //br.setBackgroundColor(color);
        br.setOnClickListener(this);
        View bk = (View)findViewById(R.id.wifi);
        //bk.setBackgroundColor(color);
        bk.setOnClickListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
    
    private class BatchSettingDialogFragment extends DialogFragment {
    	private int mId;
        public BatchSettingDialogFragment(int resId) {
        	mId = resId;
		}
        
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.batch_firewall_title,
                    		getString(mId)))
                    .setItems(R.array.firewall_batch_operation, new DialogInterface.OnClickListener() {
                    	public void onClick(DialogInterface dialog, int which) {
                    		// TODO
                    		batchSettingFirewall(mId,which);
                    	}                    	
                    })
                    .setPositiveButton(null,null)
                    .setNegativeButton(R.string.cancel,null)
                    .create();
        }
    }

    private void refreshUi(String which,boolean enable) {    	
        Intent intent = new Intent();
        intent.setAction(FireWallFragment.ACTION_REFRESH_UI);
        intent.putExtra(FireWallFragment.EXTRA_FIREWALL, which);
        intent.putExtra(FireWallFragment.EXTRA_ENABLE, enable);
        sendBroadcast(intent);
    }
    
    private void batchSettingFirewall(int which, int op) {
    	switch (which) {
    	case R.string.mobile:
    		refreshUi("mobile",op==0?true:false);
    		break;
    	case R.string.wifi:
    		refreshUi("wifi",op==0?true:false);
    		break;    		
    	}
    }
    
    private void showSettingDialog(int resId) {
    	BatchSettingDialogFragment dialog = new BatchSettingDialogFragment(resId);
        dialog.show(getFragmentManager(), "batch_setting_dialog");
    }
    
	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		    case R.id.mobile:
		    {
		    	showSettingDialog(R.string.mobile);
			    break;
		    }
		    case R.id.wifi:
		    {
		    	showSettingDialog(R.string.wifi);
		    	break;
		    }
		}
	}
       
}
