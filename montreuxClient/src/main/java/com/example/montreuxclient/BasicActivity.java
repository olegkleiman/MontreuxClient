package com.example.montreuxclient;

import android.app.Activity;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class BasicActivity extends Activity 
							implements LocationListener{

	String LOG_TAG = "montreux";
	WebAppInterface webAppInterface;
	
	private Location location;
	public Location getMyLocation() {
		return location;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_basic);
		
		try {
			LocationManager locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
			location = getLocation(locationManager);
			if( location == null ) {
	
				// Bound to updates to at most 3 sec. and for 
				// geographical accuracies of more that 300 meters
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 
													  3 * 1000, 
													  300,
													  this);
			}
		}
		catch(Exception ex){
			Log.e(LOG_TAG, ex.getMessage());
		}
		webAppInterface = new WebAppInterface(this, getMyLocation());
	}

	private Location getLocation(LocationManager lm) {

//		List<String> providers = lm.getProviders(true);
//			
//		Location l = null;
//		// The last provider will always be the most accurate,
//		// so start from the end
//		for (int i=providers.size()-1; i>=0; i--) { 
//			l = lm.getLastKnownLocation(providers.get(i)); 
//			if (l != null) 
//				break; 
//		}		
//		
//		return l;
		
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		String bestProvider = lm.getBestProvider(criteria, true);
		Location location = lm.getLastKnownLocation(bestProvider);
		if( location != null) {
			Log.i(LOG_TAG, "Location provided by " + bestProvider + location.getLatitude() + ";" + location.getLongitude());
		} else {
			Log.e(LOG_TAG, "Unable get last known location from provider " + bestProvider);
		}
		return location;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.basic, menu);
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

	@Override
	public void onLocationChanged(Location location) {
		
		this.location = location;
		
		if( webAppInterface.getLocation() == null )
			webAppInterface.setLocation(location);
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
	

	
	
	
	
}
