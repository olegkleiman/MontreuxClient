package com.example.montreuxclient;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;

import android.widget.GridView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.example.montreuxclient.fastcv.CornerActivity;

public class MainActivity extends BasicActivity {

	final boolean DEVELOPER_MODE = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
//		if( DEVELOPER_MODE ) {
//			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
//				.detectDiskReads()
//				.detectDiskWrites()
//				.detectNetwork()
//				.penaltyLog()
//				.build());
//
//			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
//				.detectLeakedSqlLiteObjects()
//				.detectLeakedClosableObjects()
//				.penaltyLog()
//				.build());
//		}
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN 
				| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
				WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
		
        GridView gridview = (GridView) findViewById(R.id.gridview);
        gridview.setAdapter(new ImageAdapter(this));	
		
        gridview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            	
            	Intent intent;
            	
            	switch( position) {
            		case 0:
        				intent = new Intent(MainActivity.this, ReportActivity.class);
        				startActivity(intent);
        				break;
        				
            		case 1:
        				intent = new Intent(MainActivity.this, PicActivity.class);
        				startActivity(intent);
            			break;
            			
            		case 2:
        				intent = new Intent(MainActivity.this, VideoActivity.class);
        				startActivity(intent);
            			break;
            		
            		case 3:
            			intent = new Intent(MainActivity.this, NFCActivity.class);
            			startActivity(intent);
            			break;
            			
            		case 4:
            			intent = new Intent(MainActivity.this, NFCWriteActivity.class);
            			startActivity(intent);
            			break;
            			
            		case 5:
            			intent = new Intent(MainActivity.this, BLEActivity.class);
            			startActivity(intent);
            			break;

                    case 7: // FastCV
                        intent = new Intent(MainActivity.this, CornerActivity.class);
                        startActivity(intent);
                        break;

                    case 8: // Prefs
                        intent = new Intent(MainActivity.this, PrefsActivity.class);
                        startActivity(intent);
                        break;

                    case 9: // Wi-Fi P2P
                        intent = new Intent(MainActivity.this, AllJoynActivity.class);
                        startActivity(intent);
                        break;

					case 10: // Wi-Fi
						intent = new Intent(MainActivity.this, WiFiActivity.class);
						startActivity(intent);
						break;
            	}
    
            }
        });

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
