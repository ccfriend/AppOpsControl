package com.zte.appopscontrol.privacyguard;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.zte.appopscontrol.R;

public class PrivacyGuardManagerActivity extends FragmentActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.privacy_guard_manager_activity);
		
		getFragmentManager().beginTransaction().add(R.id.privacy_manager_fragment_container, new PrivacyGuardManager()).commit();
	}
	
	
}