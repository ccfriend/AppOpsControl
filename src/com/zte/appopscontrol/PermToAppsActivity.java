package com.zte.appopscontrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;


public class PermToAppsActivity extends Activity {

    private static final String TAG = "PermToAppActivity";

    private LayoutInflater mInflater;
    private Context mCxt;

    private ListView mListView;

    private String mPermName;
    private String[] mEntries;


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Bundle bundle = getIntent().getExtras();
        mPermName = bundle != null ? bundle.getString("permName") : null;
        
        if (mPermName == null) {           
            finish();
        }

        //mEntries = getResources().getStringArray(R.array.perm_status_entry);

        mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mCxt = getApplicationContext();

        setContentView(R.layout.perm_apps_list_details);

        ListView lv = (ListView) findViewById(android.R.id.list);
        //lv.setOnItemClickListener(this);
        lv.setSaveEnabled(true);
        lv.setItemsCanFocus(true);
        lv.setTextFilterEnabled(true);
        mListView = lv;
 
        TextView permTxt = (TextView) findViewById(R.id.perm_name);
        String[] permLabelArray = getResources().getStringArray(
                R.array.app_ops_labels);
        permTxt.setText(mPermName);
    }
    
}