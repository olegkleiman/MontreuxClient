package com.example.montreuxclient;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class ITLVSettingsActivity extends Activity 
									implements OnItemSelectedListener{

	private static final String LOG_TAG = "montreux";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_itlvsettings);
		
        final Spinner boothSpiner = (Spinner)findViewById(R.id.boothSpiner);
        boothSpiner.setOnItemSelectedListener(this);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
        		R.array.booths, android.R.layout.simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        boothSpiner.setAdapter(adapter);
		
		try{
	        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			long boothID = sharedPrefs.getLong("boothid", 1);
			if( boothID == 0) // executed for first time w/o shared settings
				boothID = 1;
			boothSpiner.setSelection((int)boothID-1);
			
		} catch(Exception ex) {
			Log.i(LOG_TAG, ex.getMessage());
		}
        
		Button btnUpdate = (Button)findViewById(R.id.btnSettingsUpdate);
		btnUpdate.setOnClickListener(new View.OnClickListener(){
		    public void onClick(View v)
		    {
		    	long itemID = boothSpiner.getSelectedItemId();
		    	
				SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				SharedPreferences.Editor editor = sharedPrefs.edit();
				editor.putLong("boothid", itemID+1);
				editor.commit();
				
				finish();
		    } 
		});

	}

	@Override
	public void onItemSelected(AdapterView<?> parent, 
							   View view, 
							   int pos,
							   long id) {

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putLong("boothid", id);
		editor.commit();

		TextView txtBoothID = (TextView)findViewById(R.id.txtBoostID);
		txtBoothID.setText(Integer.toString(pos+1));
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// TODO Auto-generated method stub
		
	}
}
